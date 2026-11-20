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
package com.davils.kxml.document.xpath

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.name.XmlName

/**
 * A node an [XPath] expression selected, as the data model of XPath 1.0 describes it.
 *
 * A node is a snapshot of one evaluation: it holds its kind, its name and its string-value, and its range in the text
 * of the document when the document was read from text. Two nodes are equal when their kind, name, value and range are
 * equal.
 *
 * @property kind the kind of the node.
 * @property name the expanded name of an element or an attribute with the prefix it was written with; the target of a
 * processing instruction and the prefix of a namespace node as a local name; `null` for the root, a text, a comment
 * and the namespace node of the default namespace.
 * @param value the string-value of the node: the text of an element or the root, the value of an attribute, the
 * URI of a namespace node, the text of a comment or a text node and the data of a processing instruction. It may be
 * sensitive and never appears in [toString].
 * @param range where the node stands in the text of the document, or `null` when the document was built in code
 * and for namespace nodes.
 * @since 1.0.0
 */
public class XPathNode internal constructor(
    public val kind: XPathNodeKind,
    public val name: XmlName?,
    value: String,
    range: TextRange?,
) {
    /**
     * The string-value of the node, which may be sensitive.
     *
     * @since 1.0.0
     */
    public val value: String = value

    /**
     * Where the node stands in the text of the document, or `null`.
     *
     * @since 1.0.0
     */
    public val range: TextRange? = range

    private val values: List<Any?> get() = listOf(kind, name, value, range)

    /**
     * Returns whether [other] is a node of the same kind with the same name, value and range.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an equal [XPathNode].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is XPathNode && values == other.values

    /**
     * Returns a hash code of the kind, the name, the value and the range.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns the kind and the name of the node, such as `XPathNode(Element, p:a)`, without its value.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String {
        val shownName = name?.qualified ?: return "XPathNode($kind)"
        return "XPathNode($kind, $shownName)"
    }
}
