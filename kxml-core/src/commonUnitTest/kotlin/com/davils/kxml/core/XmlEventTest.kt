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

package com.davils.kxml.core

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.fixture.hostileText
import com.davils.kxml.core.fixture.shouldBeShownSafely
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.node.XmlAttribute
import com.davils.kxml.core.node.XmlDeclaration
import com.davils.kxml.core.source.XmlSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.checkAll

private const val CASES: Int = 200
private const val DOCUMENT: String = """<?xml version="1.0"?><a x="1"><!--c--><![CDATA[d]]><?pi data?>t</a>"""

private val here: TextRange = TextRange(start = 0, end = 1)
private val there: TextRange = TextRange(start = 1, end = 2)
private val name: XmlName = XmlName("a")

class XmlEventTest : FunSpec({
    context("reading") {
        test("yields one event per construct in document order") {
            val result = Xml.Default.events(XmlSource.of(DOCUMENT))
            val kinds = result.events.map { event -> event::class.simpleName }

            kinds shouldContainExactly listOf(
                "Declaration",
                "StartElement",
                "Comment",
                "CData",
                "ProcessingInstruction",
                "Text",
                "EndElement",
            )
        }
    }

    context("equality") {
        test("compares the values and the range of every kind of event") {
            val events = eventsAt(here)
            val same = eventsAt(here)
            val moved = eventsAt(there)

            events shouldBe same
            events.map(XmlEvent::hashCode) shouldBe same.map(XmlEvent::hashCode)
            events.zip(moved).forEach { (event, other) -> event shouldNotBe other }
            events.forEach { event -> event shouldNotBe name }
        }

        test("tells events with other values apart") {
            XmlEvent.Text("a", here) shouldNotBe XmlEvent.Text("b", here)
            XmlEvent.CData("a", here) shouldNotBe XmlEvent.CData("b", here)
            XmlEvent.Comment("a", here) shouldNotBe XmlEvent.Comment("b", here)
            XmlEvent.EndElement(name, here) shouldNotBe XmlEvent.EndElement(XmlName("b"), here)
            val instruction = XmlEvent.ProcessingInstruction(target = "pi", data = "a", range = here)
            instruction shouldNotBe XmlEvent.ProcessingInstruction(target = "pi", data = "b", range = here)
        }
    }

    context("toString") {
        test("names the kind and the content") {
            XmlEvent.Text("a", here).toString() shouldBe """Text("a")"""
            XmlEvent.EndElement(name, here).toString() shouldBe "EndElement(a)"
            startAt(here).toString() shouldBe "StartElement(a)"
            XmlEvent.Declaration(XmlDeclaration.Default, here).toString() shouldContain "Declaration(XmlDeclaration("
        }

        test("escapes text that came from the document") {
            checkAll(CASES, hostileText) { text ->
                XmlEvent.Text(text, here).shouldBeShownSafely()
                XmlEvent.CData(text, here).shouldBeShownSafely()
                XmlEvent.Comment(text, here).shouldBeShownSafely()
                XmlEvent.ProcessingInstruction(target = text, data = text, range = here).shouldBeShownSafely()
            }
        }
    }
})

private fun startAt(range: TextRange): XmlEvent.StartElement = XmlEvent.StartElement(
    name = name,
    attributes = listOf(XmlAttribute(XmlName("x"), "1")),
    namespaces = listOf(XmlNamespaceBinding(prefix = "p", uri = "urn:p")),
    isEmpty = false,
    range = range,
)

private fun eventsAt(range: TextRange): List<XmlEvent> = listOf(
    XmlEvent.Declaration(XmlDeclaration.Default, range),
    startAt(range),
    XmlEvent.EndElement(name, range),
    XmlEvent.Text("t", range),
    XmlEvent.CData("d", range),
    XmlEvent.Comment("c", range),
    XmlEvent.ProcessingInstruction(target = "pi", data = "data", range = range),
)
