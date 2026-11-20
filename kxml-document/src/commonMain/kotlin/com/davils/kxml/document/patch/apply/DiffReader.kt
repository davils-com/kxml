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

import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.navigation.PrefixScope
import com.davils.kxml.document.patch.XmlPatchErrorCode
import com.davils.kxml.document.patch.XmlPatchOperation
import com.davils.kxml.document.patch.XmlPatchPosition
import com.davils.kxml.document.patch.XmlPatchWhitespace

private const val ATTRIBUTE_TYPE: String = "@"
private const val NAMESPACE_TYPE: String = "namespace::"
private const val OPERATIONS_LIMIT: String = "maximumPatchOperations"
private const val NO_TYPE: String = "the type names no attribute and no namespace"

private val selectorName: XmlName = XmlName("sel")
private val typeName: XmlName = XmlName("type")
private val positionName: XmlName = XmlName("pos")
private val whitespaceName: XmlName = XmlName("ws")

private val positions: Map<String?, XmlPatchPosition> = mapOf(
    null to XmlPatchPosition.Append,
    "prepend" to XmlPatchPosition.Prepend,
    "before" to XmlPatchPosition.Before,
    "after" to XmlPatchPosition.After,
)

private val whitespaces: Map<String?, XmlPatchWhitespace> = mapOf(
    null to XmlPatchWhitespace.None,
    "before" to XmlPatchWhitespace.Before,
    "after" to XmlPatchWhitespace.After,
    "both" to XmlPatchWhitespace.Both,
)

internal class DiffReader(private val limits: XmlLimits) {
    private var index: Int = 0

    private val readers: Map<String, (XmlNode.Element, String, Map<String, String>) -> XmlPatchOperation> = mapOf(
        "add" to ::addOf,
        "replace" to ::replaceOf,
        "remove" to ::removeOf,
    )

    fun operations(diff: XmlDocument): List<XmlPatchOperation> {
        val scope = scopeOf(diff.root.namespaces, outer = PrefixScope.EMPTY)
        val operations = mutableListOf<XmlPatchOperation>()
        diff.root.children.forEach { child ->
            val element = child as? XmlNode.Element
            if (element == null) {
                refuseText(child)
                return@forEach
            }
            operations += operationOf(element, scopeOf(element.namespaces, scope))
            index += 1
            if (operations.size > limits.maximumPatchOperations) throw XmlDocumentLimitException(OPERATIONS_LIMIT)
        }
        return operations
    }

    fun failedAt(): Int = index

    private fun refuseText(node: XmlNode) {
        val text = (node as? XmlNode.Text)?.value ?: (node as? XmlNode.CData)?.value ?: return
        if (!text.all(XmlCharacters::isWhitespace)) {
            throw PatchFailure(XmlPatchErrorCode.InvalidDiffFormat, "text stands between the operations")
        }
    }

    private fun operationOf(element: XmlNode.Element, scope: Map<String, String>): XmlPatchOperation {
        val selector = element.attribute(selectorName)?.value
            ?: throw PatchFailure(XmlPatchErrorCode.InvalidDiffFormat, "an operation needs a sel attribute")
        val read = readers[element.name.local]
            ?: throw PatchFailure(XmlPatchErrorCode.InvalidDiffFormat, "the operation is not add, replace or remove")
        return read(element, selector, scope)
    }

    private fun replaceOf(element: XmlNode.Element, selector: String, scope: Map<String, String>): XmlPatchOperation =
        XmlPatchOperation.Replace(selector = selector, content = element.children, namespaces = scope)

    private fun addOf(element: XmlNode.Element, selector: String, scope: Map<String, String>): XmlPatchOperation {
        val type = element.attribute(typeName)?.value ?: return contentAddOf(element, selector, scope)
        if (element.attribute(positionName) != null) invalid("an attribute or a namespace takes no pos attribute")
        val value = textOf(element.children) ?: invalid("the content of an attribute or a namespace must be text")
        val typed = runCatching { typedOf(type = type, value = value, selector = selector, scope = scope) }
        return typed.getOrElse { failure ->
            throw failure as? PatchFailure ?: PatchFailure(XmlPatchErrorCode.InvalidAttributeValue, NO_TYPE)
        }
    }

    private fun contentAddOf(
        element: XmlNode.Element,
        selector: String,
        scope: Map<String, String>,
    ): XmlPatchOperation {
        val position = positions[element.attribute(positionName)?.value]
            ?: invalid("the pos attribute is not prepend, before or after")
        val content = element.children
        return XmlPatchOperation.Add(selector = selector, content = content, position = position, namespaces = scope)
    }

    private fun typedOf(type: String, value: String, selector: String, scope: Map<String, String>): XmlPatchOperation {
        if (type.startsWith(ATTRIBUTE_TYPE)) {
            val name = type.removePrefix(ATTRIBUTE_TYPE)
            return XmlPatchOperation.AddAttribute(selector = selector, name = name, value = value, namespaces = scope)
        }
        if (!type.startsWith(NAMESPACE_TYPE)) invalid("the type attribute is no attribute and no namespace")
        val prefix = type.removePrefix(NAMESPACE_TYPE)
        return XmlPatchOperation.AddNamespace(selector = selector, prefix = prefix, uri = value, namespaces = scope)
    }

    private fun removeOf(element: XmlNode.Element, selector: String, scope: Map<String, String>): XmlPatchOperation {
        val whitespace = whitespaces[element.attribute(whitespaceName)?.value]
            ?: invalid("the ws attribute is not before, after or both")
        return XmlPatchOperation.Remove(selector = selector, whitespace = whitespace, namespaces = scope)
    }

    private fun scopeOf(declared: List<XmlNamespaceBinding>, outer: PrefixScope): PrefixScope =
        outer.with(declared.map { binding -> prefixOf(binding) to binding.uri })

    private fun invalid(reason: String): Nothing = throw PatchFailure(XmlPatchErrorCode.InvalidAttributeValue, reason)
}
