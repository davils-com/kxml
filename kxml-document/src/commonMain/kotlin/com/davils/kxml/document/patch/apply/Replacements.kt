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

import com.davils.kxml.core.node.XmlAttribute
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.navigation.NodeKind
import com.davils.kxml.document.patch.XmlPatchErrorCode
import com.davils.kxml.document.patch.XmlPatchOperation
import kotlin.reflect.KClass

private const val MISMATCH: String = "the content does not fit the located node"

private val kindsOfNodes: Map<NodeKind, KClass<out XmlNode>> = mapOf(
    NodeKind.Element to XmlNode.Element::class,
    NodeKind.Comment to XmlNode.Comment::class,
    NodeKind.ProcessingInstruction to XmlNode.ProcessingInstruction::class,
)

internal class Replacements(private val context: OperationContext) {
    private val model: DocumentModel = context.model

    fun replaced(target: Target, operation: XmlPatchOperation.Replace): XmlDocument {
        val node = (target as? Target.Node)?.ordinal ?: return namespaceReplaced(target as Target.Namespace, operation)
        return when (model.kind(node)) {
            NodeKind.Root -> throw PatchFailure(XmlPatchErrorCode.InvalidNodeTypes, "the root node cannot be replaced")
            NodeKind.Attribute -> attributeReplaced(node, textOf(operation.content) ?: mismatch())
            NodeKind.Text -> textReplaced(node, operation.content)
            NodeKind.Element, NodeKind.Comment, NodeKind.ProcessingInstruction -> nodeReplaced(node, operation)
        }
    }

    private fun nodeReplaced(node: Int, operation: XmlPatchOperation.Replace): XmlDocument {
        val kind = kindsOfNodes.getValue(model.kind(node))
        val single = operation.content.singleOrNull()
        val replacement = single?.takeIf(kind::isInstance) ?: mismatch()
        val parent = model.parent(node)
        val content = ContentNamespaces(
            context.scopeOf(parent),
            model.name(parent),
            context.charge,
        ).mapped(listOf(replacement))
        val at = model.indexOf(node)
        return context.editor.children(parent) { children -> children.replaced(at..at, content) }
    }

    private fun attributeReplaced(attribute: Int, value: String): XmlDocument {
        val index = model.indexOf(attribute)
        return context.editor.element(model.parent(attribute)) { element ->
            val replacement = XmlAttribute(element.attributes[index].name, value)
            element.rebuilt(attributes = element.attributes.replaced(index..index, listOf(replacement)))
        }
    }

    private fun textReplaced(text: Int, content: List<XmlNode>): XmlDocument {
        val isText = content.all { node -> node is XmlNode.Text || node is XmlNode.CData }
        if (!isText) mismatch()
        val at = model.indexOf(text)
        return context.editor.children(model.parent(text)) { children ->
            children.replaced(at..<at + model.spanOf(text), content)
        }
    }

    private fun namespaceReplaced(target: Target.Namespace, operation: XmlPatchOperation.Replace): XmlDocument {
        val uri = textOf(operation.content) ?: mismatch()
        if (!context.isDeclaredOn(target.element, target.prefix)) {
            throw PatchFailure(XmlPatchErrorCode.InvalidNamespacePrefix, "the element does not declare the prefix")
        }
        if (uri.isEmpty()) throw PatchFailure(XmlPatchErrorCode.InvalidNamespaceUri, "the URI is empty")
        val use = NamespaceUse(target.prefix, context.charge)
        return context.editor.element(target.element) { element -> use.rewritten(element, uri) }
    }

    private fun mismatch(): Nothing = throw PatchFailure(XmlPatchErrorCode.InvalidNodeTypes, MISMATCH)
}
