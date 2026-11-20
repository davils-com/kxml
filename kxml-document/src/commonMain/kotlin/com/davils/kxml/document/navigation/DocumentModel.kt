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

package com.davils.kxml.document.navigation

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.syntax.XmlSyntaxTree

internal class DocumentModel internal constructor(
    private val kinds: Array<NodeKind>,
    private val parents: IntArray,
    private val ends: IntArray,
    private val attributeCounts: IntArray,
    private val names: Array<XmlName?>,
    private val values: Array<String?>,
    private val ranges: Array<TextRange?>,
    private val namespaces: Array<List<XmlNamespaceBinding>>,
) {
    val size: Int get() = kinds.size

    val root: Int get() = ROOT

    fun kind(node: Int): NodeKind = kinds[node]

    fun parent(node: Int): Int = parents[node]

    fun end(node: Int): Int = ends[node]

    fun name(node: Int): XmlName? = names[node]

    fun range(node: Int): TextRange? = ranges[node]

    fun declaredNamespaces(node: Int): List<XmlNamespaceBinding> = namespaces[node]

    fun isElement(node: Int): Boolean = kinds[node] == NodeKind.Element

    fun attributes(node: Int): IntRange = node + 1..node + attributeCounts[node]

    fun firstChild(node: Int): Int? = (node + 1 + attributeCounts[node]).takeIf { first -> first < ends[node] }

    fun nextSibling(node: Int): Int? {
        val parent = parents[node]
        val next = ends[node]
        val isSibling = parent >= 0 && kinds[node] != NodeKind.Attribute && next < ends[parent]
        return next.takeIf { isSibling }
    }

    fun children(node: Int): Sequence<Int> = generateSequence(firstChild(node), ::nextSibling)

    fun isDescendant(node: Int, ancestor: Int): Boolean = node > ancestor && node < ends[ancestor]

    fun stringValue(node: Int): String {
        val own = values[node]
        if (own != null) return own
        val text = StringBuilder()
        for (descendant in node + 1..<ends[node]) {
            if (kinds[descendant] == NodeKind.Text) text.append(values[descendant])
        }
        return text.toString()
    }

    companion object {
        const val ROOT: Int = 0

        fun of(document: XmlDocument): DocumentModel = ModelBuilder().build(document)

        fun of(tree: XmlSyntaxTree): DocumentModel? {
            val parsed = tree.compose() as? XmlParseResult.Parsed ?: return null
            return of(parsed.document)
        }
    }
}
