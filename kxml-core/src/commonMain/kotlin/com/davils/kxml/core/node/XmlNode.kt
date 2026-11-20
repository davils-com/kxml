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

package com.davils.kxml.core.node

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding

private const val HASH_FACTOR: Int = 31

/**
 * A node of the immutable document tree.
 *
 * A node read from a document carries its [range]; a node built in code with [xmlElement], [xmlText] and the other
 * factory functions has none. Two nodes are equal when their kind and content are equal; ranges, prefixes and namespace
 * declarations take no part in equality, and neither does the order of attributes. A `toString` escapes and shortens
 * the text it shows.
 *
 * @property range the characters of the source text the node was read from, or `null` for a node built in code.
 * @since 1.0.0
 */
public sealed class XmlNode(public val range: TextRange?) {
    /**
     * An element with its attributes and children.
     *
     * @property name the resolved name.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    public class Element internal constructor(
        public val name: XmlName,
        attributes: List<XmlAttribute>,
        children: List<XmlNode>,
        namespaces: List<XmlNamespaceBinding>,
        range: TextRange?,
    ) : XmlNode(range) {
        /**
         * The attributes in the order they were read or added.
         *
         * @since 1.0.0
         */
        public val attributes: List<XmlAttribute> = attributes.immutableList()

        /**
         * The child nodes in document order.
         *
         * @since 1.0.0
         */
        public val children: List<XmlNode> = children.immutableList()

        /**
         * The namespace declarations of the element; the writer adds those it needs on top of them.
         *
         * @since 1.0.0
         */
        public val namespaces: List<XmlNamespaceBinding> = namespaces.immutableList()

        private val attributeSet: Set<Pair<XmlName, String>> by lazy {
            attributes.mapTo(mutableSetOf()) { attribute -> attribute.name to attribute.value }
        }

        private val hash: Int by lazy { HASH_FACTOR * name.hashCode() + attributeSet.hashCode() + children.hashCode() }

        /**
         * The child elements in document order.
         *
         * @since 1.0.0
         */
        public val elements: List<Element> get() = children.filterIsInstance<Element>()

        /**
         * The text and character data of the direct children, joined; empty if there is none.
         *
         * @since 1.0.0
         */
        public val text: String get() = children.mapNotNull(::textOf).joinToString(separator = "")

        /**
         * Returns the first child element named [name].
         *
         * @param name the name to look for; compared by local name and namespace.
         * @return the element, or `null` if there is none.
         * @since 1.0.0
         */
        public fun firstElement(name: XmlName): Element? = elements.firstOrNull { element -> element.name == name }

        /**
         * Returns every child element named [name].
         *
         * @param name the name to look for; compared by local name and namespace.
         * @return the elements in document order; empty if there are none.
         * @since 1.0.0
         */
        public fun elements(name: XmlName): List<Element> = elements.filter { element -> element.name == name }

        /**
         * Returns the attribute named [name].
         *
         * @param name the name to look for; compared by local name and namespace.
         * @return the attribute, or `null` if there is none.
         * @since 1.0.0
         */
        public fun attribute(name: XmlName): XmlAttribute? =
            attributes.firstOrNull { attribute -> attribute.name == name }

        /**
         * Compares the name, the attributes in any order and the children.
         *
         * @param other the value to compare with.
         * @return whether [other] is an equal element.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean {
            val isSame = other is Element && sameContent(other)
            return this === other || isSame
        }

        /**
         * Returns a hash code derived from the name, the attributes and the children.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = hash

        /**
         * Returns the name and the number of children, such as `Element(a, 2 children)`.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        override fun toString(): String = "Element($name, ${children.size} children)"

        private fun sameContent(other: Element): Boolean =
            name == other.name && attributeSet == other.attributeSet && children == other.children

        private fun textOf(node: XmlNode): String? = (node as? Text)?.value ?: (node as? CData)?.value
    }

    /**
     * Character data, with every reference replaced.
     *
     * @property value the text.
     * @since 1.0.0
     */
    public class Text internal constructor(
        public val value: String,
        range: TextRange?,
    ) : XmlNode(range) {
        /**
         * Whether [value] holds only spaces, tabs, line feeds and carriage returns.
         *
         * @since 1.0.0
         */
        public val isWhitespace: Boolean get() = value.all { char -> char == ' ' || char in "\t\n\r" }

        /**
         * Compares the text.
         *
         * @param other the value to compare with.
         * @return whether [other] is a text node with equal text.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean {
            val isSame = other is Text && value == other.value
            return this === other || isSame
        }

        /**
         * Returns a hash code derived from the text.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = value.hashCode()

        /**
         * Returns the escaped text, such as `Text("a")`.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        @OptIn(DavilsInternal::class)
        override fun toString(): String = "Text(${SafeText.quoted(value)})"
    }

    /**
     * A CDATA section.
     *
     * @property value the content of the section.
     * @since 1.0.0
     */
    public class CData internal constructor(
        public val value: String,
        range: TextRange?,
    ) : XmlNode(range) {
        /**
         * Compares the content.
         *
         * @param other the value to compare with.
         * @return whether [other] is a CDATA section with equal content.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean {
            val isSame = other is CData && value == other.value
            return this === other || isSame
        }

        /**
         * Returns a hash code derived from the content.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = value.hashCode()

        /**
         * Returns the escaped content, such as `CData("a")`.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        @OptIn(DavilsInternal::class)
        override fun toString(): String = "CData(${SafeText.quoted(value)})"
    }

    /**
     * A comment.
     *
     * @property value the content of the comment.
     * @since 1.0.0
     */
    public class Comment internal constructor(
        public val value: String,
        range: TextRange?,
    ) : XmlNode(range) {
        /**
         * Compares the content.
         *
         * @param other the value to compare with.
         * @return whether [other] is a comment with equal content.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean {
            val isSame = other is Comment && value == other.value
            return this === other || isSame
        }

        /**
         * Returns a hash code derived from the content.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = value.hashCode()

        /**
         * Returns the escaped content, such as `Comment("a")`.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        @OptIn(DavilsInternal::class)
        override fun toString(): String = "Comment(${SafeText.quoted(value)})"
    }

    /**
     * A processing instruction.
     *
     * @property target the name the instruction is addressed to.
     * @property data everything after the target; empty if there is nothing.
     * @since 1.0.0
     */
    public class ProcessingInstruction internal constructor(
        public val target: String,
        public val data: String,
        range: TextRange?,
    ) : XmlNode(range) {
        /**
         * Compares the target and the data.
         *
         * @param other the value to compare with.
         * @return whether [other] is an instruction with an equal target and data.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean {
            val isSame = other is ProcessingInstruction && target == other.target && data == other.data
            return this === other || isSame
        }

        /**
         * Returns a hash code derived from the target and the data.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * target.hashCode() + data.hashCode()

        /**
         * Returns the escaped target, such as `ProcessingInstruction(php)`.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        @OptIn(DavilsInternal::class)
        override fun toString(): String = "ProcessingInstruction(${SafeText.text(target)})"
    }
}
