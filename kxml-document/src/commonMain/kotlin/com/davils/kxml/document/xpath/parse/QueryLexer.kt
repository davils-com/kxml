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
package com.davils.kxml.document.xpath.parse

import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.document.internal.charCountOf
import com.davils.kxml.document.internal.codePointAt

private const val COLON: Char = ':'
private const val NOT_AN_OPERATOR: String = "a name after an operand must be and, or, mod or div"

private val nodeTypes: Set<String> = setOf("comment", "text", "processing-instruction", "node")

private val operatorNames: Map<String, TokenKind> = mapOf(
    "and" to TokenKind.And,
    "or" to TokenKind.Or,
    "mod" to TokenKind.Mod,
    "div" to TokenKind.Div,
)

private val operandEnds: Set<TokenKind> = setOf(
    TokenKind.At,
    TokenKind.ColonColon,
    TokenKind.LeftParen,
    TokenKind.LeftBracket,
    TokenKind.Comma,
)

private val singles: Map<Char, TokenKind> = mapOf(
    '(' to TokenKind.LeftParen,
    ')' to TokenKind.RightParen,
    '[' to TokenKind.LeftBracket,
    ']' to TokenKind.RightBracket,
    '@' to TokenKind.At,
    ',' to TokenKind.Comma,
    '|' to TokenKind.Pipe,
    '+' to TokenKind.Plus,
    '-' to TokenKind.Minus,
    '=' to TokenKind.Equal,
)

internal class QueryLexer(private val text: String) {
    private var index: Int = 0
    private val tokens: MutableList<Token> = mutableListOf()
    private val pairs: Map<Char, (Int) -> Token> = mapOf(
        '.' to ::dot,
        '/' to { _ -> twice(char = '/', once = TokenKind.Slash, twice = TokenKind.DoubleSlash) },
        ':' to ::colons,
        '!' to ::notEqual,
        '<' to { _ -> twice(char = '=', once = TokenKind.Less, twice = TokenKind.LessOrEqual) },
        '>' to { _ -> twice(char = '=', once = TokenKind.Greater, twice = TokenKind.GreaterOrEqual) },
        '"' to { start -> literal(start, quote = '"') },
        '\'' to { start -> literal(start, quote = '\'') },
        '$' to ::variable,
        '*' to ::star,
    )

    fun tokens(): List<Token> {
        skipWhitespace()
        while (index < text.length) {
            tokens += next()
            skipWhitespace()
        }
        tokens += Token(kind = TokenKind.End, text = "", offset = text.length)
        return tokens
    }

    private fun next(): Token {
        val start = index
        val char = text[index]
        val single = singles[char]?.let { kind -> advance(kind = kind, length = 1) }
        return single ?: pairs[char]?.invoke(start) ?: numberOrName(start, char)
    }

    private fun numberOrName(start: Int, char: Char): Token {
        if (char in '0'..'9') return number(start)
        return name(start)
    }

    private fun advance(kind: TokenKind, length: Int): Token {
        val written = text.substring(startIndex = index, endIndex = index + length)
        val token = Token(kind = kind, text = written, offset = index)
        index += length
        return token
    }

    private fun twice(char: Char, once: TokenKind, twice: TokenKind): Token {
        val isTwice = text.getOrNull(index + 1) == char
        if (isTwice) return advance(kind = twice, length = 2)
        return advance(kind = once, length = 1)
    }

    private fun dot(start: Int): Token {
        val following = text.getOrNull(start + 1)
        val isNumber = following != null && following in '0'..'9'
        if (isNumber) return number(start)
        return twice(char = '.', once = TokenKind.Dot, twice = TokenKind.DotDot)
    }

    private fun colons(start: Int): Token {
        if (text.getOrNull(start + 1) != COLON) throw QuerySyntaxError("a single colon stands outside a name", start)
        return advance(kind = TokenKind.ColonColon, length = 2)
    }

