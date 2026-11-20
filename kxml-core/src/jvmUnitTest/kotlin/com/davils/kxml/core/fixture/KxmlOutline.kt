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
package com.davils.kxml.core.fixture

import com.davils.kxml.core.Xml
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode

internal class KxmlOutline private constructor(private val document: XmlDocument) {
    private val written = StringBuilder()
    private val pending = StringBuilder()

    private fun text(): String {
        document.prolog.forEach(::visited)
        visited(document.root)
        document.epilog.forEach(::visited)
        flushed()
        return written.toString()
    }

    private fun visited(node: XmlNode) {
        val element = node as? XmlNode.Element
        if (element != null) elementVisited(element)
        (node as? XmlNode.Text)?.let { text -> pending.append(text.value) }
        (node as? XmlNode.CData)?.let { section -> pending.append(section.value) }
        (node as? XmlNode.ProcessingInstruction)?.let(::instructionVisited)
    }

    private fun instructionVisited(instruction: XmlNode.ProcessingInstruction) {
        flushed()
        written.append("?${instruction.target} ${instruction.data}|")
    }

    private fun elementVisited(element: XmlNode.Element) {
        flushed()
        val shown = element.attributes.map { attribute ->
            "@{${attribute.name.namespace.orEmpty()}}${attribute.name.local}=${attribute.value}"
        }
        written.append("<{${element.name.namespace.orEmpty()}}${element.name.local}${shown.sorted()}")
        element.children.forEach(::visited)
        flushed()
        written.append(">")
    }

    private fun flushed() {
        if (pending.isNotEmpty()) written.append("'$pending'")
        pending.clear()
    }

    companion object {
        fun of(text: String): String {
            val parsed = Xml.Default.parse(text) as? XmlParseResult.Parsed ?: return REFUSED
            return KxmlOutline(parsed.document).text()
        }
    }
}
