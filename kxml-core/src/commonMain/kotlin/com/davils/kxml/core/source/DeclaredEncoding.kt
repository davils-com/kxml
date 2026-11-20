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

package com.davils.kxml.core.source

private const val OPEN: String = "<?xml"
private const val CLOSE: String = "?>"
private const val NAME: String = "encoding"
private const val EQUALS: Char = '='
private const val SINGLE_QUOTE: Char = '\''
private const val DOUBLE_QUOTE: Char = '"'
private const val NAME_PUNCTUATION: String = "._-"

internal object DeclaredEncoding {
    fun of(text: String): String? {
        val end = text.indexOf(CLOSE).takeIf { index -> text.startsWith(OPEN) && index >= 0 } ?: return null
        val declaration = text.substring(startIndex = OPEN.length, endIndex = end)
        val label = nameStart(declaration)?.let { start -> valueAfter(declaration, start + NAME.length) }
        return label?.takeIf(::isEncodingName)
    }

    fun isEncodingName(label: String): Boolean {
        val isLetterFirst = label.firstOrNull()?.isAsciiLetter() == true
        return isLetterFirst && label.all(::isNameCharacter)
    }

    private fun isNameCharacter(char: Char): Boolean =
        char.isAsciiLetter() || char.isAsciiDigit() || char in NAME_PUNCTUATION

    private fun nameStart(declaration: String): Int? {
        var index = declaration.indexOf(NAME)
        while (index > 0 && !isSpace(declaration[index - 1])) index = declaration.indexOf(NAME, index + 1)
        return index.takeIf { it > 0 }
    }

    private fun valueAfter(declaration: String, from: Int): String? {
        val equals = skipSpace(declaration, from).takeIf { index -> declaration.getOrNull(index) == EQUALS }
        val open = equals?.let { index -> skipSpace(declaration, index + 1) } ?: return null
        return quotedAt(declaration, open)
    }

    private fun quotedAt(declaration: String, open: Int): String? {
        val quote = declaration.getOrNull(open)?.takeIf { char -> char == SINGLE_QUOTE || char == DOUBLE_QUOTE }
            ?: return null
        val close = declaration.indexOf(quote, open + 1).takeIf { index -> index >= 0 }
        return close?.let { end -> declaration.substring(startIndex = open + 1, endIndex = end) }
    }

    private fun skipSpace(text: String, from: Int): Int {
        var index = from
        while (index < text.length && isSpace(text[index])) index++
        return index
    }

    private fun isSpace(char: Char): Boolean = char == ' ' || char == '\t' || char == '\n' || char == '\r'

    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
}
