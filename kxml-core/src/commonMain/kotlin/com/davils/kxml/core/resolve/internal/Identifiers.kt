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

package com.davils.kxml.core.resolve.internal

import com.davils.kxml.core.internal.charCountOf
import com.davils.kxml.core.internal.codePointAt
import com.davils.kxml.core.internal.textOf

private const val PUBLIC_ID_URN: String = "urn:publicid:"
private const val SPACE: Char = ' '
private const val WHITESPACE: String = " \t\r\n"
private const val ESCAPED_ASCII: String = "\"<>\\^`{|}"
private const val LAST_CONTROL: Int = 0x20
private const val DELETE: Int = 0x7F
private const val HEX_DIGITS: String = "0123456789ABCDEF"
private const val NIBBLE: Int = 4
private const val LOW_NIBBLE: Int = 0xF
private const val BYTE: Int = 0xFF
private const val SURROGATES_START: Int = 0xD800
private const val SURROGATES_END: Int = 0xDFFF

private val urnTranscriptions: List<Pair<String, String>> = listOf(
    "%2B" to "+",
    "%3A" to ":",
    "%2F" to "/",
    "%3B" to ";",
    "%27" to "'",
    "%3F" to "?",
    "%23" to "#",
    "%25" to "%",
    "+" to " ",
    ":" to "//",
    ";" to "::",
)

internal fun normalizedPublicId(publicId: String): String = buildString(publicId.length) {
    var isPendingSpace = false
    publicId.forEach { character ->
        val isWhitespace = character in WHITESPACE
        if (!isWhitespace && isPendingSpace) append(SPACE)
        if (!isWhitespace) append(character)
        isPendingSpace = isWhitespace && isNotEmpty()
    }
}

internal fun normalizedSystemId(systemId: String): String? {
    val normalized = StringBuilder(systemId.length)
    var at = 0
    while (at < systemId.length) {
        val codePoint = systemId.codePointAt(at)
        if (codePoint in SURROGATES_START..SURROGATES_END) return null
        appendNormalized(normalized, codePoint)
        at += charCountOf(codePoint)
    }
    return normalized.toString()
}

internal fun isPublicIdUrn(identifier: String): Boolean =
    identifier.length >= PUBLIC_ID_URN.length && identifier.startsWith(PUBLIC_ID_URN, ignoreCase = true)

internal fun unwrappedPublicIdUrn(urn: String): String {
    val unwrapped = StringBuilder(urn.length)
    var at = PUBLIC_ID_URN.length
    while (at < urn.length) {
        val transcription = urnTranscriptions.firstOrNull { (from, _) -> urn.startsWith(from, at, ignoreCase = true) }
        val replacement = transcription?.second ?: urn[at].toString()
        unwrapped.append(replacement)
        val consumed = transcription?.first
        at += consumed?.length ?: 1
    }
    return normalizedPublicId(unwrapped.toString())
}

private fun appendNormalized(normalized: StringBuilder, codePoint: Int) {
    val isEscaped = codePoint <= LAST_CONTROL || codePoint >= DELETE || codePoint.toChar() in ESCAPED_ASCII
    if (!isEscaped) {
        normalized.append(codePoint.toChar())
        return
    }
    val bytes = textOf(codePoint).encodeToByteArray()
    bytes.forEach { byte -> appendEscaped(normalized, byte.toInt() and BYTE) }
}

private fun appendEscaped(normalized: StringBuilder, octet: Int) {
    normalized.append('%')
    normalized.append(HEX_DIGITS[octet shr NIBBLE])
    normalized.append(HEX_DIGITS[octet and LOW_NIBBLE])
}
