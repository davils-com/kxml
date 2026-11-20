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

private const val BYTE_MASK: Int = 0xFF
private const val BYTE_BITS: Int = 8
private const val UNIT_SIZE: Int = 4
private const val SUPPLEMENTARY_START: Int = 0x10000
private const val LAST_CODE_POINT: Int = 0x10FFFF
private const val HIGH_SURROGATE_START: Int = 0xD800
private const val LOW_SURROGATE_START: Int = 0xDC00
private const val LAST_SURROGATE: Int = 0xDFFF
private const val SURROGATE_BITS: Int = 10
private const val LOW_SURROGATE_MASK: Int = 0x3FF

internal object Utf32 {
    fun decodedOrNull(bytes: ByteArray, isBigEndian: Boolean): String? {
        if (bytes.size % UNIT_SIZE != 0) return null
        val codePoints = IntArray(bytes.size / UNIT_SIZE) { index ->
            codePointAt(bytes = bytes, offset = index * UNIT_SIZE, isBigEndian = isBigEndian)
        }
        val isValid = codePoints.all(::isScalarValue)
        return codePoints.takeIf { isValid }?.let(::textOf)
    }

    private fun textOf(codePoints: IntArray): String {
        val text = StringBuilder(codePoints.size)
        codePoints.forEach { codePoint -> append(text, codePoint) }
        return text.toString()
    }

    private fun codePointAt(bytes: ByteArray, offset: Int, isBigEndian: Boolean): Int {
        var codePoint = 0
        repeat(UNIT_SIZE) { position ->
            val index = offset + (position.takeIf { isBigEndian } ?: UNIT_SIZE - 1 - position)
            codePoint = codePoint shl BYTE_BITS or (bytes[index].toInt() and BYTE_MASK)
        }
        return codePoint
    }

    private fun isScalarValue(codePoint: Int): Boolean {
        val isSurrogate = codePoint in HIGH_SURROGATE_START..LAST_SURROGATE
        return codePoint in 0..LAST_CODE_POINT && !isSurrogate
    }

    private fun append(text: StringBuilder, codePoint: Int) {
        if (codePoint < SUPPLEMENTARY_START) {
            text.append(codePoint.toChar())
            return
        }
        val offset = codePoint - SUPPLEMENTARY_START
        text.append((HIGH_SURROGATE_START + (offset ushr SURROGATE_BITS)).toChar())
        text.append((LOW_SURROGATE_START + (offset and LOW_SURROGATE_MASK)).toChar())
    }
}
