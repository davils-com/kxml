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
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.node.XmlAttribute
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.navigation.NodeKind
import com.davils.kxml.document.patch.XmlPatchErrorCode
import com.davils.kxml.document.patch.XmlPatchOperation
import com.davils.kxml.document.patch.XmlPatchPosition

private const val COLON: Char = ':'
private const val NOT_ELEMENT: String = "the selector must locate an element"
private const val NOT_SIBLING: String = "the selector must locate an element, a text, a comment or an instruction"

private val siblings: Set<NodeKind> =
    setOf(NodeKind.Element, NodeKind.Text, NodeKind.Comment, NodeKind.ProcessingInstruction)

internal class Additions(private val context: OperationContext) {
    private val model: DocumentModel = context.model

    fun added(target: Target, operation: XmlPatchOperation.Add): XmlDocument {
        val position = operation.position
        val isInside = position == XmlPatchPosition.Append || position == XmlPatchPosition.Prepend
        if (isInside) return addedInside(elementOf(target), operation)
        val node = siblingOf(target)
        val parent = model.parent(node)
        val offset = model.spanOf(node).takeIf { position == XmlPatchPosition.After } ?: 0
        val at = model.indexOf(node) + offset
        val content = mappedInto(parent, operation)
        return context.editor.children(parent) { children -> children.replaced(at..<at, content) }
    }

    fun attributeAdded(target: Target, operation: XmlPatchOperation.AddAttribute): XmlDocument {
        val element = elementOf(target)
        val written = qualifiedName(operation.name, operation.namespaces)
        val namespaces = ContentNamespaces(context.scopeOf(element), model.name(element), context.charge)
        val (name, declaration) = namespaces.attributeName(written)
        return context.editor.element(element) { found ->
            val attribute = attributeOf(found, name, operation.value)
            val namespaces = found.namespaces + listOfNotNull(declaration)
            found.rebuilt(attributes = found.attributes + attribute, namespaces = namespaces)
        }
    }

    fun namespaceAdded(target: Target, operation: XmlPatchOperation.AddNamespace): XmlDocument {
        val element = elementOf(target)
        if (context.isDeclaredOn(element, operation.prefix)) {
            throw PatchFailure(XmlPatchErrorCode.InvalidNamespacePrefix, "the element declares the prefix already")
        }
        if (operation.uri.isEmpty()) throw PatchFailure(XmlPatchErrorCode.InvalidNamespaceUri, "the URI is empty")
        val binding = XmlNamespaceBinding(prefix = operation.prefix, uri = operation.uri)
        return context.editor.element(element) { found -> found.rebuilt(namespaces = found.namespaces + binding) }
    }

    private fun attributeOf(element: XmlNode.Element, name: XmlName, value: String): XmlAttribute {
        val isTaken = element.attributes.any { attribute -> attribute.name == name }
        if (isTaken) throw PatchFailure(XmlPatchErrorCode.InvalidAttributeValue, "the attribute exists already")
        if (!isXmlText(value)) {
            throw PatchFailure(XmlPatchErrorCode.InvalidAttributeValue, "the value holds characters XML cannot carry")
        }
        return XmlAttribute(name, value)
    }

    private fun addedInside(element: Int, operation: XmlPatchOperation.Add): XmlDocument {
        val content = mappedInto(element, operation)
        val isFirst = operation.position == XmlPatchPosition.Prepend
        return context.editor.children(element) { children ->
            val at = 0.takeIf { isFirst } ?: children.size
            children.replaced(at..<at, content)
        }
    }

    private fun mappedInto(parent: Int, operation: XmlPatchOperation.Add): List<XmlNode> {
        val namespaces = ContentNamespaces(context.scopeOf(parent), model.name(parent), context.charge)
        return namespaces.mapped(operation.content)
    }

    private fun elementOf(target: Target): Int {
        val node = (target as? Target.Node)?.ordinal
        return node?.takeIf(model::isElement) ?: throw PatchFailure(XmlPatchErrorCode.InvalidNodeTypes, NOT_ELEMENT)
    }

    private fun siblingOf(target: Target): Int {
        val node = (target as? Target.Node)?.ordinal
        return node?.takeIf { found -> model.kind(found) in siblings }
            ?: throw PatchFailure(XmlPatchErrorCode.InvalidNodeTypes, NOT_SIBLING)
    }

    private fun qualifiedName(name: String, namespaces: Map<String, String>): XmlName {
        val prefix = name.substringBefore(COLON, missingDelimiterValue = "")
        val isDeclaration = name == XmlNamespaces.XMLNS_PREFIX
        if (isDeclaration) throw PatchFailure(XmlPatchErrorCode.InvalidAttributeValue, "xmlns declares a namespace")
        if (prefix.isEmpty()) return XmlName(name)
        val uri = XmlNamespaces.XML.takeIf { prefix == XmlNamespaces.XML_PREFIX } ?: namespaces[prefix]
            ?: throw PatchFailure(XmlPatchErrorCode.InvalidNamespacePrefix, "the prefix is not declared")
        return XmlName(local = name.substringAfter(COLON), namespace = uri, prefix = prefix)
    }
}
