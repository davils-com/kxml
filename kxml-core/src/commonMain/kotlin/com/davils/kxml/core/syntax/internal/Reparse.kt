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

package com.davils.kxml.core.syntax.internal

import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.edited
import com.davils.kxml.core.syntax.XmlSyntaxKind
import com.davils.kxml.core.syntax.XmlSyntaxTree

private const val END_TAG_OPEN: String = "</"
private const val END_TAG_CLOSE: String = ">"
private const val NAME_OF_TAG: Int = 1

internal class Reparse(private val tree: XmlSyntaxTree) {
    private val configuration = tree.xml.configuration

    fun apply(edits: List<TextEdit>): XmlSyntaxTree {
        val text = tree.text.edited(edits)
        if (edits.isEmpty()) return tree
        return spliced(edits, text) ?: XmlSyntaxTree.parse(text, tree.xml)
    }

    private fun spliced(edits: List<TextEdit>, text: String): XmlSyntaxTree? {
        val isReusable = text.length <= configuration.limits.maximumInputLength && isContextFree()
        val path = pathTo(spanOf(edits)).takeIf { isReusable && it.isNotEmpty() } ?: return null
        val target = path.last()
        val replaced = text.substring(startIndex = target.start, endIndex = target.end + edits.sumOf(TextEdit::delta))
        val root = reparsed(path, replaced)?.let { element -> rebuilt(path, element) }
        return root?.takeIf(::isWithinLimits)?.let { green -> treeOf(text, green) }
    }

    private fun isWithinLimits(root: GreenNode): Boolean = countOf(root) <= configuration.limits.maximumElements

    private fun treeOf(text: String, green: GreenNode): XmlSyntaxTree =
        XmlSyntaxTree(text = text, green = green, diagnostics = emptyList(), xml = tree.xml)

    private fun isContextFree(): Boolean {
        val document = tree.green
        val hasDoctype = document.children.any { child -> child.kind == XmlSyntaxKind.DOCTYPE }
        return tree.diagnostics.isEmpty() && !hasDoctype
    }

    private fun spanOf(edits: List<TextEdit>): Span {
        var start = Int.MAX_VALUE
        var end = 0
        edits.forEach { edit ->
            start = minOf(a = start, b = edit.range.start)
            end = maxOf(a = end, b = edit.range.end)
        }
        return Span(start = start, end = end)
    }

    private fun pathTo(span: Span): List<Step> {
        val path = mutableListOf<Step>()
        var node: GreenNode = tree.green
        var offset = 0
        var step = enclosing(node, offset, span)
        while (step != null) {
            path += step
            node = step.element
            offset = step.start
            step = enclosing(node, offset, span)
        }
        return path.toList()
    }

    private fun enclosing(parent: GreenNode, parentStart: Int, span: Span): Step? {
        var start = parentStart
        parent.children.forEachIndexed { index, child ->
            val end = start + child.length
            val isElement = child is GreenNode && child.kind == XmlSyntaxKind.ELEMENT
            val isInside = start < span.start && span.end < end
            if (isElement && isInside) return Step(parent = parent, index = index, element = child, start = start)
            start = end
        }
        return null
    }

    private fun reparsed(path: List<Step>, replaced: String): GreenNode? {
        val ancestors = path.dropLast(1).map(Step::element)
        val prefix = declarationText() + ancestors.joinToString(separator = "", transform = ::startTagOf)
        val suffix = ancestors.reversed().joinToString(separator = "", transform = ::endTagOf)
        val parsed = SyntaxParsing.parse(prefix + replaced + suffix, configuration)
        val isClean = parsed.diagnostics.isEmpty()
        val nested = generateSequence(soleElementOf(parsed.root), ::soleElementOf)
        val element = nested.elementAtOrNull(ancestors.size)
        return element.takeIf { isClean && element?.length == replaced.length }
    }

    private fun declarationText(): String {
        val children = tree.green.children
        val declarations = children.filter { child -> child.kind == XmlSyntaxKind.XML_DECLARATION }
        return declarations.joinToString(separator = "", transform = GreenElement::text)
    }

    private fun soleElementOf(node: GreenNode): GreenNode? =
        node.children.filterIsInstance<GreenNode>().singleOrNull { child -> child.kind == XmlSyntaxKind.ELEMENT }

    private fun startTagOf(element: GreenNode): String = startTag(element).text

    private fun endTagOf(element: GreenNode): String {
        val name = startTag(element).children[NAME_OF_TAG]
        return END_TAG_OPEN + name.text + END_TAG_CLOSE
    }

    private fun startTag(element: GreenNode): GreenElement = element.children.first()

    private fun rebuilt(path: List<Step>, element: GreenNode): GreenNode {
        var replacement = element
        path.asReversed().forEach { step ->
            val children = step.parent.children.toMutableList()
            children[step.index] = replacement
            replacement = GreenNode(step.parent.kind, children.toList())
        }
        return replacement
    }

    private fun countOf(root: GreenNode): Long {
        var count = 0L
        val pending = ArrayDeque<GreenElement>()
        pending.addLast(root)
        while (pending.isNotEmpty()) {
            val element = pending.removeLast()
            if (element.kind == XmlSyntaxKind.ELEMENT) count++
            if (element is GreenNode) pending.addAll(element.children)
        }
        return count
    }

    private data class Span(val start: Int, val end: Int)

    private data class Step(val parent: GreenNode, val index: Int, val element: GreenNode, val start: Int) {
        val end: Int get() = start + element.length
    }
}
