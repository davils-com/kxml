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

import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.limit.xmlLimits
import com.davils.kxml.document.fixture.documentOf
import com.davils.kxml.document.fixture.hostileText
import com.davils.kxml.document.fixture.numberOf
import com.davils.kxml.document.fixture.sampleNamespaces
import com.davils.kxml.document.fixture.shouldBeSafeToShow
import com.davils.kxml.document.fixture.shouldBeShownSafely
import com.davils.kxml.document.fixture.textOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.checkAll

private const val NEEDS_NAME: String = "a variable reference needs a name"
private const val OTHER_ARGUMENTS: String = "the function takes other arguments"
private const val NEEDS_NODE_SETS: String = "the function needs a node-set"
private const val NEEDS_NODE_SET: String = "the operand must be a node-set"
private const val NEEDS_TEST: String = "a step needs a node test"
private const val PREFIXED: String = "//p:a"
private const val URN_P: String = "urn:p"
private const val TERMS_PER_LEVEL: Int = 4
private const val CHILDREN: Double = 6.0
private const val NUMBERS: Double = 3.0
private const val ONE_AND_A_HALF: Double = 1.5
private const val SHALLOW: Int = 3
private const val SHORT: Int = 4
private const val CASES: Int = 200

private val refused: Map<String, String> = mapOf(
    "1 ! 2" to "an exclamation mark needs an equals sign",
    "1 !" to "an exclamation mark needs an equals sign",
    "1 :" to "a single colon stands outside a name",
    "\$:a" to NEEDS_NAME,
    "count(., .)" to OTHER_ARGUMENTS,
    "count(1 or 2)" to NEEDS_NODE_SETS,
    "count(1 and 2)" to NEEDS_NODE_SETS,
    "count(1 = 2)" to NEEDS_NODE_SETS,
    "count(1 + 2)" to NEEDS_NODE_SETS,
    "count(-1)" to NEEDS_NODE_SETS,
    "count('a')" to NEEDS_NODE_SETS,
    "sum(\$v)" to NEEDS_NODE_SETS,
    "count(//a | //b)/a" to NEEDS_NODE_SET,
    "'abc" to "a literal is not closed",
    "\$" to NEEDS_NAME,
    "\$1" to NEEDS_NAME,
    "a:" to "a prefix needs a local name",
    "a:1" to "a prefix needs a local name",
    "#" to "an unexpected character",
    "a : b" to "a single colon stands outside a name",
    "1 foo 2" to "a name after an operand must be and, or, mod or div",
    "child a" to "a name after an operand must be and, or, mod or div",
    "sideways::a" to "the axis is not one of XPath",
    "foo()" to "the function is not one of the core library",
    "p:count(.)" to "the function is not one of the core library",
    "q:a" to "the prefix is not declared",
    "\$q:a" to "the prefix is not declared",
    "count(1)" to NEEDS_NODE_SETS,
    "count()" to OTHER_ARGUMENTS,
    "concat('a')" to OTHER_ARGUMENTS,
    "1 | 2" to NEEDS_NODE_SET,
    "//a | 2" to NEEDS_NODE_SET,
    "'a'[1]" to NEEDS_NODE_SET,
    "'a'/b" to NEEDS_NODE_SET,
    "\$v//b" to NEEDS_NODE_SET,
    "(1" to "a parenthesis is not closed",
    "a[1" to "a predicate needs a closing bracket",
    "count(." to "a function call needs a closing parenthesis",
    "count(" to NEEDS_TEST,
    "processing-instruction(1)" to "a node type takes no argument",
    "@" to NEEDS_TEST,
    "child::" to NEEDS_TEST,
    "1 2" to "the expression goes on after its end",
    "a/" to NEEDS_TEST,
    "a//" to NEEDS_TEST,
)

