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

import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.serialization.XmlPolymorphism
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.internal.AbstractPolymorphicSerializer

@OptIn(InternalSerializationApi::class)
internal class PolymorphicReading(private val context: DecodeContext) {
    fun <T> decode(deserializer: DeserializationStrategy<T>, decoder: NodeDecoder): T {
        val polymorphic = deserializer as? AbstractPolymorphicSerializer<*> ?: return deserializer.deserialize(decoder)
        val element = elementOf(decoder.value)
        val (name, content) = typed(element)
        val concrete = polymorphic.findPolymorphicSerializerOrNull(decoder, name) ?: context.fail(
            XmlDiagnosticCode.UnknownPolymorphicType,
            element.range,
            "There is no type \"$name\"",
        )

        @Suppress("UNCHECKED_CAST")
        return NodeDecoder(context, XmlValue.Element(content)).decodeSerializableValue(concrete) as T
    }

    private fun elementOf(value: XmlValue): XmlNode.Element {
        val holder = value as? XmlValue.Element ?: context.mismatch(value.range, "an element")
        return holder.element
    }

    private fun typed(element: XmlNode.Element): Pair<String, XmlNode.Element> =
        when (context.configuration.polymorphism) {
            XmlPolymorphism.Attribute -> byAttribute(element)
            XmlPolymorphism.Element -> byElement(element)
        }

    private fun byAttribute(element: XmlNode.Element): Pair<String, XmlNode.Element> {
        val discriminator = context.configuration.classDiscriminator
        val attribute = element.attributes.firstOrNull { context.matches(actual = it.name, expected = discriminator) }
            ?: missing(element, "the attribute $discriminator")
        return attribute.value to element
    }

    private fun byElement(element: XmlNode.Element): Pair<String, XmlNode.Element> {
        val child = element.elements.singleOrNull() ?: missing(element, "one element named after the type")
        return child.name.local to child
    }

    private fun missing(element: XmlNode.Element, what: String): Nothing = context.fail(
        XmlDiagnosticCode.MissingPolymorphicType,
        element.range,
        "Expected $what to choose the type",
    )
}
