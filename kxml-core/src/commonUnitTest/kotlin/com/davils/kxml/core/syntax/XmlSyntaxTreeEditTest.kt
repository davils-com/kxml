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

import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange
import com.davils.kore.testing.HostileFragments
import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.xml
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll

private const val CASES: Int = 1_000
private const val MOST_STEPS: Int = 40
private const val CODES: Int = 12
private const val LONGEST_EDIT: Int = 12
private const val NEWLINE: String = "\n"
private const val ELEMENTS_OF_NESTED: Long = 5L
private const val FIRST_CHILD: Int = 1
private const val LAST_CHILD: Int = 3
private const val NESTED: String = "<r xmlns:p='urn:p'><a x='1'>one</a><b><c>two</c></b><a>three</a></r>"

private val markup: List<String> = listOf(
    "", "x", " ", "<", ">", "&", ";", "&amp;", "&#65;", "'", "\"", "=", "/", "<d/>", "<d>", "</d>", "</c>",
    "<p:e/>", "<q:e/>", "<!--n-->", "<![CDATA[<]]>", "<?pi?>", "]]>", "\u0085", "\r\n", "x='2'", "xmlns:q='urn:q'",
)

private val replacements: List<String> = markup + HostileFragments.all

private val documents = Arb.bind(Arb.list(Arb.int(0..<CODES), 0..MOST_STEPS), Arb.int(0..2)) { codes, version ->
    documentOf(codes, version)
}

private val edits = Arb.bind(
    genA = documents,
    genB = Arb.int(),
    genC = Arb.int(0..LONGEST_EDIT),
    genD = Arb.element(replacements),
    bindFn = ::editOf,
)

private val nestedEdits = Arb.bind(Arb.int(0..NESTED.length), Arb.element(replacements)) { at, replacement ->
    TextEdit(TextRange.at(at), replacement)
}

private fun editOf(document: String, at: Int, length: Int, replacement: String): Pair<String, TextEdit> {
    val start = at.mod(document.length + 1)
    val end = minOf(a = start + length, b = document.length)
    return document to TextEdit(TextRange(start = start, end = end), replacement)
}

private fun StringBuilder.close(name: String) {
    append("</")
    append(name)
    append('>')
}

private fun documentOf(codes: List<Int>, version: Int): String {
    val open = ArrayDeque<String>()
    val body = StringBuilder()
    codes.forEach { code -> step(code, open, body) }
    while (open.isNotEmpty()) body.close(open.removeLast())
    val declaration = listOf("", "<?xml version='1.0'?>", """<?xml version="1.1"?>""" + NEWLINE)[version]
    return "$declaration<r xmlns:p='urn:p'>$body</r>"
}

private fun step(code: Int, open: ArrayDeque<String>, body: StringBuilder) {
    val piece = listOf(
        "<a x='1'>", "", "t &amp; u", "<![CDATA[<c>]]>", "<!-- c -->", "<?pi d?>", "<c y=\"&lt;\"/>",
        "<p:d p:z='2'>", "\n  ", "&#x41;", "<b>", "\r\n",
    )[code]
    val opened = piece.removePrefix("<")
    val name = opened.substringBefore(' ').substringBefore('>')
    val isOpening = code == 0 || code == 7 || code == 10
    val isClosing = code == 1 && open.isNotEmpty()
    if (isClosing) body.close(open.removeLast())
    body.append(piece)
    if (isOpening) open.addLast(name)
}

private fun shouldEqualFullParse(edited: XmlSyntaxTree) {
    val full = XmlSyntaxTree.parse(edited.text, edited.xml)
    withClue(edited.text) {
        edited.green shouldBe full.green
        edited.diagnostics shouldBe full.diagnostics
        edited.root.text shouldBe edited.text
    }
}

private fun childrenOfRoot(tree: XmlSyntaxTree): List<Any> {
    val root = tree.green.children.single()
    return root.children
}

