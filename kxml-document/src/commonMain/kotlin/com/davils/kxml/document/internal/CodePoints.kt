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
package com.davils.kxml.document.internal

private const val SUPPLEMENTARY_BASE: Int = 0x10000
private const val HIGH_SHIFT: Int = 10
private const val SURROGATE_MASK: Int = 0x3FF
private const val HIGH_FIRST: Int = 0xD800
private const val LOW_FIRST: Int = 0xDC00

internal fun CharSequence.codePointAt(index: Int): Int {
    val high = this[index]
    val following = getOrNull(index + 1)
    val low = following?.takeIf { candidate -> high.isHighSurrogate() && candidate.isLowSurrogate() }
        ?: return high.code
    return SUPPLEMENTARY_BASE + (high.code - HIGH_FIRST shl HIGH_SHIFT) + (low.code - LOW_FIRST)
}

internal fun charCountOf(codePoint: Int): Int {
    if (codePoint >= SUPPLEMENTARY_BASE) return 2
    return 1
}

internal fun StringBuilder.appendCode(codePoint: Int): StringBuilder {
    if (codePoint < SUPPLEMENTARY_BASE) return append(codePoint.toChar())
    val offset = codePoint - SUPPLEMENTARY_BASE
    append((HIGH_FIRST + (offset shr HIGH_SHIFT)).toChar())
    return append((LOW_FIRST + (offset and SURROGATE_MASK)).toChar())
}

internal fun CharSequence.codePointsOf(): IntArray {
    val codes = IntArray(codePointCountOf())
    var index = 0
    var count = 0
    while (index < length) {
        val code = codePointAt(index)
        codes[count] = code
        count += 1
        index += charCountOf(code)
    }
    return codes
}

internal fun CharSequence.codePointCountOf(): Int {
    var index = 0
    var count = 0
    while (index < length) {
        index += charCountOf(codePointAt(index))
        count += 1
    }
    return count
}
