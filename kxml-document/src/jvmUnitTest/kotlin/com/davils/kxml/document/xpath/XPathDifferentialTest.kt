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

import com.davils.kxml.document.fixture.SAMPLE
import com.davils.kxml.document.fixture.numberOf
import com.davils.kxml.document.fixture.sampleNamespaces
import com.davils.kxml.document.fixture.textOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

private const val CASES: Int = 500
private const val PATH: String = "PATH"
private const val NINE: Double = 9.0
private const val MOST_STEPS: Int = 3
private const val DISALLOW_DOCTYPE: String = "http://apache.org/xml/features/disallow-doctype-decl"

private val expressions: List<String> = listOf(
    "count(//node())",
    "count(//*)",
    "count(//@*)",
    "count(//text())",
    "count(//comment())",
    "count(//processing-instruction())",
    "name(/*)",
    "local-name(//p:i)",
    "namespace-uri(//p:i)",
    "string(//d:n[2])",
    "string(//d:i)",
    "string(/)",
    "normalize-space(/)",
    "concat(//d:n, '|', //@n)",
    "sum(//d:n[number(.) = number(.)])",
    "1 div 3",
    "0.1 + 0.2",
    "-0",
    "1 div 0",
    "0 div 0",
    "123456789012345678901234567890",
    "0.000001",
    "substring('12345', 1.5, 2.6)",
    "substring('12345', -42, 1 div 0)",
    "substring-before('1999/04/01', '/')",
    "translate('bar', 'abc', 'ABC')",
    "string-length(/)",
    "name(//d:i/following::*[2])",
    "name(//d:e/preceding::*[1])",
    "name(//d:e/preceding-sibling::*[last()])",
    "name(//d:i/text()/ancestor::*[2])",
    "count(//d:n[. > 0])",
    "//d:n = //d:n",
    "//d:n != 'abc'",
    "//d:n < //@n",
    "3.5 = //d:n",
    "count(//*[lang('en')])",
    "lang('en')",
    "round(-2.5)",
    "round(2.5)",
    "5 mod -2",
    "number('  12  ')",
    "number('1.')",
    "number('-.5')",
    "number('1e3')",
    "boolean(//d:none)",
    "count(//d:n | //p:i | //@*)",
    "name((//d:n | //p:i)[1])",
    "string((//d:n)[last()])",
    "count(/d:r/*[position() mod 2 = 0])",
)

private val axes: List<String> = listOf(
    "child::", "descendant::", "descendant-or-self::", "parent::", "ancestor::", "ancestor-or-self::", "following::",
    "preceding::", "following-sibling::", "preceding-sibling::", "self::", "attribute::",
)
private val tests: List<String> = listOf("*", "node()", "text()", "comment()", "processing-instruction()", "d:n", "p:i")
private val predicates: List<String> =
    listOf("", "[1]", "[last()]", "[position() mod 2 = 1]", "[@*]", "[not(*)]", "[. = 'two']", "[2][1]")
private val wrappers: List<String> = listOf(
    "count($PATH)",
    "boolean($PATH)",
    "count($PATH | //d:n)",
    "$PATH = 'one'",
    "count($PATH/..)",
    "count($PATH/following-sibling::node())",
    "count($PATH/ancestor::*)",
    "sum($PATH[number(.) = number(.)])",
)

private val reverseAxes: Set<String> =
    setOf("parent::", "ancestor::", "ancestor-or-self::", "preceding::", "preceding-sibling::")
private val unpositioned: List<String> = listOf("", "[@*]", "[not(*)]", "[. = 'two']")

private val allSteps: List<String> = axes.flatMap { axis ->
    val allowed = unpositioned.takeIf { axis in reverseAxes } ?: predicates
    tests.flatMap { test -> allowed.map { predicate -> "$axis$test$predicate" } }
}

private val steps: Arb<String> = Arb.element(allSteps)

private val paths: Arb<List<String>> = Arb.list(steps, 1..MOST_STEPS).filter(::hasNoSiblingOfAttribute)

private val queries: Arb<String> = paths.flatMap { path ->
    val located = "//" + path.joinToString(separator = "/")
    Arb.element(wrappers).map { wrapper -> wrapper.replace(oldValue = PATH, newValue = located) }
}

private val withoutProlog: String = SAMPLE.replace(oldValue = "<!--c0-->", newValue = "")
private val inner: String = withoutProlog.replace(oldValue = "<?tail t?>", newValue = "")

private fun hasNoSiblingOfAttribute(path: List<String>): Boolean = path.zipWithNext().none { (step, next) ->
    step.startsWith("attribute::") && next.contains("sibling::")
}

private fun stringOf(expression: String): String = "string($expression)"

private fun javaDocument(text: String = SAMPLE): Document {
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = true
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    factory.setFeature(DISALLOW_DOCTYPE, true)
    val builder = factory.newDocumentBuilder()
    return StringReader(text).use { reader -> builder.parse(InputSource(reader)) }
}

private fun javaString(document: Document, expression: String): String {
    val xpath = XPathFactory.newInstance().newXPath()
    xpath.namespaceContext = object : NamespaceContext {
        override fun getNamespaceURI(prefix: String): String = sampleNamespaces[prefix] ?: XMLConstants.NULL_NS_URI

        override fun getPrefix(namespaceURI: String): String? = null

        override fun getPrefixes(namespaceURI: String): Iterator<String> = emptyList<String>().iterator()
    }
    return xpath.evaluate(stringOf(expression), document, XPathConstants.STRING) as String
}

class XPathDifferentialTest : FunSpec({
    context("an expression") {
        test("gives the string javax.xml.xpath gives") {
            val document = javaDocument()
            val differences = expressions.filter { expression ->
                textOf(stringOf(expression)) != javaString(document, expression)
            }

            differences.shouldBeEmpty()
        }

        test("counts by proximity and orders by document where javax.xml.xpath does not") {
            val document = javaDocument()
            val farthest = "count(//ancestor::node()[last()][1])"
            val first = "name(//following-sibling::processing-instruction()[last()])"

            numberOf(farthest) shouldBe 1.0
            javaString(document, farthest) shouldBe "7"
            textOf(first) shouldBe "pi"
            javaString(document, first) shouldBe "tail"
            val siblings = "count(//preceding::node()/following-sibling::node())"
            numberOf(siblings) shouldBe NINE
            javaString(document, siblings) shouldBe "7"
            val attributes = "count(//attribute::*/following-sibling::node())"
            numberOf(attributes) shouldBe 0.0
            javaString(document, attributes) shouldNotBe "0"
        }

        test("built from random steps selects the nodes javax.xml.xpath selects") {
            val document = javaDocument(inner)

            checkAll(CASES, queries) { expression ->
                textOf(stringOf(expression), text = inner) shouldBe javaString(document, expression)
            }
        }
    }
})
