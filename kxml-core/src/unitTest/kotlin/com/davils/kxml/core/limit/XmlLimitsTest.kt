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

package com.davils.kxml.core.limit

import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.fixture.refusal
import com.davils.kxml.core.fixture.root
import com.davils.kxml.core.xml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val SMALL: Int = 4

class XmlLimitsTest : FunSpec({
    context("a document beyond its limits") {
        test("is refused when the input is too long") {
            xml { limits { maximumInputLength = SMALL } }.refusal("<abc/>") shouldBe XmlDiagnosticCode.InputTooLong
        }

        test("is refused when it nests too deep") {
            xml { limits { maximumDepth = 2 } }.refusal("<a><b><c/></b></a>") shouldBe XmlDiagnosticCode.TooDeep
        }

        test("is refused when it has too many elements") {
            xml { limits { maximumElements = 2 } }.refusal(
                "<a><b/><c/></a>",
            ) shouldBe XmlDiagnosticCode.TooManyElements
        }

        test("is refused when an element has too many attributes") {
            xml { limits { maximumAttributes = 1 } }.refusal("<a x='1' y='2'/>") shouldBe
                XmlDiagnosticCode.TooManyAttributes
        }

        test("is refused when a text, a value or a comment is too long") {
            val small = xml { limits { maximumTextLength = SMALL } }

            small.refusal("<a>12345</a>") shouldBe XmlDiagnosticCode.TextTooLong
            small.refusal("<a x='12345'/>") shouldBe XmlDiagnosticCode.TextTooLong
            small.refusal("<a><!--12345--></a>") shouldBe XmlDiagnosticCode.TextTooLong
        }

        test("is refused when a name is too long") {
            xml { limits { maximumNameLength = SMALL } }.refusal("<abcde/>") shouldBe XmlDiagnosticCode.NameTooLong
        }
    }

    context("a document within its limits") {
        test("is read") {
            val shallow = xml { limits { maximumDepth = 2 } }
            val root = shallow.root("<a><b/></a>")

            root.elements.size shouldBe 1
        }
    }
})