class XmlSyntaxTreeEditTest : FunSpec({
    context("edit") {
        test("parses only the element that encloses the edit and shares the rest of the tree") {
            val tree = XmlSyntaxTree.parse(NESTED)
            val start = NESTED.indexOf("two")

            val edited = tree.edit(listOf(TextEdit(TextRange(start = start, end = start + 3), "2 &amp; <d/>")))

            edited.text shouldBe NESTED.replace(oldValue = "two", newValue = "2 &amp; <d/>")
            shouldEqualFullParse(edited)
            val before = childrenOfRoot(tree)
            val after = childrenOfRoot(edited)
            after[FIRST_CHILD] shouldBeSameInstanceAs before[FIRST_CHILD]
            after[LAST_CHILD] shouldBeSameInstanceAs before[LAST_CHILD]
        }

        test("keeps the namespaces and the version of the document around the element") {
            val tree = XmlSyntaxTree.parse("<?xml version='1.1'?><r xmlns:p='urn:p'><a>x</a></r>")
            val at = tree.text.indexOf(">x<") + 1

            val declared = tree.edit(listOf(TextEdit(TextRange.at(at), "<p:b/>\u0085")))
            val undeclared = tree.edit(listOf(TextEdit(TextRange.at(at), "<q:b/>")))

            declared.isValid shouldBe true
            shouldEqualFullParse(declared)
            undeclared.diagnostics.single().code shouldBe XmlDiagnosticCode.UndeclaredPrefix
            shouldEqualFullParse(undeclared)
        }

        test("parses the whole text when the edit reaches a tag, the tree has errors or a document type declaration") {
            val tree = XmlSyntaxTree.parse(NESTED)
            val broken = XmlSyntaxTree.parse("<r><a>x</r>")
            val internal = xml { doctype = XmlDoctype.Internal }
            val typed = XmlSyntaxTree.parse("<!DOCTYPE r [<!ENTITY e 'v'>]><r><a>x</a></r>", internal)
            val tagStart = NESTED.indexOf("<a x")

            shouldEqualFullParse(tree.edit(listOf(TextEdit(TextRange(start = tagStart, end = tagStart + 2), "<z"))))
            shouldEqualFullParse(broken.edit(listOf(TextEdit(TextRange.at(broken.text.indexOf("</r>")), "</a>"))))
            shouldEqualFullParse(typed.edit(listOf(TextEdit(TextRange.at(typed.text.indexOf('x')), "&e;"))))
        }

        test("parses the whole text when the element is no longer one element or the text grows too long") {
            val tree = XmlSyntaxTree.parse(NESTED)
            val inner = NESTED.indexOf("two")
            val closing = NESTED.indexOf("</c>")
            val short = XmlSyntaxTree.parse(NESTED, xml { limits { maximumInputLength = NESTED.length } })

            shouldEqualFullParse(tree.edit(listOf(TextEdit(TextRange.at(closing), "</c><c>"))))
            shouldEqualFullParse(tree.edit(listOf(TextEdit(TextRange(start = closing, end = closing + 4), "</c>x"))))
            val longer = short.edit(listOf(TextEdit(TextRange.at(inner), "more")))
            longer.diagnostics.single().code shouldBe XmlDiagnosticCode.InputTooLong
            shouldEqualFullParse(longer)
        }

        test("charges the limits to the whole edited document") {
            val limited = xml { limits { maximumElements = ELEMENTS_OF_NESTED } }
            val tree = XmlSyntaxTree.parse(NESTED, limited)
            val at = NESTED.indexOf("two")

            val edited = tree.edit(listOf(TextEdit(TextRange.at(at), "<d/><e/>")))

            tree.isValid shouldBe true
            edited.diagnostics.single().code shouldBe XmlDiagnosticCode.TooManyElements
            shouldEqualFullParse(edited)
        }

        test("returns the same tree for no edits and refuses overlapping ones") {
            val tree = XmlSyntaxTree.parse(NESTED)

            tree.edit(emptyList()) shouldBeSameInstanceAs tree
            shouldThrow<IllegalArgumentException> {
                val wide = TextEdit(TextRange(start = 1, end = 4), "x")
                val inner = TextEdit(TextRange(start = 2, end = 3), "y")
                tree.edit(listOf(wide, inner))
            }
        }

        test("gives the tree of a full parse for any edit of a well-formed document") {
            checkAll(CASES, edits) { (document, edit) ->
                shouldEqualFullParse(XmlSyntaxTree.parse(document).edit(listOf(edit)))
            }
        }

        test("gives the tree of a full parse for several edits at once") {
            checkAll(CASES, nestedEdits, nestedEdits) { first, second ->
                val isApart = first.range.start != second.range.start
                val edits = listOf(first, second).takeIf { isApart } ?: listOf(first)

                shouldEqualFullParse(XmlSyntaxTree.parse(NESTED).edit(edits))
            }
        }
    }
})
