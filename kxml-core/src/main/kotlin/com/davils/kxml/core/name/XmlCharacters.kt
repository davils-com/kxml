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

package com.davils.kxml.core.name

private const val HEX_RADIX: Int = 16
private const val COLON: Char = ':'
private const val RANGE_SEPARATOR: Char = '-'
private const val ENTRY_SEPARATOR: Char = ','
private const val CHARACTER_RANGES: String = "9,A,D,20-D7FF,E000-FFFD,10000-10FFFF"
private const val NAME_START_RANGES: String = "3A,41-5A,5F,61-7A,C0-D6,D8-F6,F8-2FF,370-37D,37F-1FFF,200C-200D," +
    "2070-218F,2C00-2FEF,3001-D7FF,F900-FDCF,FDF0-FFFD,10000-EFFFF"
private const val NAME_EXTRA_RANGES: String = "2D,2E,30-39,B7,300-36F,203F-2040"

internal object XmlCharacters {
    private val characters: List<IntRange> = rangesOf(CHARACTER_RANGES)

    private val nameStarts: List<IntRange> = rangesOf(NAME_START_RANGES)

    private val nameExtras: List<IntRange> = rangesOf(NAME_EXTRA_RANGES)

    fun isCharacter(code: Int): Boolean = characters.any { range -> code in range }

    fun isWhitespace(char: Char): Boolean = char == ' ' || char == '\t' || char == '\n' || char == '\r'

    fun isNameStart(code: Int): Boolean = nameStarts.any { range -> code in range }

    fun isNameCharacter(code: Int): Boolean = isNameStart(code) || nameExtras.any { range -> code in range }

    fun isName(text: String): Boolean {
        val codes = text.codePoints().toArray()
        if (codes.isEmpty() || !isNameStart(codes.first())) return false
        return codes.all(::isNameCharacter)
    }

    fun isNcName(text: String): Boolean = COLON !in text && isName(text)

    fun isQualifiedName(text: String): Boolean {
        val parts = text.split(COLON)
        return parts.size <= 2 && parts.all(::isNcName)
    }

    fun isText(text: String): Boolean = text.codePoints().allMatch(::isCharacter)

    fun isWhitespace(text: String): Boolean = text.all(::isWhitespace)

    private fun rangesOf(table: String): List<IntRange> = table.split(ENTRY_SEPARATOR).map(::rangeOf)

    private fun rangeOf(entry: String): IntRange {
        val first = entry.substringBefore(RANGE_SEPARATOR).toInt(HEX_RADIX)
        val last = entry.substringAfter(RANGE_SEPARATOR).toInt(HEX_RADIX)
        return first..last
    }
}
