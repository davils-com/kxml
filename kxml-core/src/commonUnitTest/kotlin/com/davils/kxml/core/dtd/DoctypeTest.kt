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

package com.davils.kxml.core.dtd

import com.davils.kxml.core.Xml
import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.XmlEvent
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.fixture.document
import com.davils.kxml.core.fixture.hostileText
import com.davils.kxml.core.fixture.lines
import com.davils.kxml.core.fixture.refusal
import com.davils.kxml.core.fixture.root
import com.davils.kxml.core.fixture.shouldBeShownSafely
import com.davils.kxml.core.node.XmlDoctypeDeclaration
import com.davils.kxml.core.node.XmlNotation
import com.davils.kxml.core.node.XmlUnparsedEntity
import com.davils.kxml.core.source.XmlSource
import com.davils.kxml.core.xml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.checkAll

private const val CASES: Int = 100
private const val FEW: Int = 2
private const val NOTATION_SYSTEM: String = "n.exe"
private const val ENTITY_SYSTEM: String = "e.gif"
private const val BARE: String = "<!DOCTYPE a><a/>"

private val internal: Xml = xml { doctype = XmlDoctype.Internal }

private val malformed: Map<String, String> = mapOf(
    "an entity without a value" to "<!ENTITY e>",
    "an entity with an unquoted value" to "<!ENTITY e v>",
    "a parameter entity in an entity value of the internal subset" to "<!ENTITY % p 'x'><!ENTITY e '%p;'>",
    "an external identifier without a keyword" to "<!ENTITY e 'a' 'b'>",
    "a public identifier with a character it may not hold" to "<!ENTITY e PUBLIC '{' 'x'>",
    "NDATA on a parameter entity" to "<!ENTITY % p SYSTEM 'x' NDATA n>",
    "an element type without a content model" to "<!ELEMENT a>",
    "a content model that mixes separators" to "<!ELEMENT a (b|c,d)>",
    "a choice with one part" to "<!ELEMENT a (b|)>",
    "mixed content with names but no star" to "<!ELEMENT a (#PCDATA|b)>",
    "a content model with whitespace before an indicator" to "<!ELEMENT a (b) *>",
    "an attribute without a type" to "<!ATTLIST a b #IMPLIED>",
    "an attribute without a default" to "<!ATTLIST a b CDATA>",
    "an attribute type that runs into its name" to "<!ATTLIST a b CDATAX #IMPLIED>",
    "attribute definitions without whitespace" to "<!ATTLIST a b CDATA #IMPLIED\"x\" CDATA #IMPLIED>",
    "a notation without an identifier" to "<!NOTATION n>",
    "an unknown declaration" to "<!FOO a>",
    "text in the internal subset" to "text",
    "a character reference without digits at the end of a value" to "<!ENTITY e '&#'>",
    "a character reference without ;" to "<!ENTITY e '&#65'>",
    "a character reference with too many digits" to "<!ENTITY e '&#x0000000000041;'>",
    "a character reference to a character XML does not allow" to "<!ENTITY e '&#0;'>",
    "a character reference without hex digits" to "<!ENTITY e '&#xZ;'>",
    "an entity value that is not closed" to "<!ENTITY e 'v",
    "a default value that holds <" to "<!ATTLIST a b CDATA '<'>",
    "a default value that is not closed" to "<!ATTLIST a b CDATA 'v",
    "a misspelled keyword" to "<!ATTLIST a b CDATA #FIXE 'v'>",
    "a declaration that does not end with >" to "<!ENTITY e 'v' x>",
    "a declaration that ends with the subset" to "<!ENTITY e 'v'",
    "a name that starts with a digit" to "<!ENTITY 1 'v'>",
    "an enumeration without a token" to "<!ATTLIST a b (|) #IMPLIED>",
    "an enumeration that is not closed" to "<!ATTLIST a b (x y) #IMPLIED>",
    "a system identifier without quotes" to "<!NOTATION n SYSTEM x>",
    "a content model without separators" to "<!ELEMENT a (b c)>",
    "a content model that is not closed" to "<!ELEMENT a (b",
    "a group that ends after its separator" to "<!ELEMENT a (b,",
    "a parameter reference without ;" to "<!ENTITY % p 'x'> %p ",
)

private val refusals: Set<XmlDiagnosticCode> = setOf(
    XmlDiagnosticCode.InvalidMarkupDeclaration,
    XmlDiagnosticCode.InvalidName,
    XmlDiagnosticCode.InvalidReference,
    XmlDiagnosticCode.InvalidAttribute,
    XmlDiagnosticCode.UnexpectedEnd,
)

