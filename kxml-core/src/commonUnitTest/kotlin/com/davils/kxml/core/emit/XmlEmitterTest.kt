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

package com.davils.kxml.core.emit

import com.davils.kxml.core.Xml
import com.davils.kxml.core.fixture.CR
import com.davils.kxml.core.fixture.CRLF
import com.davils.kxml.core.fixture.LF
import com.davils.kxml.core.fixture.QUOTE
import com.davils.kxml.core.fixture.TAB
import com.davils.kxml.core.fixture.document
import com.davils.kxml.core.fixture.root
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.node.XmlDeclaration
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.node.xmlComment
import com.davils.kxml.core.node.xmlElement
import com.davils.kxml.core.node.xmlInstruction
import com.davils.kxml.core.xml
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val S3: String = "http://s3.amazonaws.com/doc/2006-03-01/"
private const val OTHER: String = "urn:other"
private const val WIDE: Int = 4
private const val DEEP: Int = XmlLimits.HARD_DEPTH + 1

private val compact: Xml = xml { emitter = XmlEmitOptions.Compact }

private fun nested(depth: Int): XmlNode.Element {
    val leaf = xmlElement(XmlName("leaf"))
    return (1 until depth).fold(leaf) { child, _ -> xmlElement(XmlName("n")) { node(child) } }
}

