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

package com.davils.kxml.core.syntax.format

import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.emit.XmlEmitOptions
import com.davils.kxml.core.syntax.XmlSyntaxElement
import com.davils.kxml.core.syntax.XmlSyntaxKind
import com.davils.kxml.core.syntax.XmlSyntaxNode
import com.davils.kxml.core.syntax.XmlSyntaxTree
import com.davils.kxml.core.syntax.ast.XmlElementSyntax

private const val SPACE_ATTRIBUTE: String = "xml:space"
private const val PRESERVE: String = "preserve"
private const val DEFAULT: String = "default"
private const val WHITESPACE_CHARACTERS: String = " \t\r\n"

private val documentTokens: Set<XmlSyntaxKind> = setOf(XmlSyntaxKind.WHITESPACE, XmlSyntaxKind.BYTE_ORDER_MARK)
private val textKinds: Set<XmlSyntaxKind> = setOf(XmlSyntaxKind.TEXT, XmlSyntaxKind.WHITESPACE)
private val tagKinds: Set<XmlSyntaxKind> = setOf(XmlSyntaxKind.START_TAG, XmlSyntaxKind.END_TAG)
private val structureKinds: Set<XmlSyntaxKind> =
    setOf(XmlSyntaxKind.ELEMENT, XmlSyntaxKind.COMMENT, XmlSyntaxKind.PROCESSING_INSTRUCTION)

internal class Layout(private val tree: XmlSyntaxTree, private val options: XmlEmitOptions) {
    private val edits = mutableListOf<TextEdit>()
    private val isIndented = options.indent > 0
    private val separator = options.lineBreak.value
    private val lineBreak = separator.takeIf { isIndented }.orEmpty()

    fun edits(): List<TextEdit> {
        document(tree.root)
        val pending = ArrayDeque<Level>()
        val roots = tree.root.childNodes.filter { child -> child.kind == XmlSyntaxKind.ELEMENT }
        roots.forEach { root -> pending.addLast(Level(root, depth = 0, isPreserved = false)) }
        while (pending.isNotEmpty()) {
            val level = pending.removeLast()
            val isPreserved = isPreserved(level)
            element(level.node, level.depth, isPreserved)
            val children = level.node.childNodes.filter { child -> child.kind == XmlSyntaxKind.ELEMENT }
            children.forEach { child -> pending.addLast(Level(child, level.depth + 1, isPreserved)) }
        }
        return edits.sortedBy { edit -> edit.range.start }
    }

    private fun document(document: XmlSyntaxNode) {
        val leading = document.children.first()
        val nodes = document.children.filter { child -> child.kind !in documentTokens }
        val start = leading.range.end.takeIf { leading.kind == XmlSyntaxKind.BYTE_ORDER_MARK } ?: 0
        replace(TextRange(start = start, end = nodes.first().range.start), "")
        nodes.zipWithNext { before, after -> gap(before = before, after = after, separator = lineBreak) }
        replace(TextRange(start = nodes.last().range.end, end = document.range.end), lineBreak)
    }

    private fun element(node: XmlSyntaxNode, depth: Int, isPreserved: Boolean) {
        val content = node.children.filter { child -> child.kind !in tagKinds }
        val isElementOnly = content.any { part -> part.kind == XmlSyntaxKind.ELEMENT } && content.all(::isStructure)
        if (isPreserved || !isElementOnly) return
        val items = content.filter { part -> part.kind in structureKinds }
        val inner = lineBreak + indentOf(depth + 1)
        val startTag = node.children.first()
        val endTag = node.children.last()
        gap(before = startTag, after = items.first(), separator = inner)
        items.zipWithNext { before, after -> gap(before = before, after = after, separator = inner) }
        gap(before = items.last(), after = endTag, separator = lineBreak + indentOf(depth))
    }

    private fun isPreserved(level: Level): Boolean {
        val space = XmlElementSyntax(level.node).attribute(SPACE_ATTRIBUTE)?.value
        if (space == PRESERVE) return true
        return level.isPreserved && space != DEFAULT
    }

    private fun isStructure(part: XmlSyntaxElement): Boolean {
        val isWhitespace = part.kind in textKinds && part.text.all { character -> character in WHITESPACE_CHARACTERS }
        return part.kind in structureKinds || isWhitespace
    }

    private fun gap(before: XmlSyntaxElement, after: XmlSyntaxElement, separator: String) {
        replace(TextRange(start = before.range.end, end = after.range.start), separator)
    }

    private fun replace(range: TextRange, replacement: String) {
        val current = tree.text.substring(startIndex = range.start, endIndex = range.end)
        if (current != replacement) edits += TextEdit(range, replacement)
    }

    private fun indentOf(depth: Int): String {
        val spaces = " ".repeat(options.indent * depth)
        return spaces.takeIf { isIndented }.orEmpty()
    }

    private data class Level(val node: XmlSyntaxNode, val depth: Int, val isPreserved: Boolean)
}
