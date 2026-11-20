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

import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.source.XmlSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val DOCUMENT: String = "<?xml version='1.0'?><a x='1'><b>t</b><!--c--></a>"

class XmlReaderTest : FunSpec({
    context("a reader of a well-formed document") {
        test("reads the events of the document one by one") {
            val reader = Xml.Default.reader(XmlSource.of(DOCUMENT))
            val events = generateSequence(reader::next).toList()

            events shouldBe Xml.Default.events(XmlSource.of(DOCUMENT)).events
            events.first().shouldBeInstanceOf<XmlEvent.Declaration>()
            reader.isValid shouldBe true
            reader.next().shouldBeNull()
            reader.toString() shouldContain "done"
        }

        test("reports that it is still reading before the end") {
            val reader = Xml.Default.reader(XmlSource.of(DOCUMENT))

            reader.next().shouldBeInstanceOf<XmlEvent.Declaration>()
            reader.toString() shouldContain "reading"
        }
    }

    context("nextElement") {
        test("reads the whole root of a fresh reader, as parse does") {
            val reader = Xml.Default.reader(XmlSource.of(DOCUMENT))
            val parsed = Xml.Default.parse(DOCUMENT).shouldBeInstanceOf<XmlParseResult.Parsed>()

            val root = reader.nextElement()

            root shouldBe parsed.document.root
            root?.range shouldBe parsed.document.root.range
            reader.nextElement().shouldBeNull()
        }

        test("reads the elements of a level one by one and ends with the end tag of their parent") {
            val text = "<items> <item a='1'>x</item><!--k--> t <item/></items><!--after-->"
            val reader = xml { whitespace = XmlWhitespace.DropIgnorable }.reader(XmlSource.of(text))

            reader.next().shouldBeInstanceOf<XmlEvent.StartElement>()
            val first = reader.nextElement()
            val second = reader.nextElement()

            first?.text shouldBe "x"
            first?.range?.start shouldBe text.indexOf("<item a")
            second?.elements shouldBe emptyList()
            reader.nextElement().shouldBeNull()
            reader.next().shouldBeInstanceOf<XmlEvent.Comment>()
        }

        test("stops at an error inside the element") {
            val reader = Xml.Default.reader(XmlSource.of("<a><b></a>"))

            reader.nextElement().shouldBeNull()
            reader.isValid shouldBe false
        }
    }

    context("a reader of a broken document") {
        test("stops at the first error and keeps it") {
            val reader = Xml.Default.reader(XmlSource.of("<a><b></a>"))
            val events = generateSequence(reader::next).toList()

            events.size shouldBe 2
            reader.isValid shouldBe false
            reader.diagnostics.single().code shouldBe XmlDiagnosticCode.MismatchedTag
            reader.next().shouldBeNull()
        }

        test("reads nothing from a source that is refused before reading") {
            val small = xml { limits { maximumInputLength = 2 } }
            val reader = small.reader(XmlSource.of("<a/>"))

            reader.next().shouldBeNull()
            reader.diagnostics.single().code shouldBe XmlDiagnosticCode.InputTooLong
        }

        test("reads nothing from bytes that are not valid in their encoding") {
            val reader = Xml.Default.reader(XmlSource.of(byteArrayOf(0x3C, -1, 0x3E)))

            reader.next().shouldBeNull()
            reader.diagnostics.single().code shouldBe XmlDiagnosticCode.InvalidEncoding
        }
    }
})
