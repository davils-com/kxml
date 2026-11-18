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

package com.davils.kxml.core.internal

private const val SUPPLEMENTARY_START: Int = 0x10000
private const val HIGH_SURROGATE_START: Int = 0xD800
private const val LOW_SURROGATE_START: Int = 0xDC00
private const val SURROGATE_BITS: Int = 10
private const val LOW_SURROGATE_MASK: Int = 0x3FF
private const val HEX_RADIX: Int = 16
private const val HEX_WIDTH: Int = 4

internal fun CharSequence.codePointAt(index: Int): Int {
    val high = this[index]
    val low = getOrNull(index + 1)
    if (low == null || !high.isHighSurrogate() || !low.isLowSurrogate()) return high.code
    return codePointOf(high, low)
}

internal fun charCountOf(codePoint: Int): Int {
    if (codePoint >= SUPPLEMENTARY_START) return 2
    return 1
}

internal fun textOf(codePoint: Int): String {
    if (codePoint < SUPPLEMENTARY_START) return codePoint.toChar().toString()
    val offset = codePoint - SUPPLEMENTARY_START
    val high = (HIGH_SURROGATE_START + (offset ushr SURROGATE_BITS)).toChar()
    val low = (LOW_SURROGATE_START + (offset and LOW_SURROGATE_MASK)).toChar()
    return charArrayOf(high, low).concatToString()
}

internal inline fun CharSequence.allCodePoints(predicate: (Int) -> Boolean): Boolean {
    var index = 0
    while (index < length) {
        val codePoint = codePointAt(index)
        if (!predicate(codePoint)) return false
        index += charCountOf(codePoint)
    }
    return true
}

internal fun unicodeNameOf(codePoint: Int): String {
    val hex = codePoint.toString(HEX_RADIX).uppercase()
    return "U+${hex.padStart(HEX_WIDTH, '0')}"
}

private fun codePointOf(high: Char, low: Char): Int {
    val upper = high.code - HIGH_SURROGATE_START shl SURROGATE_BITS
    return SUPPLEMENTARY_START + upper + (low.code - LOW_SURROGATE_START)
}
