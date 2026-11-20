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

package com.davils.kxml.document.c14n

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableSet
import com.davils.kore.core.text.SafeText
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.c14n.render.AttributeScope
import com.davils.kxml.document.c14n.render.CanonicalOutput
import com.davils.kxml.document.c14n.render.CanonicalWalk
import com.davils.kxml.document.c14n.render.ExclusiveNamespaces
import com.davils.kxml.document.c14n.render.InclusiveDocumentNamespaces
import com.davils.kxml.document.c14n.render.InclusiveSubsetNamespaces
import com.davils.kxml.document.c14n.render.Inheritance
import com.davils.kxml.document.c14n.render.Membership
import com.davils.kxml.document.c14n.render.NamespacePolicy
import com.davils.kxml.document.c14n.render.NodeSubset
import com.davils.kxml.document.c14n.render.WholeDocument
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.xpath.XPath
import com.davils.kxml.document.xpath.evaluate.Evaluator
import com.davils.kxml.document.xpath.evaluate.Focus
import com.davils.kxml.document.xpath.evaluate.NodeKeys

private const val DEFAULT_TOKEN: String = "#default"
private const val DEFAULT_PREFIX: String = ""
private const val SCHEME_PUNCTUATION: String = "+-."

/**
 * Writes the canonical form of a document or of a subset of it, as [method] defines it, as UTF-8 bytes without a byte
 * order mark.
 *
 * The input is the data model of XPath 1.0 that [XmlDocument] carries: the reader has already replaced entity
 * references, normalized attribute values and line ends, added the default attributes of the internal subset and
 * turned CDATA sections into text, and the canonical form leaves out the XML declaration and the document type
 * declaration. Elements are written with start and end tags, namespace declarations and attributes in the order of
 * code points, attribute values in double quotes, and the characters each method escapes as references.
 *
 * A whole document is written in one pass that tracks the namespaces in scope, so its cost grows with the document,
 * not with the namespaces in scope on every element. A subset is the node-set an [XPath] expression selects, such as
 * `(//. | //@* | //namespace::*)[ancestor-or-self::p:signed]`; it is rendered as section 2.4 of Canonical XML 1.0
 * describes, with the attributes of the XML namespace of omitted ancestors imported into orphan elements, and as
 * Canonical XML 1.1 refines it: only `xml:lang` and `xml:space` are imported, `xml:id` never is, and the `xml:base`
 * values of contiguously omitted ancestors are joined into the one of the orphan as the prose of section 2.4 says
 * (example 3.8 of that specification also joins the value of the nearest rendered ancestor; this implementation, like
 * Apache Santuario, does not). Exclusive canonicalization renders a namespace declaration only where an element or
 * one of its rendered attributes uses its prefix and no output ancestor rendered it already, except for the prefixes
 * of [inclusivePrefixes], and imports nothing into orphan elements.
 *
 * The canonical form is bounded by [XmlLimits.maximumOutputLength] of [limits], in code units before encoding; the
 * selection of a subset by every query limit of [limits], and the import of attributes into its orphan elements counts
 * against [XmlLimits.maximumQuerySteps]. A canonicalizer is immutable and safe to share between threads.
 *
 * @property method the canonicalization method.
 * @param inclusivePrefixes the InclusiveNamespaces PrefixList of exclusive canonicalization: prefixes, and
 * [DEFAULT_NAMESPACE] for the default namespace; it must be empty for the other methods.
 * @property limits the limits of the canonical form and of the selection of subsets.
 * @since 1.0.0
 */
