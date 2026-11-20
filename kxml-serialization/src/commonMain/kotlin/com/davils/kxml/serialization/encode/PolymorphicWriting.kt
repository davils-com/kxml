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

package com.davils.kxml.serialization.encode

import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.node.XmlAttribute
import com.davils.kxml.serialization.SchemaInstance
import com.davils.kxml.serialization.XmlPolymorphism
import com.davils.kxml.serialization.shape.EntryNames
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.findPolymorphicSerializer
import kotlinx.serialization.internal.AbstractPolymorphicSerializer

@OptIn(InternalSerializationApi::class)
internal class PolymorphicWriting(private val context: EncodeContext) {
    fun <T> encode(serializer: SerializationStrategy<T>, value: T, sink: (Content) -> Unit) {
        val polymorphic = serializer as? AbstractPolymorphicSerializer<*>
        val encoder = ValueEncoder(context, sink)
        if (polymorphic == null) {
            serializer.serialize(encoder, value)
            return
        }
        val present = checkNotNull(value) { "A polymorphic value is never null here" }

        @Suppress("UNCHECKED_CAST")
        val concrete = (polymorphic as AbstractPolymorphicSerializer<Any>).findPolymorphicSerializer(encoder, present)
        val captured = mutableListOf<Content>()
        concrete.serialize(ValueEncoder(context) { content -> captured += content }, present)
        sink(typed(concrete.descriptor, captured.single()))
    }

    private fun typed(descriptor: SerialDescriptor, content: Content): Content =
        when (context.configuration.polymorphism) {
            XmlPolymorphism.Attribute -> withDiscriminator(
                descriptor = descriptor,
                content = content,
                discriminatorName = context.configuration.classDiscriminator,
            )

            XmlPolymorphism.SchemaType -> withDiscriminator(descriptor, content, SchemaInstance.type)

            XmlPolymorphism.Element -> wrapped(descriptor, content)
        }

    private fun withDiscriminator(descriptor: SerialDescriptor, content: Content, discriminatorName: XmlName): Content {
        val structure = content as? Content.Structure
            ?: throw SerializationException("The type ${descriptor.serialName} needs a class to carry its name")
        val discriminator = XmlAttribute(discriminatorName, descriptor.serialName)
        return structure.copy(attributes = listOf(discriminator) + structure.attributes)
    }

    private fun wrapped(descriptor: SerialDescriptor, content: Content): Content {
        val name = context.shapes.typeName(descriptor)
        return Content.Structure(emptyList(), ElementWriting.named(name, content, EntryNames.Default))
    }
}
