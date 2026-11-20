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
import com.davils.kxml.core.dtd.AttributeExpansion
import com.davils.kxml.core.dtd.EntityContext
import com.davils.kxml.core.internal.charCountOf
import com.davils.kxml.core.internal.codePointAt
import com.davils.kxml.core.internal.textOf
import com.davils.kxml.core.internal.unicodeNameOf
import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.syntax.XmlSyntaxKind

private const val AMPERSAND: Char = '&'
private const val LESS_THAN: Char = '<'
private const val CARRIAGE_RETURN: Char = '\r'
private const val LINE_FEED: Char = '\n'
private const val TAB: Char = '\t'
private const val BRACKET: Char = ']'
private const val DOUBLE_QUOTE: Char = '"'
private const val SINGLE_QUOTE: Char = '\''
private const val SPACE: String = " "
private const val NEWLINE: String = "\n"
private const val SPECIAL: String = "&\r]\u0085\u2028"
private const val NEXT_LINE: Char = '\u0085'
private const val LINE_SEPARATOR: Char = '\u2028'
private const val SEMICOLON: String = ";"
private const val CDATA_END: String = "]]>"
private const val CHARACTER_PREFIX: String = "#"
private const val HASH: Char = '#'
private const val HEX_PREFIX: String = "#x"
private const val HEX_RADIX: Int = 16
private const val DECIMAL_RADIX: Int = 10
private const val DECIMAL_DIGITS: String = "0123456789"
private const val HEX_DIGITS: String = "0123456789abcdefABCDEF"

private val predefined: Map<String, String> = mapOf(
    "lt" to "<",
    "gt" to ">",
    "amp" to "&",
    "apos" to "'",
    "quot" to "\"",
)