class XPathParseTest : FunSpec({
    context("an expression") {
        test("is read token by token as section 3.7 of XPath 1.0 disambiguates them") {
            numberOf("count(/d:r/child :: *)") shouldBe CHILDREN
            numberOf("count( //d:n )") shouldBe NUMBERS
            numberOf(".5 + 1.") shouldBe ONE_AND_A_HALF
            numberOf("count(..)") shouldBe 0.0
            numberOf("count(/d:r/d:i/.)") shouldBe 1.0
            numberOf("count(/*/div)") shouldBe 0.0
            numberOf("count(//d:n[. = 3.5 or . = -1])") shouldBe 2.0
            numberOf("count(/d:r/d:i/processing-instruction ( 'pi' ))") shouldBe 1.0
            textOf("\"a'b\"") shouldBe "a'b"
            textOf("'a\"b'") shouldBe "a\"b"
            numberOf("count(/)") shouldBe 1.0
        }

        test("is refused with the reason and the offset") {
            refused.forEach { (expression, reason) ->
                val error = shouldThrow<IllegalArgumentException> {
                    XPath.parse(expression, namespaces = sampleNamespaces)
                }
                error.message shouldContain reason
                XPath.parseOrNull(expression, namespaces = sampleNamespaces).shouldBeNull()
            }
            val message = shouldThrow<IllegalArgumentException> { XPath.parse("1 2") }.message.shouldNotBeNull()
            message shouldContain "at offset 2"
        }

        test("is refused when it is longer or nests deeper than its limits") {
            val limits = xmlLimits {
                maximumQueryLength = SHORT
                maximumQueryNesting = SHALLOW
            }

            XPath.parseOrNull("(1+2)", limits = limits).shouldBeNull()
            XPath.parseOrNull("1234", limits = limits).shouldNotBeNull()
            val nesting = xmlLimits { maximumQueryNesting = SHALLOW }
            XPath.parseOrNull("((1))", limits = nesting).shouldNotBeNull()
            XPath.parseOrNull("(((1)))", limits = nesting).shouldBeNull()
            XPath.parseOrNull("a[b[c[d]]]", limits = nesting).shouldBeNull()
            XPath.parseOrNull("floor(floor(floor(1)))", limits = nesting).shouldBeNull()
        }

        test("never nests deeper than the hard ceiling") {
            val deep = "(".repeat(XmlLimits.HARD_QUERY_NESTING) + "1" + ")".repeat(XmlLimits.HARD_QUERY_NESTING)
            val relaxed = XPath.parseOrNull(deep.drop(1).dropLast(1), limits = XmlLimits.Relaxed)
            relaxed.shouldNotBeNull()
            XPath.parseOrNull(deep, limits = XmlLimits.Relaxed).shouldBeNull()
            val chained = List(XmlLimits.HARD_QUERY_NESTING * TERMS_PER_LEVEL) { "1" }.joinToString(separator = " + ")
            XPath.parse(chained).evaluate(documentOf("<a/>")) shouldBe
                XPathValue.Number(XmlLimits.HARD_QUERY_NESTING * TERMS_PER_LEVEL.toDouble())
        }

        test("shows itself and its errors escaped") {
            checkAll(CASES, hostileText) { text ->
                XPath.parseOrNull(text)?.shouldBeShownSafely()
                val error = runCatching { XPath.parse(text) }.exceptionOrNull()
                (error == null || error is IllegalArgumentException) shouldBe true
                error?.message?.shouldBeSafeToShow()
                XPath.parse("'x'").toString() shouldBe "'x'"
            }
        }
    }

    context("the namespaces of an expression") {
        test("are kept and compared") {
            val first = XPath.parse(PREFIXED, namespaces = mapOf("p" to URN_P))
            val second = XPath.parse(PREFIXED, namespaces = mapOf("p" to URN_P))

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
            first.namespaces shouldBe mapOf("p" to URN_P)
            first shouldNotBe XPath.parse(PREFIXED, namespaces = mapOf("p" to "urn:q"))
            first shouldNotBe XPath.parse(PREFIXED, namespaces = mapOf("p" to URN_P), limits = XmlLimits.Relaxed)
            first shouldNotBe XPath.parse("//p:b", namespaces = mapOf("p" to URN_P))
            first.equals(PREFIXED) shouldBe false
            first.expression shouldBe PREFIXED
            first.limits shouldBe XmlLimits.Untrusted
        }

        test("must bind names to URIs") {
            shouldThrow<IllegalArgumentException> { XPath.parse("1", namespaces = mapOf("1p" to URN_P)) }
            shouldThrow<IllegalArgumentException> { XPath.parse("1", namespaces = mapOf("xmlns" to URN_P)) }
            shouldThrow<IllegalArgumentException> { XPath.parse("1", namespaces = mapOf("p" to "")) }
            shouldThrow<IllegalArgumentException> { XPath.parseOrNull("1", namespaces = mapOf("xml" to URN_P)) }
            val xml = mapOf("xml" to "http://www.w3.org/XML/1998/namespace")
            XPath.parse("//@xml:lang", namespaces = xml).isNodeSet shouldBe true
        }
    }
})