private val everyDeclaration: String = lines(
    "<!DOCTYPE a PUBLIC \"-//A//EN\" 'a.dtd' [",
    "<!ENTITY % p \"<!ENTITY q 'q'>\">",
    "%p;",
    "<!ENTITY e '&#x41;&#66;&amp;&q;\r\n'>",
    "<!NOTATION n PUBLIC '-//N//EN'>",
    "<!ENTITY u SYSTEM \"u.gif\" NDATA n>",
    "<!ELEMENT a (b|(c,d)*)+>",
    "<!ELEMENT b (#PCDATA|c)*>",
    "<!ELEMENT c (#PCDATA)>",
    "<!ATTLIST a x (one|two) 'one' y NOTATION (n) #IMPLIED z CDATA #FIXED \"z&#13;\">",
    "]><a>&e;</a>",
)

class DoctypeTest : FunSpec({
    context("a document type declaration") {
        test("is refused unless the reader may read it") {
            Xml.Default.refusal(BARE) shouldBe XmlDiagnosticCode.RefusedDoctype
        }

        test("reports its name, its identifiers, its notations and its unparsed entities") {
            val text =
                lines(
                    "<!DOCTYPE a PUBLIC '-//A//EN' 'a.dtd' [",
                    "  <!NOTATION gif PUBLIC '-//GIF//EN'>",
                    "  <!NOTATION png SYSTEM 'png.exe'>",
                    "  <!ENTITY logo SYSTEM 'logo.gif' NDATA gif>",
                    "  <!ENTITY text 't'>",
                    "]><a/>",
                )
            val doctype = internal.document(text).doctype.shouldNotBeNull()

            doctype.name shouldBe "a"
            doctype.publicId shouldBe "-//A//EN"
            doctype.systemId shouldBe "a.dtd"
            doctype.notations.map { notation -> notation.name } shouldContainExactly listOf("gif", "png")
            doctype.notations.first().systemId shouldBe null
            doctype.unparsedEntities.single().notation shouldBe "gif"
            doctype.unparsedEntities.single().systemId shouldBe "logo.gif"
        }

        test("comes as an event before the root element") {
            val events = generateSequence(internal.reader(XmlSource.of(BARE))::next).toList()
            val doctype = events.first().shouldBeInstanceOf<XmlEvent.DocumentType>()

            doctype.declaration.name shouldBe "a"
            doctype shouldBe events.first()
            doctype shouldNotBe events.last()
            doctype.hashCode() shouldBe events.first().hashCode()
            doctype.toString() shouldBe "DocumentType(${doctype.declaration})"
        }

        test("reads element types, attribute lists, comments, instructions and parameter entities") {
            val text =
                lines(
                    "<!DOCTYPE a [",
                    "  <!ELEMENT a (b, (c | d)*, e?)+>",
                    "  <!ELEMENT b (#PCDATA | c)*>",
                    "  <!ELEMENT c (#PCDATA)>",
                    "  <!ELEMENT d EMPTY>",
                    "  <!ELEMENT e ANY>",
                    "  <!ATTLIST a x CDATA #IMPLIED y (one|two) 'one' z NOTATION (n) #REQUIRED>",
                    "  <!-- comment -->",
                    "  <?target data?>",
                    "  <!ENTITY % decl \"<!ENTITY inner 'i'>\">",
                    "  %decl;",
                    "]><a>&inner;</a>",
                )

            internal.document(text).root.text shouldBe "i"
        }

        test("shows its values escaped") {
            checkAll(CASES, hostileText) { value ->
                val notation = XmlNotation(name = "n", publicId = value, systemId = value)
                val entity = XmlUnparsedEntity(name = "e", publicId = value, systemId = value, notation = "n")
                val doctype = XmlDoctypeDeclaration(
                    name = "a",
                    publicId = value,
                    systemId = value,
                    notations = listOf(notation),
                    unparsedEntities = listOf(entity),
                )

                doctype.shouldBeShownSafely()
                notation.shouldBeShownSafely()
                entity.shouldBeShownSafely()
            }
        }
    }

    context("a notation and an unparsed entity") {
        test("are equal when every property is") {
            val notation = XmlNotation(name = "n", publicId = null, systemId = NOTATION_SYSTEM)
            val sameNotation = XmlNotation(name = "n", publicId = null, systemId = NOTATION_SYSTEM)
            val entity = XmlUnparsedEntity(name = "e", publicId = null, systemId = ENTITY_SYSTEM, notation = "n")
            val sameEntity = XmlUnparsedEntity(name = "e", publicId = null, systemId = ENTITY_SYSTEM, notation = "n")
            val otherNotation = XmlNotation(name = "n", publicId = "-//N//EN", systemId = null)
            val otherEntity =
                XmlUnparsedEntity(name = "e", publicId = "-//E//EN", systemId = ENTITY_SYSTEM, notation = "n")

            notation shouldBe sameNotation
            notation.hashCode() shouldBe sameNotation.hashCode()
            notation shouldNotBe otherNotation
            notation shouldNotBe entity
            entity shouldBe sameEntity
            entity shouldNotBe otherEntity
            entity shouldNotBe notation
            notation.toString() shouldBe "XmlNotation(n, public=none, system=\"n.exe\")"
            otherNotation.toString() shouldBe "XmlNotation(n, public=\"-//N//EN\", system=none)"
            entity.hashCode() shouldNotBe otherEntity.hashCode()
            entity.toString() shouldNotBe otherEntity.toString()
        }

        test("keep the first declaration of a name") {
            val subset = "<!NOTATION n SYSTEM 'first'><!NOTATION n SYSTEM 'second'>"
            val doctype = internal.document("<!DOCTYPE a [$subset]><a/>").doctype.shouldNotBeNull()

            doctype.notations.single().systemId shouldBe "first"
        }
    }

    context("a document type declaration without identifiers") {
        test("shows none for them and differs from one with them") {
            val bare = internal.document(BARE).doctype.shouldNotBeNull()
            val identified = internal.document("<!DOCTYPE a SYSTEM 'a.dtd'><a/>").doctype.shouldNotBeNull()

            bare.toString() shouldContain "public=none"
            bare.toString() shouldContain "system=none"
            bare shouldNotBe identified
            bare shouldNotBe "a"
        }
    }

    context("grammar that is well formed") {
        test("admits choices of many parts, starred mixed content, nested groups and long enumerations") {
            val subset = lines(
                "<!ELEMENT a (b|c|d)>",
                "<!ELEMENT b (#PCDATA)*>",
                "<!ELEMENT c ((d, e)|f)?>",
                "<!ATTLIST a x (one|two|three) 'two'>",
                "<!ATTLIST a y NOTATION (n|m) #IMPLIED>",
            )
            val root = internal.root("<!DOCTYPE a [$subset]><a/>")

            root.attributes.single().value shouldBe "two"
        }

        test("refuses a parameter entity that refers to itself") {
            val text = "<!DOCTYPE a [<!ENTITY % p '&#37;p;'> %p;]><a/>"

            internal.refusal(text) shouldBe XmlDiagnosticCode.RecursiveEntity
        }
    }

    context("a document that ends inside its document type declaration") {
        test("is refused wherever it ends") {
            internal.document(everyDeclaration).root.text shouldBe "AB&q\n"
            val subset = everyDeclaration.substringBefore("]>")

            subset.indices.forEach { length ->
                internal.parse(subset.take(length)).shouldBeInstanceOf<XmlParseResult.Failed>()
            }
        }
    }

    context("a malformed declaration") {
        malformed.forEach { (case, declaration) ->
            test("is refused for $case") {
                val code = internal.refusal("<!DOCTYPE a [$declaration]><a/>")

                code shouldBeIn refusals
            }
        }
    }

    context("the limits of a document type declaration") {
        test("admit as many entity and attribute declarations as they allow") {
            val few = xml {
                doctype = XmlDoctype.Internal
                limits {
                    maximumEntityDeclarations = FEW
                    maximumAttributeDeclarations = FEW
                }
            }
            val entities = "<!DOCTYPE a [<!ENTITY a 'a'><!ENTITY b 'b'>]><a/>"
            val attributes = "<!DOCTYPE a [<!ATTLIST a x CDATA #IMPLIED y CDATA #IMPLIED>]><a/>"

            val moreEntities = entities.replace(oldValue = "]>", newValue = "<!ENTITY c 'c'>]>")
            val moreAttributes = attributes.replace(oldValue = "#IMPLIED>", newValue = "#IMPLIED z CDATA #IMPLIED>")
            val root = few.document(entities).root

            root.text shouldBe ""
            val attributed = few.document(attributes).root

            attributed.attributes.size shouldBe 0
            few.refusal(moreEntities) shouldBe XmlDiagnosticCode.TooManyEntityDeclarations
            few.refusal(moreAttributes) shouldBe XmlDiagnosticCode.TooManyAttributeDeclarations
        }

        test("bound the depth of a content model") {
            val shallow = xml {
                doctype = XmlDoctype.Internal
                limits { maximumDepth = FEW }
            }

            shallow.refusal("<!DOCTYPE a [<!ELEMENT a (((b)))>]><a/>") shouldBe
                XmlDiagnosticCode.InvalidMarkupDeclaration
        }
    }
})
