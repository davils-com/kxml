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

import com.davils.kxml.core.fixture.CR
import com.davils.kxml.core.fixture.CRLF
import com.davils.kxml.core.fixture.LF
import com.davils.kxml.core.fixture.TAB
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val UTF_16: String = "UTF-16"
private const val UTF_32: String = "UTF-32"

private val unreadable: Map<String, String> = mapOf(
    "no declaration" to "<a/>",
    "an unclosed declaration" to "<?xml version='1.0' encoding='UTF-8'",
    "no encoding" to "<?xml version='1.0'?>",
    "an encoding without whitespace before it" to "<?xml version='1.0'encoding='UTF-8'?>",
    "an encoding without =" to "<?xml version='1.0' encoding 'UTF-8'?>",
    "an unquoted encoding" to "<?xml version='1.0' encoding=UTF-8?>",
    "an unclosed quote" to "<?xml version='1.0' encoding='UTF-8?>",
    "an encoding that starts with a digit" to "<?xml version='1.0' encoding='8859-1'?>",
    "an empty encoding" to "<?xml version='1.0' encoding=''?>",
    "an encoding with a space" to "<?xml version='1.0' encoding='UTF 8'?>",
    "an encoding cut off after its name" to "<?xml version='1.0' encoding ?>",
    "an encoding cut off after =" to "<?xml version='1.0' encoding= ?>",
)

class DeclaredEncodingTest : FunSpec({
    context("the encoding a declaration names") {
        test("is read in single and double quotes with whitespace around =") {
            DeclaredEncoding.of("<?xml version='1.0' encoding='ISO-8859-1'?><a/>") shouldBe "ISO-8859-1"
            DeclaredEncoding.of("""<?xml version="1.0"${TAB}encoding = "x._y-z" ?>""") shouldBe "x._y-z"
            DeclaredEncoding.of("<?xml version='1.0'${CRLF}encoding$LF=$CR'Latin1'?>") shouldBe "Latin1"
        }

        unreadable.forEach { (case, text) ->
            test("is not read from $case") {
                DeclaredEncoding.of(text).shouldBeNull()
            }
        }
    }

    context("encoding names") {
        test("follow the grammar of EncName") {
            DeclaredEncoding.isEncodingName("Shift_JIS") shouldBe true
            DeclaredEncoding.isEncodingName("_x") shouldBe false
            DeclaredEncoding.isEncodingName("") shouldBe false
            DeclaredEncoding.isEncodingName("{x") shouldBe false
            DeclaredEncoding.isEncodingName("x{") shouldBe false
        }
    }

    context("labels") {
        test("keep the byte order the bytes were detected in") {
            XmlEncoding.forLabel(UTF_16, XmlEncoding.Utf16LittleEndian) shouldBe XmlEncoding.Utf16LittleEndian
            XmlEncoding.forLabel(UTF_32, XmlEncoding.Utf32LittleEndian) shouldBe XmlEncoding.Utf32LittleEndian
        }

        test("default to big endian without a detected byte order") {
            XmlEncoding.forLabel(UTF_16, XmlEncoding.Utf8) shouldBe XmlEncoding.Utf16BigEndian
            XmlEncoding.forLabel(UTF_32, XmlEncoding.Utf8) shouldBe XmlEncoding.Utf32BigEndian
        }

        test("match without regard to case and refuse what kxml does not read") {
            XmlEncoding.forLabel("cp819", XmlEncoding.Utf8) shouldBe XmlEncoding.Latin1
            XmlEncoding.forLabel("windows-1252", XmlEncoding.Utf8).shouldBeNull()
        }
    }
})
