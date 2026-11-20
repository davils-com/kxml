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
package com.davils.kxml.document.patch

import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.limit.xmlLimits
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.xmlComment
import com.davils.kxml.core.node.xmlElement
import com.davils.kxml.core.node.xmlText
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.fixture.documentOf
import com.davils.kxml.document.fixture.hostileText
import com.davils.kxml.document.fixture.shouldBeShownSafely
import com.davils.kxml.document.fixture.shouldFailSafely
import com.davils.kxml.document.xpath.XPath
import com.davils.kxml.document.xpath.XPathValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.checkAll

private const val TARGET: String =
    "<doc xmlns:p='urn:p' a='1'><p:e p:x='2'/>  <f/>text<!--c--><?pi d?></doc><!--tail-->"

private fun failure(operations: String, target: String = TARGET): XmlPatchError {
    val failed = patched(target, operations).shouldBeInstanceOf<XmlPatchResult.Failed>()
    return failed.error
}

private fun appliedTo(target: String, operations: String): XmlDocument {
    val applied = patched(target, operations).shouldBeInstanceOf<XmlPatchResult.Applied>()
    return applied.document
}

private fun refusal(operations: String): XmlPatchError {
    val refused = XmlPatch.read(documentOf("<diff>$operations</diff>")).shouldBeInstanceOf<XmlPatchReading.Refused>()
    return refused.error
}

private val failures: Map<String, XmlPatchErrorCode> = mapOf(
    "<remove sel='doc/missing'/>" to XmlPatchErrorCode.UnlocatedNode,
    "<remove sel='doc/node()'/>" to XmlPatchErrorCode.UnlocatedNode,
    "<remove sel='doc/['/>" to XmlPatchErrorCode.InvalidAttributeValue,
    "<remove sel='count(doc)'/>" to XmlPatchErrorCode.InvalidAttributeValue,
    "<remove sel='q:doc'/>" to XmlPatchErrorCode.InvalidNamespacePrefix,
    "<remove sel='doc'/>" to XmlPatchErrorCode.InvalidRootElementOperation,
    "<add sel='doc' pos='after'><x/></add>" to XmlPatchErrorCode.InvalidRootElementOperation,
    "<add sel='doc' pos='before'>text</add>" to XmlPatchErrorCode.InvalidXmlPrologOperation,
    "<add sel='doc/@a'><x/></add>" to XmlPatchErrorCode.InvalidNodeTypes,
    "<add sel='doc/@a' pos='after'><x/></add>" to XmlPatchErrorCode.InvalidNodeTypes,
    "<add sel='doc' type='@xmlns'>urn:evil</add>" to XmlPatchErrorCode.InvalidAttributeValue,
    "<add sel='doc' type='@a'>3</add>" to XmlPatchErrorCode.InvalidAttributeValue,
    "<add sel='doc' type='@q:b'>3</add>" to XmlPatchErrorCode.InvalidNamespacePrefix,
    "<add sel='doc' type='namespace::p'>urn:q</add>" to XmlPatchErrorCode.InvalidNamespacePrefix,
    "<add sel='doc' type='namespace::q'></add>" to XmlPatchErrorCode.InvalidNamespaceUri,
    "<replace sel='/'><x/></replace>" to XmlPatchErrorCode.InvalidNodeTypes,
    "<replace sel='doc/f'>text</replace>" to XmlPatchErrorCode.InvalidNodeTypes,
    "<replace sel='doc/f'><x/><y/></replace>" to XmlPatchErrorCode.InvalidNodeTypes,
    "<replace sel='doc/comment()'><x/></replace>" to XmlPatchErrorCode.InvalidNodeTypes,
    "<replace sel='doc/@a'><x/></replace>" to XmlPatchErrorCode.InvalidNodeTypes,
    "<replace sel='doc/text()[2]'><x/></replace>" to XmlPatchErrorCode.InvalidNodeTypes,
    "<replace sel='doc/p:e/namespace::p'>urn:q</replace>" to XmlPatchErrorCode.InvalidNamespacePrefix,
    "<replace sel='doc/namespace::p'></replace>" to XmlPatchErrorCode.InvalidNamespaceUri,
    "<replace sel='doc/namespace::p'><x/></replace>" to XmlPatchErrorCode.InvalidNodeTypes,
    "<remove sel='doc/namespace::p'/>" to XmlPatchErrorCode.InvalidNamespacePrefix,
    "<remove sel='doc/p:e/namespace::p'/>" to XmlPatchErrorCode.InvalidNamespacePrefix,
    "<remove sel='doc/namespace::p' ws='after'/>" to XmlPatchErrorCode.InvalidAttributeValue,
    "<remove sel='doc/@a' ws='after'/>" to XmlPatchErrorCode.InvalidAttributeValue,
    "<remove sel='doc/text()[1]' ws='after'/>" to XmlPatchErrorCode.InvalidAttributeValue,
    "<remove sel='/'/>" to XmlPatchErrorCode.InvalidNodeTypes,
    "<remove sel='doc/*[1]' ws='before'/>" to XmlPatchErrorCode.InvalidWhitespaceDirective,
    "<remove sel='doc/f' ws='after'/>" to XmlPatchErrorCode.InvalidWhitespaceDirective,
    "<remove sel='/comment()' ws='before'/>" to XmlPatchErrorCode.InvalidWhitespaceDirective,
)

