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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll

private const val CASES: Int = 500
private const val LONGEST_TAIL: Int = 16
private const val HEX_RADIX: Int = 16

private val knownPrefixes: Map<String, Pair<XmlEncoding, Int>> = mapOf(
    "00 00 FE FF" to (XmlEncoding.Utf32BigEndian to 4),
    "FF FE 00 00" to (XmlEncoding.Utf32LittleEndian to 4),
    "FE FF 00 3C" to (XmlEncoding.Utf16BigEndian to 2),
    "FF FE 3C 00" to (XmlEncoding.Utf16LittleEndian to 2),
    "EF BB BF 3C" to (XmlEncoding.Utf8 to 3),
    "00 00 00 3C" to (XmlEncoding.Utf32BigEndian to 0),
    "3C 00 00 00" to (XmlEncoding.Utf32LittleEndian to 0),
    "00 3C 00 3F" to (XmlEncoding.Utf16BigEndian to 0),
    "3C 00 3F 00" to (XmlEncoding.Utf16LittleEndian to 0),
    "3C 3F 78 6D" to (XmlEncoding.Utf8 to 0),
    "3C 61 2F 3E" to (XmlEncoding.Utf8 to 0),
)

private val unknownPrefixes: Map<String, String> = mapOf(
    "00 00 FF FE" to "2143",
    "FE FF 00 00" to "3412",
    "00 00 3C 00" to "2143",
    "00 3C 00 00" to "3412",
    "4C 6F A7 94" to "EBCDIC",
)

private fun bytesOf(hex: String): ByteArray {
    val values = hex.split(" ").map { byte -> byte.toInt(HEX_RADIX).toByte() }
    return values.toByteArray()
}

class EncodingDetectionTest : FunSpec({
    context("the byte order marks and prefixes of Appendix F") {
        knownPrefixes.forEach { (prefix, expected) ->
            test("$prefix is detected as ${expected.first} with a mark of ${expected.second} bytes") {
                val detection = EncodingDetection.detect(bytesOf(prefix)).shouldBeInstanceOf<Detection.Known>()

                detection.encoding shouldBe expected.first
                detection.markLength shouldBe expected.second
            }
        }

        unknownPrefixes.forEach { (prefix, layout) ->
            test("$prefix is detected as an encoding kxml does not read") {
                val detection = EncodingDetection.detect(bytesOf(prefix)).shouldBeInstanceOf<Detection.Unknown>()

                detection.layout.contains(layout) shouldBe true
            }
        }
    }

    context("bytes too short for a prefix") {
        test("are read as UTF-8 unless they hold a byte order mark") {
            val utf8 = EncodingDetection.detect(bytesOf("3C")).shouldBeInstanceOf<Detection.Known>()
            val half = EncodingDetection.detect(bytesOf("FE 28")).shouldBeInstanceOf<Detection.Known>()
            val mark = EncodingDetection.detect(bytesOf("FE FF")).shouldBeInstanceOf<Detection.Known>()

            utf8.encoding shouldBe XmlEncoding.Utf8
            half.encoding shouldBe XmlEncoding.Utf8
            EncodingDetection.detect(ByteArray(0)).shouldBeInstanceOf<Detection.Known>()
            mark.encoding shouldBe XmlEncoding.Utf16BigEndian
        }
    }

    context("what follows a prefix") {
        test("never changes the detection") {
            val prefixes = knownPrefixes.keys + unknownPrefixes.keys
            val tails = Arb.list(Arb.byte(), 0..LONGEST_TAIL)

            checkAll(CASES, Arb.element(prefixes.toList()), tails) { prefix, tail ->
                val alone = EncodingDetection.detect(bytesOf(prefix))
                val followed = EncodingDetection.detect(bytesOf(prefix) + tail.toByteArray())

                describe(followed) shouldBe describe(alone)
            }
        }
    }
})

private fun describe(detection: Detection): String = when (detection) {
    is Detection.Known -> "${detection.encoding}/${detection.markLength}"
    is Detection.Unknown -> detection.layout
}
