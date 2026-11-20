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
import com.davils.kxml.core.internal.charCountOf
import com.davils.kxml.core.internal.codePointAt
import com.davils.kxml.core.name.XmlName
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.checkAll

private const val UTF_8: String = "UTF-8"
private const val UTF_16: String = "UTF-16"
private const val UTF_16_LE: String = "UTF-16LE"
private const val UTF_32: String = "UTF-32"
private const val LATIN_1: String = "ISO-8859-1"
private const val US_ASCII: String = "US-ASCII"
private const val DOCUMENT: String = "<Key>über</Key>"
private const val ASCII_DOCUMENT: String = "<Key>uber</Key>"
private const val MARK: String = "﻿"
private const val OPEN_PARENTHESIS: Byte = 0x28
private const val BROKEN_LEAD: Int = 0xC3
private const val BYTE_MASK: Int = 0xFF
private const val BYTE_BITS: Int = 8
private const val UTF32_UNIT: Int = 4
private const val ROCKET: String = "🚀"
private const val LONE_HIGH: String = "\uD800"
private const val LONE_LOW: String = "\uDC00"
private const val CASES: Int = 200
private const val LONGEST_NOISE: Int = 64
private const val BEYOND_PEEK: Int = 1_100

private val overlongSlash: List<Int> = listOf(0xC0, 0xAF)
private val encodedSurrogate: List<Int> = listOf(0xED, 0xA0, 0x80)
private val beyondUnicode: List<Int> = listOf(0xF4, 0x90, 0x80, 0x80)
private val utf32Surrogate: List<Int> = listOf(0x00, 0x00, 0xD8, 0x00)
private val utf32BeyondUnicode: List<Int> = listOf(0x00, 0x11, 0x00, 0x00)
private val utf32Negative: List<Int> = listOf(0xFF, 0x00, 0x00, 0x41)
private val labels: List<String> = listOf(UTF_8, UTF_16, UTF_16_LE, "UTF-32BE", LATIN_1, US_ASCII, "EBCDIC")
private val noise: Arb<ByteArray> = Arb.list(Arb.byte(), 0..LONGEST_NOISE).map { bytes -> bytes.toByteArray() }

private fun bytesOf(values: List<Int>): ByteArray = ByteArray(values.size) { index -> values[index].toByte() }

private fun utf8Around(sequence: List<Int>): ByteArray =
    "<a>".encodeToByteArray() + bytesOf(sequence) + "</a>".encodeToByteArray()

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

private fun utf32(text: String, isBigEndian: Boolean): ByteArray {
    val bytes = buildList {
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            val ordered = (UTF32_UNIT - 1 downTo 0).map { position ->
                (codePoint shr BYTE_BITS * position and BYTE_MASK).toByte()
            }
            addAll(ordered.takeIf { isBigEndian } ?: ordered.asReversed())
            index += charCountOf(codePoint)
        }
    }
    return bytes.toByteArray()
}

private fun latin1(text: String): ByteArray = ByteArray(text.length) { index -> text[index].code.toByte() }

private fun declared(encoding: String, body: String = DOCUMENT): String =
    "<?xml version='1.0' encoding='$encoding'?>$body"

private fun parsed(source: XmlSource): XmlParseResult.Parsed =
    Xml.Default.parse(source).shouldBeInstanceOf<XmlParseResult.Parsed>()

private fun parsedText(source: XmlSource): String = parsed(source).document.root.text

private fun refusal(source: XmlSource): XmlDiagnosticCode {
    val result = Xml.Default.parse(source).shouldBeInstanceOf<XmlParseResult.Failed>()
    return result.diagnostics.single().code
}

