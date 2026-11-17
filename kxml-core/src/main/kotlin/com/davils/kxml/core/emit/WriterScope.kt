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

package com.davils.kxml.core.emit

import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.node.XmlNode

private const val DEFAULT: String = ""
private const val GENERATED_PREFIX: String = "ns"

internal class WriterScope private constructor(private val parent: WriterScope?, initial: Map<String, String>) {
    private val bindings = LinkedHashMap(initial)

    private val attributePrefixes = mutableMapOf<String, String>()

    val declarations: List<XmlNamespaceBinding>
        get() = bindings.map { (prefix, uri) -> XmlNamespaceBinding(prefix.takeIf { it.isNotEmpty() }, uri) }

    fun child(): WriterScope = WriterScope(parent = this, initial = emptyMap())

    fun open(element: XmlNode.Element) {
        bindElement(element.name)
        element.namespaces.forEach(::bindExplicit)
        element.attributes.forEach { attribute -> bindAttribute(attribute.name) }
    }

    fun attributeName(name: XmlName): String {
        val namespace = name.namespace ?: return name.local
        return "${attributePrefixes.getValue(namespace)}:${name.local}"
    }

    private fun lookup(prefix: String): String? = bindings[prefix] ?: parent?.lookup(prefix)

    private fun prefixesInScope(): Set<String> = bindings.keys + parent?.prefixesInScope().orEmpty()

    private fun bindElement(name: XmlName) {
        val prefix = name.prefix ?: DEFAULT
        val wanted = name.namespace ?: DEFAULT
        val current = lookup(prefix) ?: DEFAULT
        if (current != wanted) bindings[prefix] = wanted
    }

    private fun bindExplicit(binding: XmlNamespaceBinding) {
        val prefix = binding.prefix ?: DEFAULT
        val current = lookup(prefix) ?: DEFAULT
        if (prefix !in bindings && current != binding.uri) bindings[prefix] = binding.uri
    }

    private fun bindAttribute(name: XmlName) {
        val namespace = name.namespace?.takeIf { it !in attributePrefixes } ?: return
        attributePrefixes[namespace] = prefixFor(namespace, name.prefix)
    }

    private fun prefixFor(namespace: String, wanted: String?): String {
        val bound = wanted?.takeIf { lookup(it) == namespace } ?: visiblePrefixOf(namespace)
        return bound ?: declared(wanted?.takeIf { it !in bindings } ?: freshPrefix(), namespace)
    }

    private fun visiblePrefixOf(namespace: String): String? =
        prefixesInScope().firstOrNull { prefix -> prefix.isNotEmpty() && lookup(prefix) == namespace }

    private fun declared(prefix: String, namespace: String): String {
        bindings[prefix] = namespace
        return prefix
    }

    private fun freshPrefix(): String {
        val candidates = generateSequence(1) { index -> index + 1 }
        val prefixes = candidates.map { index -> "$GENERATED_PREFIX$index" }
        return prefixes.first { prefix -> lookup(prefix) == null }
    }

    companion object {
        fun root(): WriterScope = WriterScope(
            parent = null,
            initial = mapOf(XmlNamespaces.XML_PREFIX to XmlNamespaces.XML),
        )
    }
}
