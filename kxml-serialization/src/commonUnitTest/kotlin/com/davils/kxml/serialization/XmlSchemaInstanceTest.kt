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

package com.davils.kxml.serialization

import com.davils.kore.testing.HostileFragments
import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.emit.XmlEmitOptions
import com.davils.kxml.core.source.XmlSource
import com.davils.kxml.core.syntax.XmlSyntaxTree
import com.davils.kxml.core.xml
import com.davils.kxml.serialization.fixture.Annotated
import com.davils.kxml.serialization.fixture.Grant
import com.davils.kxml.serialization.fixture.Grantee
import com.davils.kxml.serialization.fixture.Nillable
import com.davils.kxml.serialization.fixture.Payloads
import com.davils.kxml.serialization.fixture.Plain
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.orNull
import io.kotest.property.checkAll
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer

private const val CASES: Int = 300
private const val MOST_PIECES: Int = 5
private const val MOST_TAGS: Int = 4
private const val XSI_DECLARATION: String = "xmlns:xsi=\"${Payloads.XSI}\""

private val compact: XmlFormat = xmlFormat {
    isDeclarationWritten = false
    xml { emitter = XmlEmitOptions.Compact }
}

private val nilWriting: XmlFormat = xmlFormat {
    isDeclarationWritten = false
    isNilWritten = true
    xml { emitter = XmlEmitOptions.Compact }
}

private val schemaTyped: XmlFormat = xmlFormat {
    isDeclarationWritten = false
    polymorphism = XmlPolymorphism.SchemaType
    namingStrategy = XmlNamingStrategy.PascalCase
    xml { emitter = XmlEmitOptions.Compact }
}

private val safePieces: List<String> = listOf("a", " ", "<", ">", "&", "\"", "'", "]]>", "\t", "\u00E4") +
    HostileFragments.all.filter { fragment -> fragment.all { char -> char >= ' ' && !char.isSurrogate() } }

private val texts: Arb<String> =
    Arb.list(Arb.element(safePieces), 0..MOST_PIECES).map { parts -> parts.joinToString(separator = "") }

private val leaves: Arb<Nillable> = Arb.bind(
    genA = texts.orNull(),
    genB = Arb.int().orNull(),
    genC = Arb.list(texts.orNull(), 0..MOST_TAGS),
) { name, count, tags -> Nillable(name = name, count = count, tags = tags) }

private val values: Arb<Nillable> = Arb.bind(genA = leaves, genB = leaves.orNull()) { value, child ->
    Nillable(name = value.name, count = value.count, tags = value.tags, child = child)
}

private fun codeOf(decoding: XmlDecoding<*>): XmlDiagnosticCode {
    val failed = decoding.shouldBeInstanceOf<XmlDecoding.Failed>()
    return failed.diagnostics.last().code
}

class XmlSchemaInstanceTest : FunSpec({
    context("xsi:nil") {
        test("reads a nil element as null and refuses it where null is not allowed") {
            val text = "<Nillable $XSI_DECLARATION><name xsi:nil='true'/><count xsi:nil='1'/></Nillable>"
            val nonNull = "<Plain $XSI_DECLARATION><name xsi:nil='true'/></Plain>"

            compact.decodeFromString<Nillable>(text) shouldBe Nillable()
            codeOf(compact.decodeOrFailure(serializer<Plain>(), nonNull)) shouldBe XmlDiagnosticCode.TypeMismatch
        }

        test("is written for null values only when the format asks for it") {
            val value = Nillable(tags = listOf("a", null))

            val written = nilWriting.encodeToString(value)

            written shouldContain """<name xmlns:xsi="${Payloads.XSI}" xsi:nil="true"/>"""
            written shouldContain """<tags xmlns:xsi="${Payloads.XSI}" xsi:nil="true"/>"""
            nilWriting.decodeFromString<Nillable>(written) shouldBe value
            compact.encodeToString(value) shouldNotContain "nil"
            nilWriting.configuration.toString() shouldContain "isNilWritten=true"
            nilWriting.encodeToString(Annotated(id = 1, text = "t")) shouldNotContain "origin"
            nilWriting.encodeToString<Nillable?>(null) shouldBe
                """<Nillable xmlns:xsi="${Payloads.XSI}" xsi:nil="true"/>"""
        }

        test("reads back every value it wrote") {
            checkAll(CASES, values) { value ->
                val written = nilWriting.encodeToString(value)

                withClue(written) { nilWriting.decodeFromString<Nillable>(written) shouldBe value }
            }
        }
    }

    context("xsi:type") {
        test("names the type of a polymorphic value") {
            val grant = Grant(Grantee.CanonicalUser(id = "abc", displayName = "owner"), "READ")

            val written = schemaTyped.encodeToString(grant)

            written shouldContain "<Grantee $XSI_DECLARATION xsi:type=\"CanonicalUser\">"
            schemaTyped.decodeFromString<Grant>(written) shouldBe grant
        }

        test("is never an unknown property, like every attribute of the schema instance namespace") {
            val located = "<Nillable $XSI_DECLARATION xsi:schemaLocation='urn:x x.xsd'><name>n</name></Nillable>"

            val decoded = compact.decodeOrFailure(serializer<Nillable>(), located)

            decoded.shouldBeInstanceOf<XmlDecoding.Decoded<Nillable>>().diagnostics shouldBe emptyList()
        }
    }

    context("decoding a syntax tree and a reader") {
        test("decodes the document of a syntax tree with ranges into its text") {
            val text = "<Nillable><count>x</count></Nillable>"
            val tree = XmlSyntaxTree.parse(text)

            val failed = compact.decodeOrFailure(serializer<Nillable>(), tree).shouldBeInstanceOf<XmlDecoding.Failed>()

            val diagnostic = failed.diagnostics.single()
            diagnostic.range.start shouldBe text.indexOf("<count>")
            codeOf(compact.decodeOrFailure(serializer<Nillable>(), XmlSyntaxTree.parse("<Nillable>"))) shouldBe
                XmlDiagnosticCode.UnclosedElement
        }

        test("decodes the elements of a level one at a time") {
            val text = "<list><Nillable><name>a</name></Nillable><!--x--><Nillable><count>2</count></Nillable></list>"
            val reader = xml { doctype = XmlDoctype.Refuse }.reader(XmlSource.of(text))
            val deserializer = serializer<Nillable>()

            reader.next()
            val first = compact.decodeNextOrNull(deserializer, reader)
            val second = compact.decodeNextOrNull(deserializer, reader)

            first.shouldBeInstanceOf<XmlDecoding.Decoded<Nillable>>().value shouldBe Nillable(name = "a")
            second.shouldBeInstanceOf<XmlDecoding.Decoded<Nillable>>().value shouldBe Nillable(count = 2)
            compact.decodeNextOrNull(deserializer, reader).shouldBeNull()
            val broken = xml { }.reader(XmlSource.of("<Nillable><name></Nillable>"))
            codeOf(compact.decodeNextOrNull(deserializer, broken).shouldBeInstanceOf<XmlDecoding.Failed>()) shouldBe
                XmlDiagnosticCode.MismatchedTag
        }
    }
})
