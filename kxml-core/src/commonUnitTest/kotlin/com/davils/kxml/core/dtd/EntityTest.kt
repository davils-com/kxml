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

package com.davils.kxml.core.dtd

import com.davils.kxml.core.Xml
import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.XmlEvent
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.fixture.document
import com.davils.kxml.core.fixture.refusal
import com.davils.kxml.core.fixture.root
import com.davils.kxml.core.fixture.shouldGrowLinearly
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.source.XmlSource
import com.davils.kxml.core.xml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val LAUGHS: Int = 10
private const val REFERENCES: Int = 100_000
private const val SMALL_COUNT: Int = 1_000
private const val GROWTH: Int = 16
private const val DEPTH: Int = 4

private val internal: Xml = xml { doctype = XmlDoctype.Internal }

private fun withSubset(subset: String, body: String): String = "<!DOCTYPE a [$subset]><a>$body</a>"

private fun billionLaughs(): String {
    val levels = (1..LAUGHS).joinToString(separator = "") { level ->
        val previous = "&l${level - 1};".repeat(LAUGHS)
        "<!ENTITY l$level '$previous'>"
    }
    return withSubset(subset = "<!ENTITY l0 'lol'>$levels", body = "&l$LAUGHS;")
}

private fun chain(depth: Int): String {
    val levels = (1..depth).joinToString(separator = "") { level -> "<!ENTITY e$level '&e${level - 1};'>" }
    return withSubset(subset = "<!ENTITY e0 'x'>$levels", body = "&e$depth;")
}

private fun manyReferences(count: Int): String = withSubset(subset = "<!ENTITY e ''>", body = "&e;".repeat(count))

