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

import com.davils.kore.core.validation.report.ValidationException
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.fixture.refusal
import com.davils.kxml.core.fixture.root
import com.davils.kxml.core.xml
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

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

    context("the hard ceilings") {
        test("admit every limit at its ceiling") {
            val ceilings = xmlLimits {
                maximumInputLength = XmlLimits.HARD_INPUT_LENGTH
                maximumDepth = XmlLimits.HARD_DEPTH
                maximumElements = XmlLimits.HARD_ELEMENTS
                maximumAttributes = XmlLimits.HARD_ATTRIBUTES
                maximumTextLength = XmlLimits.HARD_INPUT_LENGTH
                maximumNameLength = XmlLimits.HARD_NAME_LENGTH
                maximumEntityDeclarations = XmlLimits.HARD_ENTITY_DECLARATIONS
                maximumAttributeDeclarations = XmlLimits.HARD_ATTRIBUTE_DECLARATIONS
                maximumEntityExpansion = XmlLimits.HARD_ENTITY_EXPANSION
                maximumEntityDepth = XmlLimits.HARD_ENTITY_DEPTH
                maximumExternalResources = XmlLimits.HARD_EXTERNAL_RESOURCES
                maximumExternalLength = XmlLimits.HARD_INPUT_LENGTH
                maximumCatalogFiles = XmlLimits.HARD_CATALOG_FILES
                maximumCatalogDepth = XmlLimits.HARD_CATALOG_DEPTH
                maximumOutputLength = XmlLimits.HARD_INPUT_LENGTH
                maximumQueryLength = XmlLimits.HARD_QUERY_LENGTH
                maximumQueryNesting = XmlLimits.HARD_QUERY_NESTING
                maximumQuerySteps = XmlLimits.HARD_QUERY_STEPS
                maximumQueryResults = XmlLimits.HARD_QUERY_RESULTS
                maximumQueryStringLength = XmlLimits.HARD_INPUT_LENGTH
                maximumPatchOperations = XmlLimits.HARD_PATCH_OPERATIONS
                maximumDiffWork = XmlLimits.HARD_DIFF_WORK
                maximumMergeLayers = XmlLimits.HARD_MERGE_LAYERS
                maximumRedactionRules = XmlLimits.HARD_REDACTION_RULES
            }

            ceilings.maximumElements shouldBe XmlLimits.HARD_ELEMENTS
            ceilings.maximumNameLength shouldBe XmlLimits.HARD_NAME_LENGTH
            ceilings.maximumExternalResources shouldBe XmlLimits.HARD_EXTERNAL_RESOURCES
            ceilings.toString() shouldContain "externalLength=${XmlLimits.HARD_INPUT_LENGTH}"
            ceilings.toString() shouldContain "catalogFiles=${XmlLimits.HARD_CATALOG_FILES}"
            ceilings.maximumCatalogDepth shouldBe XmlLimits.HARD_CATALOG_DEPTH
            ceilings.toString() shouldContain "output=${XmlLimits.HARD_INPUT_LENGTH}"
            ceilings.maximumQueryLength shouldBe XmlLimits.HARD_QUERY_LENGTH
            ceilings.maximumQueryNesting shouldBe XmlLimits.HARD_QUERY_NESTING
            ceilings.maximumQuerySteps shouldBe XmlLimits.HARD_QUERY_STEPS
            ceilings.maximumQueryResults shouldBe XmlLimits.HARD_QUERY_RESULTS
            ceilings.toString() shouldContain "queryString=${XmlLimits.HARD_INPUT_LENGTH}"
            ceilings.maximumPatchOperations shouldBe XmlLimits.HARD_PATCH_OPERATIONS
            ceilings.maximumDiffWork shouldBe XmlLimits.HARD_DIFF_WORK
            ceilings.maximumMergeLayers shouldBe XmlLimits.HARD_MERGE_LAYERS
            ceilings.toString() shouldContain "redactionRules=${XmlLimits.HARD_REDACTION_RULES}"
            xmlLimits(base = ceilings) { } shouldBe ceilings
        }

        test("refuse every limit past its ceiling") {
            shouldThrow<ValidationException> { xmlLimits { maximumInputLength = XmlLimits.HARD_INPUT_LENGTH + 1 } }
            shouldThrow<ValidationException> { xmlLimits { maximumDepth = XmlLimits.HARD_DEPTH + 1 } }
            shouldThrow<ValidationException> { xmlLimits { maximumElements = XmlLimits.HARD_ELEMENTS + 1 } }
            shouldThrow<ValidationException> { xmlLimits { maximumAttributes = XmlLimits.HARD_ATTRIBUTES + 1 } }
            shouldThrow<ValidationException> { xmlLimits { maximumTextLength = XmlLimits.HARD_INPUT_LENGTH + 1 } }
            shouldThrow<ValidationException> { xmlLimits { maximumNameLength = XmlLimits.HARD_NAME_LENGTH + 1 } }
            shouldThrow<ValidationException> {
                xmlLimits { maximumExternalResources = XmlLimits.HARD_EXTERNAL_RESOURCES + 1 }
            }
            shouldThrow<ValidationException> { xmlLimits { maximumExternalLength = XmlLimits.HARD_INPUT_LENGTH + 1 } }
            shouldThrow<ValidationException> { xmlLimits { maximumCatalogFiles = XmlLimits.HARD_CATALOG_FILES + 1 } }
            shouldThrow<ValidationException> { xmlLimits { maximumCatalogDepth = XmlLimits.HARD_CATALOG_DEPTH + 1 } }
            shouldThrow<ValidationException> { xmlLimits { maximumOutputLength = XmlLimits.HARD_INPUT_LENGTH + 1 } }
            shouldThrow<ValidationException> { xmlLimits { maximumQueryLength = XmlLimits.HARD_QUERY_LENGTH + 1 } }
            shouldThrow<ValidationException> { xmlLimits { maximumQueryNesting = XmlLimits.HARD_QUERY_NESTING + 1 } }
            shouldThrow<ValidationException> { xmlLimits { maximumQuerySteps = XmlLimits.HARD_QUERY_STEPS + 1 } }
            shouldThrow<ValidationException> { xmlLimits { maximumQueryResults = XmlLimits.HARD_QUERY_RESULTS + 1 } }
            shouldThrow<ValidationException> {
                xmlLimits { maximumQueryStringLength = XmlLimits.HARD_INPUT_LENGTH + 1 }
            }
            shouldThrow<ValidationException> {
                xmlLimits { maximumPatchOperations = XmlLimits.HARD_PATCH_OPERATIONS + 1 }
            }
            shouldThrow<ValidationException> { xmlLimits { maximumDiffWork = XmlLimits.HARD_DIFF_WORK + 1 } }
            shouldThrow<ValidationException> { xmlLimits { maximumMergeLayers = XmlLimits.HARD_MERGE_LAYERS + 1 } }
            shouldThrow<ValidationException> {
                xmlLimits { maximumRedactionRules = XmlLimits.HARD_REDACTION_RULES + 1 }
            }
        }

        test("refuse every limit below 1") {
            shouldThrow<ValidationException> { xmlLimits { maximumInputLength = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumDepth = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumAttributes = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumTextLength = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumNameLength = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumExternalResources = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumExternalLength = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumCatalogFiles = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumCatalogDepth = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumOutputLength = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumQueryLength = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumQueryNesting = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumQuerySteps = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumQueryResults = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumQueryStringLength = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumPatchOperations = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumDiffWork = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumMergeLayers = 0 } }
            shouldThrow<ValidationException> { xmlLimits { maximumRedactionRules = 0 } }
        }

        test("keep the relaxed preset finite and within every ceiling") {
            val relaxed = XmlLimits.Relaxed

            relaxed.maximumInputLength shouldBe XmlLimits.HARD_INPUT_LENGTH
            relaxed.maximumDepth shouldBe XmlLimits.HARD_DEPTH
            relaxed.maximumElements shouldBeLessThanOrEqual XmlLimits.HARD_ELEMENTS
            relaxed.maximumAttributes.toLong() shouldBeLessThanOrEqual XmlLimits.HARD_ATTRIBUTES.toLong()
            relaxed.maximumNameLength.toLong() shouldBeLessThanOrEqual XmlLimits.HARD_NAME_LENGTH.toLong()
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
