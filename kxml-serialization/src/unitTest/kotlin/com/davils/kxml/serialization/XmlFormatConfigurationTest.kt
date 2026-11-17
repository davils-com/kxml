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

import com.davils.kore.core.validation.report.ValidationException
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.serialization.fixture.CommonPrefix
import com.davils.kxml.serialization.fixture.Payloads
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor

private const val MAX_KEYS: String = "maxKeys"
private val descriptor = PrimitiveSerialDescriptor("Name", PrimitiveKind.STRING)

class XmlFormatConfigurationTest : FunSpec({
    context("the configuration") {
        test("starts from strict defaults") {
            val configuration = XmlFormat.Default.configuration

            configuration.namespace shouldBe null
            configuration.namespaceMatching shouldBe XmlNamespaceMatching.Exact
            configuration.isUnknownElementIgnored shouldBe false
            configuration.polymorphism shouldBe XmlPolymorphism.Attribute
            configuration.classDiscriminator shouldBe XmlName("type")
            configuration.toString() shouldContain "namespace=none"
            XmlFormat.Default.toString() shouldContain "XmlFormat"
        }

        test("describes its namespace") {
            val format = xmlFormat { namespace = Payloads.S3 }

            format.configuration.toString() shouldContain "namespace=\"${Payloads.S3}\""
        }

        test("refuses an empty namespace") {
            shouldThrow<ValidationException> { xmlFormat { namespace = "" } }
        }
    }

    context("naming strategies") {
        test("rename properties") {
            XmlNamingStrategy.Identity.nameOf(descriptor, 0, MAX_KEYS) shouldBe MAX_KEYS
            XmlNamingStrategy.PascalCase.nameOf(descriptor, 0, MAX_KEYS) shouldBe "MaxKeys"
            XmlNamingStrategy.PascalCase.nameOf(descriptor, 0, "") shouldBe ""
            XmlNamingStrategy.PascalCase.nameOf(descriptor, 0, "Key") shouldBe "Key"
            XmlNamingStrategy.KebabCase.nameOf(descriptor, 0, "MaxKeysURL") shouldBe "max-keys-url"
        }
    }

    context("encoding refusals") {
        test("refuse a collection as the root") {
            shouldThrow<SerializationException> {
                XmlFormat.Default.encodeToString(ListSerializer(String.serializer()), listOf("a"))
            }
        }

        test("refuse a map as the root") {
            val serializer = MapSerializer(String.serializer(), Int.serializer())

            shouldThrow<SerializationException> { XmlFormat.Default.encodeToElement(serializer, mapOf("a" to 1)) }
        }

        test("write each present property") {
            val element = XmlFormat.Default.encodeToElement(CommonPrefix("p"))

            element.elements.size shouldBe 1
        }
    }
})
