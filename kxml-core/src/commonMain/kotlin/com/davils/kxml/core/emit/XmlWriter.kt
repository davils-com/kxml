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

package com.davils.kxml.core.emit

import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.node.XmlDeclaration
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode

private const val INDENT: String = " "
private const val VERSION_10: String = "1.0"
private const val VERSION_11: String = "1.1"
private const val UNREPRESENTABLE: String = "holds characters that reading would change or that only references carry"

private val standaloneValues: Map<Boolean, String> = mapOf(true to "yes", false to "no")

internal class XmlWriter(private val options: XmlEmitOptions, private val limits: XmlLimits) {
    private val isIndented: Boolean = options.indent > 0

    private val lineBreak: String = options.lineBreak.value

    fun write(document: XmlDocument): String {
        val output = WriterOutput(limits.maximumOutputLength)
        val isVersion11 = document.declaration?.version == VERSION_11
        val version = VERSION_11.takeIf { isVersion11 } ?: VERSION_10
        document.declaration?.let { declaration ->
            output.append(declarationOf(declaration))
            separate(output)
        }
        document.prolog.forEach { node ->
            tree(output, node, version)
            separate(output)
        }
        tree(output, document.root, version)
        document.epilog.forEach { node ->
            separate(output)
            tree(output, node, version)
        }
        separate(output)
        return output.toString()
    }

    private fun tree(output: WriterOutput, root: XmlNode, version: String) {
        val steps = ArrayDeque<WriteStep>()
        val scope = WriterScope(isUndeclaringAllowed = version == VERSION_11)
        steps.addLast(WriteStep.Node(node = root, depth = 0))
        while (steps.isNotEmpty()) perform(Writing(output, scope, version), steps.removeLast(), steps)
    }

    private fun perform(writing: Writing, step: WriteStep, steps: ArrayDeque<WriteStep>) {
        when (step) {
            is WriteStep.Node -> node(writing, step, steps)
            is WriteStep.Close -> close(writing, step.name)
            is WriteStep.Break -> newline(writing.output, step.depth)
        }
    }

    private fun node(writing: Writing, step: WriteStep.Node, steps: ArrayDeque<WriteStep>) {
        val output = writing.output
        when (val node = step.node) {
            is XmlNode.Element -> element(writing = writing, element = node, step = step, steps = steps)
            is XmlNode.Text -> output.append(XmlEscaping.text(value = node.value, version = writing.version))
            is XmlNode.CData -> output.append(XmlEscaping.cdata(value = node.value, version = writing.version))
            is XmlNode.Comment -> output.append("<!--${literal(value = node.value, version = writing.version)}-->")
            is XmlNode.ProcessingInstruction -> output.append(instructionOf(node, writing.version))
        }
    }

    private fun element(
        writing: Writing,
        element: XmlNode.Element,
        step: WriteStep.Node,
        steps: ArrayDeque<WriteStep>,
    ) {
        require(step.depth < XmlLimits.HARD_DEPTH) { "The element nests deeper than ${XmlLimits.HARD_DEPTH}" }
        val output = writing.output
        val opened = writing.scope.open(element)
        val name = element.name.qualified
        output.append("<$name")
        opened.declarations.forEach { binding ->
            val uri = XmlEscaping.attribute(value = binding.uri, version = writing.version)
            output.append(" ${declarationNameOf(binding)}=\"$uri\"")
        }
        element.attributes.forEach { attribute ->
            val value = XmlEscaping.attribute(value = attribute.value, version = writing.version)
            output.append(" ${opened.attributeName(attribute.name)}=\"$value\"")
        }
        if (element.children.isEmpty() && options.isEmptyElementCollapsed) {
            output.append("/>")
            writing.scope.close()
            return
        }
        output.append(">")
        val planned = plannedChildren(element, step.depth) + WriteStep.Close(name)
        planned.asReversed().forEach(steps::addLast)
    }

    private fun close(writing: Writing, name: String) {
        writing.output.append("</$name>")
        writing.scope.close()
    }

    private fun plannedChildren(element: XmlNode.Element, depth: Int): List<WriteStep> {
        val isBlock = isIndented && element.children.none { it is XmlNode.Text || it is XmlNode.CData }
        val planned = mutableListOf<WriteStep>()
        element.children.forEach { child ->
            if (isBlock) planned += WriteStep.Break(depth + 1)
            planned += WriteStep.Node(node = child, depth = depth + 1)
        }
        if (isBlock && element.children.isNotEmpty()) planned += WriteStep.Break(depth)
        return planned
    }

    private fun separate(output: WriterOutput) {
        if (isIndented) output.append(lineBreak)
    }

    private fun newline(output: WriterOutput, depth: Int) {
        output.append(lineBreak)
        output.append(INDENT.repeat(options.indent * depth))
    }

    private fun literal(value: String, version: String): String {
        require(XmlEscaping.isLiteral(value, version)) { "The content $UNREPRESENTABLE" }
        return value
    }

    private fun declarationOf(declaration: XmlDeclaration): String {
        val encoding = declaration.encoding?.let { " encoding=\"$it\"" }.orEmpty()
        val standalone = declaration.isStandalone?.let(::standaloneOf).orEmpty()
        return "<?xml version=\"${declaration.version}\"$encoding$standalone?>"
    }

    private fun instructionOf(node: XmlNode.ProcessingInstruction, version: String): String {
        val data = literal(value = node.data, version = version).takeIf { it.isNotEmpty() }
        val separated = data?.let { " $it" }
        return "<?${node.target}${separated.orEmpty()}?>"
    }

    private fun standaloneOf(isStandalone: Boolean): String {
        val value = standaloneValues.getValue(isStandalone)
        return " standalone=\"$value\""
    }

    private fun declarationNameOf(binding: XmlNamespaceBinding): String {
        val prefix = binding.prefix ?: return XmlNamespaces.XMLNS_PREFIX
        return "${XmlNamespaces.XMLNS_PREFIX}:$prefix"
    }

    private data class Writing(val output: WriterOutput, val scope: WriterScope, val version: String)
}
