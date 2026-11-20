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
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.collection.immutableMap
import com.davils.kore.core.text.SafeText
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.xpath.evaluate.Evaluator
import com.davils.kxml.document.xpath.evaluate.Focus
import com.davils.kxml.document.xpath.evaluate.NodeKeys
import com.davils.kxml.document.xpath.evaluate.Value
import com.davils.kxml.document.xpath.parse.QueryParsing
import com.davils.kxml.document.xpath.parse.QueryReading
import com.davils.kxml.document.xpath.syntax.Expression
import com.davils.kxml.document.xpath.syntax.ValueType

/**
 * An XPath 1.0 expression, parsed and ready to evaluate on documents.
 *
 * The expression works on the data model of XPath 1.0: a document has a root node, elements, attributes, namespace
 * nodes, text nodes, comments and processing instructions; adjacent text and CDATA sections form one text node, and
 * namespace declarations are namespace nodes, not attributes. Names in the expression are matched by their expanded
 * names: a prefix is resolved through [namespaces], an unprefixed name has no namespace, and the prefix `xml` is
 * always bound. The 27 functions of the core library are supported and no others; `id` finds elements by their
 * `xml:id` attribute, since a document carries no attribute types. Strings are compared, measured and cut by code
 * point, and a number is written as XPath 1.0 writes it, through the shortest decimal that reads back as it.
 *
 * Every expression is bounded by the [limits] it was parsed with: its text by [XmlLimits.maximumQueryLength] and its
 * nesting by [XmlLimits.maximumQueryNesting], and every evaluation by [XmlLimits.maximumQuerySteps], counted in the
 * nodes it visits, the expressions it evaluates and the text it copies, by [XmlLimits.maximumQueryResults] for every
 * node-set and by [XmlLimits.maximumQueryStringLength] for every string it builds. No evaluation compares every pair
 * of nodes of two node-sets, so `//a = //b` takes time near linear in the nodes, and no input decides the complexity
 * of a search in a string. An evaluation that exceeds a limit stops with an [XmlDocumentLimitException] and returns
 * nothing.
 *
 * An expression is immutable and safe to share between threads; every evaluation keeps its own state.
 *
 * @property expression the text of the expression exactly as it was parsed.
 * @property limits the limits the expression was parsed with and evaluates under.
 * @since 1.0.0
 */
