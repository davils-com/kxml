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
@file:OptIn(DavilsInternal::class)

package com.davils.kxml.document.xpath

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList

/**
 * The value of an XPath 1.0 expression: a node-set, a string, a number or a boolean.
 *
 * A value is immutable and safe to share between threads. Since a value may come from a sensitive document, its
 * [toString] never shows it.
 *
 * @since 1.0.0
 */
public sealed interface XPathValue {
    /**
     * A node-set, the nodes in document order without duplicates.
     *
     * Only an evaluation creates one, and it cannot be bound to a variable of another evaluation.
     *
     * @since 1.0.0
     */
    public class NodeSet internal constructor(nodes: List<XPathNode>) : XPathValue {
        /**
         * The nodes in document order, which cannot be changed.
         *
         * @since 1.0.0
         */
        public val nodes: List<XPathNode> = nodes.immutableList()

        /**
         * Returns whether [other] is a node-set of the same nodes in the same order.
         *
         * @param other the object to compare with.
         * @return `true` if [other] holds the same nodes.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is NodeSet && nodes == other.nodes

        /**
         * Returns a hash code of the nodes.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = nodes.hashCode()

        /**
         * Returns the number of nodes, such as `NodeSet(size=3)`.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        override fun toString(): String = "NodeSet(size=${nodes.size})"
    }

    /**
     * A string.
     *
     * @param value the string, which may hold any text.
     * @since 1.0.0
     */
    public class Text(value: String) : XPathValue {
        /**
         * The string.
         *
         * @since 1.0.0
         */
        public val value: String = value

        /**
         * Returns whether [other] is a string with the same characters.
         *
         * @param other the object to compare with.
         * @return `true` if [other] holds an equal [value].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Text && value == other.value

        /**
         * Returns a hash code of the string.
         *
         * @return the hash code of [value].
         * @since 1.0.0
         */
        override fun hashCode(): Int = value.hashCode()

        /**
         * Returns the length of the string, such as `Text(length=5)`, without the string.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        override fun toString(): String = "Text(length=${value.length})"
    }

    /**
     * A number, an IEEE 754 double as XPath 1.0 defines it.
     *
     * @param value the number, which may be not a number or infinite.
     * @since 1.0.0
     */
    public class Number(value: Double) : XPathValue {
        /**
         * The number.
         *
         * @since 1.0.0
         */
        public val value: Double = value

        /**
         * Returns whether [other] is a number with the same bits; not a number equals not a number, and `0` does not
         * equal `-0`.
         *
         * @param other the object to compare with.
         * @return `true` if [other] holds the same [value].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Number && value.equals(other.value)

        /**
         * Returns a hash code of the number.
         *
         * @return the hash code of [value].
         * @since 1.0.0
         */
        override fun hashCode(): Int = value.hashCode()

        /**
         * Returns `Number`, without the number.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        override fun toString(): String = "Number"
    }

    /**
     * A boolean.
     *
     * @param isTrue the boolean.
     * @since 1.0.0
     */
    public class Truth(isTrue: Boolean) : XPathValue {
        /**
         * The boolean.
         *
         * @since 1.0.0
         */
        public val isTrue: Boolean = isTrue

        /**
         * Returns whether [other] is the same boolean.
         *
         * @param other the object to compare with.
         * @return `true` if [other] holds the same [isTrue].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Truth && isTrue == other.isTrue

        /**
         * Returns a hash code of the boolean.
         *
         * @return the hash code of [isTrue].
         * @since 1.0.0
         */
        override fun hashCode(): Int = isTrue.hashCode()

        /**
         * Returns `Truth`, without the boolean.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        override fun toString(): String = "Truth"
    }
}
