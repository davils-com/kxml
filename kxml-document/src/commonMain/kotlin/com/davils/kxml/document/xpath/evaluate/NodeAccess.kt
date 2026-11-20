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

package com.davils.kxml.document.xpath.evaluate

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.navigation.NodeKind
import com.davils.kxml.document.xpath.XPathNodeKind

private const val DEFAULT_PREFIX: String = ""

private val idName: XmlName = XmlName(local = "id", namespace = XmlNamespaces.XML)
private val langName: XmlName = XmlName(local = "lang", namespace = XmlNamespaces.XML)
private val xmlBinding: XmlNamespaceBinding =
    XmlNamespaceBinding(prefix = XmlNamespaces.XML_PREFIX, uri = XmlNamespaces.XML)

internal class NodeAccess(val model: DocumentModel, private val bounds: EvaluationBounds) {
    private val inScope: MutableMap<Int, List<XmlNamespaceBinding>> = mutableMapOf()
    private var ids: OrderedMap<String, Int>? = null

    fun kindOf(key: Long): XPathNodeKind {
        if (NodeKeys.isNamespace(key)) return XPathNodeKind.Namespace
        return when (model.kind(NodeKeys.ordinalOf(key))) {
            NodeKind.Root -> XPathNodeKind.Root
            NodeKind.Element -> XPathNodeKind.Element
            NodeKind.Attribute -> XPathNodeKind.Attribute
            NodeKind.Text -> XPathNodeKind.Text
            NodeKind.Comment -> XPathNodeKind.Comment
            NodeKind.ProcessingInstruction -> XPathNodeKind.ProcessingInstruction
        }
    }

    fun parentOf(key: Long): Int? {
        val ordinal = NodeKeys.ordinalOf(key)
        if (NodeKeys.isNamespace(key)) return ordinal
        return model.parent(ordinal).takeIf { parent -> parent >= 0 }
    }

    fun namespacesOf(element: Int): List<XmlNamespaceBinding> = inScope.getOrPut(element) { inScopeOf(element) }

    fun namespaceOf(key: Long): XmlNamespaceBinding {
        val bindings = namespacesOf(NodeKeys.ordinalOf(key))
        return bindings[NodeKeys.namespaceIndexOf(key)]
    }

    fun nameOf(key: Long): XmlName? {
        if (!NodeKeys.isNamespace(key)) return model.name(NodeKeys.ordinalOf(key))
        return namespaceOf(key).prefix?.let { prefix -> XmlName(prefix) }
    }

    fun stringValueOf(key: Long): String {
        if (NodeKeys.isNamespace(key)) return namespaceOf(key).uri
        val ordinal = NodeKeys.ordinalOf(key)
        return model.value(ordinal) ?: descendantText(ordinal)
    }

    fun elementById(id: String): Int? {
        val index = ids ?: idIndex()
        ids = index
        return index[id]
    }

    fun langOf(key: Long): String? {
        var element = elementOf(key)
        while (element != null && element > DocumentModel.ROOT) {
            val lang = langAt(element)
            if (lang != null) return lang
            element = model.parent(element)
        }
        return null
    }

    private fun elementOf(key: Long): Int? {
        val ordinal = NodeKeys.ordinalOf(key)
        val isElement = !NodeKeys.isNamespace(key) && model.isElement(ordinal)
        if (isElement) return ordinal
        return parentOf(key)
    }

    private fun langAt(element: Int): String? {
        for (attribute in model.attributes(element)) {
            bounds.charge()
            if (model.name(attribute) == langName) return model.value(attribute)
        }
        return null
    }

    private fun inScopeOf(element: Int): List<XmlNamespaceBinding> {
        val found = OrderedMap<String, XmlNamespaceBinding>()
        found[XmlNamespaces.XML_PREFIX] = xmlBinding
        var current = element
        while (current > DocumentModel.ROOT) {
            model.declaredNamespaces(current).forEach { binding ->
                bounds.charge()
                val prefix = binding.prefix ?: DEFAULT_PREFIX
                if (prefix !in found) found[prefix] = binding
            }
            current = model.parent(current)
        }
        val entries = found.entries.sortedBy { entry -> entry.first }
        val bindings = entries.map { entry -> entry.second }
        return bindings.filter { binding -> binding.uri.isNotEmpty() }
    }

    private fun descendantText(ordinal: Int): String {
        val joined = TextJoin(bounds)
        for (descendant in ordinal + 1..<model.end(ordinal)) {
            bounds.charge()
            textAt(descendant)?.let(joined::add)
        }
        return joined.text()
    }

    private fun textAt(node: Int): String? = model.value(node).takeIf { model.kind(node) == NodeKind.Text }

    private fun idIndex(): OrderedMap<String, Int> {
        val index = OrderedMap<String, Int>()
        for (node in 0..<model.size) {
            bounds.charge()
            val isId = model.kind(node) == NodeKind.Attribute && model.name(node) == idName
            if (!isId) continue
            val id = normalizedSpaceOf(model.value(node).orEmpty())
            if (id !in index) index[id] = model.parent(node)
        }
        return index
    }

    private class TextJoin(private val bounds: EvaluationBounds) {
        private var first: String? = null
        private var joined: StringBuilder? = null

        fun add(text: String) {
            val builder = joined ?: first?.let(::StringBuilder)
            if (builder == null) {
                first = text
                return
            }
            bounds.length(builder.length + text.length)
            bounds.copied(text.length)
            joined = builder.append(text)
        }

        fun text(): String = joined?.toString() ?: first.orEmpty()
    }
}
