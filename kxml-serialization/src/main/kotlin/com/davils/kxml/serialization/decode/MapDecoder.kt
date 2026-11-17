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

import com.davils.kxml.core.node.XmlNode
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder

internal class MapDecoder(context: DecodeContext, value: XmlValue) : StructureDecoder(context) {
    private val entries = value as? XmlValue.Entries ?: context.mismatch(value.range, "entries")

    private var next = 0

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = entries.elements.size

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (next >= entries.elements.size * 2) return CompositeDecoder.DECODE_DONE
        return next++
    }

    override fun valueAt(index: Int): XmlValue {
        val entry = entries.elements[index / 2]
        if (index % 2 == 0) return key(entry)
        val value = entry.elements.firstOrNull { child ->
            context.matches(actual = child.name, expected = entries.value)
        }
        return value?.let(XmlValue::Element) ?: XmlValue.Absent(entry.range)
    }

    private fun key(entry: XmlNode.Element): XmlValue {
        val key = entry.elements.firstOrNull { child -> context.matches(actual = child.name, expected = entries.key) }
        return key?.let(XmlValue::Element) ?: context.mismatch(entry.range, "an entry with <${entries.key}>")
    }
}
