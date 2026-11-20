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

private const val UCS4_2143: String = "UCS-4 in the byte order 2143"
private const val UCS4_3412: String = "UCS-4 in the byte order 3412"
private const val EBCDIC: String = "EBCDIC"
private const val BYTE_MASK: Int = 0xFF
private const val HEX_RADIX: Int = 16
private const val PREFIX_LENGTH: Int = 4

private fun signature(hex: String): List<Int> = hex.split(" ").map { byte -> byte.toInt(HEX_RADIX) }

private val signatures: List<Pair<List<Int>, Detection>> = listOf(
    signature("00 00 FE FF") to Detection.Known(XmlEncoding.Utf32BigEndian, markLength = 4),
    signature("FF FE 00 00") to Detection.Known(XmlEncoding.Utf32LittleEndian, markLength = 4),
    signature("00 00 FF FE") to Detection.Unknown(UCS4_2143),
    signature("FE FF 00 00") to Detection.Unknown(UCS4_3412),
    signature("FE FF") to Detection.Known(XmlEncoding.Utf16BigEndian, markLength = 2),
    signature("FF FE") to Detection.Known(XmlEncoding.Utf16LittleEndian, markLength = 2),
    signature("EF BB BF") to Detection.Known(XmlEncoding.Utf8, markLength = 3),
    signature("00 00 00 3C") to Detection.Known(XmlEncoding.Utf32BigEndian, markLength = 0),
    signature("3C 00 00 00") to Detection.Known(XmlEncoding.Utf32LittleEndian, markLength = 0),
    signature("00 00 3C 00") to Detection.Unknown(UCS4_2143),
    signature("00 3C 00 00") to Detection.Unknown(UCS4_3412),
    signature("00 3C 00 3F") to Detection.Known(XmlEncoding.Utf16BigEndian, markLength = 0),
    signature("3C 00 3F 00") to Detection.Known(XmlEncoding.Utf16LittleEndian, markLength = 0),
    signature("4C 6F A7 94") to Detection.Unknown(EBCDIC),
)

private val asciiFamily: Detection = Detection.Known(XmlEncoding.Utf8, markLength = 0)

internal object EncodingDetection {
    fun detect(bytes: ByteArray): Detection {
        val prefix = bytes.take(PREFIX_LENGTH).map { byte -> byte.toInt() and BYTE_MASK }
        val match = signatures.firstOrNull { (signature, _) -> prefix.take(signature.size) == signature }
            ?: return asciiFamily
        return match.second
    }
}
