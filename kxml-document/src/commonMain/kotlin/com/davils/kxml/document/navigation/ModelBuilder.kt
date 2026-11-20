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
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode

internal class ModelBuilder {
    private val kinds = mutableListOf<NodeKind>()
    private val parents = mutableListOf<Int>()
    private val ends = mutableListOf<Int>()
    private val attributeCounts = mutableListOf<Int>()
    private val names = mutableListOf<XmlName?>()
    private val values = mutableListOf<String?>()
    private val ranges = mutableListOf<TextRange?>()
    private val namespaces = mutableListOf<List<XmlNamespaceBinding>>()

    fun build(document: XmlDocument): DocumentModel {
        add(kind = NodeKind.Root, parent = -1, name = null, value = null, range = null)
        val top = document.prolog + document.root + document.epilog
        val pending = ArrayDeque<Step>()
        pending.addLast(Step.Close(DocumentModel.ROOT))
        top.asReversed().forEach { node -> pending.addLast(Step.Open(node, DocumentModel.ROOT)) }
        val text = TextRun()
        while (pending.isNotEmpty()) perform(pending.removeLast(), pending, text)
        return DocumentModel(
            kinds = kinds.toTypedArray(),
            parents = parents.toIntArray(),
            ends = ends.toIntArray(),
            attributeCounts = attributeCounts.toIntArray(),
            names = names.toTypedArray(),
            values = values.toTypedArray(),
            ranges = ranges.toTypedArray(),
            namespaces = namespaces.toTypedArray(),
        )
    }

    private fun perform(step: Step, pending: ArrayDeque<Step>, text: TextRun) {
        if (step is Step.Close) {
            flush(text)
            ends[step.node] = kinds.size
            return
        }
        val open = step as Step.Open
        val parent = open.parent
        when (val node = open.node) {
            is XmlNode.Text -> {
                text.append(characters = node.value, nodeRange = node.range, owner = parent)
            }

            is XmlNode.CData -> {
                text.append(characters = node.value, nodeRange = node.range, owner = parent)
            }

            is XmlNode.Element -> {
                flush(text)
                element(node, parent, pending)
            }

            is XmlNode.Comment -> {
                flush(text)
                leaf(kind = NodeKind.Comment, parent = parent, name = null, value = node.value, range = node.range)
            }

            is XmlNode.ProcessingInstruction -> {
                flush(text)
                val name = XmlName(node.target)
                val kind = NodeKind.ProcessingInstruction
                leaf(kind = kind, parent = parent, name = name, value = node.data, range = node.range)
            }
        }
    }

    private fun element(element: XmlNode.Element, parent: Int, pending: ArrayDeque<Step>) {
        val ordinal = add(
            kind = NodeKind.Element,
            parent = parent,
            name = element.name,
            value = null,
            range = element.range,
        )
        namespaces[ordinal] = element.namespaces
        attributeCounts[ordinal] = element.attributes.size
        element.attributes.forEach { attribute ->
            leaf(
                kind = NodeKind.Attribute,
                parent = ordinal,
                name = attribute.name,
                value = attribute.value,
                range = attribute.range,
            )
        }
        pending.addLast(Step.Close(ordinal))
        element.children.asReversed().forEach { child -> pending.addLast(Step.Open(child, ordinal)) }
    }

    private fun leaf(kind: NodeKind, parent: Int, name: XmlName?, value: String, range: TextRange?) {
        val ordinal = add(kind = kind, parent = parent, name = name, value = value, range = range)
        ends[ordinal] = ordinal + 1
    }

    private fun flush(text: TextRun) {
        val parent = text.parent ?: return
        leaf(kind = NodeKind.Text, parent = parent, name = null, value = text.value, range = text.range)
        text.clear()
    }

    private fun add(kind: NodeKind, parent: Int, name: XmlName?, value: String?, range: TextRange?): Int {
        val ordinal = kinds.size
        kinds += kind
        parents += parent
        ends += ordinal + 1
        attributeCounts += 0
        names += name
        values += value
        ranges += range
        namespaces.add(emptyList())
        return ordinal
    }

    private sealed interface Step {
        data class Open(val node: XmlNode, val parent: Int) : Step

        data class Close(val node: Int) : Step
    }

    private class TextRun {
        private val text = StringBuilder()
        var parent: Int? = null
            private set
        var range: TextRange? = null
            private set

        val value: String get() = text.toString()

        fun append(characters: String, nodeRange: TextRange?, owner: Int) {
            text.append(characters)
            parent = owner
            val start = range?.start
            range = nodeRange?.let { found -> TextRange(start = start ?: found.start, end = found.end) }
        }

        fun clear() {
            text.clear()
            parent = null
            range = null
        }
    }
}
