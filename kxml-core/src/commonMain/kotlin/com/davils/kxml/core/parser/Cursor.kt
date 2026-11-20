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

private const val NEXT_LINE: Char = '\u0085'
private const val LINE_SEPARATOR: Char = '\u2028'

internal class Cursor(text: String, baseUri: String?) {
    private val frames = ArrayDeque<CursorFrame>()
    private var frame = CursorFrame(
        text = text,
        anchor = null,
        name = null,
        isReplacement = false,
        isExternal = false,
        baseUri = baseUri,
    )

    val text: String get() = frame.text

    val offset: Int get() = frame.offset

    val depth: Int get() = frames.size

    val isReplacement: Boolean get() = frame.isReplacement

    val isExternal: Boolean get() = frame.isExternal

    val baseUri: String? get() = frame.baseUri

    val isAtEnd: Boolean get() = frame.offset >= frame.text.length

    var isLineSeparatorSpace: Boolean = false

    val outermostAnchor: TextRange? get() = frames.getOrNull(1)?.anchor ?: frame.anchor.takeIf { frames.isNotEmpty() }

    val names: List<String> get() = frames.mapNotNull(CursorFrame::name) + listOfNotNull(frame.name)

    fun push(text: String, anchor: TextRange, name: String, isReplacement: Boolean) {
        val outer = frame
        frames.addLast(outer)
        frame = CursorFrame(
            text = text,
            anchor = anchor,
            name = name,
            isReplacement = isReplacement,
            isExternal = outer.isExternal,
            baseUri = outer.baseUri,
        )
    }

    fun pushExternal(text: String, anchor: TextRange, name: String?, baseUri: String?) {
        frames.addLast(frame)
        frame = CursorFrame(
            text = text,
            anchor = anchor,
            name = name,
            isReplacement = false,
            isExternal = true,
            baseUri = baseUri,
        )
    }

    fun pop() {
        frame = frames.removeLast()
    }

    fun peek(): Char = frame.text[frame.offset]

    fun codePoint(): Int = frame.text.codePointAt(frame.offset)

    fun startsWith(prefix: String): Boolean = frame.text.startsWith(prefix, frame.offset)

    fun indexOf(marker: String, from: Int = frame.offset): Int = frame.text.indexOf(marker, from)

    fun advance(count: Int = 1) {
        frame.moveTo(frame.offset + count)
    }

    fun moveTo(target: Int) {
        frame.moveTo(target)
    }

    fun skipWhitespace(): Boolean {
        val start = frame.offset
        while (!isAtEnd && isSpace(peek())) advance()
        return frame.offset > start
    }

    private fun isSpace(char: Char): Boolean {
        val isSeparator = isLineSeparatorSpace && !frame.isReplacement && (char == NEXT_LINE || char == LINE_SEPARATOR)
        return XmlCharacters.isWhitespace(char) || isSeparator
    }

    fun rangeFrom(start: Int): TextRange = outermostAnchor ?: TextRange(start, frame.offset)

    fun rest(start: Int): TextRange = outermostAnchor ?: TextRange(start, frame.text.length)
}
