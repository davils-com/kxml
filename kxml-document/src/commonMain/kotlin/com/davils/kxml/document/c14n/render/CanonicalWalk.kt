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
package com.davils.kxml.document.c14n.render

import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.navigation.NodeKind

private const val NEWLINE: String = "\n"
private const val XMLNS: String = "xmlns"

internal class CanonicalWalk(
    private val model: DocumentModel,
    private val membership: Membership,
    private val namespaces: NamespacePolicy,
    private val attributes: AttributeScope,
    private val output: CanonicalOutput,
    private val hasComments: Boolean,
) {
    private val documentElement: Int = model.children(DocumentModel.ROOT).first(model::isElement)

    private val handlers: Map<NodeKind, (Int) -> Unit> = mapOf(
        NodeKind.Element to ::element,
        NodeKind.Attribute to { _ -> },
        NodeKind.Text to ::text,
        NodeKind.Comment to ::comment,
        NodeKind.ProcessingInstruction to ::instruction,
    )

    private val open: ArrayDeque<Int> = ArrayDeque()

    fun write() {
        for (node in 1..<model.size) {
            closeUntil(node)
            handlers.getValue(model.kind(node))(node)
        }
        closeUntil(model.size)
    }

    private fun closeUntil(node: Int) {
        while (open.isNotEmpty() && model.end(open.last()) <= node) close(open.removeLast())
    }

    private fun element(element: Int) {
        val isIncluded = membership.isIncluded(element)
        val rendered = attributes.open(element, isIncluded)
        val declarations = namespaces.open(element, isIncluded, rendered)
        if (isIncluded) output.raw("<" + qualifiedName(element))
        declarations.forEach { declaration ->
            val name = XMLNS + ":${declaration.prefix}".takeIf { declaration.prefix.isNotEmpty() }.orEmpty()
            output.attribute(name = name, value = declaration.uri)
        }
        rendered.forEach { attribute -> output.attribute(name = attribute.name.qualified, value = attribute.value) }
        if (isIncluded) output.raw(">")
        open.addLast(element)
    }

    private fun close(element: Int) {
        namespaces.close()
        attributes.close()
        if (membership.isIncluded(element)) output.raw("</" + qualifiedName(element) + ">")
    }

    private fun text(node: Int) {
        if (membership.isIncluded(node)) output.text(model.value(node).orEmpty())
    }

    private fun comment(node: Int) {
        if (hasComments) outside(node, "<!--" + model.value(node).orEmpty() + "-->")
    }

    private fun instruction(node: Int) {
        val data = model.value(node).orEmpty()
        val target = model.name(node)?.local.orEmpty()
        val body = " $data".takeIf { data.isNotEmpty() }.orEmpty()
        outside(node, "<?$target$body?>")
    }

    private fun outside(node: Int, text: String) {
        if (!membership.isIncluded(node)) return
        val isTop = model.parent(node) == DocumentModel.ROOT
        val isBefore = isTop && node < documentElement
        val isAfter = isTop && node > documentElement
        if (isAfter) output.raw(NEWLINE)
        output.raw(text)
        if (isBefore) output.raw(NEWLINE)
    }

    private fun qualifiedName(element: Int): String = model.name(element)?.qualified.orEmpty()
}
