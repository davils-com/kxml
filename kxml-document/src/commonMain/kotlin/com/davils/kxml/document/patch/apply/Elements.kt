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
package com.davils.kxml.document.patch.apply

import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.node.XmlAttribute
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.node.xmlElement
import com.davils.kxml.document.internal.codePointsOf

internal fun XmlNode.Element.rebuilt(
    name: XmlName = this.name,
    attributes: List<XmlAttribute> = this.attributes,
    namespaces: List<XmlNamespaceBinding> = this.namespaces,
    children: List<XmlNode> = this.children,
): XmlNode.Element = xmlElement(name) {
    namespaces.forEach { binding -> namespace(prefix = binding.prefix, uri = binding.uri) }
    attributes.forEach { attribute -> attribute(attribute.name, attribute.value) }
    children.forEach(::node)
}

internal fun <T> List<T>.replaced(range: IntRange, nodes: List<T>): List<T> {
    val before = subList(fromIndex = 0, toIndex = range.first)
    val after = subList(fromIndex = range.last + 1, toIndex = size)
    return before + nodes + after
}

internal fun textOf(nodes: List<XmlNode>): String? {
    val texts = nodes.map { node -> (node as? XmlNode.Text)?.value }
    if (texts.any { text -> text == null }) return null
    return texts.joinToString(separator = "")
}

internal fun prefixOf(binding: XmlNamespaceBinding): String = binding.prefix.orEmpty()

internal fun isXmlText(value: String): Boolean = value.codePointsOf().all(XmlCharacters::isCharacter)