internal class CharacterReader(
    private val cursor: Cursor,
    private val budget: ParseBudget,
    private val sink: SyntaxSink,
    private val dialect: Dialect,
    entities: EntityContext,
) {
    private val expansion = AttributeExpansion(entities, dialect)

    private val handlers: Map<Char, (Boolean) -> String?> = mapOf(
        CARRIAGE_RETURN to ::carriageReturn,
        LINE_FEED to ::whitespace,
        TAB to ::whitespace,
        BRACKET to ::bracket,
        NEXT_LINE to ::lineSeparator,
        LINE_SEPARATOR to ::lineSeparator,
    )

    fun text(): XmlEvent.Text {
        val start = cursor.offset
        val value = StringBuilder()
        var run = start
        while (!cursor.isAtEnd && cursor.peek() != LESS_THAN && !isExpandableReference()) {
            run = appendNext(value, Piece(isAttribute = false, stop = LESS_THAN, run = run))
            budget.text(value.length, cursor.rangeFrom(start))
        }
        sink.token(kind = XmlSyntaxKind.TEXT, start = run, end = cursor.offset)
        return XmlEvent.Text(value.toString(), cursor.rangeFrom(start))
    }

    fun attributeValue(): String {
        val start = cursor.offset
        val quote = quoteAt(start)
        sink.startNode(XmlSyntaxKind.ATTRIBUTE_VALUE)
        cursor.advance()
        sink.token(kind = XmlSyntaxKind.QUOTE, start = start, end = cursor.offset)
        val value = StringBuilder()
        var run = cursor.offset
        while (!cursor.isAtEnd && cursor.peek() != quote) {
            if (cursor.peek() == LESS_THAN) {
                failure(
                    XmlDiagnosticCode.InvalidAttribute,
                    TextRange.of(start = cursor.offset, length = 1),
                    "A value cannot hold <",
                )
            }
            run = appendNext(value, Piece(isAttribute = true, stop = quote, run = run))
            budget.text(value.length, cursor.rangeFrom(start))
        }
        if (cursor.isAtEnd) {
            failure(
                XmlDiagnosticCode.UnexpectedEnd,
                cursor.rangeFrom(start),
                "The value is not closed",
            )
        }
        sink.token(kind = XmlSyntaxKind.ATTRIBUTE_TEXT, start = run, end = cursor.offset)
        cursor.advance()
        sink.token(kind = XmlSyntaxKind.QUOTE, start = cursor.offset - 1, end = cursor.offset)
        sink.finishNode()
        return value.toString()
    }

    private fun quoteAt(start: Int): Char {
        val quote = cursor.text.getOrNull(start)
        if (quote != DOUBLE_QUOTE && quote != SINGLE_QUOTE) {
            failure(XmlDiagnosticCode.InvalidAttribute, TextRange.at(start), "A value must stand in quotes")
        }
        return quote
    }

    private fun appendNext(value: StringBuilder, piece: Piece): Int {
        if (cursor.peek() == AMPERSAND) return appendReference(value, piece)
        val handler = handlers[cursor.peek()]
        val special = handler?.invoke(piece.isAttribute)
        value.append(special ?: plainRun(piece.isAttribute, piece.stop))
        return piece.run
    }

    private fun appendReference(value: StringBuilder, piece: Piece): Int {
        val start = cursor.offset
        val runKind = XmlSyntaxKind.ATTRIBUTE_TEXT.takeIf { piece.isAttribute } ?: XmlSyntaxKind.TEXT
        sink.token(kind = runKind, start = piece.run, end = start)
        value.append(reference(piece.isAttribute))
        val isCharacter = cursor.text.startsWith(CHARACTER_PREFIX, start + 1)
        val kind = XmlSyntaxKind.CHARACTER_REFERENCE.takeIf { isCharacter } ?: XmlSyntaxKind.ENTITY_REFERENCE
        sink.token(kind = kind, start = start, end = cursor.offset)
        return cursor.offset
    }

    private fun plainRun(isAttribute: Boolean, stop: Char): String {
        val start = cursor.offset
        advanceCharacter()
        while (!cursor.isAtEnd && isPlain(cursor.peek(), isAttribute, stop)) advanceCharacter()
        return cursor.text.substring(startIndex = start, endIndex = cursor.offset)
    }

    private fun isPlain(char: Char, isAttribute: Boolean, stop: Char): Boolean {
        val isDelimiter = char == stop || char == LESS_THAN
        val isNormalized = isAttribute && (char == LINE_FEED || char == TAB)
        return !isDelimiter && !isNormalized && char !in SPECIAL
    }

    private fun advanceCharacter() {
        val code = cursor.codePoint()
        if (!dialect.isLiteral(code)) {
            failure(
                XmlDiagnosticCode.InvalidCharacter,
                TextRange.of(start = cursor.offset, length = 1),
                "The character ${unicodeNameOf(code)} is not allowed in XML",
            )
        }
        cursor.advance(charCountOf(code))
    }

    private fun carriageReturn(isAttribute: Boolean): String {
        cursor.advance()
        val isPaired = !cursor.isReplacement && !cursor.isAtEnd && dialect.isPairedWithReturn(cursor.peek())
        if (isPaired) cursor.advance()
        val kept = CARRIAGE_RETURN.toString().takeIf { cursor.isReplacement } ?: NEWLINE
        return SPACE.takeIf { isAttribute } ?: kept
    }

    private fun lineSeparator(isAttribute: Boolean): String? {
        if (cursor.isReplacement || !dialect.isLineSeparator(cursor.peek())) return null
        cursor.advance()
        return SPACE.takeIf { isAttribute } ?: NEWLINE
    }

    private fun whitespace(isAttribute: Boolean): String? = SPACE.takeIf { isAttribute }?.also { cursor.advance() }

    private fun bracket(isAttribute: Boolean): String? {
        if (!isAttribute && cursor.startsWith(CDATA_END)) {
            failure(
                XmlDiagnosticCode.CDataEndInText,
                TextRange.of(start = cursor.offset, length = CDATA_END.length),
                "Text cannot contain $CDATA_END",
            )
        }
        return null
    }

    fun isExpandableReference(): Boolean {
        val name = referencedName() ?: return false
        return name !in predefined
    }

    fun referencedName(): String? {
        val isEntity = cursor.peek() == AMPERSAND && cursor.text.getOrNull(cursor.offset + 1) != HASH
        if (!isEntity) return null
        var end = cursor.offset + 1
        while (end < cursor.text.length && XmlCharacters.isNameCharacter(cursor.text.codePointAt(end))) {
            end += charCountOf(cursor.text.codePointAt(end))
        }
        val isTerminated = cursor.text.getOrNull(end) == SEMICOLON[0]
        val name = cursor.text.substring(startIndex = cursor.offset + 1, endIndex = end)
        return name.takeIf { isTerminated && XmlCharacters.isName(it) }
    }

    private fun reference(isAttribute: Boolean): String {
        val start = cursor.offset
        cursor.advance()
        val isCharacter = !cursor.isAtEnd && cursor.peek() == HASH
        val nameStart = cursor.offset
        while (!cursor.isAtEnd && isReferenceCharacter(cursor.codePoint(), isCharacter)) {
            cursor.advance(charCountOf(cursor.codePoint()))
        }
        budget.name(cursor.offset - nameStart, cursor.rangeFrom(start))
        if (cursor.isAtEnd || cursor.peek() != SEMICOLON[0] || cursor.offset == nameStart) {
            failure(XmlDiagnosticCode.InvalidReference, cursor.rangeFrom(start), "A reference must end with ;")
        }
        val body = cursor.text.substring(startIndex = nameStart, endIndex = cursor.offset)
        cursor.advance()
        val range = cursor.rangeFrom(start)
        if (isCharacter) return characterOf(body, range)
        return entityOf(body, range, isAttribute)
    }

    private fun isReferenceCharacter(code: Int, isCharacter: Boolean): Boolean {
        val isDigitOrMark = isCharacter && (code == HASH.code || code.toChar().isLetterOrDigit())
        val isName = !isCharacter && XmlCharacters.isNameCharacter(code)
        return isDigitOrMark || isName
    }

    private fun entityOf(name: String, range: TextRange, isAttribute: Boolean): String {
        if (!XmlCharacters.isName(name)) {
            failure(XmlDiagnosticCode.InvalidReference, range, "An entity reference must name an entity")
        }
        val known = predefined[name]
        if (known != null) return known
        if (!isAttribute) failure(XmlDiagnosticCode.UndefinedEntity, range, "The entity &$name; is not declared")
        return expansion.expand(name, range, cursor.names)
    }

    private fun characterOf(body: String, range: TextRange): String {
        val isHex = body.startsWith(HEX_PREFIX)
        val radix = HEX_RADIX.takeIf { isHex } ?: DECIMAL_RADIX
        val digits = body.removePrefix(HEX_PREFIX).removePrefix(CHARACTER_PREFIX)
        val allowed = HEX_DIGITS.takeIf { isHex } ?: DECIMAL_DIGITS
        val isDigits = digits.isNotEmpty() && digits.all { digit -> digit in allowed }
        val code = digits.takeIf { isDigits }?.toIntOrNull(radix)
        if (code == null || !dialect.isReferable(code)) {
            failure(
                XmlDiagnosticCode.InvalidReference,
                range,
                "The reference &$body; names no character XML allows",
            )
        }
        return textOf(code)
    }

    private data class Piece(val isAttribute: Boolean, val stop: Char, val run: Int)
}