class EntityTest : FunSpec({
    context("an internal entity in content") {
        test("is replaced by its text and its markup") {
            val root = internal.root(withSubset(subset = "<!ENTITY e '<b>x</b>y'>", body = "1&e;2"))

            root.text shouldBe "1y2"
            val child = root.elements.single()

            child.name shouldBe XmlName("b")
            child.text shouldBe "x"
        }

        test("is expanded inside other entities and keeps references to characters") {
            val subset = "<!ENTITY inner 'i&#38;#60;'><!ENTITY outer 'o&inner;&#13;'>"

            internal.root(withSubset(subset = subset, body = "&outer;")).text shouldBe "oi<\r"
        }

        test("gives its events the range of the reference in the document") {
            val text = withSubset(subset = "<!ENTITY e '<b/>'>", body = "&e;")
            val events = generateSequence(internal.reader(XmlSource.of(text))::next).toList()
            val start = events.filterIsInstance<XmlEvent.StartElement>().last()
            val reference = text.indexOf("&e;")

            start.range.start shouldBe reference
            start.range.end shouldBe reference + "&e;".length
        }

        test("may be referenced many times without exhausting the stack") {
            val result = internal.parse(manyReferences(REFERENCES))

            result.shouldBeInstanceOf<XmlParseResult.Parsed>()
        }
    }

    context("a reference that is not well formed") {
        test("is refused when it names no declared entity") {
            internal.refusal(withSubset(subset = "", body = "&e;")) shouldBe XmlDiagnosticCode.UndefinedEntity
        }

        test("is refused when the entity refers to itself") {
            val direct = withSubset(subset = "<!ENTITY e '&e;'>", body = "&e;")

            internal.refusal(direct) shouldBe XmlDiagnosticCode.RecursiveEntity
            val indirect = withSubset(subset = "<!ENTITY a '&b;'><!ENTITY b '&a;'>", body = "&a;")
            internal.refusal(indirect) shouldBe XmlDiagnosticCode.RecursiveEntity
        }

        test("is refused when an entity opens or closes an element it does not close or open") {
            val opening = withSubset(subset = "<!ENTITY e '<b>'>", body = "&e;</b>")

            internal.refusal(opening) shouldBe XmlDiagnosticCode.UnbalancedEntity
            internal.refusal("<!DOCTYPE a [<!ENTITY e '</a>'>]><a>&e;") shouldBe XmlDiagnosticCode.UnbalancedEntity
        }

        test("is refused for an unparsed entity and for an external one without a resolver") {
            val unparsed = "<!NOTATION n SYSTEM 'n'><!ENTITY e SYSTEM 'e' NDATA n>"

            internal.refusal(withSubset(subset = unparsed, body = "&e;")) shouldBe XmlDiagnosticCode.MisusedEntity
            internal.refusal(withSubset(subset = "<!ENTITY e SYSTEM 'e.xml'>", body = "&e;")) shouldBe
                XmlDiagnosticCode.UnresolvedEntity
        }

        test("is refused for an entity nothing declared after an unread parameter entity") {
            val subset = "%missing;<!ENTITY e 'x'>"

            internal.refusal(withSubset(subset = subset, body = "&e;")) shouldBe XmlDiagnosticCode.UnresolvedEntity
            internal.refusal("<?xml version='1.0' standalone='yes'?>" + withSubset(subset = subset, body = "")) shouldBe
                XmlDiagnosticCode.UndefinedEntity
        }
    }

    context("entities in attribute values") {
        test("are expanded and normalized") {
            val subset = "<!ENTITY ws 'a&#9;b'><!ENTITY e 'x&ws;&#x41;&amp;'>"
            val root = internal.root("<!DOCTYPE a [$subset]><a v='&e;'/>")

            root.attributes.single().value shouldBe "xa bA&"
        }

        test("expand the references to characters their entities hold") {
            val subset = "<!ENTITY e '&#38;#x41;&#38;#66;&#38;lt;'>"

            val attribute = internal.root("<!DOCTYPE a [$subset]><a v='&e;'/>").attributes.single()

            attribute.value shouldBe "AB<"
        }

        test("are refused when their entities hold a reference that is not well formed") {
            listOf("&#38;#0;", "&#38;#xZZ;", "&#38;1;", "&#38;x").forEach { value ->
                val text = "<!DOCTYPE a [<!ENTITY e '$value'>]><a v='&e;'/>"

                internal.refusal(text) shouldBe XmlDiagnosticCode.InvalidReference
            }
        }

        test("are refused when they hold < or are external") {
            val less = "<!DOCTYPE a [<!ENTITY e '&#60;'>]><a v='&e;'/>"
            val external = "<!DOCTYPE a [<!ENTITY e SYSTEM 'e.xml'>]><a v='&e;'/>"

            internal.refusal(less) shouldBe XmlDiagnosticCode.InvalidAttribute
            internal.refusal(external) shouldBe XmlDiagnosticCode.MisusedEntity
        }
    }

    context("attribute defaults") {
        test("are added where an element leaves them out and normalized by their type") {
            val subset = "<!ATTLIST a x CDATA ' one  two ' y NMTOKENS ' one  two ' z CDATA #IMPLIED>"
            val root = internal.root("<!DOCTYPE a [$subset]><a y='  three   four '/>")
            val values = root.attributes.map { attribute -> attribute.name.local to attribute.value }

            values shouldContainExactly listOf("y" to "three four", "x" to " one  two ")
        }

        test("may declare a namespace") {
            val subset = "<!ATTLIST a xmlns CDATA 'urn:a' xmlns:p CDATA 'urn:p'>"
            val root = internal.root("<!DOCTYPE a [$subset]><a><p:b/></a>")

            val child = root.elements.single()

            root.name.namespace shouldBe "urn:a"
            child.name.namespace shouldBe "urn:p"
        }

        test("may refer only to entities declared before them") {
            val before = "<!ENTITY e 'v'><!ATTLIST a x CDATA '&e;'>"
            val after = "<!ATTLIST a x CDATA '&e;'><!ENTITY e 'v'>"

            val attribute = internal.root("<!DOCTYPE a [$before]><a/>").attributes.single()

            attribute.value shouldBe "v"
            internal.refusal("<!DOCTYPE a [$after]><a/>") shouldBe XmlDiagnosticCode.UndefinedEntity
        }
    }

    context("the expansion budget") {
        test("stops a billion laughs document") {
            internal.refusal(billionLaughs()) shouldBe XmlDiagnosticCode.EntityExpansionTooLarge
        }

        test("stops entities nested deeper than the limit") {
            val shallow = xml {
                doctype = XmlDoctype.Internal
                limits { maximumEntityDepth = DEPTH }
            }

            shallow.root(chain(DEPTH - 1)).text shouldBe "x"
            shallow.refusal(chain(DEPTH)) shouldBe XmlDiagnosticCode.EntityTooDeep
        }

        test("charges every reference, so many references to an empty entity stop at it") {
            val small = xml {
                doctype = XmlDoctype.Internal
                limits { maximumEntityExpansion = SMALL_COUNT.toLong() }
            }

            small.document(manyReferences(SMALL_COUNT)).root.text shouldBe ""
            small.refusal(manyReferences(SMALL_COUNT + 1)) shouldBe XmlDiagnosticCode.EntityExpansionTooLarge
        }

        test("keeps the work of expanding linear") {
            shouldGrowLinearly(
                small = manyReferences(SMALL_COUNT),
                large = manyReferences(SMALL_COUNT * GROWTH),
            ) { text -> internal.parse(text) }
        }
    }
})
