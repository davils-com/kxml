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

import com.davils.kxml.serialization.fixture.Release
import com.davils.kxml.serialization.fixture.VersionId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

class XmlValueClassTest : FunSpec({
    context("value classes") {
        test("are written as the value they wrap") {
            val release = Release(name = "kxml", version = VersionId("v1"))
            val text = XmlFormat.Default.encodeToString(release)

            text shouldContain "<version>v1</version>"
            XmlFormat.Default.decodeFromString<Release>(text) shouldBe release
        }

        test("can stand as the root") {
            val text = XmlFormat.Default.encodeToString(VersionId("v9"))

            XmlFormat.Default.decodeFromString<VersionId>(text) shouldBe VersionId("v9")
        }

        test("cannot carry the type of a polymorphic value as an attribute") {
            val module = SerializersModule {
                polymorphic(Any::class) { subclass(VersionId::class, VersionId.serializer()) }
            }
            val format = xmlFormat { serializersModule = module }
            val serializer = PolymorphicSerializer(Any::class)

            shouldThrow<SerializationException> { format.encodeToString(serializer, VersionId("v")) }
        }
    }
})
