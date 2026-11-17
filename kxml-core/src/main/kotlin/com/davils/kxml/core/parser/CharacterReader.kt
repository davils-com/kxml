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
import java.util.Locale

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
private const val SPECIAL: String = "&\r]"
private const val SEMICOLON: String = ";"
private const val CDATA_END: String = "]]>"
private const val CHARACTER_PREFIX: String = "#"
private const val HEX_PREFIX: String = "#x"
private const val HEX_RADIX: Int = 16
private const val DECIMAL_RADIX: Int = 10
private const val MAXIMUM_REFERENCE: Int = 32

private val predefined: Map<String, String> = mapOf(
    "lt" to "<",
    "gt" to ">",
    "amp" to "&",
    "apos" to "'",
    "quot" to "\"",
)

internal class CharacterReader(private val cursor: Cursor, private val budget: ParseBudget) {
    private val handlers: Map<Char, (Boolean) -> String?> = mapOf(
        AMPERSAND to { _ -> reference() },
        CARRIAGE_RETURN to ::carriageReturn,
        LINE_FEED to ::whitespace,
        TAB to ::whitespace,
        BRACKET to ::bracket,
    )

    fun text(): XmlEvent.Text {
        val start = cursor.offset
        val value = StringBuilder()
        while (!cursor.isAtEnd && cursor.peek() != LESS_THAN) {
            appendNext(value, isAttribute = false, stop = LESS_THAN)
            budget.text(value.length, cursor.rangeFrom(start))
        }
        return XmlEvent.Text(value.toString(), cursor.rangeFrom(start))
    }

    fun attributeValue(): String {
        val start = cursor.offset
        val quote = quoteAt(start)
        cursor.advance()
        val value = StringBuilder()
        while (!cursor.isAtEnd && cursor.peek() != quote) {
            if (cursor.peek() == LESS_THAN) {
                failure(
                    XmlDiagnosticCode.InvalidAttribute,
                    TextRange.of(start = cursor.offset, length = 1),
                    "A value cannot hold <",
                )
            }
            appendNext(value, isAttribute = true, stop = quote)
            budget.text(value.length, cursor.rangeFrom(start))
        }
        if (cursor.isAtEnd) {
            failure(
                XmlDiagnosticCode.UnexpectedEnd,
                cursor.rangeFrom(start),
                "The value is not closed",
            )
        }
        cursor.advance()
        return value.toString()
    }

    private fun quoteAt(start: Int): Char {
        val quote = cursor.text.getOrNull(start)
        if (quote != DOUBLE_QUOTE && quote != SINGLE_QUOTE) {
            failure(XmlDiagnosticCode.InvalidAttribute, TextRange.at(start), "A value must stand in quotes")
        }
        return quote
    }

    private fun appendNext(value: StringBuilder, isAttribute: Boolean, stop: Char) {
        val handler = handlers[cursor.peek()]
        val special = handler?.invoke(isAttribute)
        value.append(special ?: plainRun(isAttribute, stop))
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
        if (!XmlCharacters.isCharacter(code)) {
            failure(
                XmlDiagnosticCode.InvalidCharacter,
                TextRange.of(start = cursor.offset, length = 1),
                "The character U+%04X is not allowed in XML".format(Locale.ROOT, code),
            )
        }
        cursor.advance(Character.charCount(code))
    }

    private fun carriageReturn(isAttribute: Boolean): String {
        cursor.advance()
        if (!cursor.isAtEnd && cursor.peek() == LINE_FEED) cursor.advance()
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

    private fun reference(): String {
        val start = cursor.offset
        val end = cursor.indexOf(SEMICOLON)
        val isTerminated = end > start + 1 && end - start <= MAXIMUM_REFERENCE
        if (!isTerminated) {
            failure(XmlDiagnosticCode.InvalidReference, TextRange.of(start, 1), "A reference must end with ;")
        }
        val body = cursor.text.substring(startIndex = start + 1, endIndex = end)
        cursor.moveTo(end + 1)
        val range = TextRange(start, end + 1)
        val character = body.takeIf { it.startsWith(CHARACTER_PREFIX) }?.let { characterOf(it, range) }
        return character ?: entityOf(body, range)
    }

    private fun entityOf(name: String, range: TextRange): String = predefined[name] ?: failure(
        XmlDiagnosticCode.UndefinedEntity,
        range,
        "The entity &$name; is not one of the five XML predefines",
    )

    private fun characterOf(body: String, range: TextRange): String {
        val isHex = body.startsWith(HEX_PREFIX)
        val radix = HEX_RADIX.takeIf { isHex } ?: DECIMAL_RADIX
        val digits = body.removePrefix(HEX_PREFIX).removePrefix(CHARACTER_PREFIX)
        val isDigits = digits.isNotEmpty() && digits.all { digit -> digit.digitToIntOrNull(radix) != null }
        val code = digits.takeIf { isDigits }?.toIntOrNull(radix)
        if (code == null || !XmlCharacters.isCharacter(code)) {
            failure(
                XmlDiagnosticCode.InvalidReference,
                range,
                "The reference &$body; names no character XML allows",
            )
        }
        return String(Character.toChars(code))
    }
}
