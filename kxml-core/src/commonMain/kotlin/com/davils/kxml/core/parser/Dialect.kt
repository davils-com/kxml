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

import com.davils.kxml.core.name.XmlCharacters

private const val VERSION_11: String = "1.1"
private const val NEXT_LINE: Char = '\u0085'
private const val LINE_SEPARATOR: Char = '\u2028'
private const val CARRIAGE_RETURN: Char = '\r'
private const val LINE_FEED: Char = '\n'
private val restrictedControls: List<IntRange> = listOf(0x7F..0x84, 0x86..0x9F)
private val referableControls: IntRange = 0x1..0x1F

internal class Dialect {
    var isVersion11: Boolean = false
        private set

    fun adopt(version: String) {
        isVersion11 = version == VERSION_11
    }

    fun isLiteral(code: Int): Boolean {
        val isRestricted = isVersion11 && restrictedControls.any { range -> code in range }
        return XmlCharacters.isCharacter(code) && !isRestricted
    }

    fun isReferable(code: Int): Boolean {
        val isControl = isVersion11 && code in referableControls
        return XmlCharacters.isCharacter(code) || isControl
    }

    fun isLineSeparator(char: Char): Boolean = isVersion11 && (char == NEXT_LINE || char == LINE_SEPARATOR)

    fun isPairedWithReturn(char: Char): Boolean {
        val isNextLine = isVersion11 && char == NEXT_LINE
        return char == LINE_FEED || isNextLine
    }

    fun normalizedLineEnds(raw: String): String {
        val pairs = raw.replace(oldValue = "$CARRIAGE_RETURN$LINE_FEED", newValue = "$LINE_FEED")
        val joined = pairs.takeUnless { isVersion11 } ?: separated(pairs)
        return joined.replace(oldChar = CARRIAGE_RETURN, newChar = LINE_FEED)
    }

    private fun separated(text: String): String {
        val joined = text.replace(oldValue = "$CARRIAGE_RETURN$NEXT_LINE", newValue = "$LINE_FEED")
        val nextLines = joined.replace(oldChar = NEXT_LINE, newChar = LINE_FEED)
        return nextLines.replace(oldChar = LINE_SEPARATOR, newChar = LINE_FEED)
    }
}
