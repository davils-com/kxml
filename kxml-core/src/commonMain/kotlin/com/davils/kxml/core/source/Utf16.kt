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
private const val UNIT_SIZE: Int = 2

internal object Utf16 {
    fun decodedOrNull(bytes: ByteArray, isBigEndian: Boolean): String? {
        if (bytes.size % UNIT_SIZE != 0) return null
        val units = CharArray(bytes.size / UNIT_SIZE) { index -> unitAt(bytes, index * UNIT_SIZE, isBigEndian) }
        return units.concatToString().takeIf { isPaired(units) }
    }

    private fun unitAt(bytes: ByteArray, offset: Int, isBigEndian: Boolean): Char {
        val first = bytes[offset].toInt() and BYTE_MASK
        val second = bytes[offset + 1].toInt() and BYTE_MASK
        val high = first.takeIf { isBigEndian } ?: second
        val low = second.takeIf { isBigEndian } ?: first
        return (high shl BYTE_BITS or low).toChar()
    }

    private fun isPaired(units: CharArray): Boolean {
        var index = 0
        var isValid = true
        while (isValid && index < units.size) {
            val unit = units[index]
            val next = units.getOrNull(index + 1)
            val isPair = unit.isHighSurrogate() && next != null && next.isLowSurrogate()
            isValid = isPair || !unit.isSurrogate()
            index += UNIT_SIZE.takeIf { isPair } ?: 1
        }
        return isValid
    }
}
