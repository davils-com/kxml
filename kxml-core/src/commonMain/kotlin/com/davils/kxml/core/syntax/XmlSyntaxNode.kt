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

package com.davils.kxml.core.syntax

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.syntax.internal.GreenNode
import com.davils.kxml.core.syntax.internal.GreenToken

/**
 * A node of an [XmlSyntaxTree]: a construct of the XML grammar, such as an element, a start tag or an attribute,
 * that groups the nodes and tokens it is made of.
 *
 * The children of a node cover its [range] without gaps, so the [text] of a node is the text of its tokens in order.
 * Nodes are immutable views of the tree and safe to share between threads; their children are created when they are
 * first asked for. Two nodes are equal when they stand for the same node at the same offset of the same parse.
 *
 * Every walk over the descendants of a node, such as [tokens], [descendants], [tokenAt] and [coveringNode], is
 * iterative, so a tree nested as deep as `XmlLimits.HARD_DEPTH` allows never exhausts the call stack.
 *
 * @since 1.0.0
 */
public class XmlSyntaxNode internal constructor(
    internal val green: GreenNode,
    private val offset: Int,
    override val parent: XmlSyntaxNode?,
) : XmlSyntaxElement {
    override val kind: XmlSyntaxKind get() = green.kind

    override val range: TextRange get() = TextRange.of(start = offset, length = green.length)

    override val text: String get() = green.text

    /**
     * The nodes and tokens of this node, in the order of the source text.
     *
     * @since 1.0.0
     */
    public val children: List<XmlSyntaxElement> by lazy { childrenOf() }

    /**
     * The nodes among the [children] of this node, in the order of the source text.
     *
     * @since 1.0.0
     */
    public val childNodes: List<XmlSyntaxNode> get() = children.filterIsInstance<XmlSyntaxNode>()

    /**
     * Every token below this node, in the order of the source text; the tokens of nested nodes included.
     *
     * The sequence is lazy and walks the tree iteratively, whatever its depth.
     *
     * @since 1.0.0
     */
    public val tokens: Sequence<XmlSyntaxToken>
        get() = sequence {
            val pending = ArrayList<XmlSyntaxElement>(children.asReversed())
            while (pending.isNotEmpty()) {
                val element = pending.removeAt(pending.lastIndex)
                if (element is XmlSyntaxToken) yield(element)
                if (element is XmlSyntaxNode) pending.addAll(element.children.asReversed())
            }
        }

    /**
     * Every node below this node, each before its own descendants, in the order of the source text; this node is
     * not included.
     *
     * The sequence is lazy and walks the tree iteratively, whatever its depth.
     *
     * @since 1.0.0
     */
    public val descendants: Sequence<XmlSyntaxNode>
        get() = sequence {
            val pending = ArrayList<XmlSyntaxNode>(childNodes.asReversed())
            while (pending.isNotEmpty()) {
                val node = pending.removeAt(pending.lastIndex)
                yield(node)
                pending.addAll(node.childNodes.asReversed())
            }
        }

    /**
     * Returns the first child node of [kind].
     *
     * @param kind the kind of node to look for among the [childNodes].
     * @return the first child node of [kind], or `null` if this node has none.
     * @since 1.0.0
     */
    public fun child(kind: XmlSyntaxKind): XmlSyntaxNode? = childNodes.firstOrNull { it.kind == kind }

    /**
     * Returns the first token of [kind] among the direct [children] of this node.
     *
     * @param kind the kind of token to look for; tokens of nested nodes are not searched.
     * @return the first child token of [kind], or `null` if this node has none.
     * @since 1.0.0
     */
    public fun token(kind: XmlSyntaxKind): XmlSyntaxToken? =
        children.filterIsInstance<XmlSyntaxToken>().firstOrNull { it.kind == kind }

    /**
     * Returns the token below this node that contains the character at [offset].
     *
     * @param offset a UTF-16 offset into [XmlSyntaxTree.text]; any value is accepted.
     * @return the token whose range contains [offset], or `null` if [offset] lies outside the [range] of this node.
     * @since 1.0.0
     */
    public fun tokenAt(offset: Int): XmlSyntaxToken? {
        val path = generateSequence<XmlSyntaxElement>(
            this,
        ) { element -> (element as? XmlSyntaxNode)?.childAt(offset) }
        return path.last() as? XmlSyntaxToken
    }

    /**
     * Returns the innermost node at or below this node whose range contains all of [range].
     *
     * @param range the range to cover; a range that this node does not cover returns this node.
     * @return the deepest node that covers [range], this node if no child covers it.
     * @since 1.0.0
     */
    public fun coveringNode(range: TextRange): XmlSyntaxNode =
        generateSequence(this) { node -> node.childCovering(range) }.last()

    override fun equals(other: Any?): Boolean =
        other is XmlSyntaxNode && green === other.green && offset == other.offset

    override fun hashCode(): Int = green.identity * HASH_FACTOR + offset

    override fun toString(): String = "$kind@$range"

    private fun childAt(offset: Int): XmlSyntaxElement? {
        val child = children.getOrNull(firstEndingAfter(offset))
        return child?.takeIf { found -> found.range.start <= offset }
    }

    private fun childCovering(range: TextRange): XmlSyntaxNode? {
        val first = firstEndingAfter(range.end - 1)
        val candidates = children.subList(fromIndex = first, toIndex = children.size)
        val starting = candidates.asSequence().takeWhile { child -> child.range.start <= range.start }
        return starting.filterIsInstance<XmlSyntaxNode>().firstOrNull()
    }

    private fun firstEndingAfter(offset: Int): Int {
        var low = 0
        var high = children.size
        while (low < high) {
            val middle = low + high ushr 1
            val isBefore = children[middle].range.end <= offset
            low = (middle + 1).takeIf { isBefore } ?: low
            high = high.takeIf { isBefore } ?: middle
        }
        return low
    }

    private fun childrenOf(): List<XmlSyntaxElement> {
        var position = offset
        return buildList {
            green.children.forEach { child ->
                add(element(child, position))
                position += child.length
            }
        }
    }

    private fun element(child: Any, start: Int): XmlSyntaxElement {
        val token = child as? GreenToken ?: return XmlSyntaxNode(child as GreenNode, start, this)
        return XmlSyntaxToken(token, start, this)
    }

    private companion object {
        const val HASH_FACTOR: Int = 31
    }
}
