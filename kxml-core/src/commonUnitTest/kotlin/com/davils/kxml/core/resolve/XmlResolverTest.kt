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

package com.davils.kxml.core.resolve

import com.davils.kore.core.validation.report.ValidationException
import com.davils.kxml.core.Xml
import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.fixture.hostileText
import com.davils.kxml.core.fixture.lines
import com.davils.kxml.core.fixture.shouldBeShownSafely
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.limit.xmlLimits
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.source.XmlSource
import com.davils.kxml.core.xml
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.checkAll

private const val DOCUMENT: String = "urn:test:document"
private const val SUBSET: String = "a.dtd"
private const val CHAPTER: String = "ch.xml"
private const val CASES: Int = 100
private const val WITH_SUBSET: String = "<!DOCTYPE a SYSTEM 'a.dtd'>"
private const val WITH_CHAPTER: String = "<!DOCTYPE a [<!ENTITY ch SYSTEM 'ch.xml'>]>"
private const val CHAPTER_BODY: String = "$WITH_CHAPTER<a>&ch;</a>"
private const val EMPTY_BODY: String = "$WITH_SUBSET<a/>"

private fun external(resolver: XmlResolver, limits: XmlLimits = XmlLimits.Untrusted): Xml = xml {
    doctype = XmlDoctype.External
    this.resolver = resolver
    this.limits = limits
}

private fun Xml.read(text: String): XmlParseResult = parse(XmlSource.of(text, uri = DOCUMENT))

private fun Xml.document(text: String): XmlDocument {
    val parsed = read(text).shouldBeInstanceOf<XmlParseResult.Parsed>()
    return parsed.document
}

private fun Xml.refusal(text: String): XmlDiagnosticCode {
    val failed = read(text).shouldBeInstanceOf<XmlParseResult.Failed>()
    return failed.diagnostics.last().code
}

