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
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlElementBuilder
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.node.xmlElement
import com.davils.kxml.document.patch.XmlPatchOperation
import com.davils.kxml.document.patch.XmlPatchPosition
import com.davils.kxml.document.patch.XmlPatchWhitespace

private val selectorName: XmlName = XmlName("sel")
private val typeName: XmlName = XmlName("type")
private val positionName: XmlName = XmlName("pos")
private val whitespaceName: XmlName = XmlName("ws")

private val positions: Map<XmlPatchPosition, String> = mapOf(
    XmlPatchPosition.Prepend to "prepend",
    XmlPatchPosition.Before to "before",
    XmlPatchPosition.After to "after",
)

private val whitespaces: Map<XmlPatchWhitespace, String> = mapOf(
    XmlPatchWhitespace.Before to "before",
    XmlPatchWhitespace.After to "after",
    XmlPatchWhitespace.Both to "both",
)

internal object PatchWriter {
    fun documentOf(operations: List<XmlPatchOperation>): XmlDocument =
        XmlDocument(xmlElement(XmlName("diff")) { operations.forEach { operation -> node(elementOf(operation)) } })

    private fun elementOf(operation: XmlPatchOperation): XmlNode.Element = when (operation) {
        is XmlPatchOperation.Add -> written("add", operation) {
            positions[operation.position]?.let { position -> attribute(positionName, position) }
            operation.content.forEach(::node)
        }

        is XmlPatchOperation.AddAttribute -> written("add", operation) {
            attribute(typeName, "@${operation.name}")
            textOf(operation.value)
        }

        is XmlPatchOperation.AddNamespace -> written("add", operation) {
            attribute(typeName, "namespace::${operation.prefix}")
            textOf(operation.uri)
        }

        is XmlPatchOperation.Replace -> written("replace", operation) { operation.content.forEach(::node) }

        is XmlPatchOperation.Remove -> written("remove", operation) {
            whitespaces[operation.whitespace]?.let { whitespace -> attribute(whitespaceName, whitespace) }
        }
    }

    private fun written(
        local: String,
        operation: XmlPatchOperation,
        content: XmlElementBuilder.() -> Unit,
    ): XmlNode.Element = xmlElement(XmlName(local)) {
        operation.namespaces.forEach { (prefix, uri) -> namespace(prefix = prefix.ifEmpty { null }, uri = uri) }
        attribute(selectorName, operation.selector)
        content()
    }

    private fun XmlElementBuilder.textOf(value: String) {
        if (value.isNotEmpty()) text(value)
    }
}
