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
package com.davils.kxml.document.redact

import com.davils.kore.core.validation.report.ValidationException
import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.limit.xmlLimits
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.syntax.XmlSyntaxTree
import com.davils.kxml.core.xml
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.c14n.XmlCanonicalization
import com.davils.kxml.document.c14n.XmlCanonicalizer
import com.davils.kxml.document.fixture.documentOf
import com.davils.kxml.document.fixture.hostileText
import com.davils.kxml.document.fixture.shouldBeShownSafely
import com.davils.kxml.document.fixture.shouldFailSafely
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.checkAll

private const val HOSTILE_CASES: Int = 100
private const val SECRET: String = "hunter2"
private const val CONFIG: String = "<config><user name='bob' token='$SECRET'>x</user><password>$SECRET</password>" +
    "<!-- key $SECRET --><note>call 555-1234</note><?pi $SECRET?></config>"

private val redaction: XmlRedaction = xmlRedaction {
    path("//password | //processing-instruction()")
    attribute { name -> name.local == "token" }
    value(Regex("\\d{3}-\\d{4}"))
    comment(Regex("key \\S+"))
}

private val canonicalizer: XmlCanonicalizer = XmlCanonicalizer(XmlCanonicalization.Canonical10WithComments)

private fun canonicalOf(document: XmlDocument): String = canonicalizer.canonicalize(document).decodeToString()

private fun redactedText(text: String, rules: XmlRedaction = redaction): String {
    val tree = XmlSyntaxTree.parse(text, xml { doctype = XmlDoctype.Internal })
    val result = rules.redact(tree).shouldBeInstanceOf<XmlRedactionResult.Redacted>()
    return result.tree.text
}

private fun refusalOf(text: String, rules: XmlRedaction = redaction): XmlRedactionRefusal {
    val tree = XmlSyntaxTree.parse(text, xml { doctype = XmlDoctype.Internal })
    return rules.redact(tree).shouldBeInstanceOf<XmlRedactionResult.Refused>().reason
}

