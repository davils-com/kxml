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

package com.davils.kxml.core.compose

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.XmlEvent
import com.davils.kxml.core.XmlWhitespace
import com.davils.kxml.core.node.XmlDeclaration
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode

internal class TreeComposer(private val whitespace: XmlWhitespace) {
    private val stack = ArrayDeque<OpenNode>()
    private val prolog = mutableListOf<XmlNode>()
    private val epilog = mutableListOf<XmlNode>()
    private var root: XmlNode.Element? = null
    private var declaration: XmlDeclaration? = null

    fun compose(events: List<XmlEvent>): XmlDocument {
        events.forEach(::accept)
        val composed = checkNotNull(root) { "A valid event stream has a root element" }
        return XmlDocument(
            root = composed,
            declaration = declaration,
            prolog = prolog.toList(),
            epilog = epilog.toList(),
        )
    }

    private fun accept(event: XmlEvent) {
        when (event) {
            is XmlEvent.Declaration -> declaration = event.declaration
            is XmlEvent.StartElement -> stack.addLast(OpenNode(event))
            is XmlEvent.EndElement -> close(stack.removeLast(), event)
            is XmlEvent.Text -> add(XmlNode.Text(event.value, event.range))
            is XmlEvent.CData -> add(XmlNode.CData(event.value, event.range))
            is XmlEvent.Comment -> add(XmlNode.Comment(event.value, event.range))
            is XmlEvent.ProcessingInstruction -> add(instructionOf(event))
        }
    }

    private fun instructionOf(event: XmlEvent.ProcessingInstruction): XmlNode =
        XmlNode.ProcessingInstruction(target = event.target, data = event.data, range = event.range)

    private fun close(open: OpenNode, end: XmlEvent.EndElement) {
        val start = open.start
        val element = XmlNode.Element(
            name = start.name,
            attributes = start.attributes,
            children = childrenOf(open.children),
            namespaces = start.namespaces,
            range = TextRange(start = start.range.start, end = end.range.end),
        )
        add(element)
    }

    private fun add(node: XmlNode) {
        val parent = stack.lastOrNull()
        if (parent != null) {
            parent.children += node
            return
        }
        if (node is XmlNode.Element) {
            root = node
            return
        }
        val target = epilog.takeIf { root != null } ?: prolog
        target += node
    }

    private fun childrenOf(children: List<XmlNode>): List<XmlNode> {
        val isDropped = whitespace == XmlWhitespace.DropIgnorable && children.any { it is XmlNode.Element }
        if (!isDropped) return children.toList()
        return children.filterNot { child -> child is XmlNode.Text && child.isWhitespace }
    }
}
