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

package com.davils.kxml.core.syntax.format

import com.davils.kxml.core.emit.XmlEmitOptions
import com.davils.kxml.core.emit.XmlLineBreak
import com.davils.kxml.core.syntax.XmlSyntaxTree
import com.davils.kxml.core.syntax.fixture.documents
import com.davils.kxml.core.xml
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.checkAll

private const val CASES: Int = 1_000
private const val NESTED: String = "<?xml version='1.0'?><!--c--><r><a><b/></a><c>text</c></r>"
private const val LF: String = "\n"
private const val CRLF: String = "\r\n"
private const val BYTE_ORDER_MARK: String = "\uFEFF"

private val formatted: String = listOf(
    "<?xml version='1.0'?>",
    "<!--c-->",
    "<r>",
    "  <a>",
    "    <b/>",
    "  </a>",
    "  <c>text</c>",
    "</r>",
    "",
).joinToString(separator = LF)

private fun formattedOf(text: String, formatter: XmlFormatter = XmlFormatter()): XmlFormatResult.Formatted {
    val result = formatter.format(XmlSyntaxTree.parse(text))
    return result.shouldBeInstanceOf<XmlFormatResult.Formatted>()
}

private fun refusalOf(tree: XmlSyntaxTree): XmlFormatRefusal {
    val result = XmlFormatter().format(tree)
    val refused = result.shouldBeInstanceOf<XmlFormatResult.Refused>()
    return refused.reason
}

class XmlFormatterTest : FunSpec({
    context("format") {
        test("indents element-only content and puts every node around the root on a line of its own") {
            val result = formattedOf(NESTED)

            result.tree.text shouldBe formatted
            result.tree.isValid shouldBe true
            result.toString() shouldBe "Formatted(${result.edits.size} edits)"
        }

        test("changes nothing in a formatted document and keeps a byte order mark first") {
            formattedOf(formatted).edits.shouldBeEmpty()
            formattedOf(BYTE_ORDER_MARK + NESTED).tree.text shouldBe BYTE_ORDER_MARK + formatted
        }

        test("keeps mixed content, text and everything under xml:space preserve") {
            val text = "<r><p>a <b>x</b> c</p><q xml:space='preserve'> <x> <y/></x> </q><t>&#32;<u/></t></r>"

            val result = formattedOf(text).tree.text

            result shouldContain "<p>a <b>x</b> c</p>"
            result shouldContain "<q xml:space='preserve'> <x> <y/></x> </q>"
            result shouldContain "<t>&#32;<u/></t>"
        }

        test("formats again below xml:space default") {
            val text = "<r xml:space='preserve'><s xml:space='default'><a/><b/></s> <c/></r>"

            val expected = listOf("<s xml:space='default'>", "    <a/>", "    <b/>", "  </s> <c/>").joinToString(LF)
            formattedOf(text).tree.text shouldContain expected
        }

        test("writes everything on one line without an indent and breaks lines as the options ask") {
            val compact = XmlFormatter(XmlEmitOptions.Compact)
            val crlf = XmlFormatter(xml { emitter { lineBreak = XmlLineBreak.CrLf } }.configuration.emitter)

            formattedOf(formatted, compact).tree.text shouldBe NESTED
            formattedOf(NESTED, crlf).tree.text shouldBe formatted.replace(oldValue = LF, newValue = CRLF)
            compact.toString() shouldBe "XmlFormatter(${XmlEmitOptions.Compact})"
        }

        test("refuses a tree with errors and a result too long to be read again") {
            val short = xml { limits { maximumInputLength = NESTED.length } }

            refusalOf(XmlSyntaxTree.parse("<r><a></r>")) shouldBe XmlFormatRefusal.SyntaxErrors
            refusalOf(XmlSyntaxTree.parse(NESTED, short)) shouldBe XmlFormatRefusal.OutputTooLong
            XmlFormatResult.Refused(XmlFormatRefusal.OutputTooLong).toString() shouldBe "Refused(OutputTooLong)"
        }

        test("keeps the document of any well-formed text and gives no edits when it formats its result again") {
            checkAll(CASES, documents) { text ->
                val first = formattedOf(text)
                val second = XmlFormatter().format(first.tree)

                withClue(text) {
                    second.shouldBeInstanceOf<XmlFormatResult.Formatted>().edits.shouldBeEmpty()
                    isSameInfoset(original = XmlSyntaxTree.parse(text), formatted = first.tree) shouldBe true
                }
            }
        }
    }

    context("isSameInfoset") {
        test("tells documents apart beyond whitespace in element-only content") {
            fun same(first: String, second: String): Boolean =
                isSameInfoset(original = XmlSyntaxTree.parse(first), formatted = XmlSyntaxTree.parse(second))

            same(first = "<r><a/> <b/></r>", second = "<r>\n<a/><b/></r>") shouldBe true
            val preserved = "<r xml:space='preserve'><a/> <b/></r>"
            same(first = preserved, second = preserved.replace(oldValue = "> <", newValue = "><")) shouldBe false
            same(first = "<r>a<b/></r>", second = "<r>a <b/></r>") shouldBe false
            same(first = "<r a='1'/>", second = "<r a='2'/>") shouldBe false
            same(first = "<r><a/></r>", second = "<r><b/></r>") shouldBe false
            same(first = "<!--x--><r/>", second = "<r/>") shouldBe false
            same(first = "<r/>", second = "<r") shouldBe false
            same(first = "<r", second = "<r/>") shouldBe false
            same(first = "<?xml version='1.0'?><r/>", second = "<r/>") shouldBe false
            same(first = "<r/><!--x-->", second = "<r/>") shouldBe false
            val errors = XmlFormatResult.Refused(XmlFormatRefusal.SyntaxErrors)
            (errors == XmlFormatResult.Refused(XmlFormatRefusal.OutputTooLong)) shouldBe false
            errors.equals("x") shouldBe false
            errors.hashCode() shouldBe XmlFormatRefusal.SyntaxErrors.hashCode()
            XmlFormatResult.Refused(XmlFormatRefusal.SyntaxErrors) shouldBe
                XmlFormatResult.Refused(XmlFormatRefusal.SyntaxErrors)
        }
    }
})
