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

package com.davils.kxml.serialization.decode

import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.node.xmlElement
import com.davils.kxml.serialization.XmlFormat
import com.davils.kxml.serialization.fixture.CommonPrefix
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.serialization.encoding.CompositeDecoder

private val element = xmlElement(XmlName("CommonPrefix")) {
    element(XmlName("prefix")) { text("p") }
}

class DecodersTest : FunSpec({
    context("a value decoder") {
        test("has no elements of its own") {
            val context = DecodeContext(XmlFormat.Default.configuration)
            val decoder = NodeDecoder(context, XmlValue.Element(element))

            decoder.decodeElementIndex(CommonPrefix.serializer().descriptor) shouldBe CompositeDecoder.DECODE_DONE
        }
    }

    context("a structure decoder") {
        test("shares the module of the format and decodes inline elements") {
            val configuration = XmlFormat.Default.configuration
            val context = DecodeContext(configuration)
            val descriptor = CommonPrefix.serializer().descriptor
            val decoder = ClassDecoder(context, descriptor, XmlValue.Element(element))
            val inline = decoder.decodeInlineElement(descriptor, 0)

            decoder.serializersModule shouldBeSameInstanceAs configuration.serializersModule
            inline.decodeString() shouldBe "p"
        }
    }
})
