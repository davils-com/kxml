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

import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.patch.XmlPatchErrorCode
import com.davils.kxml.document.xpath.evaluate.EvaluationBounds

private const val ONE_ROOT: String = "a document has exactly one document element"
private const val NO_TEXT: String = "only comments and processing instructions stand outside the document element"

internal class TreeEditor(
    private val document: XmlDocument,
    private val model: DocumentModel,
    private val bounds: EvaluationBounds,
) {
    fun children(parent: Int, change: (List<XmlNode>) -> List<XmlNode>): XmlDocument {
        if (parent == DocumentModel.ROOT) return topLevel(change)
        return element(parent) { element -> element.rebuilt(children = change(element.children)) }
    }

    fun element(target: Int, change: (XmlNode.Element) -> XmlNode.Element): XmlDocument {
        val path = pathOf(target)
        val chain = mutableListOf(document.root)
        path.forEach { index -> chain += chain.last().children[index] as XmlNode.Element }
        var rebuilt = change(chain.last())
        for (level in path.indices.reversed()) {
            val parent = chain[level]
            bounds.charge(parent.children.size.toLong())
            val index = path[level]
            rebuilt = parent.rebuilt(children = parent.children.replaced(index..index, listOf(rebuilt)))
        }
        return XmlDocument(
            root = rebuilt,
            declaration = document.declaration,
            prolog = document.prolog,
            epilog = document.epilog,
            doctype = document.doctype,
        )
    }

    private fun pathOf(target: Int): List<Int> {
        val path = ArrayDeque<Int>()
        var node = target
        while (model.parent(node) != DocumentModel.ROOT) {
            path.addFirst(model.indexOf(node))
            node = model.parent(node)
        }
        bounds.charge(path.size.toLong())
        return path
    }

    private fun topLevel(change: (List<XmlNode>) -> List<XmlNode>): XmlDocument {
        val changed = change(document.prolog + document.root + document.epilog)
        val elements = changed.count { node -> node is XmlNode.Element }
        if (elements != 1) throw PatchFailure(XmlPatchErrorCode.InvalidRootElementOperation, ONE_ROOT)
        if (!changed.all(::isTopLevel)) throw PatchFailure(XmlPatchErrorCode.InvalidXmlPrologOperation, NO_TEXT)
        val rootAt = changed.indexOfFirst { node -> node is XmlNode.Element }
        return XmlDocument(
            root = changed[rootAt] as XmlNode.Element,
            declaration = document.declaration,
            prolog = changed.subList(fromIndex = 0, toIndex = rootAt),
            epilog = changed.subList(fromIndex = rootAt + 1, toIndex = changed.size),
            doctype = document.doctype,
        )
    }

    private fun isTopLevel(node: XmlNode): Boolean =
        node is XmlNode.Comment || node is XmlNode.ProcessingInstruction || node is XmlNode.Element
}
