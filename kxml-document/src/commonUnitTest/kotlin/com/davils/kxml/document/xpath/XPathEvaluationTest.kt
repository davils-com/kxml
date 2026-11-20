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
package com.davils.kxml.document.xpath

import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.limit.xmlLimits
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.xmlElement
import com.davils.kxml.core.xml
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.fixture.GROWTH_FACTOR
import com.davils.kxml.document.fixture.SAMPLE
import com.davils.kxml.document.fixture.documentOf
import com.davils.kxml.document.fixture.sampleNamespaces
import com.davils.kxml.document.fixture.shouldGrowLinearly
import com.davils.kxml.document.fixture.valueOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val NEGATIVE_ZERO: Double = -0.0
private const val FEW: Int = 2
private const val SMALL: Int = 1_000
private const val LEVELS: Int = 40
private const val DECLARATIONS: Int = 30
private val variable: XmlName = XmlName("v")

private const val GROWTH_SIZE: Int = 500
private const val DEPTH: Int = 50

private fun chain(depth: Int): String = "<r>" + "<a>".repeat(depth) + "<b/>" + "</a>".repeat(depth) + "</r>"

private fun deepDocumentOf(text: String): XmlDocument {
    val reader = xml { limits { maximumDepth = XmlLimits.HARD_DEPTH } }
    val parsed = reader.parse(text).shouldBeInstanceOf<XmlParseResult.Parsed>()
    return parsed.document
}

private fun flat(count: Int): String = "<r>" + "<a>x</a><b>x</b>".repeat(count) + "</r>"

private fun nested(levels: Int, declarations: Int): String {
    val open = (0..<levels).joinToString(separator = "") { level ->
        val bindings = (0..<declarations).joinToString(separator = " ") { index ->
            "xmlns:p${level}x$index='urn:$index'"
        }
        "<e $bindings>"
    }
    return open + "</e>".repeat(levels)
}

