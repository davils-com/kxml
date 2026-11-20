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

package com.davils.kxml.core.syntax

import com.davils.kore.core.text.TextRange
import com.davils.kore.testing.HostileFragments
import com.davils.kxml.core.Xml
import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.fixture.CRLF
import com.davils.kxml.core.fixture.LF
import com.davils.kxml.core.fixture.shouldBeShownSafely
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.xml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val CASES: Int = 500
private const val MOST_FRAGMENTS: Int = 40
private const val MARK: String = "\uFEFF"
private const val DECLARATION: String = """<?xml version='1.0' encoding="UTF-8"?>"""
private const val ROOT: String = """<a xmlns:p='urn:p' p:x = "1 &amp; &#x32;">t&lt;$CRLF<![CDATA[<x>]]><b/></a >"""
private const val DOCUMENT: String = "$MARK$DECLARATION$LF<!-- c --><?pi data?>$LF$ROOT$LF"
private const val NAMESPACE_ATTRIBUTE: String = "xmlns"
private const val TOKENS_PER_LEVEL: Int = 6
private const val LAST_LINE: Int = 3
private const val SECRET: String = "s3cr3t"

private val ignoring: Xml = xml { doctype = XmlDoctype.Ignore }

private val markup: List<String> = listOf(
    "<a>", "</a>", "<b x='1'>", "</b>", "<c/>", "/>", ">", "<", "&amp;", "&#x41;", "&#65;", "&", ";", "&lt",
    "<!--c-->", "<!--", "-->", "<![CDATA[x]]>", "]]>", "<?pi d?>", "<?", "?>", "<?xml version='1.0'?>",
    "<!DOCTYPE a [<!ENTITY e 'v'>]>", "<!DOCTYPE", "text", " ", "\t", "\r\n", "\r", "'", "\"", "=", MARK,
    "xmlns:p='urn:p'", "p:x='y'",
)
private val fragments: List<String> = markup + HostileFragments.all

private val texts: Arb<String> =
    Arb.list(Arb.element(fragments), 0..MOST_FRAGMENTS).map { pieces -> pieces.joinToString("") }

private fun XmlSyntaxNode.kinds(): List<XmlSyntaxKind> = children.map(XmlSyntaxElement::kind)

private fun XmlSyntaxTree.isContiguous(): Boolean {
    var expected = 0
    val isGapless = root.tokens.all { token ->
        val isNext = token.range.start == expected
        expected = token.range.end
        isNext
    }
    return isGapless && expected == text.length
}

