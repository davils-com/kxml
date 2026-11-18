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
import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.fixture.document
import com.davils.kxml.core.fixture.refusal
import com.davils.kxml.core.xml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val refusals: Map<String, Pair<String, XmlDiagnosticCode>> = mapOf(
    "an empty document" to ("" to XmlDiagnosticCode.MissingRoot),
    "only a comment" to ("<!-- c -->" to XmlDiagnosticCode.MissingRoot),
    "text before the root" to ("hello<a/>" to XmlDiagnosticCode.UnexpectedContent),
    "text after the root" to ("<a/>hello" to XmlDiagnosticCode.ContentAfterRoot),
    "a second root" to ("<a/><b/>" to XmlDiagnosticCode.ContentAfterRoot),
    "an unclosed element" to ("<a><b></b>" to XmlDiagnosticCode.UnclosedElement),
    "text that runs to the end" to ("<a>text" to XmlDiagnosticCode.UnclosedElement),
    "a carriage return at the end" to ("<a>x\r" to XmlDiagnosticCode.UnclosedElement),
    "an end tag without a name" to ("<a></" to XmlDiagnosticCode.InvalidName),
    "an end tag with an attribute" to ("<a></a x>" to XmlDiagnosticCode.UnexpectedContent),
    "< after text in a value" to ("<a x='b<'/>" to XmlDiagnosticCode.InvalidAttribute),
    "a reference that is too long" to (
        "<a>&${"a".repeat(LONG_REFERENCE)};</a>" to XmlDiagnosticCode.InvalidReference
        ),
    "a bare declaration" to ("<?xml" to XmlDiagnosticCode.MisplacedDeclaration),
    "a declaration cut short" to ("<?xml version" to XmlDiagnosticCode.InvalidDeclaration),
    "a mismatched end tag" to ("<a></b>" to XmlDiagnosticCode.MismatchedTag),
    "an end tag without >" to ("<a></a" to XmlDiagnosticCode.UnexpectedContent),
    "an unclosed start tag" to ("<a" to XmlDiagnosticCode.UnexpectedEnd),
    "a name that starts with a digit" to ("<1a/>" to XmlDiagnosticCode.InvalidName),
    "a name with two colons" to ("<a:b:c/>" to XmlDiagnosticCode.InvalidName),
    "attributes without whitespace" to ("<a x='1'y='2'/>" to XmlDiagnosticCode.UnexpectedContent),
    "an attribute without =" to ("<a x/>" to XmlDiagnosticCode.InvalidAttribute),
    "an attribute at the end" to ("<a x" to XmlDiagnosticCode.InvalidAttribute),
    "an unquoted value" to ("<a x=1/>" to XmlDiagnosticCode.InvalidAttribute),
    "a value at the end" to ("<a x=" to XmlDiagnosticCode.InvalidAttribute),
    "an unclosed value" to ("<a x='1" to XmlDiagnosticCode.UnexpectedEnd),
    "< in a value" to ("<a x='<'/>" to XmlDiagnosticCode.InvalidAttribute),
    "a duplicate attribute" to ("<a x='1' x='2'/>" to XmlDiagnosticCode.DuplicateAttribute),
    "a duplicate expanded attribute" to (
        "<a xmlns:p='urn:x' xmlns:q='urn:x' p:x='1' q:x='2'/>" to XmlDiagnosticCode.DuplicateAttribute
        ),
    "]]> in text" to ("<a>]]></a>" to XmlDiagnosticCode.CDataEndInText),
    "an unterminated reference" to ("<a>&amp</a>" to XmlDiagnosticCode.InvalidReference),
    "an empty reference" to ("<a>&;</a>" to XmlDiagnosticCode.InvalidReference),
    "an undefined entity" to ("<a>&nbsp;</a>" to XmlDiagnosticCode.UndefinedEntity),
    "a character reference to nothing" to ("<a>&#0;</a>" to XmlDiagnosticCode.InvalidReference),
    "a character reference without digits" to ("<a>&#x;</a>" to XmlDiagnosticCode.InvalidReference),
    "a character reference with letters" to ("<a>&#12a;</a>" to XmlDiagnosticCode.InvalidReference),
    "a character reference out of range" to ("<a>&#x110000;</a>" to XmlDiagnosticCode.InvalidReference),
    "a control character" to ("<a>\u0001</a>" to XmlDiagnosticCode.InvalidCharacter),
    "a lone surrogate" to ("<a>\uD800</a>" to XmlDiagnosticCode.InvalidCharacter),
    "an unclosed comment" to ("<a><!-- c</a>" to XmlDiagnosticCode.UnexpectedEnd),
    "-- in a comment" to ("<a><!-- a -- b --></a>" to XmlDiagnosticCode.InvalidComment),
    "a comment that ends with -" to ("<a><!-- a ---></a>" to XmlDiagnosticCode.InvalidComment),
    "a control character in a comment" to ("<a><!--\u0002--></a>" to XmlDiagnosticCode.InvalidCharacter),
    "unclosed character data" to ("<a><![CDATA[x</a>" to XmlDiagnosticCode.UnexpectedEnd),
    "an unclosed instruction" to ("<a><?pi data</a>" to XmlDiagnosticCode.UnexpectedEnd),
    "an instruction without a target" to ("<a><? x?></a>" to XmlDiagnosticCode.InvalidName),
    "an instruction without whitespace" to ("<a><?pi\"x\"?></a>" to XmlDiagnosticCode.InvalidProcessingInstruction),
    "an instruction with a colon" to ("<a><?p:i?></a>" to XmlDiagnosticCode.InvalidProcessingInstruction),
    "an instruction named XmL" to ("<a><?XmL?></a>" to XmlDiagnosticCode.ReservedProcessingInstruction),
    "a declaration inside" to ("<a><?xml version='1.0'?></a>" to XmlDiagnosticCode.MisplacedDeclaration),
    "a declaration after whitespace" to (" <?xml version='1.0'?><a/>" to XmlDiagnosticCode.MisplacedDeclaration),
    "a declaration without version" to ("<?xml encoding='UTF-8'?><a/>" to XmlDiagnosticCode.InvalidDeclaration),
    "a declaration of XML 2.0" to ("<?xml version='2.0'?><a/>" to XmlDiagnosticCode.UnsupportedVersion),
    "a declaration with a bad encoding" to
        ("<?xml version='1.0' encoding='9x'?><a/>" to XmlDiagnosticCode.InvalidDeclaration),
    "a declaration with a bad standalone" to (
        "<?xml version='1.0' standalone='maybe'?><a/>" to XmlDiagnosticCode.InvalidDeclaration
        ),
    "a declaration without ?>" to ("<?xml version='1.0' ><a/>" to XmlDiagnosticCode.InvalidDeclaration),
    "a pseudo attribute without =" to ("<?xml version '1.0'?><a/>" to XmlDiagnosticCode.InvalidDeclaration),
    "a document type" to ("<!DOCTYPE a><a/>" to XmlDiagnosticCode.RefusedDoctype),
    "a markup declaration in content" to ("<a><!ELEMENT a ANY></a>" to XmlDiagnosticCode.UnexpectedContent),
    "a markup declaration outside" to ("<!ELEMENT a ANY><a/>" to XmlDiagnosticCode.UnexpectedContent),
    "an undeclared element prefix" to ("<p:a/>" to XmlDiagnosticCode.UndeclaredPrefix),
    "an undeclared attribute prefix" to ("<a p:x='1'/>" to XmlDiagnosticCode.UndeclaredPrefix),
    "an element named with xmlns" to ("<xmlns:a/>" to XmlDiagnosticCode.ReservedPrefix),
    "a declared xmlns prefix" to ("<a xmlns:xmlns='urn:x'/>" to XmlDiagnosticCode.ReservedPrefix),
    "a rebound xml prefix" to ("<a xmlns:xml='urn:x'/>" to XmlDiagnosticCode.ReservedPrefix),
    "a prefix bound to the xml namespace" to (
        "<a xmlns:p='http://www.w3.org/XML/1998/namespace'/>" to XmlDiagnosticCode.ReservedPrefix
        ),
    "an unbound prefix" to ("<a xmlns:p=''/>" to XmlDiagnosticCode.EmptyPrefixBinding),
    "a prefix used after its scope" to ("<a><b xmlns:p='urn:x'/><p:c/></a>" to XmlDiagnosticCode.UndeclaredPrefix),
)

