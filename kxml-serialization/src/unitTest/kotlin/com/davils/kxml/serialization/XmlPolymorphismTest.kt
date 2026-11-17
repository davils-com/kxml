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
import com.davils.kxml.core.emit.XmlEmitOptions
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.serialization.fixture.Filter
import com.davils.kxml.serialization.fixture.Grant
import com.davils.kxml.serialization.fixture.Grantee
import com.davils.kxml.serialization.fixture.Misplaced
import com.davils.kxml.serialization.fixture.Payloads
import com.davils.kxml.serialization.fixture.Rule
import com.davils.kxml.serialization.fixture.VersionId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

private const val UNNAMED: String = "<Grant><Grantee><Uri>u</Uri></Grantee><Permission>READ</Permission></Grant>"
private const val ROBOT: String =
    """<Grant><Grantee xmlns:xsi="${Payloads.XSI}" xsi:type="Robot"/><Permission>R</Permission></Grant>"""

private val acl: XmlFormat = xmlFormat {
    namingStrategy = XmlNamingStrategy.PascalCase
    classDiscriminator = XmlName(local = "type", namespace = Payloads.XSI, prefix = "xsi")
    xml { emitter = XmlEmitOptions.Compact }
}

private val lifecycle: XmlFormat = xmlFormat {
    namingStrategy = XmlNamingStrategy.PascalCase
    polymorphism = XmlPolymorphism.Element
    xml { emitter = XmlEmitOptions.Compact }
}

private fun codeOf(decoding: XmlDecoding<*>): XmlDiagnosticCode {
    val failed = decoding.shouldBeInstanceOf<XmlDecoding.Failed>()
    return failed.diagnostics.last().code
}

class XmlPolymorphismTest : FunSpec({
    context("a discriminator attribute") {
        test("names the type as S3 grants do") {
            val grant = Grant(Grantee.CanonicalUser(id = "abc", displayName = "owner"), "FULL_CONTROL")
            val text = acl.encodeToString(grant)

            text shouldContain """<Grantee xmlns:xsi="${Payloads.XSI}" xsi:type="CanonicalUser"><Id>abc</Id>"""
            acl.decodeFromString<Grant>(text) shouldBe grant
        }

        test("names the type of the root") {
            val grantee: Grantee = Grantee.Group("http://acs.amazonaws.com/groups/global/AllUsers")
            val text = acl.encodeToString(grantee)

            acl.decodeFromString<Grantee>(text) shouldBe grantee
        }

        test("is required to decode") {
            codeOf(acl.decodeOrFailure<Grant>(UNNAMED)) shouldBe XmlDiagnosticCode.MissingPolymorphicType
        }

        test("names a type that exists") {
            codeOf(acl.decodeOrFailure<Grant>(ROBOT)) shouldBe XmlDiagnosticCode.UnknownPolymorphicType
        }

        test("cannot be carried by a single value") {
            val module = SerializersModule {
                polymorphic(Any::class) { subclass(VersionId::class, VersionId.serializer()) }
            }
            val format = xmlFormat { serializersModule = module }

            val serializer = PolymorphicSerializer(Any::class)

            shouldThrow<SerializationException> { format.encodeToString(serializer, VersionId("v")) }
        }
    }

    context("a wrapping element") {
        test("names the type as S3 lifecycle filters do") {
            val rule = Rule("r1", Filter.Prefix("logs/"))
            val text = lifecycle.encodeToString(rule)

            text shouldContain "<Filter><Prefix>logs/</Prefix></Filter>"
            lifecycle.decodeFromString<Rule>(text) shouldBe rule
        }

        test("carries a class of its own") {
            val rule = Rule("r2", Filter.And(prefix = "a/", size = 1))
            val text = lifecycle.encodeToString(rule)

            lifecycle.decodeFromString<Rule>(text) shouldBe rule
        }

        test("is required to decode") {
            codeOf(lifecycle.decodeOrFailure<Rule>("<Rule><Id>r</Id><Filter/></Rule>")) shouldBe
                XmlDiagnosticCode.MissingPolymorphicType
        }

        test("must be an element") {
            codeOf(lifecycle.decodeOrFailure<Misplaced>("<Misplaced Filter='x'/>")) shouldBe
                XmlDiagnosticCode.TypeMismatch
        }

        test("cannot stand in an attribute") {
            shouldThrow<SerializationException> { lifecycle.encodeToString(Misplaced(Filter.Prefix("p"))) }
        }
    }
})
