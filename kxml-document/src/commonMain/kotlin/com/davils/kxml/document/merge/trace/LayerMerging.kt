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
@file:OptIn(DavilsInternal::class)

package com.davils.kxml.document.merge.trace

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.collection.OrderedSet
import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.document.merge.XmlMergeOptions
import com.davils.kxml.document.patch.apply.ContentNamespaces
import com.davils.kxml.document.patch.apply.prefixOf
import com.davils.kxml.document.xpath.evaluate.EvaluationBounds

private const val SEPARATOR: Char = '\u0000'
private const val DELETED: String = "true"

internal class LayerMerging(
    private val options: XmlMergeOptions,
    private val layer: String,
    private val bounds: EvaluationBounds,
) {
    private val charge: (Int) -> Unit = { steps -> bounds.charge(steps.toLong()) }

    fun merged(base: TracedNode.Element, overlay: XmlNode.Element, outer: Map<String, String>) {
        bounds.charge(outer.size.toLong() + base.namespaces.size + base.attributes.size + overlay.attributes.size + 1)
        val scope = outer + base.namespaces.associate { binding -> prefixOf(binding) to binding.uri }
        mergedAttributes(base, overlay)
        val declared = OrderedSet(base.namespaces.map(::prefixOf))
        base.namespaces += overlay.namespaces.filter { binding -> prefixOf(binding) !in declared }
        val hasText = overlay.children.any(::isText)
        if (hasText) replacedContent(base, overlay, scope)
        if (!hasText) mergedChildren(base, overlay, scope)
    }

    private fun mergedAttributes(base: TracedNode.Element, overlay: XmlNode.Element) {
        val existing = OrderedMap<String, TracedAttribute>()
        base.attributes.forEach { attribute -> existing[expandedOf(attribute.name)] = attribute }
        val attributes = overlay.attributes.filter { attribute -> attribute.name != options.deletionMarker }
        attributes.forEach { attribute ->
            val found = existing[expandedOf(attribute.name)]
            val added = TracedAttribute(name = attribute.name, value = attribute.value, origin = layer)
            if (found == null) base.attributes += added
            found?.set(value = attribute.value, origin = layer)
        }
    }

    private fun replacedContent(base: TracedNode.Element, overlay: XmlNode.Element, scope: Map<String, String>) {
        val mapped = ContentNamespaces(scope, base.name, charge).mapped(overlay.children)
        base.children = mapped.mapTo(mutableListOf()) { node -> TracedNode.of(node, layer) }
        base.origin = layer
    }

    private fun mergedChildren(base: TracedNode.Element, overlay: XmlNode.Element, scope: Map<String, String>) {
        bounds.charge(base.children.size.toLong() + overlay.children.size)
        val counts = OrderedMap<String, Int>()
        overlay.elements.forEach { element ->
            val name = expandedOf(element.name)
            counts[name] = (counts[name] ?: 0) + 1
        }
        val deleted = HashSet<TracedNode>()
        val candidates = Candidates(base, deleted)
        val appended = mutableListOf<TracedNode.Element>()
        overlay.elements.forEach { element ->
            val isOnly = counts[expandedOf(element.name)] == 1
            val match = candidates.matchOf(element, isOnly)
            val marker = options.deletionMarker?.let(element::attribute)
            val isDeletion = marker?.value == DELETED
            if (match != null && isDeletion) candidates.removed(match)
            if (match != null && !isDeletion) merged(match, element, scope)
            if (match == null && !isDeletion) appended += candidates.added(mappedInto(base, element, scope))
        }
        base.children.removeAll { child -> child in deleted }
        val kept = appended.filterNot { element -> element in deleted }
        appendedTo(base, kept)
    }

    private fun mappedInto(
        base: TracedNode.Element,
        element: XmlNode.Element,
        scope: Map<String, String>,
    ): TracedNode.Element {
        val namespaces = ContentNamespaces(scope, base.name, charge)
        val mapped = namespaces.mapped(listOf(element))
        return TracedNode.of(mapped.single(), layer) as TracedNode.Element
    }

    private fun appendedTo(base: TracedNode.Element, elements: List<TracedNode.Element>) {
        if (elements.isEmpty()) return
        val last = base.children.indexOfLast { child -> child is TracedNode.Element }
        val indent = base.children.getOrNull(last - 1)?.takeIf { last > 0 && isBlankText(it) }
        val added = elements.flatMap { element ->
            val copied = indent?.let { found -> TracedNode.Leaf(found.toNode(), layer) }
            listOfNotNull(copied) + element
        }
        val at = (last + 1).takeIf { last >= 0 } ?: base.children.size
        base.children.addAll(index = at, elements = added)
    }

    private inner class Candidates(base: TracedNode.Element, private val deleted: MutableSet<TracedNode>) {
        private val byKey = OrderedMap<String, ArrayDeque<TracedNode.Element>>()
        private val byName = OrderedMap<String, ArrayDeque<TracedNode.Element>>()
        private val counts = OrderedMap<String, Int>()

        init {
            base.children.filterIsInstance<TracedNode.Element>().forEach(::added)
        }

        fun matchOf(element: XmlNode.Element, isOnly: Boolean): TracedNode.Element? {
            val keys = options.keys[element.name] ?: return onlyOf(expandedOf(element.name))?.takeIf { isOnly }
            val wanted = keyOf(element.name, keys) { name -> element.attribute(name)?.value }
            return wanted?.let { found -> byKey[found]?.firstOrNull() }
        }

        fun added(element: TracedNode.Element): TracedNode.Element {
            val name = expandedOf(element.name)
            byName.getOrPut(name) { ArrayDeque() }.addLast(element)
            counts[name] = (counts[name] ?: 0) + 1
            tracedKeyOf(element)?.let { key -> byKey.getOrPut(key) { ArrayDeque() }.addLast(element) }
            return element
        }

        fun removed(element: TracedNode.Element) {
            deleted += element
            val name = expandedOf(element.name)
            counts[name] = (counts[name] ?: 0) - 1
            tracedKeyOf(element)?.let { key -> byKey[key]?.removeFirst() }
        }

        private fun tracedKeyOf(element: TracedNode.Element): String? {
            val keys = options.keys[element.name] ?: return null
            return keyOf(element.name, keys) { name -> element.valueOf(name) }
        }

        private fun onlyOf(name: String): TracedNode.Element? {
            val named = byName[name]
            if (counts[name] != 1 || named == null) return null
            named.removeAll { element -> element in deleted }
            return named.single()
        }
    }

    private fun TracedNode.Element.valueOf(name: XmlName): String? =
        attributes.firstOrNull { attribute -> attribute.name == name }?.value

    private fun keyOf(element: XmlName, keys: List<XmlName>, valueOf: (XmlName) -> String?): String? {
        val values = keys.map(valueOf)
        if (values.any { value -> value == null }) return null
        return values.joinToString(separator = SEPARATOR.toString(), prefix = expandedOf(element) + SEPARATOR)
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
