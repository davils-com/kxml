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

package com.davils.kxml.core.syntax.edit

import com.davils.kore.testing.HostileFragments
import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.syntax.XmlSyntaxTree
import com.davils.kxml.core.syntax.ast.XmlElementSyntax
import com.davils.kxml.core.xml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val CASES: Int = 500
private const val MOST_PIECES: Int = 12
private const val DOCUMENT: String = "<r xmlns:p='urn:p' xmlns:q='urn:p' a='1' b=\"2\"><c/><d>x<!--k--></d></r>"
private const val NUL: String = "\u0000"
private const val NEL: String = "\u0085"
private const val CR: String = "\r"

private val markup: List<String> = listOf(
    "a", " ", "<", ">", "&", "&amp;", ";", "'", "\"", "=", "]]>", "<!--", "-->", "<?", "?>", "\t", "\n", CR, NEL,
    "\u2028", "\u0080", "\u00E4", "\uD83D\uDE00",
)

private val pieces: List<String> = markup + HostileFragments.all

private val values: Arb<String> =
    Arb.list(Arb.element(pieces), 0..MOST_PIECES).map { parts -> parts.joinToString(separator = "") }

private fun treeOf(text: String = DOCUMENT): XmlSyntaxTree = XmlSyntaxTree.parse(text)

private fun XmlSyntaxTree.rootElement(): XmlElementSyntax = document.root.shouldNotBeNull()

private fun XmlEditResult.edited(): XmlSyntaxTree {
    val edited = shouldBeInstanceOf<XmlEditResult.Edited>()
    edited.tree.isValid shouldBe true
    return edited.tree
}

private fun XmlEditResult.refusal(): XmlEditRefusal = shouldBeInstanceOf<XmlEditResult.Refused>().reason

private fun composedRoot(tree: XmlSyntaxTree): XmlNode.Element {
    val parsed = tree.compose().shouldBeInstanceOf<XmlParseResult.Parsed>()
    return parsed.document.root
}

