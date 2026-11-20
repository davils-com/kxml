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

package com.davils.kxml.core.syntax.ast

import com.davils.kore.testing.HostileFragments
import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.fixture.shouldBeShownSafely
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.syntax.XmlSyntaxKind
import com.davils.kxml.core.syntax.XmlSyntaxNode
import com.davils.kxml.core.syntax.XmlSyntaxTree
import com.davils.kxml.core.xml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val CASES: Int = 500
private const val MOST_FRAGMENTS: Int = 30
private const val ITEM: String = "p:item"
private const val TAB: String = "\t"
private const val LF: String = "\n"
private const val FOUR_LINES: String = "a${LF}b${LF}c${LF}d"
private const val CRLF: String = "\r\n"
private const val CR: String = "\r"
private const val NEL: String = "\u0085"
private const val LINE_SEPARATOR: String = "\u2028"
private const val DOCUMENT: String = "<?xml version='1.0'?><!--before--><?pi a?>" +
    "<r xmlns='urn:d' xmlns:p='urn:p' xml:lang='de'>" +
    "<p:item id='1' note='a&amp;b&#65;${TAB}c${CRLF}d'>one &lt;two&gt;<![CDATA[<x>]]>&#x41;</p:item>" +
    "<p:item id='2'/><other xmlns:p='urn:q'/>" +
    "</r><!--after-->"

private val markup: List<String> = listOf(
    "<a>", "</a>", "<b x='1'>", "</b>", "<c/>", "/>", ">", "<", "&amp;", "&#x41;", "&e;", "&", "'", "\"", "=",
    "<!--c-->", "<![CDATA[x]]>", "<?pi d?>", "<?xml version='1.1'?>", "xmlns:p='urn:p'", "p:x='y'", "text", " ",
)

private val texts: Arb<String> =
    Arb.list(Arb.element(markup + HostileFragments.all), 0..MOST_FRAGMENTS).map { pieces -> pieces.joinToString("") }

private fun documentOf(text: String): XmlDocumentSyntax = XmlSyntaxTree.parse(text).document

private fun rootOf(text: String): XmlElementSyntax = documentOf(text).root.shouldNotBeNull()

private fun everyElement(document: XmlDocumentSyntax): List<XmlElementSyntax> {
    val pending = ArrayDeque(listOfNotNull(document.root))
    val elements = mutableListOf<XmlElementSyntax>()
    while (pending.isNotEmpty()) {
        val element = pending.removeFirst()
        elements += element
        pending.addAll(element.elements)
    }
    return elements.toList()
}

