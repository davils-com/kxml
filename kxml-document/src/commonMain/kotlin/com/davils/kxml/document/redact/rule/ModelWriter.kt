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
package com.davils.kxml.document.redact.rule

import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.node.xmlComment
import com.davils.kxml.core.node.xmlElement
import com.davils.kxml.core.node.xmlInstruction
import com.davils.kxml.core.node.xmlText
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.navigation.NodeKind

internal class ModelWriter(
    private val model: DocumentModel,
    private val plan: RedactionPlan,
    private val replacement: String,
) {
    private val writers: Map<NodeKind, (Int) -> XmlNode?> = mapOf(
        NodeKind.Element to ::elementOf,
        NodeKind.Text to ::textOf,
        NodeKind.Comment to { node -> xmlComment(valueOf(node)) },
        NodeKind.ProcessingInstruction to { node ->
            xmlInstruction(
                target = nameOf(node).local,
                data = valueOf(node),
            )
        },
    )

    fun documentOf(original: XmlDocument): XmlDocument {
        val top = model.children(DocumentModel.ROOT).toList()
        val rootAt = top.indexOfFirst(model::isElement)
        return XmlDocument(
            root = elementOf(top[rootAt]),
            declaration = original.declaration,
            prolog = top.subList(fromIndex = 0, toIndex = rootAt).mapNotNull(::nodeOf),
            epilog = top.subList(fromIndex = rootAt + 1, toIndex = top.size).mapNotNull(::nodeOf),
            doctype = original.doctype,
        )
    }

    private fun nodeOf(node: Int): XmlNode? = writers.getValue(model.kind(node))(node)

    private fun elementOf(node: Int): XmlNode.Element = xmlElement(nameOf(node)) {
        model.declaredNamespaces(node).forEach { binding -> namespace(prefix = binding.prefix, uri = binding.uri) }
        model.attributes(node).forEach { attribute -> attribute(model.attributeName(attribute), valueOf(attribute)) }
        if (plan.isWhole(node) && replacement.isNotEmpty()) text(replacement)
        if (!plan.isWhole(node)) model.children(node).forEach { child -> nodeOf(child)?.let(::node) }
    }

    private fun valueOf(node: Int): String {
        if (plan.isRedacted(node)) return replacement
        return plan.values[node] ?: model.value(node).orEmpty()
    }

    private fun textOf(node: Int): XmlNode? {
        val value = valueOf(node)
        return value.takeIf(String::isNotEmpty)?.let(::xmlText)
    }

    private fun nameOf(node: Int): XmlName = checkNotNull(model.name(node))
}