public class XmlCanonicalizer(
    public val method: XmlCanonicalization,
    inclusivePrefixes: Set<String> = emptySet(),
    public val limits: XmlLimits = XmlLimits.Untrusted,
) {
    /**
     * The prefixes Exclusive XML Canonicalization handles as Canonical XML does, the InclusiveNamespaces PrefixList of
     * the method, with [DEFAULT_NAMESPACE] for the default namespace; empty for the other methods.
     *
     * @since 1.0.0
     */
    public val inclusivePrefixes: Set<String> = inclusivePrefixes.immutableSet()

    private val exclusivePrefixes: Set<String> = inclusivePrefixes.mapTo(mutableSetOf()) { prefix ->
        DEFAULT_PREFIX.takeIf { prefix == DEFAULT_TOKEN } ?: prefix
    }

    init {
        require(inclusivePrefixes.isEmpty() || method.isExclusive) {
            "Only exclusive canonicalization takes inclusive prefixes"
        }
        inclusivePrefixes.forEach { prefix ->
            require(prefix == DEFAULT_TOKEN || XmlCharacters.isNcName(prefix)) {
                "${SafeText.quoted(prefix)} is no prefix and not $DEFAULT_TOKEN"
            }
        }
    }

    /**
     * Writes the canonical form of [document].
     *
     * @param document the document.
     * @return the canonical form in UTF-8.
     * @throws IllegalArgumentException if the document declares a namespace by a relative URI, which canonical XML
     * refuses.
     * @throws XmlDocumentLimitException if the canonical form is longer than [XmlLimits.maximumOutputLength].
     * @since 1.0.0
     */
    public fun canonicalize(document: XmlDocument): ByteArray {
        val model = DocumentModel.of(document)
        return written(model, WholeDocument) { }
    }

    /**
     * Writes the canonical form of the subset of [document] that [subset] selects.
     *
     * @param document the document.
     * @param subset an expression that gives a node-set, evaluated with the root node as its context; its own limits
     * bound the selection.
     * @return the canonical form of the subset in UTF-8; it may not be well-formed XML, as section 2.4 of Canonical XML
     * 1.0 allows.
     * @throws IllegalArgumentException if [subset] gives no node-set or the document declares a namespace by a
     * relative URI.
     * @throws XmlDocumentLimitException if the selection or the import of attributes exceeds a query limit of
     * [limits], or the canonical form is longer than [XmlLimits.maximumOutputLength].
     * @since 1.0.0
     */
    public fun canonicalize(document: XmlDocument, subset: XPath): ByteArray {
        require(subset.isNodeSet) { "The subset must be an expression that gives a node-set" }
        val model = DocumentModel.of(document)
        val evaluator = Evaluator(model, limits, emptyMap())
        val focus = Focus(key = NodeKeys.keyOf(DocumentModel.ROOT), position = 1, size = 1)
        val keys = evaluator.nodesOf(subset.parsed, focus)
        val membership = NodeSubset(keys, evaluator.nodes)
        return written(model, membership) { steps -> evaluator.bounds.charge(steps.toLong()) }
    }

    /**
     * Returns whether [other] is a canonicalizer with the same method, inclusive prefixes and limits.
     *
     * @param other the object to compare with.
     * @return `true` if [other] canonicalizes the same way.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is XmlCanonicalizer) return false
        return method == other.method && inclusivePrefixes == other.inclusivePrefixes && limits == other.limits
    }

    /**
     * Returns a hash code of the method and the inclusive prefixes.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = method.hashCode() * HASH_FACTOR + inclusivePrefixes.hashCode()

    /**
     * Returns the method and the number of inclusive prefixes, such as `XmlCanonicalizer(Exclusive10, prefixes=2)`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlCanonicalizer($method, prefixes=${inclusivePrefixes.size})"

    private fun written(model: DocumentModel, membership: Membership, charge: (Int) -> Unit): ByteArray {
        refuseRelativeNamespaces(model)
        val output = CanonicalOutput(limits.maximumOutputLength)
        val attributes = AttributeScope(
            model = model,
            membership = membership,
            inheritance = inheritanceOf(membership),
            charge = charge,
        )
        val walk = CanonicalWalk(
            model = model,
            membership = membership,
            namespaces = namespacesOf(model, membership),
            attributes = attributes,
            output = output,
            hasComments = method.hasComments,
        )
        walk.write()
        return output.bytes()
    }

    private fun namespacesOf(model: DocumentModel, membership: Membership): NamespacePolicy {
        if (method.isExclusive) return ExclusiveNamespaces(model, membership, exclusivePrefixes)
        val subset = InclusiveSubsetNamespaces(membership).takeIf { membership.isSubset }
        return subset ?: InclusiveDocumentNamespaces(model)
    }

    private fun inheritanceOf(membership: Membership): Inheritance {
        val isInheriting = membership.isSubset && !method.isExclusive
        if (!isInheriting) return Inheritance.None
        return Inheritance.SimpleXml.takeIf { method in canonical11 } ?: Inheritance.AllXml
    }

    private fun refuseRelativeNamespaces(model: DocumentModel) {
        for (node in 0..<model.size) {
            val isRelative = model.declaredNamespaces(node).any { binding -> isRelative(binding.uri) }
            require(!isRelative) { "Canonical XML refuses a document that declares a relative namespace URI" }
        }
    }

    private fun isRelative(uri: String): Boolean {
        if (uri.isEmpty()) return false
        val colon = uri.indexOf(':')
        val scheme = uri.substring(startIndex = 0, endIndex = maxOf(a = colon, b = 0))
        val isScheme = scheme.isNotEmpty() && scheme.first().isLetter() && scheme.all(::isSchemeCharacter)
        return !isScheme
    }

    private fun isSchemeCharacter(char: Char): Boolean {
        val isAlphanumeric = char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9'
        return isAlphanumeric || char in SCHEME_PUNCTUATION
    }

    /**
     * Holds the token of the default namespace in [inclusivePrefixes].
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The token `#default`, which stands for the default namespace in [inclusivePrefixes].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_NAMESPACE: String = DEFAULT_TOKEN

        private const val HASH_FACTOR: Int = 31

        private val canonical11: Set<XmlCanonicalization> =
            setOf(XmlCanonicalization.Canonical11, XmlCanonicalization.Canonical11WithComments)
    }
}
