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

import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.node.XmlAttribute
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.document.c14n.render.CodePointOrder

private const val DEFAULT_PREFIX: String = ""
private const val GENERATED_PREFIX: String = "ns"

internal class ContentNamespaces(
    private val scope: Map<String, String>,
    private val context: XmlName?,
    private val charge: (Int) -> Unit,
) {
    fun mapped(nodes: List<XmlNode>): List<XmlNode> = nodes.map { node -> mappedNode(node, scope, context) }

    fun attributeName(name: XmlName): Pair<XmlName, XmlNamespaceBinding?> {
        val mapping = Mapping(scope, context)
        val mapped = mapping.name(name, isElement = false)
        return mapped to mapping.added.firstOrNull()
    }

    private fun mappedNode(node: XmlNode, outer: Map<String, String>, parent: XmlName?): XmlNode {
        val element = node as? XmlNode.Element ?: return node
        return mappedElement(element, outer, parent)
    }

    private fun mappedElement(element: XmlNode.Element, outer: Map<String, String>, parent: XmlName?): XmlNode.Element {
        val declared = element.namespaces
        charge(outer.size + declared.size)
        val mapping = Mapping(outer + declared.asScope(), parent)
        val name = mapping.name(element.name, isElement = true)
        val attributes = element.attributes.map { attribute ->
            XmlAttribute(mapping.name(attribute.name, isElement = false), attribute.value)
        }
        val inner = mapping.scope()
        val children = element.children.map { child -> mappedNode(child, inner, name) }
        return element.rebuilt(
            name = name,
            attributes = attributes,
            namespaces = declared + mapping.added,
            children = children,
        )
    }

    private fun List<XmlNamespaceBinding>.asScope(): Map<String, String> =
        associate { binding -> prefixOf(binding) to binding.uri }

    private inner class Mapping(private val outer: Map<String, String>, private val parent: XmlName?) {
        val added: MutableList<XmlNamespaceBinding> = mutableListOf()

        fun scope(): Map<String, String> {
            charge(outer.size + added.size)
            return outer + added.asScope()
        }

        fun name(name: XmlName, isElement: Boolean): XmlName {
            val uri = name.namespace ?: return unqualified(name, isElement)
            val scope = scope()
            val candidates = scope.filter { (prefix, bound) -> bound == uri && (isElement || prefix.isNotEmpty()) }
            val written = name.prefix ?: DEFAULT_PREFIX
            val prefix = chosen(candidates = candidates.keys, written = written, uri = uri)
                ?: declared(name, uri = uri, isElement = isElement)
            return XmlName(local = name.local, namespace = uri, prefix = prefix.takeIf(String::isNotEmpty))
        }

        private fun unqualified(name: XmlName, isElement: Boolean): XmlName {
            val isDefaulted = isElement && !scope()[DEFAULT_PREFIX].isNullOrEmpty()
            if (isDefaulted) added += XmlNamespaceBinding(prefix = null, uri = "")
            return XmlName(name.local)
        }

        private fun chosen(candidates: Set<String>, written: String, uri: String): String? {
            val parentPrefix = parent?.takeIf { found -> found.namespace == uri }?.prefix ?: DEFAULT_PREFIX
            val preferred = written.takeIf { it in candidates }
                ?: parentPrefix.takeIf { parent?.namespace == uri && it in candidates }
            if (preferred != null) return preferred
            val sorted = candidates.sortedWith(CodePointOrder)
            val before = sorted.lastOrNull { prefix -> CodePointOrder.compare(a = prefix, b = written) < 0 }
            return before ?: sorted.firstOrNull()
        }

        private fun declared(name: XmlName, uri: String, isElement: Boolean): String {
            val written = name.prefix ?: DEFAULT_PREFIX.takeIf { isElement } ?: freshPrefix()
            added += XmlNamespaceBinding(prefix = written.takeIf(String::isNotEmpty), uri = uri)
            return written
        }

        private fun freshPrefix(): String {
            val scope = scope()
            val prefixes = generateSequence(0) { index -> index + 1 }.map { index -> "$GENERATED_PREFIX$index" }
            return prefixes.first { prefix -> prefix !in scope }
        }
    }
}