class XmlEditorTest : FunSpec({
    context("setAttribute") {
        test("replaces the value of an attribute in place and keeps its quotes") {
            val tree = treeOf()
            val editor = XmlEditor(tree)

            val edited = editor.setAttribute(element = tree.rootElement(), name = "a", value = "x'y").edited()

            edited.text shouldBe DOCUMENT.replace(oldValue = "a='1'", newValue = "a='x&apos;y'")
        }

        test("adds a new attribute after the others in double quotes") {
            val tree = treeOf()
            val editor = XmlEditor(tree)
            val child = tree.rootElement().elements.first()

            val edited = editor.setAttribute(element = child, name = "p:z", value = "\"<&>").edited()

            edited.text shouldContain "<c p:z=\"&quot;&lt;&amp;&gt;\"/>"
        }

        test("refuses invalid, undeclared, namespace and duplicate names") {
            val tree = treeOf("<r xmlns:p='urn:p' xmlns:q='urn:p' p:x='1'/>")
            val editor = XmlEditor(tree)
            val root = tree.rootElement()

            editor.setAttribute(element = root, name = "1a", value = "v").refusal() shouldBe XmlEditRefusal.InvalidName
            editor.setAttribute(element = root, name = "z:a", value = "v").refusal() shouldBe
                XmlEditRefusal.UndeclaredPrefix
            editor.setAttribute(element = root, name = "xmlns:z", value = "urn:z").refusal() shouldBe
                XmlEditRefusal.NamespaceDeclaration
            editor.setAttribute(element = root, name = "xmlns", value = "urn:z").refusal() shouldBe
                XmlEditRefusal.NamespaceDeclaration
            editor.setAttribute(element = root, name = "q:x", value = "v").refusal() shouldBe
                XmlEditRefusal.DuplicateAttribute
            editor.setAttribute(element = root, name = "x", value = "v").edited().isValid shouldBe true
        }

        test("reads back exactly the value that was set, or refuses a character it cannot represent") {
            checkAll(CASES, values) { value ->
                val tree = treeOf()
                val editor = XmlEditor(tree)
                val result = editor.setAttribute(element = tree.rootElement(), name = "n", value = value)

                if (result is XmlEditResult.Edited) {
                    composedRoot(result.edited()).attribute(XmlName("n"))?.value shouldBe value
                }
                if (result is XmlEditResult.Refused) result.reason shouldBe XmlEditRefusal.InvalidCharacter
            }
        }
    }

    context("removeAttribute") {
        test("removes an attribute with the whitespace before it") {
            val tree = treeOf()
            val editor = XmlEditor(tree)

            editor.removeAttribute(tree.rootElement(), "a").edited().text shouldBe
                DOCUMENT.replace(oldValue = " a='1'", newValue = "")
        }

        test("refuses a missing attribute and a namespace declaration") {
            val tree = treeOf()
            val editor = XmlEditor(tree)

            editor.removeAttribute(tree.rootElement(), "z").refusal() shouldBe XmlEditRefusal.MissingAttribute
            editor.removeAttribute(tree.rootElement(), "xmlns:p").refusal() shouldBe XmlEditRefusal.NamespaceDeclaration
        }
    }

    context("setText") {
        test("replaces the whole content and writes an empty-element tag as two tags") {
            val tree = treeOf()
            val editor = XmlEditor(tree)
            val (empty, full) = tree.rootElement().elements

            editor.setText(full, "a<b").edited().text shouldContain "<d>a&lt;b</d>"
            editor.setText(empty, "&").edited().text shouldContain "<c>&amp;</c>"
        }

        test("writes characters of XML 1.1 that would change as references and refuses NUL") {
            val tree = treeOf("<?xml version='1.1'?><r/>")
            val editor = XmlEditor(tree)

            editor.setText(tree.rootElement(), "a${NEL}b${CR}c").edited().text shouldContain "a&#x85;b&#xD;c"
            editor.setText(tree.rootElement(), "a$NUL").refusal() shouldBe XmlEditRefusal.InvalidCharacter
        }

        test("reads back exactly the text that was set, or refuses a character it cannot represent") {
            checkAll(CASES, values) { text ->
                val tree = treeOf()
                val editor = XmlEditor(tree)
                val result = editor.setText(tree.rootElement().elements.last(), text)

                if (result is XmlEditResult.Edited) {
                    val child = composedRoot(result.edited()).elements.last()
                    child.text shouldBe text
                }
                if (result is XmlEditResult.Refused) result.reason shouldBe XmlEditRefusal.InvalidCharacter
            }
        }
    }

    context("elements") {
        test("appends a child to an empty element and to one with content") {
            val tree = treeOf()
            val editor = XmlEditor(tree)
            val (empty, full) = tree.rootElement().elements

            editor.appendElement(empty, "p:e").edited().text shouldContain "<c><p:e/></c>"
            editor.appendElement(full, "e").edited().text shouldContain "<d>x<!--k--><e/></d>"
            editor.appendElement(full, "z:e").refusal() shouldBe XmlEditRefusal.UndeclaredPrefix
            editor.appendElement(full, "xmlns:e").refusal() shouldBe XmlEditRefusal.InvalidName
            editor.appendElement(full, "<e").refusal() shouldBe XmlEditRefusal.InvalidName
        }

        test("removes a child but never the root") {
            val tree = treeOf()
            val editor = XmlEditor(tree)

            editor.removeElement(tree.rootElement().elements.first()).edited().text shouldBe
                DOCUMENT.replace(oldValue = "<c/>", newValue = "")
            editor.removeElement(tree.rootElement()).refusal() shouldBe XmlEditRefusal.RootElement
        }

        test("renames both tags") {
            val tree = treeOf()
            val editor = XmlEditor(tree)
            val (empty, full) = tree.rootElement().elements

            val renamed = editor.rename(full, "p:f")

            renamed.edited().text shouldContain "<p:f>x<!--k--></p:f>"
            (renamed as XmlEditResult.Edited).edits shouldHaveSize 2
            renamed.toString() shouldBe "Edited(2 edits)"
            editor.rename(empty, "g").edited().text shouldContain "<g/>"
            editor.rename(empty, "z:g").refusal() shouldBe XmlEditRefusal.UndeclaredPrefix
        }
    }

    context("an editor") {
        test("keeps the entity references and declarations it does not touch") {
            val text = "<!DOCTYPE r [<!ENTITY e 'v'>]><r><a>&e;</a><b/></r>"
            val tree = XmlSyntaxTree.parse(text, xml { doctype = XmlDoctype.Internal })
            val b = tree.rootElement().elements.last()
            val editor = XmlEditor(tree)

            val edited = editor.setAttribute(element = b, name = "n", value = "1").edited()

            edited.text shouldBe text.replace(oldValue = "<b/>", newValue = "<b n=\"1\"/>")
        }

        test("refuses a tree with errors and an element of another tree") {
            val broken = treeOf("<r><a></r>")
            val other = treeOf()

            val brokenEditor = XmlEditor(broken)
            val otherEditor = XmlEditor(treeOf())

            brokenEditor.setText(broken.rootElement(), "x").refusal() shouldBe XmlEditRefusal.InvalidDocument
            otherEditor.setText(other.rootElement(), "x").refusal() shouldBe XmlEditRefusal.ForeignElement
            XmlEditResult.Refused(XmlEditRefusal.RootElement).toString() shouldBe "Refused(RootElement)"
            val cases = listOf(broken to XmlEditRefusal.InvalidDocument, other to XmlEditRefusal.ForeignElement)
            cases.forEach { (source, reason) ->
                val element = source.rootElement()
                val editor = XmlEditor(broken.takeIf { reason == XmlEditRefusal.InvalidDocument } ?: treeOf())
                val results = listOf(
                    editor.setAttribute(element = element, name = "n", value = "v"),
                    editor.removeAttribute(element, "n"),
                    editor.appendElement(element, "e"),
                    editor.removeElement(element),
                    editor.rename(element, "e"),
                )
                results.forEach { result -> result.refusal() shouldBe reason }
            }
        }

        test("describes its results") {
            val tree = treeOf()
            val editor = XmlEditor(tree)
            val refused = XmlEditResult.Refused(XmlEditRefusal.RootElement)

            editor.setText(tree.rootElement(), "x").toString() shouldBe "Edited(1 edit)"
            refused shouldBe XmlEditResult.Refused(XmlEditRefusal.RootElement)
            refused.hashCode() shouldBe XmlEditResult.Refused(XmlEditRefusal.RootElement).hashCode()
            (refused == XmlEditResult.Refused(XmlEditRefusal.MissingAttribute)) shouldBe false
            refused.equals("x") shouldBe false
            editor.toString() shouldBe "XmlEditor($tree)"
        }
    }
})
