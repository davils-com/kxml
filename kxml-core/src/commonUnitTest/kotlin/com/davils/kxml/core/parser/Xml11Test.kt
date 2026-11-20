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

package com.davils.kxml.core.parser

import com.davils.kxml.core.Xml
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.emit.XmlEmitOptions
import com.davils.kxml.core.fixture.LF
import com.davils.kxml.core.fixture.document
import com.davils.kxml.core.fixture.refusal
import com.davils.kxml.core.fixture.root
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.xml
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val VERSION_11: String = "<?xml version='1.1'?>"
private const val VERSION_10: String = "<?xml version='1.0'?>"
private const val NEXT_LINE: String = "\u0085"
private const val LINE_SEPARATOR: String = " "
private const val CONTROL: String = "\u0080"
private const val RETURN: String = "\r"

private fun textOf(text: String): String = Xml.Default.root(text).text

class Xml11Test : FunSpec({
    context("line ends") {
        test("become line feeds in the text of a document of XML 1.1") {
            val text = "$VERSION_11<a>1${NEXT_LINE}2${LINE_SEPARATOR}3$RETURN${NEXT_LINE}4$RETURN${LINE_SEPARATOR}5</a>"

            textOf(text) shouldBe "1${LF}2${LF}3${LF}4$LF${LF}5"
        }

        test("become spaces in the attributes of a document of XML 1.1") {
            val element = Xml.Default.root("$VERSION_11<a x='1${NEXT_LINE}2${LINE_SEPARATOR}3'/>")

            element.attributes.single().value shouldBe "1 2 3"
        }

        test("become line feeds in comments, character data and instructions of XML 1.1") {
            val text = "$VERSION_11<a><!--1${NEXT_LINE}2--><![CDATA[3${LINE_SEPARATOR}4]]></a>"
            val document = Xml.Default.document(text)
            val children = document.root.children

            children[0].shouldBeInstanceOf<XmlNode.Comment>().value shouldBe "1${LF}2"
            children[1].shouldBeInstanceOf<XmlNode.CData>().value shouldBe "3${LF}4"
        }

        test("stay as they are in a document of XML 1.0 or of a later minor version") {
            textOf("$VERSION_10<a>1${NEXT_LINE}2${LINE_SEPARATOR}3</a>") shouldBe "1${NEXT_LINE}2${LINE_SEPARATOR}3"
            textOf("<?xml version='1.2'?><a>1${NEXT_LINE}2</a>") shouldBe "1${NEXT_LINE}2"
            textOf("<a>1${NEXT_LINE}2</a>") shouldBe "1${NEXT_LINE}2"
        }
    }

    context("restricted characters") {
        test("are refused as they stand in a document of XML 1.1") {
            Xml.Default.refusal("$VERSION_11<a>$CONTROL</a>") shouldBe XmlDiagnosticCode.InvalidCharacter
            Xml.Default.refusal("$VERSION_11<a x='$CONTROL'/>") shouldBe XmlDiagnosticCode.InvalidCharacter
            Xml.Default.refusal("$VERSION_11<a><!--$CONTROL--></a>") shouldBe XmlDiagnosticCode.InvalidCharacter
        }

        test("are read as they stand in a document of XML 1.0") {
            textOf("$VERSION_10<a>$CONTROL</a>") shouldBe CONTROL
        }

        test("are read from character references to C0 controls only in a document of XML 1.1") {
            textOf("$VERSION_11<a>&#x1;&#x1F;</a>") shouldBe "\u0001\u001F"
            Xml.Default.refusal("$VERSION_10<a>&#x1;</a>") shouldBe XmlDiagnosticCode.InvalidReference
            Xml.Default.refusal("$VERSION_11<a>&#x0;</a>") shouldBe XmlDiagnosticCode.InvalidReference
        }
    }

    context("prefixes") {
        test("may be undeclared in a document of XML 1.1 and are unknown afterwards") {
            val text = "$VERSION_11<a xmlns:p='urn:p'><b xmlns:p=''><p:c/></b></a>"

            Xml.Default.refusal(text) shouldBe XmlDiagnosticCode.UndeclaredPrefix
            val root = Xml.Default.root("$VERSION_11<a xmlns:p='urn:p'><b xmlns:p=''/></a>")

            root.elements.size shouldBe 1
        }

        test("may not be undeclared in a document of XML 1.0") {
            Xml.Default.refusal("<a xmlns:p='urn:p'><b xmlns:p=''/></a>") shouldBe XmlDiagnosticCode.EmptyPrefixBinding
        }

        test("are undeclared by the writer only in a document of XML 1.1") {
            val text = "$VERSION_11<a xmlns:p='urn:p'><b xmlns:p=''/></a>"
            val compact = xml { emitter = XmlEmitOptions.Compact }
            val document = compact.document(text)
            val written = compact.emit(document)

            compact.document(written) shouldBe document
            shouldThrow<IllegalArgumentException> { compact.emit(document.root) }
        }
    }
})
