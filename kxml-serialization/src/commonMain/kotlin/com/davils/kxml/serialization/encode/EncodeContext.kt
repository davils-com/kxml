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

import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.node.xmlElement
import com.davils.kxml.serialization.XmlFormatConfiguration
import com.davils.kxml.serialization.shape.Shapes
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlin.io.encoding.Base64

private val binary: SerialDescriptor = ByteArraySerializer().descriptor

internal class EncodeContext(val configuration: XmlFormatConfiguration) {
    private val polymorphism = PolymorphicWriting(this)

    val shapes: Shapes = Shapes(configuration)

    fun <T> root(serializer: SerializationStrategy<T>, value: T): XmlNode.Element {
        val captured = mutableListOf<Content>()
        encode(serializer, value) { content -> captured += content }
        val name = shapes.rootName(serializer.descriptor)
        return when (val content = captured.single()) {
            is Content.Structure -> ElementWriting.structured(name, content)
            is Content.Text -> xmlElement(name) { text(content.value) }
            is Content.Absent -> xmlElement(name)
            is Content.Nil -> ElementWriting.nil(name)
            is Content.Items -> refuseRoot()
            is Content.Entries -> refuseRoot()
        }
    }

    fun collector(descriptor: SerialDescriptor): ContentCollector {
        val items = ItemsCollector().takeIf { descriptor.kind == StructureKind.LIST }
        val entries = EntriesCollector().takeIf { descriptor.kind == StructureKind.MAP }
        return items ?: entries ?: ClassCollector(shapes.of(descriptor))
    }

    fun <T> encode(serializer: SerializationStrategy<T>, value: T, sink: (Content) -> Unit) {
        if (value is ByteArray && serializer.descriptor == binary) {
            sink(Content.Text(Base64.encode(value)))
            return
        }
        polymorphism.encode(serializer, value, sink)
    }

    private fun refuseRoot(): Nothing =
        throw SerializationException("The root of an XML document must be a class or a single value")
}
