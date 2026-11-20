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
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.node.xmlElement
import com.davils.kxml.serialization.SchemaInstance
import com.davils.kxml.serialization.shape.EntryNames
import com.davils.kxml.serialization.shape.PropertyShape

private const val NIL: String = "true"

internal object ElementWriting {
    fun elementsOf(shape: PropertyShape, content: Content): List<XmlNode> {
        val item = shape.item ?: return named(shape.name, content, shape.entries)
        val wrapped = named(item, content, shape.entries)
        return listOf(xmlElement(shape.name) { wrapped.forEach(::node) })
    }

    fun named(name: XmlName, content: Content, entries: EntryNames): List<XmlNode> = when (content) {
        is Content.Absent -> emptyList()
        is Content.Nil -> listOf(nil(name))
        is Content.Text -> listOf(xmlElement(name) { text(content.value) })
        is Content.Structure -> listOf(structured(name, content))
        is Content.Items -> content.items.flatMap { item -> named(name, item, entries) }
        is Content.Entries -> content.entries.map { (key, value) -> entry(name, key to value, entries) }
    }

    fun nil(name: XmlName): XmlNode.Element = xmlElement(name) { attribute(SchemaInstance.nil, NIL) }

    fun structured(name: XmlName, content: Content.Structure): XmlNode.Element = xmlElement(name) {
        content.attributes.forEach { attribute -> attribute(attribute.name, attribute.value) }
        content.children.forEach(::node)
    }

    private fun entry(name: XmlName, entry: Pair<Content, Content>, entries: EntryNames): XmlNode.Element =
        xmlElement(name) {
            named(entries.key, entry.first, EntryNames.Default).forEach(::node)
            named(entries.value, entry.second, EntryNames.Default).forEach(::node)
        }
}