class XmlRedactionTest : FunSpec({
    context("a document") {
        test("loses every secret") {
            val redacted = redaction.redact(documentOf(CONFIG))
            val expected = "<config><user name='bob' token='***'>x</user><password>***</password>" +
                "<!-- *** --><note>call ***</note><?pi ***?></config>"

            canonicalOf(redacted) shouldBe canonicalOf(documentOf(expected))
            canonicalOf(redacted) shouldNotContain SECRET
        }

        test("leaves secret elements and texts empty when the replacement is empty") {
            val emptied = xmlRedaction {
                replacement = ""
                path("//password")
                value(Regex("\\d{3}-\\d{4}"))
            }
            val redacted = emptied.redact(documentOf("<a><password>s</password><b>555-1234</b><c>x</c></a>"))

            canonicalOf(redacted) shouldBe canonicalOf(documentOf("<a><password/><b/><c>x</c></a>"))
        }

        test("loses the content and the attributes of a secret element") {
            val rules = xmlRedaction { element { name -> name.local == "user" } }
            val redacted = rules.redact(documentOf("<!--c--><a><user id='1'><b>x</b></user></a>"))

            canonicalOf(redacted) shouldBe canonicalOf(documentOf("<!--c--><a><user id='***'>***</user></a>"))
            val root = xmlRedaction { path("/") }.redact(documentOf("<a b='1'>x</a>"))
            canonicalOf(root) shouldBe canonicalOf(documentOf("<a b='***'>***</a>"))
        }
    }

    context("a syntax tree") {
        test("is redacted in place and keeps its layout") {
            val lines = listOf(
                "<config>",
                """  <user token="$SECRET" a='&amp;'/>""",
                "  <password><![CDATA[$SECRET]]></password>",
                "</config>",
            )
            val redacted = listOf(
                "<config>",
                """  <user token="***" a='&amp;'/>""",
                "  <password>***</password>",
                "</config>",
            )

            redactedText(lines.joinToString("\n")) shouldBe redacted.joinToString("\n")
            redactedText("<a><password/></a>") shouldBe "<a><password>***</password></a>"
            redactedText("<a><!-- key $SECRET --></a>") shouldBe "<a><!-- *** --></a>"
        }

        test("redacts instructions, comments and secret elements with their attributes") {
            val markup = xmlRedaction { path("//processing-instruction() | //comment()") }

            redactedText("<a><?pi?><!----><?pi $SECRET?></a>", markup) shouldBe "<a><?pi ***?><!--***--><?pi ***?></a>"
            redactedText("<a><password p='1'>x</password></a>") shouldBe "<a><password p='***'>***</password></a>"
            val escaping = xmlRedaction {
                replacement = "<&>"
                path("//password")
                attribute { name -> name.local == "c" }
            }
            redactedText("<a><password>s</password><b c='x'/></a>", escaping) shouldBe
                "<a><password>&lt;&amp;&gt;</password><b c='&lt;&amp;>'/></a>"
            redactedText("<a b='1'>x</a>", xmlRedaction { path("/") }) shouldBe "<a b='***'>***</a>"
            redaction.redact(XmlSyntaxTree.parse("<a><password>s</password></a>")).toString() shouldBe
                "Redacted(edits=1)"
        }

        test("redacts the declaration of an entity a secret comes from") {
            val text = "<!DOCTYPE a [<!ENTITY s '$SECRET'>]><a><password>&s;</password></a>"

            redactedText(text) shouldBe "<!DOCTYPE a [<!ENTITY s '***'>]><a><password>***</password></a>"
            val entities = "<!DOCTYPE a [<!ENTITY % s 'x'><!ENTITY t 'y'><!ENTITY s '$SECRET'>]>"
            val content = "<a>&t;<b/>555-1234<b/>&t;<password>&s;&s;</password></a>"
            redactedText(entities + content) shouldBe
                "<!DOCTYPE a [<!ENTITY % s 'x'><!ENTITY t 'y'><!ENTITY s '***'>]>" +
                "<a>&t;<b/>***<b/>&t;<password>***</password></a>"
        }

        test("is refused when the redaction cannot reach a secret or the text is no document") {
            refusalOf("<a><password>") shouldBe XmlRedactionRefusal.InvalidDocument
            val defaulted = "<!DOCTYPE a [<!ATTLIST user token CDATA '$SECRET'>]><a><user/></a>"
            refusalOf(defaulted) shouldBe XmlRedactionRefusal.Undecidable
            val nested = "<!DOCTYPE a [<!ENTITY s '&#38;t;'><!ENTITY t 'x'>]><a><password>&s;</password></a>"
            refusalOf(nested) shouldBe XmlRedactionRefusal.Undecidable
            val again = xmlRedaction { value(Regex("\\*")) }
            refusalOf("<a>*</a>", again) shouldBe XmlRedactionRefusal.Undecidable
            val limited = xmlRedaction {
                limits = xmlLimits { maximumQueryStringLength = 2 }
                value(Regex("x"))
            }
            refusalOf("<a>xyz</a>", limited) shouldBe XmlRedactionRefusal.LimitExceeded
            shouldThrow<XmlDocumentLimitException> { limited.redact(documentOf("<a>xyz</a>")) }
            canonicalOf(limited.redact(documentOf("<a>xy</a>"))) shouldBe "<a>***y</a>"
        }
    }

    context("a redaction") {
        test("refuses paths, replacements and rules it cannot use") {
            shouldThrow<ValidationException> { xmlRedaction { path("count(//a)") } }
            shouldThrow<ValidationException> { xmlRedaction { path("//[") } }
            shouldThrow<ValidationException> {
                xmlRedaction {
                    namespace(prefix = "1p", uri = "urn:p")
                    path("//a")
                }
            }
            shouldThrow<ValidationException> { xmlRedaction { replacement = "a--b" } }
            shouldThrow<ValidationException> { xmlRedaction { replacement = "-x" } }
            shouldThrow<ValidationException> { xmlRedaction { replacement = "\u0001" } }
            val single = xmlLimits { maximumRedactionRules = 1 }
            xmlRedaction {
                limits = single
                path("//a")
            }.paths shouldBe listOf("//a")
            shouldThrow<ValidationException> {
                xmlRedaction {
                    limits = single
                    path("//a")
                    path("//b")
                }
            }
        }

        test("fails closed when a predicate throws and shows nothing secret") {
            val failing = xmlRedaction { element { error(SECRET) } }
            val error = shouldThrow<IllegalStateException> { failing.redact(documentOf("<a/>")) }

            error.message.orEmpty() shouldNotContain SECRET
            val named = xmlRedaction {
                namespace(prefix = "p", uri = "urn:p")
                path("//p:password")
            }
            named.toString() shouldBe """XmlRedaction(replacement="***", paths=["//p:password"])"""
            named.paths shouldBe listOf("//p:password")
            XmlRedactionResult.Refused(XmlRedactionRefusal.Undecidable).toString() shouldBe "Refused(Undecidable)"
        }

        test("shows hostile paths and replacements escaped") {
            checkAll(HOSTILE_CASES, hostileText) { text ->
                val literal = text.filterNot { char -> char == '\'' }
                xmlRedaction { path("//a[. = '$literal']") }.shouldBeShownSafely()
                val invalid = runCatching { xmlRedaction { path(text) } }
                invalid.shouldFailSafely()
                val replaced = runCatching { xmlRedaction { replacement = text } }
                replaced.getOrNull()?.shouldBeShownSafely()
                replaced.shouldFailSafely()
            }
        }
    }
})
