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

package com.davils.kxml.core.diagnostic

import com.davils.kore.core.text.TextRange
import com.davils.kore.core.validation.violation.Severity
import com.davils.kxml.core.fixture.hostileText
import com.davils.kxml.core.fixture.shouldBeShownSafely
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.checkAll

private const val CASES: Int = 200

private val start: TextRange = TextRange.at(0)

class XmlDiagnosticTest : FunSpec({
    context("diagnostics") {
        test("take the severity of their code unless told otherwise") {
            XmlDiagnostic(XmlDiagnosticCode.IgnoredDoctype, start, "m").severity shouldBe Severity.Warning
            XmlDiagnostic(XmlDiagnosticCode.TooDeep, start, "m").severity shouldBe Severity.Error
        }

        test("are equal by every property") {
            val diagnostic = XmlDiagnostic(XmlDiagnosticCode.TooDeep, start, "m")

            diagnostic shouldBe XmlDiagnostic(XmlDiagnosticCode.TooDeep, start, "m")
            val same = XmlDiagnostic(XmlDiagnosticCode.TooDeep, start, "m")
            diagnostic.hashCode() shouldBe same.hashCode()
            diagnostic shouldNotBe XmlDiagnostic(XmlDiagnosticCode.TooDeep, start, "n")
            diagnostic shouldNotBe XmlDiagnostic(
                code = XmlDiagnosticCode.TooDeep,
                range = start,
                message = "m",
                severity = Severity.Warning,
            )
            diagnostic shouldNotBe "m"
        }

        test("show the severity, the code, the range and the message") {
            XmlDiagnostic(XmlDiagnosticCode.TooDeep, start, "deep").toString() shouldBe
                """Error kxml.limit.depth at 0..0: "deep""""
        }

        test("escape a message that quotes the document") {
            checkAll(CASES, hostileText) { text ->
                XmlDiagnostic(XmlDiagnosticCode.InvalidName, start, text).shouldBeShownSafely()
            }
        }
    }

    context("codes") {
        test("are shown as their dotted value") {
            XmlDiagnosticCode.entries.forEach { code -> code.toString() shouldBe code.value }
        }
    }
})
