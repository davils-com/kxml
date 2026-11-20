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
package com.davils.kxml.document.diff

import com.davils.kxml.core.limit.xmlLimits
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.xmlText
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.c14n.XmlCanonicalization
import com.davils.kxml.document.c14n.XmlCanonicalizer
import com.davils.kxml.document.fixture.documentOf
import com.davils.kxml.document.fixture.documentTexts
import com.davils.kxml.document.patch.XmlPatch
import com.davils.kxml.document.patch.XmlPatchOperation
import com.davils.kxml.document.patch.XmlPatchPosition
import com.davils.kxml.document.patch.XmlPatchReading
import com.davils.kxml.document.patch.XmlPatchResult
import com.davils.kxml.document.patch.XmlPatchWhitespace
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.checkAll

private const val CASES: Int = 300
private const val SMALL: Long = 30L
private const val VISITS: Long = 6L
private const val IN_PLACE: Int = 8

private val pretty: String = """
    |<a>
    |  <b/>
    |</a>
""".trimMargin()
private val prettier: String = """
    |<a>
    |  <b/>
    |  <c/>
    |</a>
""".trimMargin()

private val canonicalizer: XmlCanonicalizer = XmlCanonicalizer(XmlCanonicalization.Canonical10WithComments)

private fun canonicalOf(document: XmlDocument): String = canonicalizer.canonicalize(document).decodeToString()

private fun shouldTurnInto(old: String, new: String, diff: XmlDiff = XmlDiff()): XmlPatch {
    val patch = diff.diff(old = documentOf(old), new = documentOf(new))
    val applied = patch.apply(documentOf(old)).shouldBeInstanceOf<XmlPatchResult.Applied>()
    canonicalOf(applied.document) shouldBe canonicalOf(documentOf(new))
    val written = XmlPatch.read(patch.toDocument()).shouldBeInstanceOf<XmlPatchReading.Read>().patch
    val reapplied = written.apply(documentOf(old)).shouldBeInstanceOf<XmlPatchResult.Applied>()
    canonicalOf(reapplied.document) shouldBe canonicalOf(documentOf(new))
    return patch
}

class XmlDiffTest : FunSpec({
    context("a diff") {
        test("is empty for equal documents") {
            shouldTurnInto(old = "<a x='1'><b/>t</a>", new = "<a x='1'><b/>t</a>").operations.shouldBeEmpty()
        }

        test("changes attributes, texts, comments and instructions in place") {
            val patch = shouldTurnInto(
                old = "<a x='1' y='2' xml:lang='en'><b>t</b><!--c--><?p d?></a>",
                new = "<a x='3' z='4' xml:lang='de'><b>u</b><!--e--><?p f?></a>",
            )
            patch.operations.size shouldBe IN_PLACE
            shouldTurnInto(old = "<a x='1'/>", new = "<a x=''/>")
            shouldTurnInto(
                old = "<a xmlns:p='urn:p' p:x='1'/>",
                new = "<a xmlns:p='urn:p' p:y='2'/>",
            )
        }

        test("inserts and removes children with the white space around them") {
            shouldTurnInto(old = pretty, new = prettier)
            shouldTurnInto(old = prettier, new = pretty)
            shouldTurnInto(old = "<a><b/><c/><d/></a>", new = "<a><x/><c/><y/><z/></a>")
            shouldTurnInto(old = "<a>x<b/>y</a>", new = "<a>xy</a>")
            shouldTurnInto(old = "<a/>", new = "<a><b/>t</a>")
        }

        test("replaces elements whose name or namespaces changed") {
            shouldTurnInto(old = "<a/>", new = "<b/>")
            shouldTurnInto(old = "<r><a xmlns:p='urn:p'/></r>", new = "<r><a xmlns:p='urn:q'/></r>")
            shouldTurnInto(old = "<r><a/></r>", new = "<r><b/></r>")
        }

        test("changes the nodes around the document element") {
            shouldTurnInto(old = "<!--a--><r/><?p?>", new = "<?q?><r/><!--b--><!--c-->")
            shouldTurnInto(old = "<r/>", new = "<!--a--><r/>")
            shouldTurnInto(old = "<!--a--><!--b--><r/>", new = "<r/>")
        }

        test("turns any document into any other") {
            checkAll(CASES, documentTexts, documentTexts) { old, new -> shouldTurnInto(old, new) }
        }
    }

    context("a patch written as a document") {
        test("reads back as the same operations") {
            val operations = listOf(
                XmlPatchOperation.Add(
                    selector = "a",
                    content = listOf(xmlText("t")),
                    position = XmlPatchPosition.Prepend,
                    namespaces = mapOf("" to "urn:d"),
                ),
                XmlPatchOperation.Add("a", listOf(xmlText("t")), XmlPatchPosition.Before),
                XmlPatchOperation.Add("a", listOf(xmlText("t"))),
                XmlPatchOperation.AddAttribute(
                    selector = "a",
                    name = "p:b",
                    value = "",
                    namespaces = mapOf("p" to "urn:p"),
                ),
                XmlPatchOperation.AddNamespace(selector = "a", prefix = "q", uri = "urn:q"),
                XmlPatchOperation.Replace("a", emptyList()),
                XmlPatchOperation.Remove("a", XmlPatchWhitespace.Both),
                XmlPatchOperation.Remove("a", XmlPatchWhitespace.After),
                XmlPatchOperation.Remove("a"),
            )
            val written = XmlPatch(operations).toDocument()

            XmlPatch.read(written).shouldBeInstanceOf<XmlPatchReading.Read>().patch shouldBe XmlPatch(operations)
        }
    }

    context("the work of a diff") {
        test("grows coarser instead of failing when an alignment exceeds it") {
            val old = "<r>" + "<a/>".repeat(SMALL.toInt()) + "</r>"
            val new = "<r>" + "<b/>".repeat(SMALL.toInt()) + "</r>"
            val coarse = shouldTurnInto(old, new, XmlDiff(xmlLimits { maximumDiffWork = SMALL * SMALL / 2 }))

            coarse.operations.size shouldBeLessThanOrEqual 1
        }

        test("stops a diff that visits too many nodes") {
            val diff = XmlDiff(xmlLimits { maximumDiffWork = VISITS })
            val error = shouldThrow<XmlDocumentLimitException> {
                diff.diff(old = documentOf("<r><a/><b/></r>"), new = documentOf("<r><a/><c/></r>"))
            }

            error.limit shouldBe "maximumDiffWork"
            diff shouldNotBe XmlDiff()
            diff shouldBe XmlDiff(xmlLimits { maximumDiffWork = VISITS })
            diff.hashCode() shouldBe XmlDiff(xmlLimits { maximumDiffWork = VISITS }).hashCode()
            diff.toString() shouldBe "XmlDiff(work=6)"
            diff.equals("x") shouldBe false
        }
    }
})
