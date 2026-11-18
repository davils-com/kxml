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
import com.davils.kxml.core.internal.codePointAt
import com.davils.kxml.core.name.XmlCharacters

internal class Cursor(val text: String) {
    var offset: Int = 0
        private set

    val isAtEnd: Boolean get() = offset >= text.length

    fun peek(): Char = text[offset]

    fun codePoint(): Int = text.codePointAt(offset)

    fun startsWith(prefix: String): Boolean = text.startsWith(prefix, offset)

    fun indexOf(marker: String, from: Int = offset): Int = text.indexOf(marker, from)

    fun advance(count: Int = 1) {
        offset += count
    }

    fun moveTo(target: Int) {
        offset = target
    }

    fun skipWhitespace(): Boolean {
        val start = offset
        while (!isAtEnd && XmlCharacters.isWhitespace(peek())) offset++
        return offset > start
    }

    fun rangeFrom(start: Int): TextRange = TextRange(start, offset)

    fun rest(start: Int): TextRange = TextRange(start, text.length)
}
