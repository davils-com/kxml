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

package com.davils.kxml.core.node

import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaces
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

private val bucket: XmlName = XmlName("Bucket")
private val key: XmlName = XmlName("Key")
private val id: XmlName = XmlName("id")
private const val CONTROL: String = "\u0001"
private const val URN: String = "urn:x"
private const val XMLNS: String = "xmlns"

class XmlNodeTest : FunSpec({
    context("elements") {
        test("are built with attributes, children and namespaces") {
            val element = xmlElement(bucket) {
                attribute(id, "1")
                namespace(prefix = "p", uri = "urn:p")
                element(key) { text("photo.jpg") }
                cdata("raw")
                comment("note")
                instruction(target = "pi", data = "data")
                node(xmlText("tail"))
            }

            element.firstElement(key)?.text shouldBe "photo.jpg"
            element.elements(key).size shouldBe 1
            element.firstElement(bucket) shouldBe null
            element.text shouldBe "rawtail"
            element shouldBe element
            element.namespaces.single().prefix shouldBe "p"
            element.toString() shouldContain "5 children"
        }

        test("are equal by name, attributes in any order and children") {
            val first = xmlElement(bucket) {
                attribute(id, "1")
                attribute(key, "2")
                text("x")
            }
            val second = xmlElement(bucket) {
                attribute(key, "2")
                attribute(id, "1")
                text("x")
            }

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
            first shouldNotBe xmlElement(bucket)
            first shouldNotBe xmlElement(key) {
                attribute(key, "2")
                attribute(id, "1")
                text("x")
            }
            first shouldNotBe "x"
            first shouldNotBe xmlElement(bucket) {
                attribute(key, "2")
                attribute(id, "1")
                text("y")
            }
            first shouldNotBe xmlElement(bucket) {
                attribute(key, "3")
                attribute(id, "1")
                text("x")
            }
        }

        test("refuse an attribute set twice or holding a control character") {
            shouldThrow<IllegalArgumentException> {
                xmlElement(bucket) {
                    attribute(id, "1")
                    attribute(id, "2")
                }
            }
            shouldThrow<IllegalArgumentException> { xmlElement(bucket) { attribute(id, "\u0000") } }
        }

        test("refuse an attribute that would declare a namespace") {
            shouldThrow<IllegalArgumentException> { XmlAttribute(XmlName(XMLNS), URN) }
            shouldThrow<IllegalArgumentException> {
                xmlElement(bucket) { attribute(XmlName("p", namespace = XmlNamespaces.XMLNS), URN) }
            }
            XmlAttribute(XmlName(XMLNS, namespace = URN, prefix = "p"), "v").name.local shouldBe XMLNS
        }
    }

    context("leaves") {
        test("are equal by content") {
            val text = xmlText("a")
            val cdata = xmlCData("a")
            val comment = xmlComment("a")
            val instruction = xmlInstruction(target = "pi", data = "d")

            text shouldBe text
            cdata shouldBe cdata
            comment shouldBe comment
            instruction shouldBe instruction
            instruction shouldNotBe xmlInstruction(target = "other", data = "d")
            xmlText("a") shouldBe xmlText("a")
            xmlText("a").hashCode() shouldBe xmlText("a").hashCode()
            xmlText("a") shouldNotBe xmlCData("a")
            xmlCData("a") shouldBe xmlCData("a")
            xmlCData("a").hashCode() shouldBe "a".hashCode()
            xmlCData("a") shouldNotBe xmlText("a")
            xmlComment("a") shouldBe xmlComment("a")
            xmlComment("a").hashCode() shouldBe "a".hashCode()
            xmlComment("a") shouldNotBe xmlText("a")
            xmlInstruction(target = "pi", data = "d") shouldBe xmlInstruction(target = "pi", data = "d")
            val equalInstruction = xmlInstruction(target = "pi", data = "d")
            equalInstruction.hashCode() shouldBe xmlInstruction(target = "pi", data = "d").hashCode()
            xmlInstruction(target = "pi", data = "d") shouldNotBe xmlInstruction(target = "pi", data = "e")
            xmlInstruction("pi") shouldNotBe xmlText("pi")
        }

        test("describe themselves") {
            xmlText("a").toString() shouldBe "Text(\"a\")"
            xmlCData("a").toString() shouldBe "CData(\"a\")"
            xmlComment("a").toString() shouldBe "Comment(\"a\")"
            xmlInstruction("pi").toString() shouldBe "ProcessingInstruction(pi)"
            xmlText(" \t").isWhitespace shouldBe true
            xmlText(" x").isWhitespace shouldBe false
        }

        test("refuse what XML cannot carry") {
            shouldThrow<IllegalArgumentException> { xmlText(CONTROL) }
            shouldThrow<IllegalArgumentException> { xmlCData(CONTROL) }
            shouldThrow<IllegalArgumentException> { xmlComment(CONTROL) }
            shouldThrow<IllegalArgumentException> { xmlComment("a--b") }
            shouldThrow<IllegalArgumentException> { xmlComment("a-") }
            shouldThrow<IllegalArgumentException> { xmlInstruction("a:b") }
            shouldThrow<IllegalArgumentException> { xmlInstruction("XML") }
            shouldThrow<IllegalArgumentException> { xmlInstruction(target = "pi", data = "a?>b") }
            shouldThrow<IllegalArgumentException> { xmlInstruction(target = "pi", data = CONTROL) }
        }
    }

    context("documents") {
        test("hold only comments and instructions around the root") {
            val root = xmlElement(bucket)

            XmlDocument(root, prolog = listOf(xmlComment("c"), xmlInstruction("pi"))).prolog.size shouldBe 2
            shouldThrow<IllegalArgumentException> { XmlDocument(root, epilog = listOf(xmlText("t"))) }
        }

        test("describe attributes and declarations") {
            XmlAttribute(id, "1").toString() shouldBe "id=\"1\""
            XmlDeclaration.Default.encoding shouldBe "UTF-8"
        }
    }
})