private val refusals: Map<String, XmlPatchErrorCode> = mapOf(
    "<move sel='doc'/>" to XmlPatchErrorCode.InvalidDiffFormat,
    "<remove/>" to XmlPatchErrorCode.InvalidDiffFormat,
    "text" to XmlPatchErrorCode.InvalidDiffFormat,
    "<add sel='doc' pos='middle'/>" to XmlPatchErrorCode.InvalidAttributeValue,
    "<add sel='doc' type='@a' pos='before'>1</add>" to XmlPatchErrorCode.InvalidAttributeValue,
    "<add sel='doc' type='@a'><x/></add>" to XmlPatchErrorCode.InvalidAttributeValue,
    "<add sel='doc' type='child::a'>1</add>" to XmlPatchErrorCode.InvalidAttributeValue,
    "<add sel='doc' type='@1a'>1</add>" to XmlPatchErrorCode.InvalidAttributeValue,
    "<add sel='doc' type='namespace::xml'>urn:x</add>" to XmlPatchErrorCode.InvalidAttributeValue,
    "<remove sel='doc' ws='around'/>" to XmlPatchErrorCode.InvalidAttributeValue,
)

private const val HOSTILE_CASES: Int = 100
private const val SELECTOR: String = "/a"

private fun appliedTo(text: String, limits: XmlLimits, vararg operations: XmlPatchOperation): XmlPatchResult =
    XmlPatch(operations.toList(), limits).apply(documentOf(text))

private fun limitOf(text: String, limits: XmlLimits, vararg operations: XmlPatchOperation): String {
    val error = shouldThrow<XmlDocumentLimitException> { appliedTo(text, limits, *operations) }
    return error.limit
}