private const val LONG_REFERENCE: Int = 40

class XmlWellFormednessTest : FunSpec({
    context("documents that are not well formed") {
        refusals.forEach { (case, expectation) ->
            val (text, code) = expectation

            test("refuses $case with $code") {
                Xml.Default.refusal(text) shouldBe code
            }
        }
    }

    context("documents that are well formed") {
        test("read the declaration") {
            val text = """<?xml version="1.0" encoding="UTF-8" standalone="yes" ?><a/>"""
            val declaration = Xml.Default.document(text).declaration

            declaration?.version shouldBe "1.0"
            declaration?.encoding shouldBe "UTF-8"
            declaration?.isStandalone shouldBe true
        }

        test("read a declaration with only a version") {
            val declaration = Xml.Default.document("<?xml version='1.1'?>\n<a/>").declaration

            declaration?.version shouldBe "1.1"
            declaration?.encoding shouldBe null
            declaration?.isStandalone shouldBe null
        }

        test("read a declaration that is not standalone") {
            val declaration = Xml.Default.document("<?xml version='1.0' standalone='no'?><a/>").declaration

            declaration?.isStandalone shouldBe false
        }

        test("read a document type when it may be ignored") {
            val text = """<!DOCTYPE a [ <!ENTITY e "x>y"> <!-- ] > --> ]> <a/>"""
            val result = xml { doctype = XmlDoctype.Ignore }.parse(text)

            result.diagnostics.single().code shouldBe XmlDiagnosticCode.IgnoredDoctype
        }

        test("refuse a second document type even when it may be ignored") {
            xml { doctype = XmlDoctype.Ignore }.refusal("<!DOCTYPE a><!DOCTYPE a><a/>") shouldBe
                XmlDiagnosticCode.MisplacedDoctype
        }

        test("refuse an unclosed document type") {
            xml { doctype = XmlDoctype.Ignore }.refusal(
                "<!DOCTYPE a [",
            ) shouldBe XmlDiagnosticCode.UnexpectedEnd
            xml { doctype = XmlDoctype.Ignore }.refusal(
                "<!DOCTYPE a \"x",
            ) shouldBe XmlDiagnosticCode.UnexpectedEnd
        }

        test("read an instruction whose target only starts like a declaration") {
            val document = Xml.Default.document("<?xml-stylesheet href='s.xsl'?><a/>")

            document.declaration shouldBe null
            document.prolog.size shouldBe 1
        }

        test("keep ]]> inside an attribute value") {
            val root = Xml.Default.document("<a x=']]>'/>").root

            root.attributes.single().value shouldBe "]]>"
        }

        test("keep comments and instructions around the root") {
            val document = Xml.Default.document("<!--before--><?pi data?>\n<a/>\n<!--after-->")

            document.prolog.size shouldBe 2
            document.epilog.size shouldBe 1
        }
    }
})
