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
import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.parser.Cursor
import com.davils.kxml.core.parser.Dialect
import com.davils.kxml.core.parser.ParseBudget
import com.davils.kxml.core.parser.SyntaxSink
import com.davils.kxml.core.parser.failure
import com.davils.kxml.core.parser.skipWhitespace
import com.davils.kxml.core.syntax.XmlSyntaxKind

private const val DOUBLE_QUOTE: Char = '"'
private const val SINGLE_QUOTE: Char = '\''
private const val LETTERS: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val PUBLIC_ID_CHARACTERS: String = " \r\n0123456789-'()+,./:=?;!*#@\$_%$LETTERS"

internal class DtdLexer(
    val cursor: Cursor,
    val sink: SyntaxSink,
    val dialect: Dialect,
    val budget: ParseBudget,
) {
    fun isKeyword(word: String): Boolean {
        val next = cursor.text.getOrNull(cursor.offset + word.length)
        val isEnd = next == null || !XmlCharacters.isNameCharacter(next.code)
        return cursor.startsWith(word) && isEnd
    }

    fun keyword(word: String, kind: XmlSyntaxKind = XmlSyntaxKind.KEYWORD) {
        val start = cursor.offset
        if (!cursor.startsWith(word)) invalid(start, "$word was expected")
        cursor.advance(word.length)
        sink.token(kind = kind, start = start, end = cursor.offset)
    }

    fun punctuation(char: Char) {
        val start = cursor.offset
        if (cursor.isAtEnd || cursor.peek() != char) invalid(start, "$char was expected")
        cursor.advance()
        sink.token(kind = XmlSyntaxKind.PUNCTUATION, start = start, end = cursor.offset)
    }

    fun space(): Boolean = cursor.skipWhitespace(sink)

    fun requiredSpace(what: String) {
        val start = cursor.offset
        if (!space()) invalid(start, "Whitespace was expected $what")
    }

    fun close(start: Int) {
        space()
        if (cursor.isAtEnd || cursor.peek() != CLOSE) invalid(start, "The declaration must end with >")
        cursor.advance()
        sink.token(kind = XmlSyntaxKind.GREATER_THAN, start = cursor.offset - 1, end = cursor.offset)
    }

    fun scanName(): String {
        val start = cursor.offset
        if (cursor.isAtEnd || !XmlCharacters.isNameStart(cursor.codePoint())) {
            failure(XmlDiagnosticCode.InvalidName, TextRange.at(start), "A name was expected")
        }
        while (!cursor.isAtEnd && XmlCharacters.isNameCharacter(cursor.codePoint())) {
            cursor.advance(charCountOf(cursor.codePoint()))
        }
        budget.name(cursor.offset - start, cursor.rangeFrom(start))
        return cursor.text.substring(startIndex = start, endIndex = cursor.offset)
    }

    fun name(isQualified: Boolean): String {
        val start = cursor.offset
        val name = scanName()
        val isAllowed = XmlCharacters.isQualifiedName(name).takeIf { isQualified } ?: XmlCharacters.isNcName(name)
        if (!isAllowed) {
            failure(XmlDiagnosticCode.InvalidName, cursor.rangeFrom(start), "The name $name holds a colon")
        }
        sink.token(kind = XmlSyntaxKind.NAME, start = start, end = cursor.offset)
        return name
    }

    fun nameToken(): String {
        val start = cursor.offset
        while (!cursor.isAtEnd && XmlCharacters.isNameCharacter(cursor.codePoint())) {
            cursor.advance(charCountOf(cursor.codePoint()))
        }
        if (cursor.offset == start) invalid(start, "A name token was expected")
        budget.name(cursor.offset - start, cursor.rangeFrom(start))
        sink.token(kind = XmlSyntaxKind.NAME, start = start, end = cursor.offset)
        return cursor.text.substring(startIndex = start, endIndex = cursor.offset)
    }

    fun systemLiteral(): String = literal { code -> dialect.isLiteral(code) }

    fun publicLiteral(): String = literal { code ->
        code < CHARACTER_TABLE_END && code.toChar() in PUBLIC_ID_CHARACTERS
    }

    fun isQuoteAhead(): Boolean {
        val next = cursor.text.getOrNull(cursor.offset)
        return next == DOUBLE_QUOTE || next == SINGLE_QUOTE
    }

    fun quote(): Char {
        val quote = cursor.text.getOrNull(cursor.offset)
        if (quote != DOUBLE_QUOTE && quote != SINGLE_QUOTE) invalid(cursor.offset, "A quoted literal was expected")
        return quote
    }

    fun invalid(start: Int, message: String): Nothing =
        failure(XmlDiagnosticCode.InvalidMarkupDeclaration, cursor.rangeFrom(start), message)

    private fun literal(isAllowed: (Int) -> Boolean): String {
        val start = cursor.offset
        val quote = quote()
        cursor.advance()
        while (!cursor.isAtEnd && cursor.peek() != quote) {
            val code = cursor.codePoint()
            if (!isAllowed(code)) invalid(cursor.offset, "The literal holds a character it may not hold")
            cursor.advance(charCountOf(code))
        }
        if (cursor.isAtEnd) {
            failure(XmlDiagnosticCode.UnexpectedEnd, cursor.rangeFrom(start), "The literal is not closed")
        }
        cursor.advance()
        budget.text(cursor.offset - start, cursor.rangeFrom(start))
        sink.token(kind = XmlSyntaxKind.LITERAL, start = start, end = cursor.offset)
        val raw = cursor.text.substring(startIndex = start + 1, endIndex = cursor.offset - 1)
        return dialect.normalizedLineEnds(raw)
    }

    private companion object {
        const val CLOSE: Char = '>'
        const val CHARACTER_TABLE_END: Int = 128
    }
}
