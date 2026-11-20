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

import com.davils.kxml.core.internal.allCodePoints
import com.davils.kxml.core.internal.codePointAt

private const val HEX_RADIX: Int = 16
private const val COLON: Char = ':'
private const val RANGE_SEPARATOR: Char = '-'
private const val ENTRY_SEPARATOR: Char = ','
private const val CHARACTER_RANGES: String = "9,A,D,20-D7FF,E000-FFFD,10000-10FFFF"
private const val NAME_START_RANGES: String = "3A,41-5A,5F,61-7A,C0-D6,D8-F6,F8-2FF,370-37D,37F-1FFF,200C-200D," +
    "2070-218F,2C00-2FEF,3001-D7FF,F900-FDCF,FDF0-FFFD,10000-EFFFF"
private const val NAME_EXTRA_RANGES: String = "2D,2E,30-39,B7,300-36F,203F-2040"

/**
 * Tells which characters XML 1.0 allows in a document and in a name, by the productions `Char`, `S`, `NameStartChar`
 * and `NameChar` of XML 1.0 and `NCName` of Namespaces in XML 1.0, for code that reads or checks a grammar built on
 * them, such as XPath.
 *
 * Every function is pure, takes constant time for a character and time linear in the length of a text, and is safe to
 * call from any thread.
 *
 * @since 1.0.0
 */
public object XmlCharacters {
    private val characters: List<IntRange> = rangesOf(CHARACTER_RANGES)

    private val nameStarts: List<IntRange> = rangesOf(NAME_START_RANGES)

    private val nameExtras: List<IntRange> = rangesOf(NAME_EXTRA_RANGES)

    /**
     * Returns whether [code] is a character XML 1.0 allows, by the production `Char`.
     *
     * @param code the code point; a value outside the range of Unicode is no character.
     * @return `true` for a tab, a line feed, a carriage return and the code points from U+0020 to U+D7FF, from U+E000
     * to U+FFFD and from U+10000 to U+10FFFF.
     * @since 1.0.0
     */
    public fun isCharacter(code: Int): Boolean = characters.any { range -> code in range }

    /**
     * Returns whether [char] is white space by the production `S`.
     *
     * @param char the character.
     * @return `true` for a space, a tab, a line feed and a carriage return.
     * @since 1.0.0
     */
    public fun isWhitespace(char: Char): Boolean = char == ' ' || char == '\t' || char == '\n' || char == '\r'

    /**
     * Returns whether [code] may start a name, by the production `NameStartChar`; the colon is one.
     *
     * @param code the code point.
     * @return `true` if a name may start with it.
     * @since 1.0.0
     */
    public fun isNameStart(code: Int): Boolean = nameStarts.any { range -> code in range }

    /**
     * Returns whether [code] may stand in a name after its first character, by the production `NameChar`.
     *
     * @param code the code point.
     * @return `true` if a name may hold it.
     * @since 1.0.0
     */
    public fun isNameCharacter(code: Int): Boolean = isNameStart(code) || nameExtras.any { range -> code in range }

    internal fun isName(text: String): Boolean {
        if (text.isEmpty() || !isNameStart(text.codePointAt(0))) return false
        return text.allCodePoints(::isNameCharacter)
    }

    /**
     * Returns whether [text] is a name without a colon, by the production `NCName`, such as a prefix or the local part
     * of a qualified name.
     *
     * @param text the text to check, which may contain anything.
     * @return `true` if [text] is not empty, starts with a name start character and holds only name characters and no
     * colon.
     * @since 1.0.0
     */
    public fun isNcName(text: String): Boolean = COLON !in text && isName(text)

    internal fun isQualifiedName(text: String): Boolean {
        val parts = text.split(COLON)
        return parts.size <= 2 && parts.all(::isNcName)
    }

    internal fun isText(text: String): Boolean = text.allCodePoints(::isCharacter)

    internal fun isWhitespace(text: String): Boolean = text.all(::isWhitespace)

    private fun rangesOf(table: String): List<IntRange> = table.split(ENTRY_SEPARATOR).map(::rangeOf)

    private fun rangeOf(entry: String): IntRange {
        val first = entry.substringBefore(RANGE_SEPARATOR).toInt(HEX_RADIX)
        val last = entry.substringAfter(RANGE_SEPARATOR).toInt(HEX_RADIX)
        return first..last
    }
}