    private fun notEqual(start: Int): Token {
        if (text.getOrNull(start + 1) != '=') throw QuerySyntaxError("an exclamation mark needs an equals sign", start)
        return advance(kind = TokenKind.NotEqual, length = 2)
    }

    private fun literal(start: Int, quote: Char): Token {
        val end = text.indexOf(quote, startIndex = start + 1)
        if (end < 0) throw QuerySyntaxError("a literal is not closed", start)
        index = end + 1
        val value = text.substring(startIndex = start + 1, endIndex = end)
        return Token(kind = TokenKind.Literal, text = value, offset = start)
    }

    private fun variable(start: Int): Token {
        index += 1
        val name = qualifiedName() ?: throw QuerySyntaxError("a variable reference needs a name", start)
        return Token(kind = TokenKind.Variable, text = name, offset = start)
    }

    private fun star(start: Int): Token {
        if (isAfterOperand()) return advance(kind = TokenKind.Multiply, length = 1)
        index += 1
        return Token(kind = TokenKind.NameTest, text = "*", offset = start)
    }

    private fun number(start: Int): Token {
        while (index < text.length && text[index] in '0'..'9') index += 1
        if (index < text.length && text[index] == '.') index += 1
        while (index < text.length && text[index] in '0'..'9') index += 1
        val digits = text.substring(startIndex = start, endIndex = index)
        return Token(kind = TokenKind.Number, text = digits, offset = start)
    }

    private fun name(start: Int): Token {
        val name = qualifiedName() ?: throw QuerySyntaxError("an unexpected character", start)
        if (isAfterOperand()) return operatorOf(name, start)
        return Token(kind = kindOf(name), text = name, offset = start)
    }

    private fun operatorOf(name: String, start: Int): Token {
        val operator = operatorNames[name] ?: throw QuerySyntaxError(NOT_AN_OPERATOR, start)
        return Token(kind = operator, text = name, offset = start)
    }

    private fun kindOf(name: String): TokenKind {
        val following = firstAfterWhitespace().takeUnless { name.endsWith(":*") }
        if (following == '(') return TokenKind.NodeType.takeIf { name in nodeTypes } ?: TokenKind.FunctionName
        val isAxis = following == COLON && text.getOrNull(nextNonWhitespace() + 1) == COLON
        return TokenKind.AxisName.takeIf { isAxis } ?: TokenKind.NameTest
    }

    private fun qualifiedName(): String? {
        val start = index
        if (!skipNcName()) return null
        val isPrefixed = text.getOrNull(index) == COLON && text.getOrNull(index + 1) != COLON
        if (isPrefixed) skipLocalPart()
        return text.substring(startIndex = start, endIndex = index)
    }

    private fun skipLocalPart() {
        val colon = index
        index += 1
        val isWildcard = text.getOrNull(index) == '*'
        if (isWildcard) index += 1
        if (!isWildcard && !skipNcName()) throw QuerySyntaxError("a prefix needs a local name", colon)
    }

    private fun skipNcName(): Boolean {
        val isStart = index < text.length && isNcNameStart(text.codePointAt(index))
        if (!isStart) return false
        index += charCountOf(text.codePointAt(index))
        while (index < text.length && isNcNameCharacter(text.codePointAt(index))) {
            index += charCountOf(text.codePointAt(index))
        }
        return true
    }

    private fun isNcNameStart(code: Int): Boolean = code != COLON.code && XmlCharacters.isNameStart(code)

    private fun isNcNameCharacter(code: Int): Boolean = code != COLON.code && XmlCharacters.isNameCharacter(code)

    private fun isAfterOperand(): Boolean {
        val previous = tokens.lastOrNull() ?: return false
        return previous.kind !in operandEnds && !previous.kind.isOperator
    }

    private fun nextNonWhitespace(): Int {
        var position = index
        while (position < text.length && XmlCharacters.isWhitespace(text[position])) position += 1
        return position
    }

    private fun firstAfterWhitespace(): Char? = text.getOrNull(nextNonWhitespace())

    private fun skipWhitespace() {
        index = nextNonWhitespace()
    }
}
