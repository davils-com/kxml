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
private const val VERSION_11: String = "1.1"

private val standaloneValues: Map<Boolean, String> = mapOf(true to "yes", false to "no")

internal class XmlWriter(private val options: XmlEmitOptions) {
    private val isIndented: Boolean = options.indent > 0

    private val lineBreak: String = options.lineBreak.value

    fun write(document: XmlDocument): String {
        val output = StringBuilder()
        val isVersion11 = document.declaration?.version == VERSION_11
        document.declaration?.let { declaration ->
            output.append(declarationOf(declaration))
            separate(output)
        }
        document.prolog.forEach { node ->
            tree(output, node, isVersion11)
            separate(output)
        }
        tree(output, document.root, isVersion11)
        document.epilog.forEach { node ->
            separate(output)
            tree(output, node, isVersion11)
        }
        separate(output)
        return output.toString()
    }

    private fun tree(output: StringBuilder, root: XmlNode, isVersion11: Boolean) {
        val steps = ArrayDeque<WriteStep>()
        val scope = WriterScope.root(isUndeclaringAllowed = isVersion11)
        steps.addLast(WriteStep.Node(node = root, depth = 0, scope = scope))
        while (steps.isNotEmpty()) perform(output, steps.removeLast(), steps)
    }

    private fun perform(output: StringBuilder, step: WriteStep, steps: ArrayDeque<WriteStep>) {
        when (step) {
            is WriteStep.Node -> node(output, step, steps)
            is WriteStep.Close -> output.append("</${step.name}>")
            is WriteStep.Break -> newline(output, step.depth)
        }
    }

    private fun node(output: StringBuilder, step: WriteStep.Node, steps: ArrayDeque<WriteStep>) {
        when (val node = step.node) {
            is XmlNode.Element -> element(output = output, element = node, step = step, steps = steps)
            is XmlNode.Text -> output.append(XmlEscaping.text(node.value))
            is XmlNode.CData -> output.append("<![CDATA[${node.value}]]>")
            is XmlNode.Comment -> output.append("<!--${node.value}-->")
            is XmlNode.ProcessingInstruction -> output.append(instructionOf(node))
        }
    }

    private fun element(
        output: StringBuilder,
        element: XmlNode.Element,
        step: WriteStep.Node,
        steps: ArrayDeque<WriteStep>,
    ) {
        require(step.depth < XmlLimits.HARD_DEPTH) { "The element nests deeper than ${XmlLimits.HARD_DEPTH}" }
        val scope = step.scope.child()
        scope.open(element)
        val name = element.name.qualified
        output.append("<$name")
        scope.declarations.forEach { binding ->
            output.append(" ${declarationNameOf(binding)}=\"${XmlEscaping.attribute(binding.uri)}\"")
        }
        element.attributes.forEach { attribute ->
            output.append(" ${scope.attributeName(attribute.name)}=\"${XmlEscaping.attribute(attribute.value)}\"")
        }
        if (element.children.isEmpty() && options.isEmptyElementCollapsed) {
            output.append("/>")
            return
        }
        output.append(">")
        val planned = plannedChildren(element, step.depth, scope) + WriteStep.Close(name)
        planned.asReversed().forEach(steps::addLast)
    }

    private fun plannedChildren(element: XmlNode.Element, depth: Int, scope: WriterScope): List<WriteStep> {
        val isBlock = isIndented && element.children.none { it is XmlNode.Text || it is XmlNode.CData }
        val planned = mutableListOf<WriteStep>()
        element.children.forEach { child ->
            if (isBlock) planned += WriteStep.Break(depth + 1)
            planned += WriteStep.Node(node = child, depth = depth + 1, scope = scope)
        }
        if (isBlock && element.children.isNotEmpty()) planned += WriteStep.Break(depth)
        return planned
    }

    private fun separate(output: StringBuilder) {
        if (isIndented) output.append(lineBreak)
    }

    private fun newline(output: StringBuilder, depth: Int) {
        output.append(lineBreak)
        output.append(INDENT.repeat(options.indent * depth))
    }

    private fun declarationOf(declaration: XmlDeclaration): String {
        val encoding = declaration.encoding?.let { " encoding=\"$it\"" }.orEmpty()
        val standalone = declaration.isStandalone?.let(::standaloneOf).orEmpty()
        return "<?xml version=\"${declaration.version}\"$encoding$standalone?>"
    }

    private fun instructionOf(node: XmlNode.ProcessingInstruction): String {
        val data = node.data.takeIf { it.isNotEmpty() }
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
}
