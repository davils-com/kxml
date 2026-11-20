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

package com.davils.kxml.core.emit

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.node.XmlNode

private const val DEFAULT: String = ""
private const val GENERATED_PREFIX: String = "ns"

internal class WriterScope(private val isUndeclaringAllowed: Boolean) {
    private val uris = OrderedMap<String, ArrayDeque<String>>()
    private val prefixes = OrderedMap<String, ArrayDeque<String>>()
    private val frames = ArrayDeque<List<String>>()
    private var generated = 0

    init {
        bind(prefix = XmlNamespaces.XML_PREFIX, uri = XmlNamespaces.XML)
    }

    fun open(element: XmlNode.Element): OpenedElement {
        val opened = OpenedElement()
        bindElement(element.name, opened)
        element.namespaces.forEach { binding -> bindExplicit(binding, opened) }
        element.attributes.forEach { attribute -> bindAttribute(attribute.name, opened) }
        frames.addLast(opened.prefixes)
        return opened
    }

    fun close() {
        val bound = frames.removeLast()
        bound.asReversed().forEach(::unbind)
    }

    private fun lookup(prefix: String): String? = uris[prefix]?.lastOrNull()

    private fun bind(prefix: String, uri: String) {
        val boundUris = uris.getOrPut(prefix) { ArrayDeque() }
        val boundPrefixes = prefixes.getOrPut(uri) { ArrayDeque() }
        boundUris.addLast(uri)
        boundPrefixes.addLast(prefix)
    }

    private fun unbind(prefix: String) {
        val uri = uris[prefix]?.removeLast().orEmpty()
        prefixes[uri]?.removeLast()
    }

    private fun declare(prefix: String, uri: String, opened: OpenedElement) {
        bind(prefix, uri)
        opened.declare(prefix, uri)
    }

    private fun bindElement(name: XmlName, opened: OpenedElement) {
        val prefix = name.prefix ?: DEFAULT
        val wanted = name.namespace ?: DEFAULT
        val current = lookup(prefix) ?: DEFAULT
        if (current != wanted) declare(prefix, wanted, opened)
    }

    private fun bindExplicit(binding: XmlNamespaceBinding, opened: OpenedElement) {
        val isUndeclaring = binding.prefix != null && binding.uri.isEmpty()
        require(!isUndeclaring || isUndeclaringAllowed) { "A prefix can only be undeclared in a document of XML 1.1" }
        val prefix = binding.prefix ?: DEFAULT
        val current = lookup(prefix) ?: DEFAULT
        if (!opened.isDeclared(prefix) && current != binding.uri) declare(prefix, binding.uri, opened)
    }

    private fun bindAttribute(name: XmlName, opened: OpenedElement) {
        val namespace = name.namespace?.takeUnless(opened::hasAttributePrefix) ?: return
        opened.useAttributePrefix(namespace, prefixFor(namespace, name.prefix, opened))
    }

    private fun prefixFor(namespace: String, wanted: String?, opened: OpenedElement): String {
        val bound = wanted?.takeIf { prefix -> lookup(prefix) == namespace } ?: visiblePrefixOf(namespace)
        if (bound != null) return bound
        val prefix = wanted?.takeUnless(opened::isDeclared) ?: freshPrefix()
        declare(prefix, namespace, opened)
        return prefix
    }

    private fun visiblePrefixOf(namespace: String): String? {
        val candidates = prefixes[namespace].orEmpty()
        return candidates.asReversed().firstOrNull { prefix -> prefix.isNotEmpty() && lookup(prefix) == namespace }
    }

    private fun freshPrefix(): String {
        var prefix = GENERATED_PREFIX + ++generated
        while (lookup(prefix) != null) prefix = GENERATED_PREFIX + ++generated
        return prefix
    }
}
