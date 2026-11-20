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
package com.davils.kxml.document.merge.trace

import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.node.xmlElement

internal sealed interface TracedNode {
    val origin: String

    fun toNode(): XmlNode

    class Leaf(val node: XmlNode, override val origin: String) : TracedNode {
        override fun toNode(): XmlNode = node
    }

    class Element(
        val name: XmlName,
        val namespaces: MutableList<XmlNamespaceBinding>,
        val attributes: MutableList<TracedAttribute>,
        var children: MutableList<TracedNode>,
        override var origin: String,
    ) : TracedNode {
        override fun toNode(): XmlNode.Element = xmlElement(name) {
            namespaces.forEach { binding -> namespace(prefix = binding.prefix, uri = binding.uri) }
            attributes.forEach { attribute -> attribute(attribute.name, attribute.value) }
            children.forEach { child -> node(child.toNode()) }
        }
    }

    companion object {
        fun of(node: XmlNode, origin: String): TracedNode {
            val element = node as? XmlNode.Element ?: return Leaf(node, origin)
            return Element(
                name = element.name,
                namespaces = element.namespaces.toMutableList(),
                attributes = element.attributes.mapTo(mutableListOf()) { attribute ->
                    TracedAttribute(attribute.name, attribute.value, origin)
                },
                children = element.children.mapTo(mutableListOf()) { child -> of(child, origin) },
                origin = origin,
            )
        }
    }
}
