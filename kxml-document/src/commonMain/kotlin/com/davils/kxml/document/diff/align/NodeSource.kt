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

package com.davils.kxml.document.diff.align

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.collection.OrderedSet
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.node.xmlComment
import com.davils.kxml.core.node.xmlInstruction
import com.davils.kxml.core.node.xmlText
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.navigation.NodeKind
import com.davils.kxml.document.patch.apply.prefixOf
import com.davils.kxml.document.patch.apply.rebuilt

internal class NodeSource(
    private val document: XmlDocument,
    private val model: DocumentModel,
    private val budget: WorkBudget,
) {
    private val inheritedByParent: MutableMap<Int, List<XmlNamespaceBinding>> = mutableMapOf()

    private val leaves: Map<NodeKind, (Int) -> XmlNode> = mapOf(
        NodeKind.Element to ::standalone,
        NodeKind.Comment to { ordinal -> xmlComment(valueOf(ordinal)) },
        NodeKind.ProcessingInstruction to { ordinal ->
            xmlInstruction(target = targetOf(ordinal), data = valueOf(ordinal))
        },
        NodeKind.Text to { ordinal -> xmlText(valueOf(ordinal)) },
    )

    fun nodeAt(ordinal: Int): XmlNode = leaves.getValue(model.kind(ordinal))(ordinal)

    private fun valueOf(ordinal: Int): String = model.value(ordinal).orEmpty()

    private fun targetOf(ordinal: Int): String = model.name(ordinal)?.local.orEmpty()

    private fun elementAt(ordinal: Int): XmlNode.Element {
        val path = ArrayDeque<Int>()
        var node = ordinal
        while (model.parent(node) != DocumentModel.ROOT) {
            path.addFirst(model.indexOf(node))
            node = model.parent(node)
        }
        budget.charge(path.size.toLong() + 1)
        var element = document.root
        path.forEach { index -> element = element.children[index] as XmlNode.Element }
        return element
    }

    private fun standalone(ordinal: Int): XmlNode.Element {
        val element = elementAt(ordinal)
        val declared = OrderedSet(element.namespaces.map(::prefixOf))
        val scope = inheritedByParent.getOrPut(model.parent(ordinal)) { inheritedOf(model.parent(ordinal)) }
        budget.charge(scope.size.toLong())
        val inherited = scope.filter { binding -> prefixOf(binding) !in declared }
        if (inherited.isEmpty()) return element
        return element.rebuilt(namespaces = element.namespaces + inherited)
    }

    private fun inheritedOf(parent: Int): List<XmlNamespaceBinding> {
        val found = OrderedMap<String, XmlNamespaceBinding>()
        var current = parent
        while (current > DocumentModel.ROOT) {
            val declared = model.declaredNamespaces(current)
            budget.charge(declared.size.toLong() + 1)
            declared.forEach { binding -> found.getOrPut(prefixOf(binding)) { binding } }
            current = model.parent(current)
        }
        val bindings = found.entries.map { (_, binding) -> binding }
        return bindings.filter { binding -> binding.uri.isNotEmpty() && binding.prefix != XmlNamespaces.XML_PREFIX }
    }
}
