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
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction

internal object SourceDecoding {
    private const val BYTE_ORDER_MARK: String = "﻿"
    private const val ZERO: Byte = 0
    private const val MARK_HIGH: Byte = 0xFE.toByte()
    private const val MARK_LOW: Byte = 0xFF.toByte()
    private const val NARROW: Int = 2

    private val charsets: Map<XmlEncoding, Charset> = mapOf(
        XmlEncoding.Utf8 to Charsets.UTF_8,
        XmlEncoding.Utf16LittleEndian to Charsets.UTF_16LE,
        XmlEncoding.Utf16BigEndian to Charsets.UTF_16BE,
    )

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
        val charset = charsets.getValue(encoding)
        val decoder = charset.newDecoder()
        decoder.onMalformedInput(CodingErrorAction.REPORT)
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT)
        val decoded = decodedOrNull(decoder, bytes) ?: return DecodedSource("", encoding, listOf(invalid(encoding)))
        return DecodedSource(decoded.removePrefix(BYTE_ORDER_MARK), encoding, emptyList())
    }

    private fun decodedOrNull(decoder: CharsetDecoder, bytes: ByteArray): String? = try {
        decoder.decode(ByteBuffer.wrap(bytes)).toString()
    } catch (_: CharacterCodingException) {
        null
    }

    private fun invalid(encoding: XmlEncoding): XmlDiagnostic = XmlDiagnostic(
        code = XmlDiagnosticCode.InvalidEncoding,
        range = TextRange.at(0),
        message = "The source is not valid $encoding",
    )
}
