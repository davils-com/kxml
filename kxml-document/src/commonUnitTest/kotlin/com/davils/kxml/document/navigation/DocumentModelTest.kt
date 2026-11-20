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

package com.davils.kxml.document.navigation

import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.xmlElement
import com.davils.kxml.core.syntax.XmlSyntaxTree
import com.davils.kxml.document.fixture.GROWTH_FACTOR
import com.davils.kxml.document.fixture.documentOf
import com.davils.kxml.document.fixture.documentTexts
import com.davils.kxml.document.fixture.shouldGrowLinearly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll

private const val CASES: Int = 500
private const val SMALL: Int = 2_000
private const val PROLOG_COMMENT: Int = 1
private const val ROOT_ELEMENT: Int = 2
private const val FIRST_ATTRIBUTE: Int = 3
private const val SECOND_ATTRIBUTE: Int = 4
private const val JOINED_TEXT: Int = 5
private const val CHILD: Int = 6
private const val CHILD_TEXT: Int = 7
private const val INSTRUCTION: Int = 8
private const val EPILOG_COMMENT: Int = 9
private const val CAPACITY: Int = 130
private const val LAST: Int = 129
private const val MIDDLE: Int = 64
private const val THIRD: Int = 3
private const val TEXT: String = "<!--p--><r a='1' b='2'>x<![CDATA[y]]><c>z</c><?pi d?></r><!--e-->"

private fun model(text: String = TEXT): DocumentModel = DocumentModel.of(documentOf(text))

private fun DocumentModel.shouldBeConsistent() {
    for (node in 1..<size) {
        val parent = parent(node)
        (parent in 0..<node) shouldBe true
        (end(node) in node + 1..end(parent)) shouldBe true
        isDescendant(node = node, ancestor = parent) shouldBe true
    }
    for (node in 0..<size) {
        val attributes = attributes(node).toList()
        attributes.forEach { attribute ->
            kind(attribute) shouldBe NodeKind.Attribute
            parent(attribute) shouldBe node
        }
        val covered = children(node).sumOf { child -> end(child) - child }
        covered + attributes.size + 1 shouldBe end(node) - node
    }
}

class DocumentModelTest : FunSpec({
    context("a document model") {
        test("orders the nodes of the XPath data model in document order") {
            val model = model()

            (0..<model.size).map(model::kind) shouldContainExactly listOf(
                NodeKind.Root,
                NodeKind.Comment,
                NodeKind.Element,
                NodeKind.Attribute,
                NodeKind.Attribute,
                NodeKind.Text,
                NodeKind.Element,
                NodeKind.Text,
                NodeKind.ProcessingInstruction,
                NodeKind.Comment,
            )
            model.name(FIRST_ATTRIBUTE) shouldBe XmlName("a")
            model.name(INSTRUCTION) shouldBe XmlName("pi")
            model.children(ROOT_ELEMENT).toList() shouldContainExactly listOf(JOINED_TEXT, CHILD, INSTRUCTION)
            val topLevel = listOf(PROLOG_COMMENT, ROOT_ELEMENT, EPILOG_COMMENT)
            model.children(model.root).toList() shouldContainExactly topLevel
            model.attributes(ROOT_ELEMENT).toList() shouldContainExactly listOf(FIRST_ATTRIBUTE, SECOND_ATTRIBUTE)
            model.nextSibling(FIRST_ATTRIBUTE).shouldBeNull()
            model.firstChild(JOINED_TEXT).shouldBeNull()
        }

        test("joins adjacent text and CDATA into one text node with one range") {
            val model = model()

            model.stringValue(JOINED_TEXT) shouldBe "xy"
            model.range(JOINED_TEXT)?.start shouldBe TEXT.indexOf('x')
            model.range(JOINED_TEXT)?.end shouldBe TEXT.indexOf("<c>")
            model.stringValue(ROOT_ELEMENT) shouldBe "xyz"
            model.stringValue(model.root) shouldBe "xyz"
            model.stringValue(PROLOG_COMMENT) shouldBe "p"
            model.isElement(ROOT_ELEMENT) shouldBe true
            model.isDescendant(node = CHILD_TEXT, ancestor = ROOT_ELEMENT) shouldBe true
            model.isDescendant(node = EPILOG_COMMENT, ancestor = ROOT_ELEMENT) shouldBe false
            model.isDescendant(node = ROOT_ELEMENT, ancestor = CHILD_TEXT) shouldBe false
            model.isElement(PROLOG_COMMENT) shouldBe false
            model.nextSibling(model.root).shouldBeNull()
        }

        test("joins text without ranges in a document built in code") {
            val root = xmlElement(XmlName("r")) {
                text("x")
                cdata("y")
            }
            val model = DocumentModel.of(XmlDocument(root))
            val element = model.firstChild(model.root).shouldNotBeNull()
            val text = model.firstChild(element).shouldNotBeNull()

            model.stringValue(text) shouldBe "xy"
            model.range(text).shouldBeNull()
        }

        test("keeps the namespace declarations of an element") {
            val model = model("<r xmlns:p='urn:p'><p:a/></r>")

            val outer = model.firstChild(model.root).shouldNotBeNull()
            val inner = model.firstChild(outer).shouldNotBeNull()

            model.declaredNamespaces(outer).single().uri shouldBe "urn:p"
            model.declaredNamespaces(inner) shouldBe emptyList()
        }

        test("is built from the document of a syntax tree, or not at all from one with errors") {
            val fromTree = DocumentModel.of(XmlSyntaxTree.parse(TEXT)).shouldNotBeNull()
            fromTree.size shouldBe model().size
            DocumentModel.of(XmlSyntaxTree.parse("<r>")).shouldBeNull()
        }

        test("keeps its structure consistent for any document") {
            checkAll(CASES, documentTexts) { text -> model(text).shouldBeConsistent() }
        }

        test("grows linearly with the nodes of the document") {
            shouldGrowLinearly(
                small = "<r>" + "<a b='c'>t</a>".repeat(SMALL) + "</r>",
                large = "<r>" + "<a b='c'>t</a>".repeat(SMALL * GROWTH_FACTOR) + "</r>",
            ) { text -> model(text) }
        }
    }

    context("a node set") {
        test("holds nodes in document order") {
            val set = NodeSet(capacity = CAPACITY)
            val other = NodeSet(capacity = CAPACITY)

            set.add(LAST)
            set.add(THIRD)
            other.add(MIDDLE)
            set.addAll(other)

            set.nodes().toList() shouldContainExactly listOf(THIRD, MIDDLE, LAST)
            set.size shouldBe THIRD
            set.first() shouldBe THIRD
            (MIDDLE in set) shouldBe true
            (MIDDLE + 1 in set) shouldBe false
            val copied = set.copy()
            copied.nodes().toList() shouldBe set.nodes().toList()
            NodeSet(capacity = 1).isEmpty shouldBe true
            set.isEmpty shouldBe false
        }
    }
})
