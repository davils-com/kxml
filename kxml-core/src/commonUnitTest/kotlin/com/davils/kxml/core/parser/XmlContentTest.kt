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

package com.davils.kxml.core.parser

import com.davils.kxml.core.Xml
import com.davils.kxml.core.XmlWhitespace
import com.davils.kxml.core.fixture.CR
import com.davils.kxml.core.fixture.CRLF
import com.davils.kxml.core.fixture.LF
import com.davils.kxml.core.fixture.QUOTE
import com.davils.kxml.core.fixture.TAB
import com.davils.kxml.core.fixture.root
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.source.XmlSource
import com.davils.kxml.core.xml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val S3: String = "http://s3.amazonaws.com/doc/2006-03-01/"
private const val GRINNING: String = "😀"
private const val ROOT_END: Int = 15
private const val CHILD_START: Int = 3
private const val TEXT_START: Int = 6
private const val MIXED_CHILDREN: Int = 3

private fun rootOf(text: String): XmlNode.Element = Xml.Default.root(text)

class XmlContentTest : FunSpec({
    context("text") {
        test("resolves the predefined entities and character references") {
            val root = rootOf("<a>&lt;&gt;&amp;&apos;&quot;&#65;&#x42;&#x1F600;</a>")

            root.text shouldBe "<>&'${QUOTE}AB$GRINNING"
        }

        test("normalizes line breaks") {
            val root = rootOf("<a>1${CRLF}2${CR}3${LF}4</a>")

            root.text shouldBe "1${LF}2${LF}3${LF}4"
        }

        test("keeps a bracket that ends no character data") {
            val root = rootOf("<a>]] ]</a>")

            root.text shouldBe "]] ]"
        }

        test("joins character data with text") {
            val root = rootOf("<a>x<![CDATA[<y>$CRLF]]>z</a>")

            root.text shouldBe "x<y>${LF}z"
        }

        test("reads comments and instructions inside an element") {
            val root = rootOf("<a><!-- note --><?pi?><?target some data ?></a>")
            val comment = root.children.first()
            val instructions = root.children.filterIsInstance<XmlNode.ProcessingInstruction>()

            comment.shouldBeInstanceOf<XmlNode.Comment>()
            comment.value shouldBe " note "
            instructions.map { it.data } shouldBe listOf("", "some data ")
        }
    }

    context("attributes") {
        test("normalize whitespace that stands literally") {
            val root = rootOf("<a x='1${TAB}2${LF}3${CRLF}4${CR}5' y='&#10;'/>")

            root.attribute(XmlName("x"))?.value shouldBe "1 2 3 4 5"
            root.attribute(XmlName("y"))?.value shouldBe LF
        }

        test("allow whitespace around =") {
            val root = rootOf("<a x = 'v' ></a>")

            root.attribute(XmlName("x"))?.value shouldBe "v"
        }

        test("are missing when not set") {
            rootOf("<a/>").attribute(XmlName("x")) shouldBe null
        }
    }

    context("namespaces") {
        test("resolve the default namespace for elements but not for attributes") {
            val root = rootOf("<a xmlns='$S3' x='1'><b/></a>")
            val child = root.elements.single()

            root.name shouldBe XmlName(local = "a", namespace = S3)
            child.name shouldBe XmlName(local = "b", namespace = S3)
            root.attributes.single().name shouldBe XmlName("x")
            root.namespaces shouldBe listOf(XmlNamespaceBinding(prefix = null, uri = S3))
        }

        test("resolve prefixes for elements and attributes") {
            val root = rootOf("<p:a xmlns:p='urn:p' p:x='1' xml:lang='en'/>")

            root.name shouldBe XmlName(local = "a", namespace = "urn:p", prefix = "p")
            root.name.prefix shouldBe "p"
            root.attribute(XmlName(local = "x", namespace = "urn:p"))?.value shouldBe "1"
            root.attribute(XmlName(local = "lang", namespace = XmlNamespaces.XML))?.value shouldBe "en"
        }

        test("undeclare the default namespace") {
            val root = rootOf("<a xmlns='$S3'><b xmlns=''/></a>")
            val child = root.elements.single()

            child.name shouldBe XmlName("b")
        }

        test("accept the xml prefix bound to its own namespace") {
            val root = rootOf("<a xmlns:xml='http://www.w3.org/XML/1998/namespace'/>")

            root.namespaces shouldHaveSize 1
        }
    }

    context("whitespace") {
        test("is kept by default") {
            val root = rootOf("<a>$LF  <b/>$LF</a>")

            root.children shouldHaveSize MIXED_CHILDREN
        }

        test("between elements is dropped when asked") {
            val dropping = xml { whitespace = XmlWhitespace.DropIgnorable }
            val root = dropping.root("<a>$LF  <b> x </b>$LF</a>")
            val child = root.elements.single()

            root.children shouldHaveSize 1
            child.text shouldBe " x "
        }
    }

    context("ranges") {
        test("cover each node") {
            val root = rootOf("<a><b>x</b></a>")
            val child = root.elements.single()
            val text = child.children.single()

            root.range?.start shouldBe 0
            root.range?.end shouldBe ROOT_END
            child.range?.start shouldBe CHILD_START
            text.range?.start shouldBe TEXT_START
        }
    }

    context("events") {
        test("report an empty element as a start and an end") {
            val result = Xml.Default.events(XmlSource.of("<a/>"))

            result.isValid shouldBe true
            result.events shouldHaveSize 2
        }
    }
})
