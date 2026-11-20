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

import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.navigation.NodeKind
import com.davils.kxml.document.patch.XmlPatchErrorCode
import com.davils.kxml.document.patch.XmlPatchOperation
import com.davils.kxml.document.patch.XmlPatchWhitespace

private const val NO_WHITESPACE: String = "white space is removed only around elements, comments and instructions"
private const val NO_SPACE_THERE: String = "no white space text node stands there"

private val before: Set<XmlPatchWhitespace> = setOf(XmlPatchWhitespace.Before, XmlPatchWhitespace.Both)
private val after: Set<XmlPatchWhitespace> = setOf(XmlPatchWhitespace.After, XmlPatchWhitespace.Both)

internal class Removals(private val context: OperationContext) {
    private val model: DocumentModel = context.model

    fun removed(target: Target, operation: XmlPatchOperation.Remove): XmlDocument {
        val node = (target as? Target.Node)?.ordinal ?: return namespaceRemoved(target as Target.Namespace, operation)
        return when (model.kind(node)) {
            NodeKind.Root -> {
                throw PatchFailure(XmlPatchErrorCode.InvalidNodeTypes, "the root node cannot be removed")
            }

            NodeKind.Attribute -> {
                attributeRemoved(node, operation)
            }

            NodeKind.Text -> {
                textRemoved(node, operation)
            }

            NodeKind.Element, NodeKind.Comment, NodeKind.ProcessingInstruction -> {
                nodeRemoved(node, operation.whitespace)
            }
        }
    }

    private fun nodeRemoved(node: Int, whitespace: XmlPatchWhitespace): XmlDocument {
        val parent = model.parent(node)
        val isDocumentElement = parent == DocumentModel.ROOT && model.isElement(node)
        if (isDocumentElement) {
            throw PatchFailure(XmlPatchErrorCode.InvalidRootElementOperation, "the document element cannot be removed")
        }
        val first = model.previousSibling(node).whitespaceIf(whitespace in before) ?: node
        val last = model.nextSibling(node).whitespaceIf(whitespace in after) ?: node
        val until = model.indexOf(last) + model.spanOf(last)
        val removed = model.indexOf(first)..<until
        return context.editor.children(parent) { children -> children.replaced(removed, emptyList()) }
    }

    private fun attributeRemoved(attribute: Int, operation: XmlPatchOperation.Remove): XmlDocument {
        refuseWhitespace(operation)
        val index = model.indexOf(attribute)
        return context.editor.element(model.parent(attribute)) { element ->
            element.rebuilt(attributes = element.attributes.replaced(index..index, emptyList()))
        }
    }

    private fun textRemoved(text: Int, operation: XmlPatchOperation.Remove): XmlDocument {
        refuseWhitespace(operation)
        val at = model.indexOf(text)
        return context.editor.children(model.parent(text)) { children ->
            children.replaced(at..<at + model.spanOf(text), emptyList())
        }
    }

    private fun namespaceRemoved(target: Target.Namespace, operation: XmlPatchOperation.Remove): XmlDocument {
        refuseWhitespace(operation)
        if (!context.isDeclaredOn(target.element, target.prefix)) {
            throw PatchFailure(XmlPatchErrorCode.InvalidNamespacePrefix, "the element does not declare the prefix")
        }
        val use = NamespaceUse(target.prefix, context.charge)
        return context.editor.element(target.element) { element ->
            if (use.isUsed(element)) {
                throw PatchFailure(XmlPatchErrorCode.InvalidNamespacePrefix, "the prefix is still in use")
            }
            element.rebuilt(namespaces = element.namespaces.filterNot { binding -> prefixOf(binding) == target.prefix })
        }
    }

    private fun Int?.whitespaceIf(isRequired: Boolean): Int? {
        if (!isRequired) return null
        val sibling = this?.takeIf { found -> model.kind(found) == NodeKind.Text }
        val value = sibling?.let(model::value).orEmpty()
        return sibling?.takeIf { value.all(XmlCharacters::isWhitespace) }
            ?: throw PatchFailure(XmlPatchErrorCode.InvalidWhitespaceDirective, NO_SPACE_THERE)
    }

    private fun refuseWhitespace(operation: XmlPatchOperation.Remove) {
        if (operation.whitespace != XmlPatchWhitespace.None) {
            throw PatchFailure(XmlPatchErrorCode.InvalidAttributeValue, NO_WHITESPACE)
        }
    }
}
