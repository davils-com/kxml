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

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.fixture.hostileText
import com.davils.kxml.core.fixture.shouldBeShownSafely
import com.davils.kxml.core.name.XmlName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.checkAll

private const val CASES: Int = 200

private val id: XmlName = XmlName("id")
private val root: XmlNode.Element = xmlElement(XmlName("a"))

class XmlValuesTest : FunSpec({
    context("attributes") {
        test("are equal by name and value, wherever they were read") {
            val attribute = XmlAttribute(id, "1", TextRange(start = 0, end = 1))

            attribute shouldBe XmlAttribute(id, "1")
            val same = XmlAttribute(id, "1")
            attribute.hashCode() shouldBe same.hashCode()
            attribute shouldNotBe XmlAttribute(id, "2")
            attribute shouldNotBe "1"
        }

        test("escape their value when they are shown") {
            XmlAttribute(id, """a"b""").toString() shouldBe """id="a\"b""""
            checkAll(CASES, hostileText) { text -> XmlAttribute(id, text).shouldBeShownSafely() }
        }
    }

    context("declarations") {
        test("are equal by version, encoding and standalone flag") {
            XmlDeclaration() shouldBe XmlDeclaration.Default
            val declaration = XmlDeclaration()
            declaration.hashCode() shouldBe XmlDeclaration.Default.hashCode()
            XmlDeclaration(encoding = null) shouldNotBe XmlDeclaration.Default
            XmlDeclaration(isStandalone = true) shouldNotBe XmlDeclaration.Default
            XmlDeclaration.Default shouldNotBe XmlDeclaration.VERSION
        }

        test("show every value, and none for those left out") {
            XmlDeclaration.Default.toString() shouldBe
                """XmlDeclaration(version="1.0", encoding="UTF-8", standalone=none)"""
            XmlDeclaration(encoding = null, isStandalone = true).toString() shouldBe
                """XmlDeclaration(version="1.0", encoding=none, standalone=true)"""
        }

        test("escape their values when they are shown") {
            checkAll(CASES, hostileText) { text ->
                XmlDeclaration(version = text, encoding = text).shouldBeShownSafely()
            }
        }
    }

    context("documents") {
        test("are equal by root, declaration, prolog and epilog") {
            val document = documentAround(root)
            val same = documentAround(root)

            document shouldBe same
            document.hashCode() shouldBe same.hashCode()
            document shouldNotBe XmlDocument(root)
            document shouldNotBe root
        }

        test("show the root and the declaration") {
            XmlDocument(root).toString() shouldBe "XmlDocument(Element(a, 0 children), no declaration)"
        }

        test("refuse anything but comments and instructions around the root") {
            shouldThrow<IllegalArgumentException> { XmlDocument(root, prolog = listOf(xmlText("x"))) }
            shouldThrow<IllegalArgumentException> { XmlDocument(root, epilog = listOf(root)) }
        }

        test("keep their own copy of the prolog and the epilog") {
            val prolog = mutableListOf<XmlNode>(xmlComment("p"))
            val document = XmlDocument(root, prolog = prolog)
            prolog += xmlComment("q")

            document.prolog.size shouldBe 1
        }
    }

    context("nodes") {
        test("escape their content when they are shown") {
            checkAll(CASES, hostileText) { text ->
                XmlNode.Text(text, range = null).shouldBeShownSafely()
                XmlNode.CData(text, range = null).shouldBeShownSafely()
                XmlNode.Comment(text, range = null).shouldBeShownSafely()
                XmlNode.ProcessingInstruction(target = text, data = text, range = null).shouldBeShownSafely()
            }
        }
    }
})

private fun documentAround(element: XmlNode.Element): XmlDocument = XmlDocument(
    root = element,
    declaration = XmlDeclaration.Default,
    prolog = listOf(xmlComment("p")),
    epilog = listOf(xmlComment("e")),
)
