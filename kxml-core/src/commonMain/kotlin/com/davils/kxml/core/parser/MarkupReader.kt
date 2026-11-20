/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kxml.core.parser

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.XmlEvent
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.syntax.XmlSyntaxKind

private const val COMMENT_OPEN: String = "<!--"
private const val COMMENT_DASHES: String = "--"
private const val COMMENT_CLOSE: String = "-->"
private const val CDATA_OPEN: String = "<![CDATA["
private const val CDATA_CLOSE: String = "]]>"
private const val INSTRUCTION_OPEN: String = "<?"
private const val INSTRUCTION_CLOSE: String = "?>"
private const val DOCTYPE_OPEN: String = "<!DOCTYPE"
private const val DOCTYPE_CLOSE: String = ">"
private const val RESERVED_TARGET: String = "xml"

private val depthChanges: Map<Char, Int> = mapOf('[' to 1, ']' to -1)
private val skippedSections: List<Pair<String, String>> = listOf(
    "\"" to "\"",
    "'" to "'",
    COMMENT_OPEN to COMMENT_CLOSE,
    INSTRUCTION_OPEN to INSTRUCTION_CLOSE,
)

internal class MarkupReader(
    private val cursor: Cursor,
    private val budget: ParseBudget,
    private val tags: TagReader,
    private val sink: SyntaxSink,
    private val dialect: Dialect,
) {
    fun comment(): XmlEvent.Comment {
        val start = cursor.offset
        val from = start + COMMENT_OPEN.length
        val dashes = cursor.indexOf(COMMENT_DASHES, from)
        if (dashes < 0) failure(XmlDiagnosticCode.UnexpectedEnd, cursor.rest(start), "The comment is not closed")
        if (!cursor.text.startsWith(COMMENT_CLOSE, dashes)) {
            failure(
                XmlDiagnosticCode.InvalidComment,
                TextRange.of(start = dashes, length = COMMENT_DASHES.length),
                "A comment cannot contain -- or end with -",
            )
        }
        val value = section(from, dashes, start)
        cursor.moveTo(dashes + COMMENT_CLOSE.length)
        sink.startNode(XmlSyntaxKind.COMMENT)
        sink.token(kind = XmlSyntaxKind.COMMENT_OPEN, start = start, end = from)
        sink.token(kind = XmlSyntaxKind.COMMENT_TEXT, start = from, end = dashes)
        sink.token(kind = XmlSyntaxKind.COMMENT_CLOSE, start = dashes, end = cursor.offset)
        sink.finishNode()
        return XmlEvent.Comment(value, cursor.rangeFrom(start))
    }

    fun cdata(): XmlEvent.CData {
        val start = cursor.offset
        val from = start + CDATA_OPEN.length
        val end = cursor.indexOf(CDATA_CLOSE, from)
        if (end < 0) {
            failure(
                XmlDiagnosticCode.UnexpectedEnd,
                cursor.rest(start),
                "The character data is not closed",
            )
        }
        val value = section(from, end, start)
        cursor.moveTo(end + CDATA_CLOSE.length)
        sink.startNode(XmlSyntaxKind.CDATA_SECTION)
        sink.token(kind = XmlSyntaxKind.CDATA_OPEN, start = start, end = from)
        sink.token(kind = XmlSyntaxKind.CDATA_TEXT, start = from, end = end)
        sink.token(kind = XmlSyntaxKind.CDATA_CLOSE, start = end, end = cursor.offset)
        sink.finishNode()
        return XmlEvent.CData(value, cursor.rangeFrom(start))
    }

    fun instruction(): XmlEvent.ProcessingInstruction {
        val start = cursor.offset
        sink.startNode(XmlSyntaxKind.PROCESSING_INSTRUCTION)
        cursor.advance(INSTRUCTION_OPEN.length)
        sink.token(kind = XmlSyntaxKind.INSTRUCTION_OPEN, start = start, end = cursor.offset)
        val target = tags.name()
        checkTarget(target, start)
        val hasSpace = cursor.skipWhitespace(sink)
        val end = cursor.indexOf(INSTRUCTION_CLOSE)
        if (end < 0) failure(XmlDiagnosticCode.UnexpectedEnd, cursor.rest(start), "The instruction is not closed")
        if (!hasSpace && end != cursor.offset) {
            failure(
                XmlDiagnosticCode.InvalidProcessingInstruction,
                TextRange.at(cursor.offset),
                "The target of an instruction needs whitespace before its data",
            )
        }
        val data = section(from = cursor.offset, to = end, start = start)
        sink.token(kind = XmlSyntaxKind.INSTRUCTION_TEXT, start = cursor.offset, end = end)
        cursor.moveTo(end + INSTRUCTION_CLOSE.length)
        sink.token(kind = XmlSyntaxKind.INSTRUCTION_CLOSE, start = end, end = cursor.offset)
        sink.finishNode()
        return XmlEvent.ProcessingInstruction(target, data, cursor.rangeFrom(start))
    }

    fun skipDoctype() {
        val start = cursor.offset
        sink.startNode(XmlSyntaxKind.DOCTYPE)
        cursor.advance(DOCTYPE_OPEN.length)
        sink.token(kind = XmlSyntaxKind.DOCTYPE_OPEN, start = start, end = cursor.offset)
        if (!cursor.skipWhitespace(sink)) {
            failure(
                XmlDiagnosticCode.UnexpectedContent,
                TextRange.at(cursor.offset),
                "A document type needs whitespace before its name",
            )
        }
        val content = cursor.offset
        var depth = 0
        while (depth > 0 || !cursor.startsWith(DOCTYPE_CLOSE)) {
            if (cursor.isAtEnd) {
                failure(XmlDiagnosticCode.UnexpectedEnd, cursor.rangeFrom(start), "The document type is not closed")
            }
            depth += skipUnit(start)
        }
        sink.token(kind = XmlSyntaxKind.DOCTYPE_TEXT, start = content, end = cursor.offset)
        cursor.advance(DOCTYPE_CLOSE.length)
        sink.token(kind = XmlSyntaxKind.GREATER_THAN, start = cursor.offset - DOCTYPE_CLOSE.length, end = cursor.offset)
        sink.finishNode()
    }

    private fun skipUnit(start: Int): Int {
        val change = depthChanges[cursor.peek()] ?: 0
        val skipped = skippedSections.firstOrNull { (opening, _) -> cursor.startsWith(opening) }
        val end = skipped?.let { (opening, closing) -> endOf(opening, closing, start) } ?: cursor.offset.inc()
        cursor.moveTo(end)
        return change
    }

    private fun endOf(opening: String, closing: String, start: Int): Int {
        val index = cursor.indexOf(closing, cursor.offset + opening.length)
        if (index < 0) {
            failure(
                XmlDiagnosticCode.UnexpectedEnd,
                cursor.rest(start),
                "The document type is not closed",
            )
        }
        return index + closing.length
    }

    private fun section(from: Int, to: Int, start: Int): String {
        budget.text(to - from, TextRange(start, to))
        return Sections.normalized(text = cursor.text, from = from, to = to, dialect = dialect)
    }

    private fun checkTarget(target: String, start: Int) {
        val range = cursor.rangeFrom(start)
        if (target == RESERVED_TARGET) {
            failure(XmlDiagnosticCode.MisplacedDeclaration, range, "The XML declaration may only open the document")
        }
        if (target.equals(RESERVED_TARGET, ignoreCase = true)) {
            failure(XmlDiagnosticCode.ReservedProcessingInstruction, range, "The target $target is reserved by XML")
        }
        if (!XmlCharacters.isNcName(target)) {
            failure(XmlDiagnosticCode.InvalidProcessingInstruction, range, "The target $target cannot hold a colon")
        }
    }
}
