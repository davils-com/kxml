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
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.fixture.GROWTH_FACTOR
import com.davils.kxml.core.fixture.shouldGrowLinearly
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.node.XmlDeclaration
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlElementBuilder
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.node.xmlComment
import com.davils.kxml.core.node.xmlElement
import com.davils.kxml.core.node.xmlInstruction
import com.davils.kxml.core.xml
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val CASES: Int = 500
private const val MOST_STEPS: Int = 30
private const val MOST_PIECES: Int = 6
private const val SMALL: Int = 50
private const val SHORT_OUTPUT: Int = 10
private const val CR: String = "\r"
private const val NEL: String = "\u0085"
private const val LINE_SEPARATOR: String = "\u2028"
private const val C1: String = "\u0080"
private const val VERSION_11: String = "1.1"
private const val CHARACTERS: String = "T:"
private const val ROOT: String = "r"

private val compact: Xml = xml { emitter = XmlEmitOptions.Compact }

private val pieces: List<String> = listOf(
    "a", " ", "<", ">", "&", "\"", "'", "]]>", "--", "?>", "\t", "\n", CR, NEL, LINE_SEPARATOR, C1, "\u00e4",
    "\uD83D\uDE00",
)
private val names: List<XmlName> = listOf(
    XmlName("a"),
    XmlName(local = "b", namespace = "urn:b"),
    XmlName(local = "c", namespace = "urn:c", prefix = "p"),
    XmlName(local = "d", namespace = "urn:b", prefix = "q"),
)

private val values: Arb<String> =
    Arb.list(Arb.element(pieces), 0..MOST_PIECES).map { parts -> parts.joinToString(separator = "") }

private val steps: List<XmlElementBuilder.(Int, String) -> Unit> = listOf(
    { position, value -> withAttribute(position, value) },
    { _, value -> text("$value|") },
    { _, value -> cdata(value) },
    { position, _ -> comment("c$position") },
    { position, _ -> instruction(target = "pi", data = "d$position") },
    { _, _ -> element(names[2]) { element(names[3]) { text("deep") } } },
)

private fun XmlElementBuilder.withAttribute(position: Int, value: String) {
    val attributeName = names[(position + 1) % names.size]
    element(names[position % names.size]) { attribute(attributeName, value) }
}

private val documents: Arb<XmlDocument> = Arb.bind(
    genA = Arb.list(Arb.int(steps.indices), 0..MOST_STEPS),
    genB = Arb.list(values, 0..MOST_STEPS),
    genC = Arb.boolean(),
    bindFn = ::documentOf,
)

private fun documentOf(codes: List<Int>, texts: List<String>, isVersion11: Boolean): XmlDocument {
    val root = xmlElement(names[1]) {
        codes.forEachIndexed { position, code -> steps[code](this, position, texts.getOrElse(position) { "x" }) }
    }
    val declaration = XmlDeclaration(version = VERSION_11).takeIf { isVersion11 }
    return XmlDocument(root, declaration = declaration)
}

private fun characterDataOf(node: XmlNode): String? = (node as? XmlNode.Text)?.value ?: (node as? XmlNode.CData)?.value

private fun shapeOf(node: XmlNode): String {
    if (node !is XmlNode.Element) return node.toString() + characterDataOf(node).orEmpty()
    val attributes = node.attributes.sortedBy { attribute -> attribute.name.toString() }
    val content = mutableListOf<String>()
    node.children.forEach { child -> appendShape(content, child) }
    return "E(${node.name}|$attributes|$content)"
}

private fun appendShape(content: MutableList<String>, child: XmlNode) {
    val characters = characterDataOf(child)
    val isJoined = characters != null && content.lastOrNull()?.startsWith(CHARACTERS) == true
    if (isJoined) content[content.lastIndex] = content.last() + characters
    if (characters != null && !isJoined) content += CHARACTERS + characters
    if (characters == null) content += shapeOf(child)
}

private fun readBack(text: String): XmlDocument {
    val parsed = compact.parse(text).shouldBeInstanceOf<XmlParseResult.Parsed>()
    return parsed.document
}

private fun namespacesOnLevels(depth: Int): XmlNode.Element {
    val leaf = xmlElement(XmlName("f")) {
        for (level in 0..<depth) attribute(XmlName(local = "x", namespace = "urn:$level"), "v")
    }
    return (depth - 1 downTo 0).fold(leaf) { child, level ->
        xmlElement(XmlName(local = "e", namespace = "urn:$level", prefix = "p$level")) { node(child) }
    }
}

private fun inVersion11(root: XmlNode.Element): XmlDocument =
    XmlDocument(root, declaration = XmlDeclaration(version = VERSION_11))

class XmlWriterTest : FunSpec({
    context("XML 1.1") {
        test("writes the characters reading would change as references and reads them back") {
            val value = "a${C1}b${NEL}c${LINE_SEPARATOR}d"
            val root = xmlElement(XmlName(ROOT)) { text(value) }

            val written = compact.emit(inVersion11(root))

            written shouldContain "a&#128;b&#133;c&#8232;d"
            readBack(written).root.text shouldBe value
            compact.emit(root) shouldContain value
        }

        test("refuses comments and instructions that would change when they are read") {
            val comment = inVersion11(xmlElement(XmlName(ROOT)) { node(xmlComment("a${NEL}b")) })
            val instruction = xmlElement(XmlName(ROOT)) { node(xmlInstruction(target = "pi", data = "a${CR}b")) }

            shouldThrow<IllegalArgumentException> { compact.emit(comment) }
            shouldThrow<IllegalArgumentException> { compact.emit(instruction) }
        }
    }

    context("CDATA sections") {
        test("split around ]]> and around characters only references carry") {
            val written = compact.emit(xmlElement(XmlName(ROOT)) { cdata("a]]>b${CR}c") })

            written shouldBe "<r><![CDATA[a]]]]><![CDATA[>b]]>&#13;<![CDATA[c]]></r>"
            readBack(written).root.text shouldBe "a]]>b${CR}c"
        }
    }

    context("the limit of the output") {
        test("stops the writer before it builds a longer text") {
            val short = xml {
                emitter = XmlEmitOptions.Compact
                limits { maximumOutputLength = SHORT_OUTPUT }
            }

            short.emit(xmlElement(XmlName(ROOT))) shouldBe "<r/>"
            shouldThrow<IllegalStateException> { short.emit(xmlElement(XmlName(ROOT)) { text("0123456789") }) }
        }
    }

    context("a round trip") {
        test("reads back what was written, for any tree of either version") {
            checkAll(CASES, documents) { document ->
                val written = compact.emit(document)

                withClue(written) { shapeOf(readBack(written).root) shouldBe shapeOf(document.root) }
            }
        }
    }

    context("the work of writing") {
        test("grows linearly with namespaces declared on many levels and used at once") {
            shouldGrowLinearly(small = "$SMALL", large = "${SMALL * GROWTH_FACTOR}") { depth ->
                compact.emit(namespacesOnLevels(depth.toInt()))
            }
        }
    }
})
