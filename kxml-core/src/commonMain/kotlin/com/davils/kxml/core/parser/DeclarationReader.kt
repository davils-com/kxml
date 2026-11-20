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
import com.davils.kxml.core.node.XmlDeclaration
import com.davils.kxml.core.source.XmlEncoding
import com.davils.kxml.core.syntax.XmlSyntaxKind

private const val OPEN: String = "<?xml"
private const val CLOSE: String = "?>"
private const val VERSION: String = "version"
private const val ENCODING: String = "encoding"
private const val STANDALONE: String = "standalone"
private const val EQUALS: Char = '='
private const val YES: String = "yes"
private const val NO: String = "no"
private const val INSTRUCTION_OPEN_LENGTH: Int = 2

private val versionPattern: Regex = Regex("1\\.[0-9]+")
private val encodingPattern: Regex = Regex("[A-Za-z][A-Za-z0-9._-]*")

internal class DeclarationReader(
    private val cursor: Cursor,
    private val characters: CharacterReader,
    private val sink: SyntaxSink,
) {
    fun isAhead(): Boolean {
        val next = cursor.text.getOrNull(cursor.offset + OPEN.length)
        val isSeparated = next != null && XmlCharacters.isWhitespace(next)
        return cursor.startsWith(OPEN) && isSeparated
    }

    fun read(encoding: XmlEncoding?): XmlEvent.Declaration {
        val start = cursor.offset
        sink.startNode(XmlSyntaxKind.XML_DECLARATION)
        cursor.advance(OPEN.length)
        sink.token(kind = XmlSyntaxKind.INSTRUCTION_OPEN, start = start, end = start + INSTRUCTION_OPEN_LENGTH)
        sink.token(kind = XmlSyntaxKind.NAME, start = start + INSTRUCTION_OPEN_LENGTH, end = cursor.offset)
        val version = pseudoAttribute(VERSION, start) ?: invalid(start, "The declaration needs a version")
        if (!versionPattern.matches(version)) {
            failure(
                XmlDiagnosticCode.UnsupportedVersion,
                cursor.rangeFrom(start),
                "kxml reads XML 1.x, but the document declares $version",
            )
        }
        val declared = pseudoAttribute(ENCODING, start)
        if (declared != null && !encodingPattern.matches(declared)) invalid(start, "The encoding $declared is no name")
        checkEncoding(declared, encoding, start)
        val standalone = pseudoAttribute(STANDALONE, start)
        if (standalone != null && standalone != YES && standalone != NO) invalid(start, "standalone is yes or no")
        cursor.skipWhitespace(sink)
        if (!cursor.startsWith(CLOSE)) invalid(start, "The declaration must end with $CLOSE")
        cursor.advance(CLOSE.length)
        sink.token(kind = XmlSyntaxKind.INSTRUCTION_CLOSE, start = cursor.offset - CLOSE.length, end = cursor.offset)
        sink.finishNode()
        val declaration = XmlDeclaration(version, declared, standalone?.let { value -> value == YES })
        return XmlEvent.Declaration(declaration, cursor.rangeFrom(start))
    }

    private fun pseudoAttribute(name: String, start: Int): String? {
        val mark = cursor.offset
        val hasSpace = cursor.skipWhitespace()
        val isPresent = hasSpace && cursor.startsWith(name)
        if (!isPresent) cursor.moveTo(mark)
        return name.takeIf { isPresent }?.let { valueOf(it, mark, start) }
    }

    private fun valueOf(name: String, mark: Int, start: Int): String {
        sink.token(kind = XmlSyntaxKind.WHITESPACE, start = mark, end = cursor.offset)
        sink.startNode(XmlSyntaxKind.ATTRIBUTE)
        cursor.advance(name.length)
        sink.token(kind = XmlSyntaxKind.NAME, start = cursor.offset - name.length, end = cursor.offset)
        cursor.skipWhitespace(sink)
        if (cursor.isAtEnd || cursor.peek() != EQUALS) invalid(start, "The pseudo attribute $name needs =")
        cursor.advance()
        sink.token(kind = XmlSyntaxKind.EQUALS, start = cursor.offset - 1, end = cursor.offset)
        cursor.skipWhitespace(sink)
        val value = characters.attributeValue()
        sink.finishNode()
        return value
    }

    private fun checkEncoding(declared: String?, detected: XmlEncoding?, start: Int) {
        val isCompatible = declared == null || detected == null || detected.accepts(declared)
        if (isCompatible) return
        failure(
            XmlDiagnosticCode.EncodingMismatch,
            cursor.rangeFrom(start),
            "The document declares $declared, but its bytes are $detected",
        )
    }

    private fun invalid(start: Int, message: String): Nothing =
        failure(XmlDiagnosticCode.InvalidDeclaration, TextRange(start, cursor.offset), message)
}