class XmlSourceTest : FunSpec({
    context("encodings with a byte order mark") {
        val encoders: Map<String, (String) -> ByteArray> = mapOf(
            UTF_8 to String::encodeToByteArray,
            UTF_16_LE to ::utf16LittleEndian,
            "UTF-16BE" to ::utf16BigEndian,
            "UTF-32LE" to { text -> utf32(text, isBigEndian = false) },
            "UTF-32BE" to { text -> utf32(text, isBigEndian = true) },
        )

        encoders.forEach { (encoding, encode) ->
            test("read $encoding") {
                parsedText(XmlSource.of(encode(MARK + DOCUMENT), name = "key.xml")) shouldBe "über"
            }

            test("read characters outside the basic plane from $encoding") {
                parsedText(XmlSource.of(encode("$MARK<Key>$ROCKET</Key>"))) shouldBe ROCKET
            }
        }

        test("strip a byte order mark from text") {
            parsedText(XmlSource.of(MARK + DOCUMENT)) shouldBe "über"
        }
    }

    context("encodings without a byte order mark") {
        test("read UTF-8 by default") {
            parsedText(XmlSource.of(DOCUMENT.encodeToByteArray())) shouldBe "über"
        }

        test("read UTF-16 and UTF-32 when the document starts with a declaration") {
            parsedText(XmlSource.of(utf16LittleEndian(declared(UTF_16)))) shouldBe "über"
            parsedText(XmlSource.of(utf16BigEndian(declared(UTF_16)))) shouldBe "über"
            parsedText(XmlSource.of(utf32(declared(UTF_32), isBigEndian = false))) shouldBe "über"
            parsedText(XmlSource.of(utf32(declared(UTF_32), isBigEndian = true))) shouldBe "über"
        }

        test("refuse UTF-16 that neither has a byte order mark nor starts with a declaration") {
            Xml.Default.parse(XmlSource.of(utf16LittleEndian("<a/>"))).shouldBeInstanceOf<XmlParseResult.Failed>()
        }

        test("read ISO-8859-1 when the declaration names it, in any case") {
            parsedText(XmlSource.of(latin1(declared(LATIN_1)))) shouldBe "über"
            parsedText(XmlSource.of(latin1(declared("latin1")))) shouldBe "über"
        }

        test("read US-ASCII strictly when the declaration names it") {
            val escaped = declared(US_ASCII, body = "<Key>&#252;ber</Key>")

            parsedText(XmlSource.of(escaped.encodeToByteArray())) shouldBe "über"
            refusal(XmlSource.of(latin1(declared(US_ASCII)))) shouldBe XmlDiagnosticCode.InvalidEncoding
        }
    }

    context("bytes that are not valid in their encoding") {
        test("are refused in UTF-8") {
            refusal(XmlSource.of(invalidUtf8)) shouldBe XmlDiagnosticCode.InvalidEncoding
        }

        test("are refused as overlong forms, encoded surrogates and code points above U+10FFFF in UTF-8") {
            val forbidden = listOf(overlongSlash, encodedSurrogate, beyondUnicode)

            forbidden.forEach { sequence ->
                refusal(XmlSource.of(utf8Around(sequence))) shouldBe XmlDiagnosticCode.InvalidEncoding
            }
        }

        test("are refused as an odd number of bytes in UTF-16") {
            val bytes = utf16LittleEndian("$MARK<a/>") + byteArrayOf(OPEN_PARENTHESIS)

            refusal(XmlSource.of(bytes)) shouldBe XmlDiagnosticCode.InvalidEncoding
        }

        test("are refused as a surrogate without its partner in UTF-16") {
            val unpaired = listOf("<a>$LONE_HIGH</a>", "<a>$LONE_LOW</a>", "<a>a$LONE_HIGH")

            unpaired.forEach { text ->
                refusal(XmlSource.of(utf16LittleEndian(MARK + text))) shouldBe XmlDiagnosticCode.InvalidEncoding
            }
        }

        test("are refused as a length that is no multiple of four, a surrogate or a code point too large in UTF-32") {
            val open = utf32("$MARK<a>", isBigEndian = true)
            val close = utf32("</a>", isBigEndian = true)

            refusal(XmlSource.of(open + close + byteArrayOf(0))) shouldBe XmlDiagnosticCode.InvalidEncoding
            refusal(XmlSource.of(open + bytesOf(utf32Surrogate) + close)) shouldBe XmlDiagnosticCode.InvalidEncoding
            refusal(XmlSource.of(open + bytesOf(utf32BeyondUnicode) + close)) shouldBe
                XmlDiagnosticCode.InvalidEncoding
            refusal(XmlSource.of(open + bytesOf(utf32Negative) + close)) shouldBe XmlDiagnosticCode.InvalidEncoding
        }
    }

    context("declared encodings") {
        test("accept what matches the bytes, in any case") {
            parsedText(XmlSource.of(declared("utf-8").encodeToByteArray())) shouldBe "über"
            parsedText(XmlSource.of(utf16LittleEndian(MARK + declared(UTF_16)))) shouldBe "über"
            parsedText(XmlSource.of(utf16BigEndian(MARK + declared("utf-16be")))) shouldBe "über"
        }

        test("refuse what contradicts the bytes") {
            val utf16OnAscii = declared(UTF_16, body = ASCII_DOCUMENT).encodeToByteArray()
            val utf8OnUtf16 = utf16LittleEndian(MARK + declared(UTF_8))
            val latinAfterMark = (MARK + declared(LATIN_1, body = ASCII_DOCUMENT)).encodeToByteArray()

            refusal(XmlSource.of(utf16OnAscii)) shouldBe XmlDiagnosticCode.EncodingMismatch
            refusal(XmlSource.of(utf8OnUtf16)) shouldBe XmlDiagnosticCode.EncodingMismatch
            refusal(XmlSource.of(latinAfterMark)) shouldBe XmlDiagnosticCode.EncodingMismatch
        }

        test("refuse an encoding kxml does not read") {
            val windows = declared("windows-1252", body = ASCII_DOCUMENT).encodeToByteArray()

            refusal(XmlSource.of(windows)) shouldBe XmlDiagnosticCode.UnsupportedEncoding
        }

        test("are not trusted beyond the first 1,024 bytes") {
            val spaced = "<?xml version='1.0'${" ".repeat(BEYOND_PEEK)}encoding='ISO-8859-1'?>$ASCII_DOCUMENT"

            refusal(XmlSource.of(spaced.encodeToByteArray())) shouldBe XmlDiagnosticCode.EncodingMismatch
        }

        test("are ignored for text") {
            parsedText(XmlSource.of(declared(LATIN_1))) shouldBe "über"
        }
    }

    context("the charset of a source") {
        test("decides the encoding of bytes without a declaration") {
            parsedText(XmlSource.of(latin1(DOCUMENT), charset = "iso-8859-1")) shouldBe "über"
            parsedText(XmlSource.of(utf16BigEndian(DOCUMENT), charset = UTF_16)) shouldBe "über"
            parsedText(XmlSource.of(utf16LittleEndian(DOCUMENT), charset = UTF_16_LE)) shouldBe "über"
        }

        test("overrides the declared encoding with a warning") {
            val result = parsed(XmlSource.of(latin1(declared(UTF_8)), charset = LATIN_1))
            val codes = result.diagnostics.map { diagnostic -> diagnostic.code }

            result.document.root.text shouldBe "über"
            codes shouldContain XmlDiagnosticCode.EncodingOverridden
        }

        test("agrees with a declaration that names the same encoding without a warning") {
            val result = parsed(XmlSource.of(declared(UTF_8).encodeToByteArray(), charset = "utf-8"))

            result.document.root.text shouldBe "über"
            result.diagnostics.map { diagnostic -> diagnostic.code } shouldBe emptyList()
        }

        test("gives way to a byte order mark") {
            parsedText(XmlSource.of((MARK + DOCUMENT).encodeToByteArray(), charset = LATIN_1)) shouldBe "über"
        }

        test("is refused when kxml does not read it") {
            refusal(XmlSource.of(DOCUMENT.encodeToByteArray(), charset = "Shift_JIS")) shouldBe
                XmlDiagnosticCode.UnsupportedEncoding
        }
    }

    context("random bytes") {
        test("never make the reader throw, whatever the charset") {
            checkAll(CASES, noise, Arb.element(labels).orNull()) { bytes, charset ->
                Xml.Default.parse(XmlSource.of(bytes, charset = charset))
            }
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
            parsed(XmlSource.of(DOCUMENT)).document.root.name shouldBe XmlName("Key")
        }
    }
})
