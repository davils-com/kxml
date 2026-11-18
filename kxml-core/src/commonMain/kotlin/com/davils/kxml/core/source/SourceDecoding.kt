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

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.diagnostic.XmlDiagnostic
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode

internal object SourceDecoding {
    private const val BYTE_ORDER_MARK: String = "﻿"
    private const val ZERO: Byte = 0
    private const val MARK_HIGH: Byte = 0xFE.toByte()
    private const val MARK_LOW: Byte = 0xFF.toByte()
    private const val NARROW: Int = 2

    fun decode(source: XmlSource): DecodedSource {
        val text = source.text ?: return decode(source.bytes)
        return DecodedSource(text.removePrefix(BYTE_ORDER_MARK), encoding = null, diagnostics = emptyList())
    }

    fun encodingOf(bytes: ByteArray): XmlEncoding {
        if (bytes.size < NARROW) return XmlEncoding.Utf8
        val first = bytes[0]
        val second = bytes[1]
        val isBigMark = first == MARK_HIGH && second == MARK_LOW
        val isLittleMark = first == MARK_LOW && second == MARK_HIGH
        val isBigPattern = first == ZERO && second != ZERO
        val isLittlePattern = first != ZERO && second == ZERO
        val big = XmlEncoding.Utf16BigEndian.takeIf { isBigMark || isBigPattern }
        val little = XmlEncoding.Utf16LittleEndian.takeIf { isLittleMark || isLittlePattern }
        return big ?: little ?: XmlEncoding.Utf8
    }

    private fun decode(bytes: ByteArray): DecodedSource {
        val encoding = encodingOf(bytes)
        val decoded = decodedOrNull(encoding, bytes) ?: return DecodedSource("", encoding, listOf(invalid(encoding)))
        return DecodedSource(decoded.removePrefix(BYTE_ORDER_MARK), encoding, emptyList())
    }

    private fun decodedOrNull(encoding: XmlEncoding, bytes: ByteArray): String? = when (encoding) {
        XmlEncoding.Utf8 -> utf8OrNull(bytes)
        XmlEncoding.Utf16LittleEndian -> Utf16.decodedOrNull(bytes, isBigEndian = false)
        XmlEncoding.Utf16BigEndian -> Utf16.decodedOrNull(bytes, isBigEndian = true)
    }

    private fun utf8OrNull(bytes: ByteArray): String? = try {
        bytes.decodeToString(throwOnInvalidSequence = true)
    } catch (_: CharacterCodingException) {
        null
    }

    private fun invalid(encoding: XmlEncoding): XmlDiagnostic = XmlDiagnostic(
        code = XmlDiagnosticCode.InvalidEncoding,
        range = TextRange.at(0),
        message = "The source is not valid $encoding",
    )
}
