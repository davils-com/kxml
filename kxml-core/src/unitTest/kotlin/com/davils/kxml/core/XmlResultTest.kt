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

package com.davils.kxml.core

import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.source.XmlSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val VALID: String = "<a><b/></a>"
private const val BROKEN: String = "<a>"

class XmlResultTest : FunSpec({
    context("the result of parsing") {
        test("is equal for equal documents and differs from a failure") {
            val parsed = Xml.Default.parse(VALID)
            val again = Xml.Default.parse(VALID)
            val failed = Xml.Default.parse(BROKEN)

            parsed shouldBe again
            parsed.hashCode() shouldBe again.hashCode()
            parsed shouldNotBe failed
            failed shouldBe Xml.Default.parse(BROKEN)
            failed.hashCode() shouldBe Xml.Default.parse(BROKEN).hashCode()
            failed shouldNotBe parsed
        }

        test("describes the root or the diagnostics") {
            Xml.Default.parse(VALID).toString() shouldContain "Parsed(Element(a, 1 children)"
            val failed = Xml.Default.parse(BROKEN).shouldBeInstanceOf<XmlParseResult.Failed>()

            failed.toString() shouldContain XmlDiagnosticCode.UnclosedElement.value
        }
    }

    context("the result of reading events") {
        test("is equal for equal documents") {
            val events = Xml.Default.events(XmlSource.of(VALID))

            events shouldBe Xml.Default.events(XmlSource.of(VALID))
            events.hashCode() shouldBe Xml.Default.events(XmlSource.of(VALID)).hashCode()
            events shouldNotBe Xml.Default.events(XmlSource.of(BROKEN))
            events shouldNotBe VALID
        }

        test("describes the number of events and the diagnostics") {
            Xml.Default.events(XmlSource.of(VALID)).toString() shouldBe "XmlEventResult(4 events, [])"
        }
    }
})