class XPathEvaluationTest : FunSpec({
    context("select") {
        test("returns the nodes with their kinds, names, values and ranges") {
            val query = XPath.parse("/d:r/p:i | //@n | //comment()", namespaces = sampleNamespaces)
            val nodes = query.select(documentOf(SAMPLE))

            nodes.map(XPathNode::kind) shouldContainExactly listOf(
                XPathNodeKind.Comment,
                XPathNodeKind.Element,
                XPathNodeKind.Attribute,
                XPathNodeKind.Comment,
            )
            nodes[1].value shouldBe "one"
            nodes[1].name?.namespace shouldBe "urn:p"
            nodes[1].range.shouldNotBeNull().start shouldBe SAMPLE.indexOf("<p:i")
            nodes[2].value shouldBe "2"
            nodes[0].name.shouldBeNull()
        }

        test("leaves out the ranges of a document built in code and of namespace nodes") {
            val document = XmlDocument(xmlElement(XmlName("a")) { text("x") })

            val text = XPath.parse("/a/text()").select(document)
            val namespace = XPath.parse("/a/namespace::*").select(document)
            val root = XPath.parse("/").select(document)

            text.single().range.shouldBeNull()
            namespace.single().range.shouldBeNull()
            root.single().kind shouldBe XPathNodeKind.Root
        }

        test("refuses an expression that gives no node-set") {
            val query = XPath.parse("count(//a)")

            query.isNodeSet shouldBe false
            shouldThrow<IllegalStateException> { query.select(documentOf("<a/>")) }
        }
    }

    context("variables") {
        test("are bound to strings, numbers and booleans by their expanded names") {
            val document = documentOf("<a/>")
            val prefixed = XmlName(local = "v", namespace = "urn:v")
            val query = XPath.parse("concat(\$v, \$w:v, \$t)", namespaces = mapOf("w" to "urn:v"))
            val variables = mapOf(
                variable to XPathValue.Text("x"),
                prefixed to XPathValue.Number(2.0),
                XmlName("t") to XPathValue.Truth(true),
            )

            query.evaluate(document, variables) shouldBe XPathValue.Text("x2true")
        }

        test("are refused when they are not bound or hold a node-set") {
            val document = documentOf("<a/>")
            val nodes = XPath.parse("/a").evaluate(document)

            shouldThrow<IllegalArgumentException> { XPath.parse("\$v").evaluate(document) }
            shouldThrow<IllegalArgumentException> { XPath.parse("\$v").evaluate(document, mapOf(variable to nodes)) }
        }
    }

    context("the limits of an evaluation") {
        test("stop it after too many steps") {
            val limits = xmlLimits { maximumQuerySteps = SMALL.toLong() }
            val error = shouldThrow<XmlDocumentLimitException> {
                XPath.parse("count(//a)", limits = limits).evaluate(documentOf(flat(SMALL)))
            }

            error.limit shouldBe "maximumQuerySteps"
            error.message.shouldNotBeNull() shouldNotContain "<a>"
        }

        test("stop it at too many nodes in a node-set") {
            val limits = xmlLimits { maximumQueryResults = FEW }

            XPath.parse("count(/r/a)", limits = limits).evaluate(documentOf(flat(FEW))) shouldBe XPathValue.Number(2.0)
            val error = shouldThrow<XmlDocumentLimitException> {
                XPath.parse("count(/r/a)", limits = limits).evaluate(documentOf(flat(FEW + 1)))
            }
            error.limit shouldBe "maximumQueryResults"
            shouldThrow<XmlDocumentLimitException> {
                XPath.parse("count(/r/a[1]/following::node())", limits = limits).evaluate(documentOf(flat(FEW)))
            }
        }

        test("stop it at too long a string") {
            val limits = xmlLimits { maximumQueryStringLength = FEW + 1 }
            val document = documentOf(flat(FEW))

            XPath.parse("concat('a', 'b', 'c')", limits = limits).evaluate(document) shouldBe XPathValue.Text("abc")
            shouldThrow<XmlDocumentLimitException> {
                XPath.parse("concat('a', 'b', 'cd')", limits = limits).evaluate(document)
            }
            shouldThrow<XmlDocumentLimitException> { XPath.parse("string(/)", limits = limits).evaluate(document) }
        }

        test("keep the namespace axis from exploding") {
            val document = documentOf(nested(levels = LEVELS, declarations = DECLARATIONS))
            val limits = xmlLimits { maximumQuerySteps = SMALL.toLong() }

            shouldThrow<XmlDocumentLimitException> {
                XPath.parse("count(//namespace::*)", limits = limits).evaluate(document)
            }
            val all = XPath.parse("count(//namespace::*)").evaluate(document)
            all shouldNotBe XPathValue.Number(0.0)
        }

        test("let concat over every node stop at its budget") {
            val limits = xmlLimits { maximumQuerySteps = SMALL.toLong() }
            val repeated = List(SMALL) { "/" }.joinToString(separator = ", ")

            shouldThrow<XmlDocumentLimitException> {
                XPath.parse("concat($repeated)", limits = limits).evaluate(documentOf(flat(SMALL)))
            }
        }
    }

    context("the cost of an evaluation") {
        test("grows linearly when node-sets are compared") {
            val query = XPath.parse("//a = //b")
            val small = flat(GROWTH_SIZE)
            val large = flat(GROWTH_SIZE * GROWTH_FACTOR)
            val documents = mapOf(small to documentOf(small), large to documentOf(large))

            shouldGrowLinearly(small, large) { text -> query.evaluate(documents.getValue(text)) }
        }

        test("grows linearly for descendants of descendants") {
            val query = XPath.parse("count(//a//b)")
            val small = chain(DEPTH)
            val large = chain(DEPTH * GROWTH_FACTOR)
            val documents = mapOf(small to deepDocumentOf(small), large to deepDocumentOf(large))

            shouldGrowLinearly(small, large) { text -> query.evaluate(documents.getValue(text)) }
        }
    }

    context("values") {
        test("are equal by content and never show it") {
            XPathValue.Text("x") shouldBe XPathValue.Text("x")
            XPathValue.Text("x").hashCode() shouldBe XPathValue.Text("x").hashCode()
            XPathValue.Text("x") shouldNotBe XPathValue.Text("y")
            XPathValue.Text("secret").toString() shouldBe "Text(length=6)"
            XPathValue.Number(Double.NaN) shouldBe XPathValue.Number(Double.NaN)
            XPathValue.Number(0.0) shouldNotBe XPathValue.Number(NEGATIVE_ZERO)
            XPathValue.Number(1.0).hashCode() shouldBe XPathValue.Number(1.0).hashCode()
            XPathValue.Number(1.0).toString() shouldBe "Number"
            XPathValue.Truth(true) shouldBe XPathValue.Truth(true)
            XPathValue.Truth(true) shouldNotBe XPathValue.Truth(false)
            XPathValue.Truth(true).hashCode() shouldBe true.hashCode()
            XPathValue.Truth(true).toString() shouldBe "Truth"
            XPathValue.Text("x").equals(XPathValue.Truth(true)) shouldBe false
            XPathValue.Number(1.0).equals(XPathValue.Truth(true)) shouldBe false
            XPathValue.Truth(true).equals(XPathValue.Number(1.0)) shouldBe false
        }

        test("hold node-sets that compare by their nodes") {
            val document = documentOf(SAMPLE)
            val first = valueOf("//d:n").shouldBeInstanceOf<XPathValue.NodeSet>()
            val second = XPath.parse("//d:n", namespaces = sampleNamespaces).evaluate(document)

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
            first.toString() shouldBe "NodeSet(size=3)"
            first shouldNotBe valueOf("//p:i")
            first.equals(XPathValue.Truth(true)) shouldBe false
            first.nodes[0] shouldBe first.nodes[0]
            first.nodes[0] shouldNotBe first.nodes[1]
            val secondNodes = second.shouldBeInstanceOf<XPathValue.NodeSet>().nodes
            first.nodes[0].hashCode() shouldBe secondNodes[0].hashCode()
            first.nodes[0].equals("n") shouldBe false
            first.nodes[0].toString() shouldBe "XPathNode(Element, n)"
            val root = valueOf("/").shouldBeInstanceOf<XPathValue.NodeSet>()
            root.nodes[0].toString() shouldBe "XPathNode(Root)"
        }
    }
})
