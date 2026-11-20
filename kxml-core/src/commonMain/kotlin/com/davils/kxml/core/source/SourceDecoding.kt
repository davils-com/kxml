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

private const val BYTE_ORDER_MARK: String = "﻿"
private const val PEEK_LENGTH: Int = 1_024

internal object SourceDecoding {
    fun decode(source: XmlSource): DecodedSource {
        val text = source.text ?: return decode(source.bytes, source.charset)
        return DecodedSource(text.removePrefix(BYTE_ORDER_MARK), encoding = null, diagnostics = emptyList())
    }

    private fun decode(bytes: ByteArray, charset: String?): DecodedSource =
        when (val detection = EncodingDetection.detect(bytes)) {
            is Detection.Unknown -> refused(
                XmlDiagnosticCode.UnsupportedEncoding,
                "The bytes start like ${detection.layout}, which kxml does not read",
            )

            is Detection.Known -> decodeKnown(bytes, detection, charset)
        }

    private fun decodeKnown(bytes: ByteArray, known: Detection.Known, charset: String?): DecodedSource {
        val hinted = charset?.takeUnless { known.hasMark } ?: return decodeDetected(bytes, known)
        return decodeAs(bytes, known, hinted)
    }

    private fun decodeDetected(bytes: ByteArray, known: Detection.Known): DecodedSource {
        val encoding = encodingOf(bytes, known) ?: return refused(
            XmlDiagnosticCode.UnsupportedEncoding,
            "The document declares an encoding kxml does not read",
        )
        return decoded(bytes = bytes, markLength = known.markLength, encoding = encoding, isAuthoritative = false)
    }

    private fun encodingOf(bytes: ByteArray, known: Detection.Known): XmlEncoding? {
        val isFixed = known.hasMark || !known.encoding.isAsciiCompatible
        if (isFixed) return known.encoding
        val prolog = Latin1.decoded(bytes.copyOf(minOf(a = bytes.size, b = PEEK_LENGTH)))
        val declared = DeclaredEncoding.of(prolog)
        val resolved = declared?.let { label -> asciiCompatibleFor(label, known.encoding) }
        return resolved ?: known.encoding.takeIf { declared == null }
    }

    private fun asciiCompatibleFor(label: String, detected: XmlEncoding): XmlEncoding? {
        val resolved = XmlEncoding.forLabel(label, detected) ?: return null
        return resolved.takeIf { encoding -> encoding.isAsciiCompatible } ?: detected
    }

    private fun decodeAs(bytes: ByteArray, known: Detection.Known, charset: String): DecodedSource {
        val encoding = XmlEncoding.forLabel(charset, known.encoding) ?: return refused(
            XmlDiagnosticCode.UnsupportedEncoding,
            "The charset of the source names an encoding kxml does not read",
        )
        val decoded = decoded(bytes = bytes, markLength = 0, encoding = encoding, isAuthoritative = true)
        val declared = DeclaredEncoding.of(decoded.text)
        val isOverridden = declared != null && !encoding.accepts(declared)
        return decoded.takeUnless { isOverridden } ?: overridden(decoded, declared.orEmpty())
    }

    private fun overridden(decoded: DecodedSource, declared: String): DecodedSource {
        val warning = XmlDiagnostic(
            code = XmlDiagnosticCode.EncodingOverridden,
            range = TextRange.at(0),
            message = "The charset of the source overrides the encoding $declared the document declares",
        )
        return DecodedSource(decoded.text, decoded.encoding, decoded.diagnostics + warning)
    }

    private fun decoded(
        bytes: ByteArray,
        markLength: Int,
        encoding: XmlEncoding,
        isAuthoritative: Boolean,
    ): DecodedSource {
        val content = bytes.copyOfRange(fromIndex = markLength, toIndex = bytes.size)
        val text = textOrNull(content, encoding) ?: return refused(
            XmlDiagnosticCode.InvalidEncoding,
            "The source is not valid $encoding",
        )
        return DecodedSource(text, encoding.takeUnless { isAuthoritative }, diagnostics = emptyList())
    }

    private fun textOrNull(bytes: ByteArray, encoding: XmlEncoding): String? = when (encoding) {
        XmlEncoding.Utf8 -> utf8OrNull(bytes)
        XmlEncoding.UsAscii -> Ascii.decodedOrNull(bytes)
        XmlEncoding.Latin1 -> Latin1.decoded(bytes)
        XmlEncoding.Utf16LittleEndian -> Utf16.decodedOrNull(bytes, isBigEndian = false)
        XmlEncoding.Utf16BigEndian -> Utf16.decodedOrNull(bytes, isBigEndian = true)
        XmlEncoding.Utf32LittleEndian -> Utf32.decodedOrNull(bytes, isBigEndian = false)
        XmlEncoding.Utf32BigEndian -> Utf32.decodedOrNull(bytes, isBigEndian = true)
    }

    private fun utf8OrNull(bytes: ByteArray): String? = try {
        bytes.decodeToString(throwOnInvalidSequence = true)
    } catch (_: CharacterCodingException) {
        null
    }

    private fun refused(code: XmlDiagnosticCode, message: String): DecodedSource =
        DecodedSource("", encoding = null, diagnostics = listOf(XmlDiagnostic(code, TextRange.at(0), message)))
}
