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

package com.davils.kxml.core.dtd

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.internal.charCountOf
import com.davils.kxml.core.internal.textOf
import com.davils.kxml.core.parser.failure
import com.davils.kxml.core.syntax.XmlSyntaxKind

private const val PERCENT: Char = '%'
private const val AMPERSAND: Char = '&'
private const val SEMICOLON: Char = ';'
private const val HASH: Char = '#'
private const val HEX: Char = 'x'
private const val LESS_THAN: Char = '<'
private const val CARRIAGE_RETURN: Char = '\r'
private const val LINE_FEED: Char = '\n'
private const val DECIMAL_DIGITS: String = "0123456789"
private const val HEX_DIGITS: String = "0123456789abcdefABCDEF"
private const val HEX_RADIX: Int = 16
private const val DECIMAL_RADIX: Int = 10
private const val MOST_DIGITS: Int = 8

internal class DtdLiterals(private val lexer: DtdLexer) {
    private val cursor = lexer.cursor
    private val sink = lexer.sink

    fun entityValue(): String {
        val start = cursor.offset
        val quote = lexer.quote()
        cursor.advance()
        val value = StringBuilder()
        while (!cursor.isAtEnd && cursor.peek() != quote) appendValue(value)
        if (cursor.isAtEnd) failure(XmlDiagnosticCode.UnexpectedEnd, cursor.rangeFrom(start), "The value is not closed")
        cursor.advance()
        lexer.budget.text(value.length, cursor.rangeFrom(start))
        sink.token(kind = XmlSyntaxKind.LITERAL, start = start, end = cursor.offset)
        return value.toString()
    }

    private fun appendValue(value: StringBuilder) {
        val char = cursor.peek()
        if (char == PERCENT) {
            lexer.invalid(cursor.offset, "A parameter entity may not be referenced inside a declaration here")
        }
        if (char == AMPERSAND) return appendReference(value)
        if (!cursor.isReplacement && (char == CARRIAGE_RETURN || lexer.dialect.isLineSeparator(char))) {
            return appendLineEnd(value)
        }
        val code = cursor.codePoint()
        if (!lexer.dialect.isLiteral(code)) {
            failure(XmlDiagnosticCode.InvalidCharacter, TextRange.at(cursor.offset), "The character is not allowed")
        }
        cursor.advance(charCountOf(code))
        value.append(textOf(code))
    }

    private fun appendLineEnd(value: StringBuilder) {
        val isReturn = cursor.peek() == CARRIAGE_RETURN
        cursor.advance()
        val isPaired = isReturn && !cursor.isAtEnd && lexer.dialect.isPairedWithReturn(cursor.peek())
        if (isPaired) cursor.advance()
        value.append(LINE_FEED)
    }

    private fun appendReference(value: StringBuilder) {
        val start = cursor.offset
        cursor.advance()
        if (!cursor.isAtEnd && cursor.peek() == HASH) {
            value.append(characterReference(start))
            return
        }
        val name = lexer.scanName()
        if (cursor.isAtEnd || cursor.peek() != SEMICOLON) {
            failure(XmlDiagnosticCode.InvalidReference, cursor.rangeFrom(start), "A reference must end with ;")
        }
        cursor.advance()
        value.append("$AMPERSAND$name$SEMICOLON")
    }

    private fun characterReference(start: Int): String {
        cursor.advance()
        val isHex = !cursor.isAtEnd && cursor.peek() == HEX
        if (isHex) cursor.advance()
        val digits = StringBuilder()
        val allowed = HEX_DIGITS.takeIf { isHex } ?: DECIMAL_DIGITS
        while (!cursor.isAtEnd && cursor.peek() in allowed && digits.length <= MOST_DIGITS) {
            digits.append(cursor.peek())
            cursor.advance()
        }
        val code = digits.toString().toIntOrNull(HEX_RADIX.takeIf { isHex } ?: DECIMAL_RADIX)
        val isTerminated = !cursor.isAtEnd && cursor.peek() == SEMICOLON
        if (code == null || !isTerminated || !lexer.dialect.isReferable(code)) {
            failure(XmlDiagnosticCode.InvalidReference, cursor.rangeFrom(start), "The reference names no character")
        }
        cursor.advance()
        return textOf(code)
    }

    fun defaultValue(): String {
        val start = cursor.offset
        val quote = lexer.quote()
        cursor.advance()
        val value = StringBuilder()
        while (!cursor.isAtEnd && cursor.peek() != quote) {
            if (cursor.peek() == LESS_THAN) {
                failure(XmlDiagnosticCode.InvalidAttribute, TextRange.at(cursor.offset), "A value cannot hold <")
            }
            appendDefault(value)
        }
        if (cursor.isAtEnd) failure(XmlDiagnosticCode.UnexpectedEnd, cursor.rangeFrom(start), "The value is not closed")
        cursor.advance()
        lexer.budget.text(value.length, cursor.rangeFrom(start))
        sink.token(kind = XmlSyntaxKind.LITERAL, start = start, end = cursor.offset)
        return value.toString()
    }

    private fun appendDefault(value: StringBuilder) {
        val char = cursor.peek()
        if (char == AMPERSAND) {
            appendRawReference(value)
            return
        }
        if (char == PERCENT) {
            cursor.advance()
            value.append(char)
            return
        }
        appendValue(value)
    }

    private fun appendRawReference(value: StringBuilder) {
        val start = cursor.offset
        appendReference(StringBuilder())
        value.append(cursor.text.substring(startIndex = start, endIndex = cursor.offset))
    }
}