class XmlSyntaxTreeTest : FunSpec({
    context("the tree of a well-formed document") {
        val tree = XmlSyntaxTree.parse(DOCUMENT)

        test("keeps every character of the text") {
            tree.root.text shouldBe DOCUMENT
            tree.isContiguous() shouldBe true
            tree.isValid shouldBe true
            tree.diagnostics.shouldBeEmpty()
        }

        test("groups the declaration, the prolog and the root") {
            tree.root.kind shouldBe XmlSyntaxKind.DOCUMENT
            tree.root.childNodes.map(XmlSyntaxNode::kind) shouldContainExactly listOf(
                XmlSyntaxKind.XML_DECLARATION,
                XmlSyntaxKind.COMMENT,
                XmlSyntaxKind.PROCESSING_INSTRUCTION,
                XmlSyntaxKind.ELEMENT,
            )
            val mark = tree.root.children.first()

            mark.kind shouldBe XmlSyntaxKind.BYTE_ORDER_MARK
        }

        test("divides an element into its tags, attributes and content") {
            val element = tree.root.child(XmlSyntaxKind.ELEMENT).shouldNotBeNull()
            val start = element.child(XmlSyntaxKind.START_TAG).shouldNotBeNull()
            val attributes = start.childNodes.filter { node -> node.kind == XmlSyntaxKind.ATTRIBUTE }
            val last = attributes.last()
            val value = last.child(XmlSyntaxKind.ATTRIBUTE_VALUE).shouldNotBeNull()

            attributes.map { attribute -> attribute.token(XmlSyntaxKind.NAME)?.text } shouldBe listOf("xmlns:p", "p:x")
            value.kinds() shouldContainExactly listOf(
                XmlSyntaxKind.QUOTE,
                XmlSyntaxKind.ATTRIBUTE_TEXT,
                XmlSyntaxKind.ENTITY_REFERENCE,
                XmlSyntaxKind.ATTRIBUTE_TEXT,
                XmlSyntaxKind.CHARACTER_REFERENCE,
                XmlSyntaxKind.QUOTE,
            )
            element.childNodes.map(XmlSyntaxNode::kind) shouldContainExactly listOf(
                XmlSyntaxKind.START_TAG,
                XmlSyntaxKind.CDATA_SECTION,
                XmlSyntaxKind.ELEMENT,
                XmlSyntaxKind.END_TAG,
            )
        }

        test("finds the token at an offset and the node that covers a range") {
            val offset = DOCUMENT.indexOf("&lt;")
            val token = tree.root.tokenAt(offset).shouldNotBeNull()
            val empty = DOCUMENT.indexOf("<b/>")

            token.kind shouldBe XmlSyntaxKind.ENTITY_REFERENCE
            token.text shouldBe "&lt;"
            token.ancestors.first().kind shouldBe XmlSyntaxKind.ELEMENT
            val covering = tree.root.coveringNode(TextRange.of(start = empty, length = 4))

            covering.kind shouldBe XmlSyntaxKind.START_TAG
            covering.parent?.kind shouldBe XmlSyntaxKind.ELEMENT
            tree.root.tokenAt(DOCUMENT.length).shouldBeNull()
            tree.root.tokenAt(-1).shouldBeNull()
            tree.root.coveringNode(TextRange.of(start = 0, length = DOCUMENT.length)) shouldBe tree.root
            tree.root.descendants.count { node -> node.kind == XmlSyntaxKind.ELEMENT } shouldBe 2
        }

        test("composes the same document as the reader") {
            val composed = tree.compose().shouldBeInstanceOf<XmlParseResult.Parsed>()

            composed shouldBe Xml.Default.parse(DOCUMENT)
        }

        test("compares views by position and shows no text") {
            val again = XmlSyntaxTree.parse(DOCUMENT)
            val first = tree.root.tokens.first()

            tree.root shouldBe tree.root
            tree.root shouldNotBe again.root
            val firstAgain = tree.root.tokens.first()

            first shouldBe firstAgain
            first.hashCode() shouldBe firstAgain.hashCode()
            first shouldNotBe tree.root.tokens.last()
            first shouldNotBe again.root.tokens.first()
            tree.root shouldNotBe first
            tree.root.hashCode() shouldBe tree.root.hashCode()
            tree.green shouldBe again.green
            tree.green.hashCode() shouldBe again.green.hashCode()
            tree.green shouldNotBe XmlSyntaxTree.parse("<a/>").green
            tree.lines.lineOf(DOCUMENT.length - 1) shouldBe LAST_LINE
            tree.toString() shouldNotContain NAMESPACE_ATTRIBUTE
            tree.root.toString() shouldNotContain NAMESPACE_ATTRIBUTE
            first.toString() shouldNotContain NAMESPACE_ATTRIBUTE
        }
    }

    context("the tree of a broken document") {
        test("keeps the rest of the text after the construct with the first error in one error token") {
            val text = "<a><b>x</c></a> tail"
            val tree = XmlSyntaxTree.parse(text)
            val error = tree.root.tokens.last()

            tree.root.text shouldBe text
            tree.isValid shouldBe false
            tree.diagnostics.map { diagnostic -> diagnostic.code } shouldBe listOf(XmlDiagnosticCode.MismatchedTag)
            error.kind shouldBe XmlSyntaxKind.ERROR
            error.text shouldBe "</a> tail"
        }

        test("reads a document type it may ignore and refuses one by default") {
            val text = "<!DOCTYPE a [<!ENTITY e 'v'>]><a/>"
            val ignored = XmlSyntaxTree.parse(text, ignoring)
            val refused = XmlSyntaxTree.parse(text)

            val doctype = ignored.root.childNodes.first()
            val error = refused.root.tokens.single()

            doctype.kind shouldBe XmlSyntaxKind.DOCTYPE
            ignored.diagnostics.single().code shouldBe XmlDiagnosticCode.IgnoredDoctype
            error.kind shouldBe XmlSyntaxKind.ERROR
            refused.diagnostics.single().code shouldBe XmlDiagnosticCode.RefusedDoctype
        }

        test("refuses a text beyond the input limit before it is read") {
            val small = xml { limits { maximumInputLength = 3 } }
            val tree = XmlSyntaxTree.parse("<a/>", small)
            val error = tree.root.tokens.single()

            error.kind shouldBe XmlSyntaxKind.ERROR
            tree.diagnostics.single().code shouldBe XmlDiagnosticCode.InputTooLong
        }
    }

    context("any text") {
        test("gives a tree whose tokens cover the text without gaps and the diagnostics of the reader") {
            checkAll(CASES, texts) { text ->
                val tree = XmlSyntaxTree.parse(text, ignoring)
                val parsed = ignoring.parse(text)
                val isStripped = text.startsWith(MARK)

                tree.root.text shouldBe text
                tree.isContiguous() shouldBe true
                if (!isStripped) tree.diagnostics shouldBe parsed.diagnostics
            }
        }

        test("is never shown in a toString") {
            val tree = XmlSyntaxTree.parse("<a x='$SECRET'>$SECRET</a>")

            tree.shouldBeShownSafely()
            tree.root.descendants.forEach { node -> node.toString() shouldNotContain SECRET }
            tree.root.tokens.forEach { token -> token.toString() shouldNotContain SECRET }
        }
    }

    context("a tree nested as deep as the hard limit") {
        test("is walked without exhausting the stack") {
            val depth = XmlLimits.HARD_DEPTH
            val text = "<a>".repeat(depth) + "</a>".repeat(depth)
            val tree = XmlSyntaxTree.parse(text, xml { limits = XmlLimits.Relaxed })

            tree.isValid shouldBe true
            tree.root.descendants.count { node -> node.kind == XmlSyntaxKind.ELEMENT } shouldBe depth
            tree.root.tokens.count() shouldBe depth * TOKENS_PER_LEVEL
            tree.green shouldBe XmlSyntaxTree.parse(text, xml { limits = XmlLimits.Relaxed }).green
        }
    }
})
