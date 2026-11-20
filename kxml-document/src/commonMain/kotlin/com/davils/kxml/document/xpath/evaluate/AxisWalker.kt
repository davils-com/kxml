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
package com.davils.kxml.document.xpath.evaluate

import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.navigation.NodeKind
import com.davils.kxml.document.xpath.syntax.Axis

internal class AxisWalker(private val nodes: NodeAccess, private val bounds: EvaluationBounds) {
    private val model: DocumentModel = nodes.model

    private val walks: Map<Axis, (Long, (Long) -> Unit) -> Unit> = mapOf(
        Axis.Self to { key, visit -> visit(key) },
        Axis.Child to ::children,
        Axis.Descendant to ::descendants,
        Axis.DescendantOrSelf to { key, visit ->
            visit(key)
            descendants(key, visit)
        },
        Axis.Parent to { key, visit -> nodes.parentOf(key)?.let { parent -> visit(NodeKeys.keyOf(parent)) } },
        Axis.Ancestor to ::ancestors,
        Axis.AncestorOrSelf to { key, visit ->
            visit(key)
            ancestors(key, visit)
        },
        Axis.FollowingSibling to ::followingSiblings,
        Axis.PrecedingSibling to ::precedingSiblings,
        Axis.Following to ::following,
        Axis.Preceding to ::preceding,
        Axis.Attribute to ::attributes,
        Axis.Namespace to ::namespaces,
    )

    fun walk(axis: Axis, key: Long, visit: (Long) -> Unit) {
        val walk = walks.getValue(axis)
        walk(key) { found ->
            bounds.charge()
            visit(found)
        }
    }

    private fun children(key: Long, visit: (Long) -> Unit) {
        if (NodeKeys.isNamespace(key)) return
        model.children(NodeKeys.ordinalOf(key)).forEach { child -> visit(NodeKeys.keyOf(child)) }
    }

    private fun descendants(key: Long, visit: (Long) -> Unit) {
        if (NodeKeys.isNamespace(key)) return
        val ordinal = NodeKeys.ordinalOf(key)
        for (descendant in ordinal + 1..<model.end(ordinal)) {
            if (model.kind(descendant) != NodeKind.Attribute) visit(NodeKeys.keyOf(descendant))
        }
    }

    private fun ancestors(key: Long, visit: (Long) -> Unit) {
        var ancestor = nodes.parentOf(key) ?: return
        while (ancestor >= 0) {
            visit(NodeKeys.keyOf(ancestor))
            ancestor = model.parent(ancestor)
        }
    }

    private fun followingSiblings(key: Long, visit: (Long) -> Unit) {
        if (NodeKeys.isNamespace(key)) return
        var sibling = model.nextSibling(NodeKeys.ordinalOf(key))
        while (sibling != null) {
            visit(NodeKeys.keyOf(sibling))
            sibling = model.nextSibling(sibling)
        }
    }

    private fun precedingSiblings(key: Long, visit: (Long) -> Unit) {
        if (NodeKeys.isNamespace(key)) return
        var sibling = model.previousSibling(NodeKeys.ordinalOf(key))
        while (sibling != null) {
            visit(NodeKeys.keyOf(sibling))
            sibling = model.previousSibling(sibling)
        }
    }

    private fun following(key: Long, visit: (Long) -> Unit) {
        val ordinal = NodeKeys.ordinalOf(key)
        val isInside = NodeKeys.isNamespace(key) || model.kind(ordinal) == NodeKind.Attribute
        val first = (ordinal + 1).takeIf { isInside } ?: model.end(ordinal)
        for (node in first..<model.size) {
            if (model.kind(node) != NodeKind.Attribute) visit(NodeKeys.keyOf(node))
        }
    }

    private fun preceding(key: Long, visit: (Long) -> Unit) {
        val ordinal = NodeKeys.ordinalOf(key)
        val isAttribute = !NodeKeys.isNamespace(key) && model.kind(ordinal) == NodeKind.Attribute
        val reference = model.parent(ordinal).takeIf { isAttribute } ?: ordinal
        var ancestor = model.parent(reference)
        for (node in reference - 1 downTo 0) {
            val isAncestor = node == ancestor
            if (isAncestor) ancestor = model.parent(node)
            if (!isAncestor && model.kind(node) != NodeKind.Attribute) visit(NodeKeys.keyOf(node))
        }
    }

    private fun attributes(key: Long, visit: (Long) -> Unit) {
        if (NodeKeys.isNamespace(key)) return
        for (attribute in model.attributes(NodeKeys.ordinalOf(key))) visit(NodeKeys.keyOf(attribute))
    }

    private fun namespaces(key: Long, visit: (Long) -> Unit) {
        val ordinal = NodeKeys.ordinalOf(key)
        val isElement = !NodeKeys.isNamespace(key) && model.isElement(ordinal)
        if (!isElement) return
        val count = nodes.namespacesOf(ordinal).size
        for (index in 0..<count) visit(NodeKeys.namespaceKeyOf(ordinal, index))
    }
}
