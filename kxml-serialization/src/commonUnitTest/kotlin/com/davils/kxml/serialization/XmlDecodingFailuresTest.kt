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

import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.node.xmlElement
import com.davils.kxml.serialization.fixture.Annotated
import com.davils.kxml.serialization.fixture.AttributeOwner
import com.davils.kxml.serialization.fixture.BucketList
import com.davils.kxml.serialization.fixture.Checksum
import com.davils.kxml.serialization.fixture.CommonPrefix
import com.davils.kxml.serialization.fixture.Labels
import com.davils.kxml.serialization.fixture.ObjectSummary
import com.davils.kxml.serialization.fixture.Odd
import com.davils.kxml.serialization.fixture.Primitives
import com.davils.kxml.serialization.fixture.Tagging
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.serializer

private const val PRIMITIVES: String = "<Primitives><byte>1</byte><short>1</short><int>1</int><long>1</long>" +
    "<float>1</float><double>1</double><char>c</char><isOn>1</isOn></Primitives>"
private const val UNKNOWNS: Int = 3
private const val STRAY: String = "<CommonPrefix x='1'>stray<prefix>p</prefix><extra/></CommonPrefix>"
private const val UNKNOWN_CLASS: String =
    "<ObjectSummary><key>k</key><eTag>e</eTag><size>1</size><storageClass>X</storageClass></ObjectSummary>"
private const val KEYLESS: String = "<Tagging><tagSet><Tag><Value>v</Value></Tag></tagSet></Tagging>"

private fun codeOf(decoding: XmlDecoding<*>): XmlDiagnosticCode {
    val failed = decoding.shouldBeInstanceOf<XmlDecoding.Failed>()
    return failed.diagnostics.last().code
}

private inline fun <reified T> failure(text: String): XmlDiagnosticCode =
    codeOf(XmlFormat.Default.decodeOrFailure<T>(text))

private fun primitives(element: String, value: String): String =
    PRIMITIVES.replace(Regex("<$element>[^<]*</$element>"), "<$element>$value</$element>")

class XmlDecodingFailuresTest : FunSpec({
    context("documents that do not fit the type") {
        test("are refused when they are no XML") {
            failure<CommonPrefix>("<CommonPrefix>") shouldBe XmlDiagnosticCode.UnclosedElement
        }

        test("are refused when the root has another name") {
            failure<CommonPrefix>("<Other><prefix>p</prefix></Other>") shouldBe XmlDiagnosticCode.UnexpectedRoot
        }

        test("are refused when a property is missing") {
            failure<CommonPrefix>("<CommonPrefix/>") shouldBe XmlDiagnosticCode.MissingProperty
            failure<BucketList>("<ListAllMyBucketsResult><owner><id>o</id></owner></ListAllMyBucketsResult>") shouldBe
                XmlDiagnosticCode.MissingProperty
        }

        test("report unknown elements, attributes and text") {
            val decoding = XmlFormat.Default.decodeOrFailure<CommonPrefix>(STRAY)
            val codes = decoding.diagnostics.map { it.code }

            codes shouldBe List(UNKNOWNS) { XmlDiagnosticCode.UnknownProperty }
        }

        test("ignore unknown elements when asked") {
            val lenient = xmlFormat { isUnknownElementIgnored = true }

            lenient.decodeFromString<CommonPrefix>(STRAY) shouldBe CommonPrefix("p")
        }

        test("are refused when a value is not what the type needs") {
            failure<Primitives>(primitives(element = "int", value = "x")) shouldBe XmlDiagnosticCode.TypeMismatch
            failure<Primitives>(primitives(element = "byte", value = "300")) shouldBe XmlDiagnosticCode.NumberOutOfRange
            val belowRange = primitives(element = "byte", value = "-300")
            failure<Primitives>(belowRange) shouldBe XmlDiagnosticCode.NumberOutOfRange
            failure<Primitives>(primitives(element = "double", value = "x")) shouldBe XmlDiagnosticCode.TypeMismatch
            failure<Primitives>(primitives(element = "char", value = "xy")) shouldBe XmlDiagnosticCode.TypeMismatch
            failure<Primitives>(primitives(element = "isOn", value = "yes")) shouldBe XmlDiagnosticCode.TypeMismatch
            failure<Primitives>(primitives(element = "int", value = "<a/>")) shouldBe XmlDiagnosticCode.TypeMismatch
        }

        test("are refused for an enum value that does not exist") {
            failure<ObjectSummary>(UNKNOWN_CLASS) shouldBe XmlDiagnosticCode.UnknownEnumValue
        }

        test("are refused for broken base64") {
            failure<Checksum>("<Checksum><value>!!</value></Checksum>") shouldBe XmlDiagnosticCode.TypeMismatch
        }

        test("are refused for an entry without a key") {
            failure<Tagging>(KEYLESS) shouldBe XmlDiagnosticCode.TypeMismatch
            failure<Labels>("<Labels><label/></Labels>") shouldBe XmlDiagnosticCode.TypeMismatch
        }

        test("are refused for an attribute where an element or a number is needed") {
            failure<Annotated>("""<a:Annotated xmlns:a="urn:annotated" id="x">t</a:Annotated>""") shouldBe
                XmlDiagnosticCode.TypeMismatch
            failure<AttributeOwner>("<AttributeOwner owner='x'/>") shouldBe XmlDiagnosticCode.TypeMismatch
        }

        test("are refused for repeated elements where one value is needed") {
            failure<Odd>("<Odd><tags>a</tags><tags>b</tags></Odd>") shouldBe XmlDiagnosticCode.TypeMismatch
        }

        test("are refused when a collection stands at the root") {
            val list = xmlElement(XmlName("ArrayList")) { element(XmlName("item")) }
            val map = xmlElement(XmlName("LinkedHashMap")) { element(XmlName("item")) }

            codeOf(XmlFormat.Default.decodeOrFailure(serializer<List<String>>(), list)) shouldBe
                XmlDiagnosticCode.TypeMismatch
            codeOf(XmlFormat.Default.decodeOrFailure(serializer<Map<String, String>>(), map)) shouldBe
                XmlDiagnosticCode.TypeMismatch
        }
    }

    context("the exception") {
        test("names the position and the code") {
            val failure = shouldThrow<XmlDecodingException> {
                XmlFormat.Default.decodeFromString<CommonPrefix>("<CommonPrefix>\n</CommonPrefix>")
            }

            failure.diagnostics.single().code shouldBe XmlDiagnosticCode.MissingProperty
            failure.message shouldContain "kxml.decode.missing-property"
        }

        test("is thrown for an element as well") {
            val element = xmlElement(XmlName("Other"))

            shouldThrow<XmlDecodingException> { XmlFormat.Default.decodeFromElement<CommonPrefix>(element) }
        }
    }
})