public class XPath private constructor(
    public val expression: String,
    public val limits: XmlLimits,
    bindings: OrderedMap<String, String>,
    private val syntax: Expression,
) {
    /**
     * The prefixes the expression may use and the namespace URIs they stand for, which cannot be changed.
     *
     * @since 1.0.0
     */
    public val namespaces: Map<String, String> = bindings.entries.toMap().immutableMap()

    /**
     * Whether the expression gives a node-set, such as a location path, so that [select] can evaluate it.
     *
     * @since 1.0.0
     */
    public val isNodeSet: Boolean get() = syntax.type == ValueType.NodeSet

    /**
     * Evaluates the expression with the root node of [document] as the context node, at position 1 of 1.
     *
     * @param document the document to evaluate on.
     * @param variables the values of the variables the expression refers to, by their expanded names.
     * @return the value of the expression; a node-set holds its nodes in document order.
     * @throws XmlDocumentLimitException if the evaluation exceeds a limit of [limits]; nothing is returned then.
     * @throws IllegalArgumentException if the expression refers to a variable that [variables] does not bind, or one
     * that is bound to a node-set.
     * @since 1.0.0
     */
    public fun evaluate(document: XmlDocument, variables: Map<XmlName, XPathValue> = emptyMap()): XPathValue {
        val evaluator = Evaluator(DocumentModel.of(document), limits, variables)
        val focus = Focus(key = NodeKeys.keyOf(DocumentModel.ROOT), position = 1, size = 1)
        val value = evaluator.evaluate(syntax, focus)
        return valueOf(evaluator, value)
    }

    /**
     * Evaluates the expression, which must give a node-set, and returns its nodes.
     *
     * @param document the document to evaluate on.
     * @param variables the values of the variables the expression refers to, by their expanded names.
     * @return the selected nodes in document order, which cannot be changed; empty if nothing matches.
     * @throws IllegalStateException if the expression gives no node-set; see [isNodeSet].
     * @throws XmlDocumentLimitException if the evaluation exceeds a limit of [limits].
     * @throws IllegalArgumentException if a variable is not bound or is bound to a node-set.
     * @since 1.0.0
     */
    public fun select(document: XmlDocument, variables: Map<XmlName, XPathValue> = emptyMap()): List<XPathNode> {
        check(isNodeSet) { "The expression gives no node-set" }
        val selected = evaluate(document, variables) as XPathValue.NodeSet
        return selected.nodes
    }

    /**
     * Returns whether [other] is an expression with the same text, namespaces and limits.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an [XPath] with an equal [expression], equal [namespaces] and equal [limits].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is XPath) return false
        return expression == other.expression && namespaces == other.namespaces && limits == other.limits
    }

    /**
     * Returns a hash code of the text of the expression.
     *
     * @return the hash code of [expression].
     * @since 1.0.0
     */
    override fun hashCode(): Int = expression.hashCode()

    /**
     * Returns the [expression] escaped and shortened through [SafeText], since it may come from an attacker.
     *
     * @return the expression as it is safe to show.
     * @since 1.0.0
     */
    override fun toString(): String = SafeText.text(expression)

    private fun valueOf(evaluator: Evaluator, value: Value): XPathValue = when (value) {
        is Value.Nodes -> XPathValue.NodeSet(value.keys.map { key -> nodeOf(evaluator, key) })
        is Value.Text -> XPathValue.Text(value.value)
        is Value.Number -> XPathValue.Number(value.value)
        is Value.Truth -> XPathValue.Truth(isTrue = value.isTrue)
    }

    private fun nodeOf(evaluator: Evaluator, key: Long): XPathNode {
        val nodes = evaluator.nodes
        val range = nodes.model.range(NodeKeys.ordinalOf(key)).takeUnless { NodeKeys.isNamespace(key) }
        val value = nodes.stringValueOf(key)
        evaluator.bounds.copied(value.length)
        return XPathNode(kind = nodes.kindOf(key), name = nodes.nameOf(key), value = value, range = range)
    }

    /**
     * Parses XPath 1.0 expressions.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Parses [expression] as an XPath 1.0 expression.
         *
         * @param expression the expression, such as `/catalog/book[@id = 'b1']/title`.
         * @param namespaces the prefixes the expression may use and their namespace URIs; empty by default.
         * @param limits the limits the expression is parsed with and evaluates under; [XmlLimits.Untrusted] by default.
         * @return the expression.
         * @throws IllegalArgumentException if [expression] is not a well-formed XPath 1.0 expression, uses a prefix
         * [namespaces] does not bind, calls a function outside the core library or with the wrong arguments, uses a
         * value that is no node-set where a node-set is needed, is longer than [XmlLimits.maximumQueryLength] or nests
         * deeper than [XmlLimits.maximumQueryNesting]; the message shows the expression escaped and shortened, the
         * reason and the offset. Also if [namespaces] binds a prefix that is no NCName, the prefix `xmlns`, an empty
         * URI, or the prefix `xml` to another namespace than the one of XML.
         * @since 1.0.0
         */
        public fun parse(
            expression: String,
            namespaces: Map<String, String> = emptyMap(),
            limits: XmlLimits = XmlLimits.Untrusted,
        ): XPath {
            val bindings = bindingsOf(namespaces)
            return when (val reading = QueryParsing.read(expression, namespaces = bindings::get, limits = limits)) {
                is QueryReading.Read -> {
                    XPath(expression = expression, limits = limits, bindings = bindings, syntax = reading.expression)
                }

                is QueryReading.Refused -> {
                    throw IllegalArgumentException(
                        "${SafeText.quoted(expression)} is not an XPath 1.0 expression: ${reading.reason} at " +
                            "offset ${reading.offset}",
                    )
                }
            }
        }

        /**
         * Parses [expression] as an XPath 1.0 expression and returns `null` where [parse] throws for the expression.
         *
         * @param expression the expression, such as `count(//item)`.
         * @param namespaces the prefixes the expression may use and their namespace URIs; empty by default.
         * @param limits the limits the expression is parsed with and evaluates under; [XmlLimits.Untrusted] by default.
         * @return the expression, or `null` if [expression] is not one or exceeds a limit, as [parse] describes.
         * @throws IllegalArgumentException if [namespaces] binds an invalid prefix or URI, as [parse] describes.
         * @since 1.0.0
         */
        public fun parseOrNull(
            expression: String,
            namespaces: Map<String, String> = emptyMap(),
            limits: XmlLimits = XmlLimits.Untrusted,
        ): XPath? {
            val bindings = bindingsOf(namespaces)
            return when (val reading = QueryParsing.read(expression, namespaces = bindings::get, limits = limits)) {
                is QueryReading.Read -> {
                    XPath(expression = expression, limits = limits, bindings = bindings, syntax = reading.expression)
                }

                is QueryReading.Refused -> {
                    null
                }
            }
        }

        private fun bindingsOf(namespaces: Map<String, String>): OrderedMap<String, String> {
            val bindings = OrderedMap<String, String>()
            namespaces.forEach { (prefix, uri) ->
                val isPrefix = XmlCharacters.isNcName(prefix) && prefix != XmlNamespaces.XMLNS_PREFIX
                require(isPrefix) { "${SafeText.quoted(prefix)} cannot be the prefix of an XPath expression" }
                require(uri.isNotEmpty()) { "The prefix $prefix needs a namespace URI" }
                val isXml = prefix != XmlNamespaces.XML_PREFIX || uri == XmlNamespaces.XML
                require(isXml) { "The prefix xml is bound to ${XmlNamespaces.XML}" }
                bindings[prefix] = uri
            }
            return bindings
        }
    }
}
