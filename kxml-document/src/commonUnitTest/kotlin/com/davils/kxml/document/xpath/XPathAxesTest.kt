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

import com.davils.kxml.document.fixture.namesOf
import com.davils.kxml.document.fixture.numberOf
import com.davils.kxml.document.fixture.textOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private const val TEXT: String = "#text"
private const val COMMENT: String = "#comment"
private const val UNDECLARED_ELEMENTS: Int = 3
private const val TEXTS: Int = 5
private const val ITEM_AND_CONTENT: Int = 4
private const val UNDECLARED: String = "<a xmlns='urn:x'><b xmlns=''><c/></b></a>"
private const val ALL_NAMESPACES: Double = 21.0

class XPathAxesTest : FunSpec({
    context("the forward axes") {
        test("select children, descendants and the node itself") {
            namesOf("/d:r/*") shouldContainExactly listOf("p:i", "i", "e", "n", "n", "n")
            namesOf("/d:r/d:i/node()") shouldContainExactly listOf(TEXT, COMMENT, "pi")
            namesOf("//d:n/text()/self::node()") shouldContainExactly listOf(TEXT, TEXT, TEXT)
            namesOf("/d:r/d:i/descendant::node()") shouldContainExactly listOf(TEXT, COMMENT, "pi")
            namesOf("/d:r/d:i/descendant-or-self::node()").size shouldBe ITEM_AND_CONTENT
            namesOf("//d:n") shouldContainExactly listOf("n", "n", "n")
            namesOf("/") shouldContainExactly listOf("#root")
        }

        test("select following siblings and following nodes") {
            namesOf("/d:r/d:i/following-sibling::*") shouldContainExactly listOf("e", "n", "n", "n")
            namesOf("/d:r/p:i/following::*") shouldContainExactly listOf("i", "e", "n", "n", "n")
            namesOf("/d:r/d:n[3]/following::node()") shouldContainExactly listOf("tail")
            namesOf("/d:r/@a/following::d:e") shouldContainExactly listOf("e")
            namesOf("/d:r/@a/following-sibling::node()").shouldBeEmpty()
        }

        test("select attributes but no namespace declarations") {
            namesOf("/d:r/@*") shouldContainExactly listOf("a", "xml:lang")
            namesOf("/d:r/attribute::a") shouldContainExactly listOf("a")
            namesOf("//@xml:id") shouldContainExactly listOf("xml:id")
            namesOf("/d:r/text()/@*").shouldBeEmpty()
        }
    }

    context("the reverse axes") {
        test("count positions from the context node outwards") {
            namesOf("/d:r/d:e/preceding-sibling::*") shouldContainExactly listOf("p:i", "i")
            namesOf("/d:r/d:e/preceding-sibling::*[1]") shouldContainExactly listOf("i")
            namesOf("//d:i/text()/ancestor::*[1]") shouldContainExactly listOf("i")
            namesOf("//d:i/text()/ancestor-or-self::node()[1]") shouldContainExactly listOf(TEXT)
            namesOf("//d:i/text()/ancestor::node()") shouldContainExactly listOf("#root", "r", "i")
            namesOf("/d:r/d:e/preceding::*[1]") shouldContainExactly listOf("i")
            namesOf("/d:r/d:e/preceding::node()") shouldContainExactly
                listOf(COMMENT, "p:i", TEXT, "i", TEXT, COMMENT, "pi")
        }

        test("start from the element of an attribute") {
            namesOf("/d:r/@a/..") shouldContainExactly listOf("r")
            namesOf("/d:r/d:i/@n/preceding::*") shouldContainExactly listOf("p:i")
            namesOf("/d:r/d:i/@n/preceding-sibling::node()").shouldBeEmpty()
            namesOf("/d:r/d:i/@n/ancestor::*") shouldContainExactly listOf("r", "i")
            namesOf("/..").shouldBeEmpty()
            namesOf("/ancestor::node()").shouldBeEmpty()
        }

        test("find no sibling before the first child") {
            namesOf("/d:r/p:i/preceding-sibling::node()").shouldBeEmpty()
            namesOf("/d:r/preceding-sibling::node()") shouldContainExactly listOf(COMMENT)
            namesOf("/preceding-sibling::node()").shouldBeEmpty()
            namesOf("/following-sibling::node()").shouldBeEmpty()
        }
    }

    context("the namespace axis") {
        test("holds every namespace in scope, ordered by prefix") {
            namesOf("/d:r/namespace::*") shouldContainExactly listOf("#namespace", "p", "xml")
            textOf("string(/d:r/namespace::p)") shouldBe "urn:p"
            textOf("name(/d:r/namespace::p)") shouldBe "p"
            textOf("namespace-uri(/d:r/namespace::p)") shouldBe ""
            numberOf("count(//namespace::*)") shouldBe ALL_NAMESPACES
            numberOf("count(/d:r/@a/namespace::*)") shouldBe 0.0
        }

        test("leaves out a default namespace that is undeclared") {
            val names = namesOf("/*/*/namespace::*", text = UNDECLARED)

            names shouldContainExactly listOf("xml")
            namesOf("//*/namespace::xml", text = UNDECLARED).size shouldBe UNDECLARED_ELEMENTS
        }

        test("relates namespace nodes to the other nodes") {
            namesOf("/d:r/namespace::p/parent::*") shouldContainExactly listOf("r")
            namesOf("/d:r/namespace::p/ancestor::*") shouldContainExactly listOf("r")
            namesOf("/d:r/namespace::p/following::d:e") shouldContainExactly listOf("e")
            namesOf("/d:r/namespace::p/preceding::node()") shouldContainExactly listOf(COMMENT)
            namesOf("/d:r/namespace::p/self::node()") shouldContainExactly listOf("p")
            namesOf("/d:r/namespace::p/descendant-or-self::node()") shouldContainExactly listOf("p")
            val empty = "count(/d:r/namespace::p/child::node() | /d:r/namespace::p/descendant::node() | " +
                "/d:r/namespace::p/following-sibling::node() | /d:r/namespace::p/preceding-sibling::node() | " +
                "/d:r/namespace::p/attribute::node() | /d:r/namespace::p/namespace::node())"
            numberOf(empty) shouldBe 0.0
        }

        test("orders namespace nodes before the attributes of their element in a union") {
            namesOf("/d:r/@a | /d:r/namespace::p | /d:r/namespace::*") shouldContainExactly
                listOf("#namespace", "p", "xml", "a")
        }
    }

    context("node tests") {
        test("match by kind and by expanded name") {
            namesOf("//comment()") shouldContainExactly listOf(COMMENT, COMMENT)
            namesOf("//processing-instruction()") shouldContainExactly listOf("pi", "tail")
            namesOf("//processing-instruction('pi')") shouldContainExactly listOf("pi")
            namesOf("//processing-instruction('none')").shouldBeEmpty()
            namesOf("//p:*") shouldContainExactly listOf("p:i")
            namesOf("//i").shouldBeEmpty()
            namesOf("/d:r/@d:a").shouldBeEmpty()
            namesOf("//text()").size shouldBe TEXTS
        }
    }
})