class XmlSyntaxViewTest : FunSpec({
    context("a document") {
        test("names its declaration, version, root and the comments and instructions around the root") {
            val document = documentOf(DOCUMENT)

            document.declaration.shouldNotBeNull()
            document.version shouldBe "1.0"
            document.doctype.shouldBeNull()
            document.root?.name shouldBe "r"
            document.prolog.map(XmlSyntaxNode::kind) shouldContainExactly
                listOf(XmlSyntaxKind.COMMENT, XmlSyntaxKind.PROCESSING_INSTRUCTION)
            document.epilog.map(XmlSyntaxNode::text) shouldContainExactly listOf("<!--after-->")
            document.toString() shouldBe "XmlDocumentSyntax(${document.node.range})"
        }

        test("defaults to version 1.0 and has no root in an empty text") {
            val empty = documentOf("")

            empty.version shouldBe "1.0"
            empty.root.shouldBeNull()
            empty.prolog.shouldBeEmpty()
            documentOf("<?xml version=\"1.1\"?><r/>").version shouldBe "1.1"
            documentOf("<?xml encoding='UTF-8'?><r/>").version shouldBe "1.0"
        }

        test("shows its document type declaration") {
            val internal = xml { doctype = XmlDoctype.Internal }
            val document = XmlSyntaxTree.parse("<!DOCTYPE r [<!ENTITY e 'v'>]><r>&e;</r>", internal).document
            val root = document.root.shouldNotBeNull()

            document.doctype?.kind shouldBe XmlSyntaxKind.DOCTYPE
            root.text.shouldBeNull()
        }
    }

    context("an element") {
        val root = rootOf(DOCUMENT)
        val item = root.elements(ITEM).first()

        test("splits its name and resolves its namespace through the declarations in scope") {
            item.name shouldBe ITEM
            item.prefix shouldBe "p"
            item.localName shouldBe "item"
            item.namespace shouldBe "urn:p"
            root.namespace shouldBe "urn:d"
            root.prefix.shouldBeNull()
            root.namespaceFor("xml") shouldBe XmlNamespaces.XML
            root.namespaceFor("q").shouldBeNull()
            val other = root.elements("other").single()
            val undeclaring = rootOf("<?xml version='1.1'?><r xmlns:p='urn:p'><o xmlns:p=''/></r>").elements.single()
            other.namespaceFor("p") shouldBe "urn:q"
            undeclaring.namespaceFor("p").shouldBeNull()
        }

        test("names its tags, attributes, child elements and parent") {
            item.startTag.kind shouldBe XmlSyntaxKind.START_TAG
            item.endTag?.text shouldBe "</p:item>"
            item.isEmptyTag shouldBe false
            val empty = root.elements(ITEM).last()
            empty.isEmptyTag shouldBe true
            empty.endTag.shouldBeNull()
            root.attributes.map(XmlAttributeSyntax::name) shouldContainExactly listOf("xmlns", "xmlns:p", "xml:lang")
            root.elements.map(XmlElementSyntax::name) shouldContainExactly listOf(ITEM, ITEM, "other")
            item.parent?.name shouldBe "r"
            root.parent.shouldBeNull()
            item.attribute("missing").shouldBeNull()
            item.toString() shouldBe "XmlElementSyntax(\"p:item\", ${item.node.range})"
        }

        test("decodes its own character data") {
            item.text shouldBe "one <two><x>A"
            item.content.map(com.davils.kxml.core.syntax.XmlSyntaxElement::kind) shouldContainExactly listOf(
                XmlSyntaxKind.TEXT,
                XmlSyntaxKind.ENTITY_REFERENCE,
                XmlSyntaxKind.TEXT,
                XmlSyntaxKind.ENTITY_REFERENCE,
                XmlSyntaxKind.CDATA_SECTION,
                XmlSyntaxKind.CHARACTER_REFERENCE,
            )
            rootOf("<r>a${CRLF}b${CR}c<!--x--><?p?><e/>d</r>").text shouldBe "a\nb\ncd"
            rootOf("<?xml version='1.1'?><r>a${NEL}b$CR${NEL}c${LINE_SEPARATOR}d</r>").text shouldBe FOUR_LINES
            rootOf("<r>a${NEL}b</r>").text shouldBe "a${NEL}b"
            rootOf("<?xml version='1.1'?><r>a${CR}b</r>").text shouldBe "a${LF}b"
            rootOf("<r><![CDATA[]]></r>").text shouldBe ""
        }
    }

    context("an attribute") {
        val root = rootOf(DOCUMENT)
        val item = root.elements(ITEM).first()
        val note = item.attribute("note").shouldNotBeNull()

        test("gives its value as written and as a reader reports it") {
            note.rawValue shouldBe "a&amp;b&#65;${TAB}c${CRLF}d"
            note.value shouldBe "a&bA c d"
            val range = note.valueRange
            DOCUMENT.substring(startIndex = range.start, endIndex = range.end) shouldBe note.rawValue
            note.prefix.shouldBeNull()
            note.localName shouldBe "note"
            note.isNamespaceDeclaration shouldBe false
            note.toString() shouldBe "XmlAttributeSyntax(\"note\", ${note.node.range})"
        }

        test("tells namespace declarations and prefixed names") {
            val root = rootOf(DOCUMENT)

            root.attribute("xmlns")?.isNamespaceDeclaration shouldBe true
            root.attribute("xmlns:p")?.isNamespaceDeclaration shouldBe true
            root.attribute("xml:lang")?.prefix shouldBe "xml"
            rootOf("<r a=''/>").attribute("a")?.value shouldBe ""
        }
    }

    context("a document with errors") {
        test("is shown as far as it was parsed") {
            val root = rootOf("<r a='1' b='&e;'><c")

            root.attribute("a")?.value shouldBe "1"
            root.attribute("b")?.value.shouldBeNull()
            root.endTag.shouldBeNull()
            val bare = rootOf("<r a>").attributes.single()
            bare.rawValue shouldBe ""
            bare.valueRange.isEmpty shouldBe true
        }

        test("binds no namespace through a reference to an entity") {
            val internal = xml { doctype = XmlDoctype.Internal }
            val text = "<!DOCTYPE r [<!ENTITY u 'urn:u'>]><r xmlns:p='&u;'><p:a/></r>"
            val document = XmlSyntaxTree.parse(text, internal).document
            val root = document.root.shouldNotBeNull()

            root.namespaceFor("p").shouldBeNull()
        }

        test("never throws and is shown safely for any text") {
            checkAll(CASES, texts) { text ->
                val document = documentOf(text)
                document.version
                document.prolog
                everyElement(document).forEach { element ->
                    element.text
                    element.namespace
                    element.shouldBeShownSafely()
                    element.attributes.forEach { attribute ->
                        attribute.value
                        attribute.valueRange
                        attribute.shouldBeShownSafely()
                    }
                }
            }
        }
    }
})