class XmlResolverTest : FunSpec({
    context("a resolver") {
        test("is consulted only under XmlDoctype.External, which needs one") {
            val resolver = RecordingResolver(CHAPTER to "x")

            shouldThrow<ValidationException> { xml { this.resolver = resolver } }
            shouldThrow<ValidationException> { xml { doctype = XmlDoctype.External } }
            xml { doctype = XmlDoctype.Internal }.refusal(CHAPTER_BODY) shouldBe
                XmlDiagnosticCode.UnresolvedEntity
            resolver.requests.shouldBeEmpty()
            external(resolver).configuration.toString() shouldContain "resolver=set"
        }

        test("hands over the external subset with its declarations, defaults and parameter entities") {
            val subset = lines(
                "<?xml encoding='UTF-8'?>",
                "<!ENTITY % kind 'CDATA'>",
                "<!ATTLIST a v %kind; 'x'>",
                "<!ENTITY e 'from the subset'>",
            )
            val resolver = RecordingResolver(SUBSET to subset)
            val root = external(resolver).document("$WITH_SUBSET<a>&e;</a>").root
            val request = resolver.requests.single()

            root.text shouldBe "from the subset"
            root.attributes.single().value shouldBe "x"
            request.kind shouldBe XmlResourceKind.ExternalSubset
            request.name shouldBe null
            request.systemId shouldBe SUBSET
            request.baseUri shouldBe DOCUMENT
        }

        test("gives the internal subset precedence over the external one") {
            val resolver = RecordingResolver(SUBSET to "<!ENTITY e 'external'>")
            val text = "<!DOCTYPE a SYSTEM 'a.dtd' [<!ENTITY e 'internal'>]><a>&e;</a>"

            external(resolver).document(text).root.text shouldBe "internal"
        }

        test("hands over an external entity once, however often it is referenced") {
            val resolver = RecordingResolver(CHAPTER to "<?xml version='1.0' encoding='UTF-8'?><b>x</b>")
            val root = external(resolver).document("$WITH_CHAPTER<a>&ch;&ch;</a>").root
            val request = resolver.requests.single()

            root.elements.size shouldBe 2
            request.kind shouldBe XmlResourceKind.ExternalEntity
            request.name shouldBe "ch"
        }

        test("names the resource that declares an entity as its base") {
            val resolver = RecordingResolver(SUBSET to "<!ENTITY ch SYSTEM 'ch.xml'>", CHAPTER to "x")

            external(resolver).document("$WITH_SUBSET<a>&ch;</a>").root.text shouldBe "x"
            resolver.requests.last().baseUri shouldBe TEST_SCHEME + SUBSET
        }

        test("hands over external parameter entities between and inside declarations") {
            val subset = lines(
                "<!ENTITY % decl SYSTEM 'decl.ent'>%decl;",
                "<!ENTITY % model SYSTEM 'model.ent'><!ELEMENT a %model;>",
            )
            val resolver = RecordingResolver(
                SUBSET to subset,
                "decl.ent" to "<!ENTITY e 'declared'>",
                "model.ent" to "(#PCDATA)",
            )

            external(resolver).document("$WITH_SUBSET<a>&e;</a>").root.text shouldBe "declared"
            resolver.requests.map(XmlResourceRequest::kind).toSet() shouldBe
                setOf(XmlResourceKind.ExternalSubset, XmlResourceKind.ParameterEntity)
        }
    }

    context("the external subset") {
        test("reads conditional sections and parameter entities in values and declarations") {
            val subset = lines(
                "<!ENTITY % on 'INCLUDE'>",
                "<!ENTITY % off 'IGNORE'>",
                "<![%on;[<!ENTITY e 'in'>]]>",
                "<![ %off; [<!ENTITY f 'out'> <![INCLUDE[ ]]> ]]>",
                "<!ENTITY % model '(#PCDATA)'>",
                "<!ELEMENT a %model;>",
                "<!ENTITY % word \"x%on;\">",
                "<!ENTITY g '%word;'>",
            )
            val reader = external(RecordingResolver(SUBSET to subset))

            reader.document("$WITH_SUBSET<a>&e;&g;</a>").root.text shouldBe "inxINCLUDE"
            reader.refusal("$WITH_SUBSET<a>&f;</a>") shouldBe XmlDiagnosticCode.UnresolvedEntity
        }

        test("refuses a conditional section that is malformed or stands in the internal subset") {
            val unclosed = external(RecordingResolver(SUBSET to "<![IGNORE[ <![ ]]>"))
            val unknown = external(RecordingResolver(SUBSET to "<![MAYBE[ ]]>"))
            val internal = xml { doctype = XmlDoctype.Internal }

            unclosed.refusal(EMPTY_BODY) shouldBe XmlDiagnosticCode.UnexpectedEnd
            unknown.refusal(EMPTY_BODY) shouldBe XmlDiagnosticCode.InvalidMarkupDeclaration
            internal.refusal("<!DOCTYPE a [<![INCLUDE[ ]]>]><a/>") shouldBe XmlDiagnosticCode.InvalidMarkupDeclaration
        }

        test("refuses a parameter entity inside a declaration that nothing declares") {
            val reader = external(RecordingResolver(SUBSET to "<!ELEMENT a %missing;>"))

            reader.refusal(EMPTY_BODY) shouldBe XmlDiagnosticCode.UndefinedEntity
        }

        test("is not used by a standalone document") {
            val reader = external(RecordingResolver(SUBSET to "<!ENTITY e 'x'>"))
            val text = "<?xml version='1.0' standalone='yes'?>$WITH_SUBSET<a>&e;</a>"

            reader.refusal(text) shouldBe XmlDiagnosticCode.UndefinedEntity
        }
    }

    context("an external resource that is not well formed") {
        test("is refused when the resolver refuses it") {
            val reader = external(RecordingResolver())

            reader.refusal(EMPTY_BODY) shouldBe XmlDiagnosticCode.UnresolvedEntity
            reader.refusal(CHAPTER_BODY) shouldBe XmlDiagnosticCode.UnresolvedEntity
        }

        test("is refused for a text declaration that is wrong") {
            val later = external(RecordingResolver(CHAPTER to "<?xml version='1.1' encoding='UTF-8'?>x"))
            val unnamed = external(RecordingResolver(CHAPTER to "<?xml version='1.0'?>x"))
            val unknown = external(RecordingResolver(CHAPTER to "<?xml version='2.0' encoding='UTF-8'?>x"))
            val text = CHAPTER_BODY

            later.refusal(text) shouldBe XmlDiagnosticCode.UnsupportedVersion
            unnamed.refusal(text) shouldBe XmlDiagnosticCode.InvalidDeclaration
            unknown.refusal(text) shouldBe XmlDiagnosticCode.UnsupportedVersion
        }

        test("is refused when its bytes contradict the encoding it declares") {
            val bytes = "<?xml encoding='UTF-16'?>x".encodeToByteArray()
            val resolver = XmlResolver { XmlSource.of(bytes) }

            external(resolver).refusal(CHAPTER_BODY) shouldBe XmlDiagnosticCode.EncodingMismatch
        }

        test("is refused when it refers to itself") {
            val reader = external(RecordingResolver(CHAPTER to "&ch;"))

            reader.refusal(CHAPTER_BODY) shouldBe XmlDiagnosticCode.RecursiveEntity
        }
    }

    context("the limits of external resources") {
        test("admit as many resources as they allow and refuse one more") {
            val subset = "<!ENTITY one SYSTEM 'one'><!ENTITY two SYSTEM 'two'>"
            val resolver = RecordingResolver("one" to "1", "two" to "2")
            val two = external(resolver, xmlLimits { maximumExternalResources = 2 })
            val one = external(resolver, xmlLimits { maximumExternalResources = 1 })
            val text = "<!DOCTYPE a [$subset]><a>&one;&two;</a>"

            two.document(text).root.text shouldBe "12"
            one.refusal(text) shouldBe XmlDiagnosticCode.TooManyExternalResources
        }

        test("admit a resource as long as they allow and refuse a longer one") {
            val resolver = RecordingResolver(CHAPTER to "1234")
            val fits = external(resolver, xmlLimits { maximumExternalLength = 4 })
            val short = external(resolver, xmlLimits { maximumExternalLength = 3 })
            val text = CHAPTER_BODY

            fits.document(text).root.text shouldBe "1234"
            short.refusal(text) shouldBe XmlDiagnosticCode.ExternalResourceTooLong
        }

        test("charge every reference to an external entity to the expansion budget") {
            val reader = external(RecordingResolver(CHAPTER to "12345678"), xmlLimits { maximumEntityExpansion = 10 })

            reader.refusal("$WITH_CHAPTER<a>&ch;&ch;</a>") shouldBe XmlDiagnosticCode.EntityExpansionTooLarge
        }
    }

    context("a request") {
        test("is equal to a request with the same values and shows them escaped") {
            checkAll(CASES, hostileText) { value ->
                val request = XmlResourceRequest(
                    kind = XmlResourceKind.ExternalEntity,
                    name = value,
                    publicId = value,
                    systemId = value,
                    baseUri = value,
                )
                val same = XmlResourceRequest(
                    kind = XmlResourceKind.ExternalEntity,
                    name = value,
                    publicId = value,
                    systemId = value,
                    baseUri = value,
                )

                request shouldBe same
                request.hashCode() shouldBe same.hashCode()
                request.shouldBeShownSafely()
            }
        }

        test("shows none for what it lacks and differs from another kind") {
            val subset = XmlResourceRequest(
                kind = XmlResourceKind.ExternalSubset,
                name = null,
                publicId = null,
                systemId = SUBSET,
                baseUri = null,
            )
            val entity = XmlResourceRequest(
                kind = XmlResourceKind.ParameterEntity,
                name = null,
                publicId = null,
                systemId = SUBSET,
                baseUri = null,
            )

            val shown = "XmlResourceRequest(ExternalSubset, name=none, public=none, system=\"a.dtd\", base=none)"

            subset.toString() shouldBe shown
            subset shouldNotBe entity
            subset shouldNotBe SUBSET
            XmlSource.of(SUBSET, uri = DOCUMENT).uri shouldBe DOCUMENT
        }
    }
})