class XmlEmitterTest : FunSpec({
    context("compact output") {
        test("writes elements, attributes and escaped text") {
            val element = xmlElement(XmlName(local = "Delete", namespace = S3)) {
                attribute(XmlName("note"), "a${QUOTE}b<c>&$TAB$LF$CR")
                element(XmlName(local = "Object", namespace = S3)) {
                    element(XmlName(local = "Key", namespace = S3)) { text("a&b<c>$CR") }
                }
                element(XmlName(local = "Quiet", namespace = S3))
            }

            compact.emit(element) shouldBe """<Delete xmlns="$S3" note="a&quot;b&lt;c&gt;&amp;&#9;&#10;&#13;">""" +
                "<Object><Key>a&amp;b&lt;c&gt;&#13;</Key></Object><Quiet/></Delete>"
        }

        test("writes the declaration, character data, comments and instructions") {
            val root = xmlElement(XmlName("a")) {
                cdata("<raw>")
                comment(" c ")
                instruction(target = "pi", data = "data")
                instruction("bare")
            }
            val document = XmlDocument(
                root = root,
                declaration = XmlDeclaration(isStandalone = true),
                prolog = listOf(xmlComment("before")),
                epilog = listOf(xmlInstruction("after")),
            )

            compact.emit(document) shouldBe """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
                "<!--before--><a><![CDATA[<raw>]]><!-- c --><?pi data?><?bare?></a><?after?>"
        }

        test("writes a declaration that is not standalone and names no encoding") {
            val declaration = XmlDeclaration(encoding = null, isStandalone = false)
            val document = XmlDocument(xmlElement(XmlName("a")), declaration)

            compact.emit(document) shouldBe """<?xml version="1.0" standalone="no"?><a/>"""
        }

        test("writes empty elements in full when asked") {
            val open = xml { emitter { isEmptyElementCollapsed = false } }

            open.emit(xmlElement(XmlName("a"))) shouldBe "<a></a>$LF"
        }
    }

    context("indented output") {
        test("indents element content and keeps mixed content inline") {
            val root = xmlElement(XmlName("a")) {
                element(XmlName("b")) { text("x") }
                element(XmlName("c")) {
                    text("y")
                    element(XmlName("d"))
                }
            }
            val expected = listOf("<a>", "  <b>x</b>", "  <c>y<d/></c>", "</a>")

            Xml.Default.emit(root) shouldBe expected.joinToString(separator = LF, postfix = LF)
        }

        test("uses the line break and indent that were asked for") {
            val windows = xml {
                emitter {
                    indent = WIDE
                    lineBreak = XmlLineBreak.CrLf
                }
            }
            val root = xmlElement(XmlName("a")) { element(XmlName("b")) }
            val expected = listOf("<a>", "    <b/>", "</a>")

            windows.emit(root) shouldBe expected.joinToString(separator = CRLF, postfix = CRLF)
        }
    }

    context("namespaces") {
        test("declare prefixes where they are first needed") {
            val root = xmlElement(XmlName(local = "a", namespace = S3, prefix = "s")) {
                element(XmlName(local = "b", namespace = S3, prefix = "s"))
                element(XmlName(local = "c", namespace = OTHER, prefix = "o"))
            }

            compact.emit(root) shouldBe """<s:a xmlns:s="$S3"><s:b/><o:c xmlns:o="$OTHER"/></s:a>"""
        }

        test("undeclare the default namespace for an element without one") {
            val root = xmlElement(XmlName(local = "a", namespace = S3)) { element(XmlName("b")) }

            compact.emit(root) shouldBe """<a xmlns="$S3"><b xmlns=""/></a>"""
        }

        test("keep explicit declarations once") {
            val root = xmlElement(XmlName("a")) {
                namespace(prefix = "x", uri = OTHER)
                namespace(null, "")
                element(XmlName("b")) { namespace(prefix = "x", uri = OTHER) }
            }

            compact.emit(root) shouldBe """<a xmlns:x="$OTHER"><b/></a>"""
        }

        test("give attributes a prefix of their namespace") {
            val root = xmlElement(XmlName(local = "a", namespace = OTHER, prefix = "o")) {
                attribute(XmlName(local = "x", namespace = OTHER), "1")
                attribute(XmlName(local = "y", namespace = S3, prefix = "o"), "2")
                attribute(XmlName(local = "z", namespace = S3), "3")
                val lang = XmlName(local = "lang", namespace = XmlNamespaces.XML, prefix = XmlNamespaces.XML_PREFIX)
                attribute(lang, "en")
                element(XmlName("b")) { attribute(XmlName(local = "w", namespace = OTHER, prefix = "o"), "4") }
                element(XmlName("c")) { attribute(XmlName(local = "v", namespace = "urn:new", prefix = "n"), "5") }
            }

            compact.emit(root) shouldBe """<o:a xmlns:o="$OTHER" xmlns:ns1="$S3" o:x="1" ns1:y="2" ns1:z="3" """ +
                """xml:lang="en"><b o:w="4"/><c xmlns:n="urn:new" n:v="5"/></o:a>"""
        }

        test("give an attribute in the default namespace a prefix of its own") {
            val root = xmlElement(XmlName(local = "a", namespace = S3)) {
                attribute(XmlName(local = "x", namespace = S3), "1")
            }

            compact.emit(root) shouldBe """<a xmlns="$S3" xmlns:ns1="$S3" ns1:x="1"/>"""
        }

        test("generate a prefix that is still free") {
            val root = xmlElement(XmlName("a")) {
                attribute(XmlName(local = "x", namespace = S3), "1")
                element(XmlName("b")) { attribute(XmlName(local = "y", namespace = OTHER), "2") }
            }

            compact.emit(root) shouldBe """<a xmlns:ns1="$S3" ns1:x="1"><b xmlns:ns2="$OTHER" ns2:y="2"/></a>"""
        }

        test("round trip through the reader") {
            val text = """<s:a xmlns:s="$S3" xmlns:o="$OTHER" o:x="1"><s:b>t</s:b></s:a>"""

            compact.emit(compact.document(text)) shouldBe text
        }
    }

    context("deep trees") {
        test("are written and compared at the hard depth without exhausting the stack") {
            val deepest = nested(XmlLimits.HARD_DEPTH)
            val trusted = xml {
                limits = XmlLimits.Trusted
                emitter = XmlEmitOptions.Compact
            }
            val text = trusted.emit(deepest)

            trusted.root(text) shouldBe deepest
        }
    }

    context("refusals") {
        test("refuse a namespace XML cannot carry") {
            val root = xmlElement(XmlName("a")) { namespace(prefix = "p", uri = "urn:\u0001") }

            shouldThrow<IllegalArgumentException> { compact.emit(root) }
        }

        test("refuse an element tree deeper than the hard limit") {
            shouldThrow<IllegalArgumentException> { compact.emit(nested(DEEP)) }
        }
    }
})