class XmlPatchErrorTest : FunSpec({
    context("an operation") {
        test("fails with the error condition of RFC 5261") {
            failures.forEach { (operation, code) -> failure(operation).code shouldBe code }
        }

        test("leaves the patch atomic and names the operation that failed") {
            val error = failure("<remove sel='doc/f'/><remove sel='doc/f'/>")

            error.code shouldBe XmlPatchErrorCode.UnlocatedNode
            error.operation shouldBe 1
            error.toString() shouldContain "unlocated-node at 1"
            XmlPatchResult.Failed(error).toString() shouldContain "Failed"
        }

        test("resolves unprefixed element names through the default namespace of the diff") {
            val target = "<doc xmlns='urn:d'><a/></doc>"
            val diff = documentOf("<diff xmlns='urn:d'><remove sel='doc/a'/></diff>")
            val patch = XmlPatch.read(diff).shouldBeInstanceOf<XmlPatchReading.Read>().patch

            patch.apply(documentOf(target)).shouldBeInstanceOf<XmlPatchResult.Applied>()
            patched(target, "<remove sel='doc/a'/>").shouldBeInstanceOf<XmlPatchResult.Failed>()
        }

        test("declares the namespaces of added content that the target lacks") {
            val result = patched(
                target = "<doc xmlns='urn:d'/>",
                operations = "<add sel='*'><x/><q:y xmlns:q='urn:q' q:a='1'/></add>",
            )
            val document = result.shouldBeInstanceOf<XmlPatchResult.Applied>().document
            val qualified = XPath.parse("count(/*/q:y/@q:a)", namespaces = mapOf("q" to "urn:q"))

            XPath.parse("count(/*/x)").evaluate(document) shouldBe XPathValue.Number(1.0)
            qualified.evaluate(document) shouldBe XPathValue.Number(1.0)
        }

        test("chooses the target prefix that comes before the prefix of the diff") {
            val target = "<doc xmlns:x='urn:a' xmlns:y='urn:a'/>"

            val addedElement = "<add sel='doc' xmlns:xx='urn:a'><xx:e xx:b='1'/></add>"
            val addedAttribute = "<add sel='doc' type='@a:b' xmlns:a='urn:a'>1</add>"
            val element = appliedTo(target = target, operations = addedElement).root.elements.single()
            val attribute = appliedTo(target = target, operations = addedAttribute).root.attributes.single()
            val fresh = appliedTo(target = "<doc/>", operations = addedAttribute).root.namespaces.single()

            element.name.prefix shouldBe "x"
            attribute.name.prefix shouldBe "x"
            fresh.prefix shouldBe "a"
        }
    }

    context("a diff document") {
        test("is refused when it is no patch of RFC 5261") {
            refusals.forEach { (operations, code) -> refusal(operations).code shouldBe code }
            XmlPatch.read(documentOf("<diff><!--c--> <?pi?></diff>")).toString() shouldBe "Read(operations=0)"
            XmlPatchReading.Refused(refusal("<move sel='a'/>")).toString() shouldContain "invalid-diff-format"
        }

        test("is bounded by maximumPatchOperations") {
            val limits = xmlLimits { maximumPatchOperations = 1 }
            val diff = documentOf("<diff><remove sel='a'/><remove sel='b'/></diff>")
            val single = XmlPatchOperation.Remove("a")

            XmlPatch(listOf(single), limits).operations shouldBe listOf(single)
            val one = XmlPatch.read(documentOf("<diff><remove sel='a'/></diff>"), limits)
            val read = one.shouldBeInstanceOf<XmlPatchReading.Read>()
            read.patch.operations.size shouldBe 1
            shouldThrow<XmlDocumentLimitException> { XmlPatch.read(diff, limits) }
            shouldThrow<XmlDocumentLimitException> { XmlPatch(listOf(single, single), limits) }
        }

        test("never produces a document deeper or larger than a reader would accept") {
            val child = XmlPatchOperation.Add(SELECTOR, content = listOf(xmlElement(XmlName("b"))))
            val nested = listOf(xmlElement(XmlName("b")) { element(XmlName("c")) })
            val grandchild = XmlPatchOperation.Add(SELECTOR, content = nested)
            val shallow = xmlLimits { maximumDepth = 2 }
            val few = xmlLimits { maximumElements = 2 }
            val attribute = XmlPatchOperation.AddAttribute(selector = SELECTOR, name = "y", value = "2")

            appliedTo("<a/>", shallow, child).shouldBeInstanceOf<XmlPatchResult.Applied>()
            appliedTo("<a/>", few, child).shouldBeInstanceOf<XmlPatchResult.Applied>()
            limitOf("<a/>", shallow, grandchild) shouldBe "maximumDepth"
            limitOf("<a x='1'/>", xmlLimits { maximumAttributes = 1 }, attribute) shouldBe "maximumAttributes"
            limitOf("<a/>", few, child, child) shouldBe "maximumElements"
        }

        test("is applied within maximumQuerySteps") {
            val limits = xmlLimits { maximumQuerySteps = 10L }
            val patch = XmlPatch(listOf(XmlPatchOperation.Remove("doc/f")), limits)

            shouldThrow<XmlDocumentLimitException> { patch.apply(documentOf(TARGET)) }
        }
    }

    context("operations built in code") {
        test("compare by kind, selector, namespaces and content") {
            val add = XmlPatchOperation.Add("doc", listOf(xmlElement(XmlName("x"))), XmlPatchPosition.Prepend)

            val same = XmlPatchOperation.Add("doc", listOf(xmlElement(XmlName("x"))), XmlPatchPosition.Prepend)
            add shouldBe same
            add.hashCode() shouldBe same.hashCode()
            add shouldNotBe XmlPatchOperation.Replace("doc", listOf(xmlElement(XmlName("x"))))
            add.equals("doc") shouldBe false
            add.toString() shouldBe "Add(doc)"
            val attribute = XmlPatchOperation.AddAttribute(selector = "doc", name = "a", value = "1")
            attribute shouldBe XmlPatchOperation.AddAttribute(selector = "doc", name = "a", value = "1")
            val namespace = XmlPatchOperation.AddNamespace(selector = "doc", prefix = "p", uri = "urn:p")
            namespace shouldNotBe XmlPatchOperation.AddNamespace(selector = "doc", prefix = "p", uri = "urn:q")
            XmlPatchOperation.Remove("doc", XmlPatchWhitespace.Both).whitespace shouldBe XmlPatchWhitespace.Both
            XmlPatchOperation.Replace("doc", listOf(xmlText("x"), xmlComment("c"))).content.size shouldBe 2
            XmlPatch(listOf(add)) shouldBe XmlPatch(listOf(add))
            XmlPatch(listOf(add)).hashCode() shouldBe XmlPatch(listOf(add)).hashCode()
            XmlPatch(listOf(add)).equals(add) shouldBe false
            XmlPatch(listOf(add)).toString() shouldBe "XmlPatch(operations=1)"
            XmlPatchErrorCode.UnlocatedNode.element shouldBe "unlocated-node"
        }

        test("refuse names and namespaces they cannot use") {
            shouldThrow<IllegalArgumentException> { XmlPatchOperation.Remove("a", namespaces = mapOf("1" to "urn:x")) }
            shouldThrow<IllegalArgumentException> { XmlPatchOperation.Remove("a", namespaces = mapOf("p" to "")) }
            shouldThrow<IllegalArgumentException> {
                XmlPatchOperation.AddAttribute(selector = "a", name = "a:b:c", value = "1")
            }
            shouldThrow<IllegalArgumentException> {
                XmlPatchOperation.AddNamespace(selector = "a", prefix = "xmlns", uri = "urn:x")
            }
        }
    }

    context("the sinks of a patch") {
        test("show hostile selectors, names and prefixes escaped and never a value") {
            checkAll(HOSTILE_CASES, hostileText) { text ->
                val operation = XmlPatchOperation.Remove(text)
                operation.shouldBeShownSafely()
                val target = documentOf("<a/>")
                val failed = XmlPatch(listOf(operation)).apply(target)
                failed.shouldBeShownSafely()
                val literal = XmlPatchOperation.Remove("/a[. = '${text.filterNot { char -> char == '\'' }}']")
                val applied = XmlPatch(listOf(literal)).apply(target)
                applied.shouldBeShownSafely()
                runCatching { XmlPatchOperation.AddAttribute(selector = SELECTOR, name = text, value = "v") }
                    .shouldFailSafely()
                runCatching { XmlPatchOperation.AddNamespace(selector = SELECTOR, prefix = text, uri = "u") }
                    .shouldFailSafely()
                runCatching { XmlPatchOperation.Remove(selector = SELECTOR, namespaces = mapOf(text to "u")) }
                    .shouldFailSafely()
            }
            val secret = XmlPatchOperation.AddAttribute(selector = SELECTOR, name = "token", value = "hunter2")
            secret.toString() shouldNotContain "hunter2"
        }
    }
})
