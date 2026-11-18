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

package com.davils.kxml.serialization

import com.davils.kxml.serialization.fixture.CommonPrefix
import com.davils.kxml.serialization.fixture.ObjectSummary
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.decodeFromString

private const val LINE_SEPARATOR: String = "\u2028"
private const val RIGHT_TO_LEFT_OVERRIDE: String = "‮"
private const val VALID: String = "<CommonPrefix><prefix>p</prefix></CommonPrefix>"
private const val BROKEN: String = "<CommonPrefix>"
private const val HOSTILE_CLASS: String = "<ObjectSummary><key>k</key><eTag>e</eTag><size>1</size>" +
    "<storageClass>X$RIGHT_TO_LEFT_OVERRIDE${LINE_SEPARATOR}Y</storageClass></ObjectSummary>"

class XmlDecodingTest : FunSpec({
    context("a decoded value") {
        test("is equal to another with an equal value and equal diagnostics") {
            val decoded = XmlFormat.Default.decodeOrFailure<CommonPrefix>(VALID)
            val again = XmlFormat.Default.decodeOrFailure<CommonPrefix>(VALID)

            decoded shouldBe again
            decoded.hashCode() shouldBe again.hashCode()
            decoded shouldNotBe XmlFormat.Default.decodeOrFailure<CommonPrefix>(BROKEN)
        }

        test("never shows the value, which may be sensitive") {
            XmlFormat.Default.decodeOrFailure<CommonPrefix>(VALID).toString() shouldBe "Decoded([])"
        }
    }

    context("a failure") {
        test("is equal to another with equal diagnostics") {
            val failed = XmlFormat.Default.decodeOrFailure<CommonPrefix>(BROKEN)
            val again = XmlFormat.Default.decodeOrFailure<CommonPrefix>(BROKEN)

            failed shouldBe again
            failed.hashCode() shouldBe again.hashCode()
            failed shouldNotBe XmlFormat.Default.decodeOrFailure<CommonPrefix>(VALID)
            failed.toString() shouldContain "kxml.syntax.unclosed-element"
        }
    }

    context("the exception") {
        test("escapes the parts of its message that quote the document") {
            val refusal = shouldThrow<XmlDecodingException> {
                XmlFormat.Default.decodeFromString<ObjectSummary>(HOSTILE_CLASS)
            }
            val message = refusal.message.orEmpty()

            message shouldContain "X\\u{202E}\\u{2028}Y"
            message shouldNotContain LINE_SEPARATOR
            message shouldNotContain RIGHT_TO_LEFT_OVERRIDE
        }

        test("keeps its own copy of the diagnostics") {
            val refusal = shouldThrow<XmlDecodingException> { XmlFormat.Default.decodeFromString<CommonPrefix>(BROKEN) }

            val diagnostic = refusal.diagnostics.single()
            diagnostic.code.value shouldBe "kxml.syntax.unclosed-element"
        }
    }

    context("the configuration") {
        test("escapes its namespace when it is shown") {
            val format = xmlFormat { namespace = "urn:$RIGHT_TO_LEFT_OVERRIDE" }

            format.configuration.toString() shouldContain "urn:\\u{202E}"
        }
    }
})
