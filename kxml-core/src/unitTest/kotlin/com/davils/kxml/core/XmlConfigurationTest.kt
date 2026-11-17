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

import com.davils.kore.core.validation.report.ValidationException
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.emit.XmlEmitOptions
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.limit.xmlLimits
import com.davils.kxml.core.source.XmlSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

private const val DEPTH: Int = 12
private const val TOO_WIDE: Int = XmlEmitOptions.MAXIMUM_INDENT + 1

class XmlConfigurationTest : FunSpec({
    context("the configuration") {
        test("starts from safe defaults") {
            val configuration = Xml.Default.configuration

            configuration.doctype shouldBe XmlDoctype.Refuse
            configuration.whitespace shouldBe XmlWhitespace.Keep
            configuration.limits shouldBe XmlLimits.Untrusted
            configuration.emitter shouldBe XmlEmitOptions.Default
            configuration.toString() shouldContain "doctype=Refuse"
            Xml.Default.toString() shouldContain "Xml"
        }

        test("refuses limits that are not positive") {
            shouldThrow<ValidationException> { xml { limits { maximumElements = 0 } } }
        }

        test("refuses a depth beyond the hard limit") {
            shouldThrow<ValidationException> { xml { limits { maximumDepth = XmlLimits.HARD_DEPTH + 1 } } }
        }

        test("refuses an indent that is too wide") {
            shouldThrow<ValidationException> { xml { emitter { indent = TOO_WIDE } } }
            shouldThrow<ValidationException> { xml { emitter { indent = -1 } } }
        }

        test("builds limits from a base") {
            val limits = xmlLimits(XmlLimits.Trusted) { maximumDepth = DEPTH }

            limits.maximumDepth shouldBe DEPTH
            limits.maximumElements shouldBe Long.MAX_VALUE
            limits.toString() shouldContain "depth=$DEPTH"
            limits shouldNotBe XmlLimits.Trusted
            limits shouldNotBe DEPTH
            xmlLimits { }.hashCode() shouldBe XmlLimits.Untrusted.hashCode()
        }

        test("keeps limits and emitters set directly") {
            xml { limits = XmlLimits.Trusted }.configuration.limits shouldBe XmlLimits.Trusted
            xml { emitter = XmlEmitOptions.Compact }.configuration.emitter shouldBe XmlEmitOptions.Compact
        }

        test("compares and describes emitters") {
            XmlEmitOptions.Compact shouldNotBe XmlEmitOptions.Default
            XmlEmitOptions.Compact shouldNotBe DEPTH
            XmlEmitOptions.Default.hashCode() shouldBe xml { emitter { } }.configuration.emitter.hashCode()
            XmlEmitOptions.Default.toString() shouldContain "indent=2"
        }
    }

    context("diagnostic codes") {
        test("describe themselves by their value") {
            XmlDiagnosticCode.MissingRoot.toString() shouldBe "kxml.syntax.missing-root"
        }
    }

    context("events") {
        test("report whether they are valid") {
            Xml.Default.events(XmlSource.of("<a/>")).isValid shouldBe true
            Xml.Default.events(XmlSource.of("<a>")).isValid shouldBe false
        }
    }
})
