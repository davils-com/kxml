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

import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.document.merge.XmlMergeOptions
import com.davils.kxml.document.patch.apply.ContentNamespaces
import com.davils.kxml.document.patch.apply.prefixOf

private const val SEPARATOR: Char = '\u0000'
private const val DELETED: String = "true"

internal class LayerMerging(private val options: XmlMergeOptions, private val layer: String) {
    fun merged(base: TracedNode.Element, overlay: XmlNode.Element, outer: Map<String, String>) {
        val scope = outer + base.namespaces.associate { binding -> prefixOf(binding) to binding.uri }
        val attributes = overlay.attributes.filter { attribute -> attribute.name != options.deletionMarker }
        attributes.forEach { attribute ->
            val found = base.attributes.firstOrNull { existing -> existing.name == attribute.name }
            val added = TracedAttribute(name = attribute.name, value = attribute.value, origin = layer)
            if (found == null) base.attributes += added
            found?.set(value = attribute.value, origin = layer)
        }
        val declared = base.namespaces.map(::prefixOf).toSet()
        base.namespaces += overlay.namespaces.filter { binding -> prefixOf(binding) !in declared }
        val hasText = overlay.children.any(::isText)
        if (hasText) replacedContent(base, overlay, scope)
        if (!hasText) mergedChildren(base, overlay, scope)
    }

    private fun replacedContent(base: TracedNode.Element, overlay: XmlNode.Element, scope: Map<String, String>) {
        val mapped = ContentNamespaces(scope, base.name).mapped(overlay.children)
        base.children = mapped.mapTo(mutableListOf()) { node -> TracedNode.of(node, layer) }
        base.origin = layer
    }

    private fun mergedChildren(base: TracedNode.Element, overlay: XmlNode.Element, scope: Map<String, String>) {
        val counts = overlay.elements.groupingBy { element -> expandedOf(element.name) }.eachCount()
        overlay.elements.forEach { element ->
            val isOnly = counts[expandedOf(element.name)] == 1
            val match = matchOf(base, element, isOnly)
            val marker = options.deletionMarker?.let(element::attribute)
            val isDeletion = marker?.value == DELETED
            if (match != null && isDeletion) base.children.remove(match)
            if (match != null && !isDeletion) merged(match, element, scope)
            if (match == null && !isDeletion) appended(base, element, scope)
        }
    }

    private fun matchOf(base: TracedNode.Element, element: XmlNode.Element, isOnly: Boolean): TracedNode.Element? {
        val elements = base.children.filterIsInstance<TracedNode.Element>()
        val candidates = elements.filter { child -> child.name == element.name }
        val keys = options.keys[element.name] ?: return candidates.singleOrNull()?.takeIf { isOnly }
        val wanted = keyOf(keys) { name -> element.attribute(name)?.value }
        return candidates.firstOrNull { candidate -> wanted != null && keyOf(keys) { candidate.valueOf(it) } == wanted }
    }

    private fun TracedNode.Element.valueOf(name: XmlName): String? =
        attributes.firstOrNull { attribute -> attribute.name == name }?.value

    private fun keyOf(keys: List<XmlName>, valueOf: (XmlName) -> String?): String? {
        val values = keys.map(valueOf)
        if (values.any { value -> value == null }) return null
        return values.joinToString(separator = SEPARATOR.toString())
    }

    private fun appended(base: TracedNode.Element, element: XmlNode.Element, scope: Map<String, String>) {
        val namespaces = ContentNamespaces(scope, base.name)
        val mapped = namespaces.mapped(listOf(element)).single()
        val last = base.children.indexOfLast { child -> child is TracedNode.Element }
        val indent = base.children.getOrNull(last - 1)?.takeIf { last > 0 && isBlankText(it) }
        val copied = indent?.let { found -> TracedNode.Leaf(found.toNode(), layer) }
        val added = listOfNotNull(copied) + TracedNode.of(mapped, layer)
        val at = (last + 1).takeIf { last >= 0 } ?: base.children.size
        base.children.addAll(index = at, elements = added)
    }

    private fun isText(node: XmlNode): Boolean {
        val value = (node as? XmlNode.Text)?.value ?: (node as? XmlNode.CData)?.value ?: return false
        return !value.all(XmlCharacters::isWhitespace)
    }

    private fun isBlankText(node: TracedNode): Boolean {
        val text = (node as? TracedNode.Leaf)?.node as? XmlNode.Text ?: return false
        return text.value.all(XmlCharacters::isWhitespace)
    }

    private fun expandedOf(name: XmlName): String = "{${name.namespace.orEmpty()}}${name.local}"
}
