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
    private val indices = mutableListOf<Int>()
    private val spans = mutableListOf<Int>()

    fun build(document: XmlDocument): DocumentModel {
        add(kind = NodeKind.Root, parent = -1, name = null, value = null, range = null, index = -1)
        val top = document.prolog + document.root + document.epilog
        val pending = ArrayDeque<Step>()
        pending.addLast(Step.Close(DocumentModel.ROOT))
        for (index in top.indices.reversed()) {
            pending.addLast(Step.Open(node = top[index], parent = DocumentModel.ROOT, index = index))
        }
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
            locations = Locations(indices = indices.toIntArray(), spans = spans.toIntArray()),
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
        val index = open.index
        when (val node = open.node) {
            is XmlNode.Text -> {
                text.append(characters = node.value, nodeRange = node.range, owner = parent, index = index)
            }

            is XmlNode.CData -> {
                text.append(characters = node.value, nodeRange = node.range, owner = parent, index = index)
            }

            is XmlNode.Element -> {
                flush(text)
                element(node, Placement(parent, index), pending)
            }

            is XmlNode.Comment -> {
                flush(text)
                val placement = Placement(parent, index)
                val kind = NodeKind.Comment
                leaf(kind = kind, placement = placement, name = null, value = node.value, range = node.range)
            }

            is XmlNode.ProcessingInstruction -> {
                flush(text)
                val name = XmlName(node.target)
                val kind = NodeKind.ProcessingInstruction
                val placement = Placement(parent, index)
                leaf(kind = kind, placement = placement, name = name, value = node.data, range = node.range)
            }
        }
    }

    private fun element(element: XmlNode.Element, placement: Placement, pending: ArrayDeque<Step>) {
        val ordinal = add(
            kind = NodeKind.Element,
            parent = placement.parent,
            name = element.name,
            value = null,
            range = element.range,
            index = placement.index,
        )
        namespaces[ordinal] = element.namespaces
        attributeCounts[ordinal] = element.attributes.size
        element.attributes.forEachIndexed { index, attribute ->
            leaf(
                kind = NodeKind.Attribute,
                placement = Placement(parent = ordinal, index = index),
                name = attribute.name,
                value = attribute.value,
                range = attribute.range,
            )
        }
        pending.addLast(Step.Close(ordinal))
        val children = element.children
        for (index in children.indices.reversed()) {
            pending.addLast(Step.Open(node = children[index], parent = ordinal, index = index))
        }
    }

    private fun leaf(kind: NodeKind, placement: Placement, name: XmlName?, value: String, range: TextRange?): Int {
        val index = placement.index
        val ordinal = add(
            kind = kind,
            parent = placement.parent,
            name = name,
            value = value,
            range = range,
            index = index,
        )
        ends[ordinal] = ordinal + 1
        return ordinal
    }

    private fun flush(text: TextRun) {
        val parent = text.parent ?: return
        val placement = Placement(parent = parent, index = text.first)
        val value = text.value
        val ordinal = leaf(kind = NodeKind.Text, placement = placement, name = null, value = value, range = text.range)
        spans[ordinal] = text.count
        text.clear()
    }

    private fun add(kind: NodeKind, parent: Int, name: XmlName?, value: String?, range: TextRange?, index: Int): Int {
        val ordinal = kinds.size
        kinds += kind
        parents += parent
        ends += ordinal + 1
        attributeCounts += 0
        names += name
        values += value
        ranges += range
        namespaces.add(emptyList())
        indices += index
        spans += 1
        return ordinal
    }

    private data class Placement(val parent: Int, val index: Int)

    private sealed interface Step {
        data class Open(val node: XmlNode, val parent: Int, val index: Int) : Step

        data class Close(val node: Int) : Step
    }

    private class TextRun {
        private val text = StringBuilder()
        var parent: Int? = null
            private set
        var range: TextRange? = null
            private set
        var first: Int = 0
            private set
        var count: Int = 0
            private set

        val value: String get() = text.toString()

        fun append(characters: String, nodeRange: TextRange?, owner: Int, index: Int) {
            range = nodeRange.takeIf { count == 0 } ?: joined(current = range, next = nodeRange)
            if (count == 0) first = index
            count += 1
            text.append(characters)
            parent = owner
        }

        private fun joined(current: TextRange?, next: TextRange?): TextRange? {
            if (current == null || next == null) return null
            return next.takeIf { found -> found.start >= current.end }?.let { found ->
                TextRange(start = current.start, end = found.end)
            }
        }

        fun clear() {
            text.clear()
            parent = null
            range = null
            count = 0
        }
    }
}
