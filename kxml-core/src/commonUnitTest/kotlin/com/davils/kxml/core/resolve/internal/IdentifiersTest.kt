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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val DELETE_AND_NUL: String = "\u007F\u0000"

private const val GRINNING_FACE: String = "\uD83D\uDE00"
private const val UMLAUT: String = "\u00e4"
private const val EURO: String = "\u20ac"
private const val TAB: String = "\t"
private const val RETURN: String = "\r"
private const val NEWLINE: String = "\n"

class IdentifiersTest : FunSpec({
    context("normalizedPublicId") {
        test("collapses whitespace into single spaces and drops it at both ends") {
            normalizedPublicId("  -//A//DTD $TAB$RETURN$NEWLINE B//EN $NEWLINE") shouldBe "-//A//DTD B//EN"
            normalizedPublicId("") shouldBe ""
        }
    }

    context("normalizedSystemId") {
        test("escapes the characters of section 6.3 as uppercase UTF-8 octets and keeps percent and number signs") {
            normalizedSystemId("""a b"<>\^`{|}""" + DELETE_AND_NUL) shouldBe "a%20b%22%3C%3E%5C%5E%60%7B%7C%7D%7F%00"
            normalizedSystemId(UMLAUT + EURO + GRINNING_FACE) shouldBe "%C3%A4%E2%82%AC%F0%9F%98%80"
            normalizedSystemId("a%2F#f[1]~") shouldBe "a%2F#f[1]~"
        }

        test("refuses a lone surrogate") {
            normalizedSystemId("a\uD800").shouldBeNull()
            normalizedSystemId("\uDC00b").shouldBeNull()
        }
    }

    context("unwrappedPublicIdUrn") {
        test("transcribes a urn:publicid URN by the table of section 6.4") {
            unwrappedPublicIdUrn("urn:publicid:-:OASIS:DTD+DocBook+XML+V4.1.2:EN") shouldBe
                "-//OASIS//DTD DocBook XML V4.1.2//EN"
            unwrappedPublicIdUrn("URN:PUBLICID:a;b%2B%3a%2F%3B%27%3F%23%25c") shouldBe "a::b+:/;'?#%c"
            isPublicIdUrn("urn:publicid:") shouldBe true
            isPublicIdUrn("urn:public") shouldBe false
        }
    }
})
