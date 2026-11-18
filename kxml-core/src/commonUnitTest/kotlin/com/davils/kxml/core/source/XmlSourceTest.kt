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

import com.davils.kxml.core.Xml
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.fixture.hostileText
import com.davils.kxml.core.fixture.shouldBeShownSafely
import com.davils.kxml.core.name.XmlName
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.checkAll

private const val DOCUMENT: String = "<Key>über</Key>"
private const val MARK: String = "﻿"
private const val OPEN_PARENTHESIS: Byte = 0x28
private const val MARK_HIGH: Byte = -2
private const val MARK_LOW: Byte = -1
private const val BROKEN_LEAD: Int = 0xC3
private const val BYTE_MASK: Int = 0xFF
private const val BYTE_BITS: Int = 8
private const val ROCKET: String = "\uD83D\uDE80"
private const val LONE_HIGH: String = "\uD800"
private const val LONE_LOW: String = "\uDC00"

private val invalidUtf8: ByteArray = "<a>".encodeToByteArray() + byteArrayOf(BROKEN_LEAD.toByte(), OPEN_PARENTHESIS)

private fun utf16(text: String, isBigEndian: Boolean): ByteArray {
    val bytes = text.flatMap { char ->
        val high = (char.code shr BYTE_BITS and BYTE_MASK).toByte()
        val low = (char.code and BYTE_MASK).toByte()
        listOf(high, low).takeIf { isBigEndian } ?: listOf(low, high)
    }
    return bytes.toByteArray()
}

private fun utf16LittleEndian(text: String): ByteArray = utf16(text, isBigEndian = false)

private fun utf16BigEndian(text: String): ByteArray = utf16(text, isBigEndian = true)

private fun parsedText(source: XmlSource): String {
    val result = Xml.Default.parse(source).shouldBeInstanceOf<XmlParseResult.Parsed>()
    return result.document.root.text
}

private fun refusal(source: XmlSource): XmlDiagnosticCode {
    val result = Xml.Default.parse(source).shouldBeInstanceOf<XmlParseResult.Failed>()
    return result.diagnostics.single().code
}

private const val CASES: Int = 200

class XmlSourceTest : FunSpec({
    context("encodings") {
        val encoders: Map<XmlEncoding, (String) -> ByteArray> = mapOf(
            XmlEncoding.Utf8 to String::encodeToByteArray,
            XmlEncoding.Utf16LittleEndian to ::utf16LittleEndian,
            XmlEncoding.Utf16BigEndian to ::utf16BigEndian,
        )

        encoders.forEach { (encoding, encode) ->
            test("detects $encoding without a byte order mark") {
                SourceDecoding.encodingOf(encode(DOCUMENT)) shouldBe encoding
            }

            test("detects $encoding with a byte order mark and reads it") {
                val bytes = encode(MARK + DOCUMENT)

                SourceDecoding.encodingOf(bytes) shouldBe encoding
                parsedText(XmlSource.of(bytes, name = "key.xml")) shouldBe "über"
            }
        }

        test("reads a half byte order mark as UTF-8") {
            SourceDecoding.encodingOf(byteArrayOf(MARK_HIGH, OPEN_PARENTHESIS)) shouldBe XmlEncoding.Utf8
            SourceDecoding.encodingOf(byteArrayOf(MARK_LOW, OPEN_PARENTHESIS)) shouldBe XmlEncoding.Utf8
        }

        test("reads input shorter than a byte order mark as UTF-8") {
            SourceDecoding.encodingOf(byteArrayOf('<'.code.toByte())) shouldBe XmlEncoding.Utf8
        }

        test("refuses bytes that are not valid UTF-8") {
            refusal(XmlSource.of(invalidUtf8)) shouldBe XmlDiagnosticCode.InvalidEncoding
        }

        test("reads characters outside the basic plane from UTF-16") {
            parsedText(XmlSource.of(utf16LittleEndian("<Key>$ROCKET</Key>"))) shouldBe ROCKET
            parsedText(XmlSource.of(utf16BigEndian("$MARK<Key>$ROCKET</Key>"))) shouldBe ROCKET
        }

        test("refuses UTF-16 with an odd number of bytes") {
            val bytes = utf16LittleEndian("<a/>") + byteArrayOf(OPEN_PARENTHESIS)

            refusal(XmlSource.of(bytes)) shouldBe XmlDiagnosticCode.InvalidEncoding
        }

        test("refuses UTF-16 with a surrogate that has no partner") {
            refusal(XmlSource.of(utf16LittleEndian("<a>$LONE_HIGH</a>"))) shouldBe XmlDiagnosticCode.InvalidEncoding
            refusal(XmlSource.of(utf16LittleEndian("<a>$LONE_LOW</a>"))) shouldBe XmlDiagnosticCode.InvalidEncoding
            refusal(XmlSource.of(utf16LittleEndian("<a>a$LONE_HIGH"))) shouldBe XmlDiagnosticCode.InvalidEncoding
        }

        test("strips a byte order mark from text") {
            parsedText(XmlSource.of(MARK + DOCUMENT)) shouldBe "über"
        }
    }

    context("declared encodings") {
        test("accept what matches the bytes") {
            val utf8 = "<?xml version='1.0' encoding='utf-8'?>$DOCUMENT".encodeToByteArray()
            val utf16 = utf16LittleEndian("<?xml version='1.0' encoding='UTF-16'?>$DOCUMENT")

            parsedText(XmlSource.of(utf8)) shouldBe "über"
            parsedText(XmlSource.of(utf16)) shouldBe "über"
        }

        test("refuse what does not match the bytes") {
            val latin = "<?xml version='1.0' encoding='ISO-8859-1'?>$DOCUMENT".encodeToByteArray()

            refusal(XmlSource.of(latin)) shouldBe XmlDiagnosticCode.UnsupportedEncoding
        }

        test("are ignored for text") {
            val text = "<?xml version='1.0' encoding='ISO-8859-1'?>$DOCUMENT"

            parsedText(XmlSource.of(text)) shouldBe "über"
        }
    }

    context("sources") {
        test("describe themselves") {
            XmlSource.of("<a/>", name = "a.xml").toString() shouldContain "a.xml"
            XmlSource.of("<a/>").toString() shouldContain "unnamed"
        }

        test("escape their name when they are shown") {
            checkAll(CASES, hostileText) { text ->
                XmlSource.of("<a/>", name = text).shouldBeShownSafely()
            }
        }

        test("read the name of the root") {
            val result = Xml.Default.parse(XmlSource.of(DOCUMENT))
            val parsed = result.shouldBeInstanceOf<XmlParseResult.Parsed>()

            parsed.document.root.name shouldBe XmlName("Key")
        }
    }
})
