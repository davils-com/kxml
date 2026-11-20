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

package com.davils.kxml.document.patch

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.SafeText
import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.document.navigation.PrefixScope

/**
 * One operation of an XML patch of RFC 5261: `<add>`, `<replace>` or `<remove>`.
 *
 * The [selector] is an XPath 1.0 expression that must locate exactly one node of the document, evaluated with the
 * root node as context. Its prefixes resolve through [namespaces], the declarations in scope on the operation element
 * of the diff document, and an unprefixed element name is in the default namespace of [namespaces], keyed by the
 * empty prefix, as section 4.2.1 of RFC 5261 requires, unlike XPath 1.0.
 *
 * An operation is immutable; its [toString] shows the kind and the selector escaped.
 *
 * @property selector the `sel` attribute.
 * @param namespaces the namespace declarations in scope on the operation; see [namespaces].
 * @since 1.0.0
 */
public sealed class XmlPatchOperation(public val selector: String, namespaces: Map<String, String>) {
    /**
     * The namespace declarations in scope on the operation in the diff document, prefix to URI, with the empty prefix
     * for the default namespace; they resolve the prefixes of [selector] and of added names.
     *
     * @since 1.0.0
     */
    public val namespaces: Map<String, String> = PrefixScope.of(namespaces)

    init {
        namespaces.forEach { (prefix, uri) ->
            require(prefix.isEmpty() || XmlCharacters.isNcName(prefix)) { "${SafeText.quoted(prefix)} is no prefix" }
            require(uri.isNotEmpty()) { "The prefix ${SafeText.quoted(prefix)} needs a namespace URI" }
        }
    }

    internal abstract val values: List<Any>

    internal abstract val label: String

    /**
     * Returns whether [other] is the same kind of operation with equal selector, namespaces and content.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an equal operation.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is XmlPatchOperation && this::class == other::class && values == other.values

    /**
     * Returns a hash code of the selector, the namespaces and the content.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns the kind of operation and its selector escaped and shortened, such as `Remove(doc/foo)`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String = "$label(${SafeText.text(selector)})"

    /**
     * Adds [content] at [position] relative to the located node: into an element by [XmlPatchPosition.Append] and
     * [XmlPatchPosition.Prepend], beside an element, a text, a comment or a processing instruction by
     * [XmlPatchPosition.Before] and [XmlPatchPosition.After].
     *
     * The namespaces of added elements and attributes are mapped onto the declarations of the target as section 4.2.3
     * of RFC 5261 describes; a namespace the target does not declare is declared on the added element.
     *
     * @param selector the `sel` attribute.
     * @param content the nodes to add: elements, texts, CDATA sections, comments and processing instructions.
     * @param position where the nodes go.
     * @param namespaces the namespace declarations in scope on the operation.
     * @since 1.0.0
     */
    public class Add(
        selector: String,
        content: List<XmlNode>,
        public val position: XmlPatchPosition = XmlPatchPosition.Append,
        namespaces: Map<String, String> = emptyMap(),
    ) : XmlPatchOperation(selector, namespaces) {
        /**
         * The nodes to add, which cannot be changed.
         *
         * @since 1.0.0
         */
        public val content: List<XmlNode> = content.immutableList()

        override val values: List<Any> get() = listOf(selector, namespaces, content, position)

        override val label: String get() = "Add"
    }

    /**
     * Adds the attribute [name] with [value] to the located element, `type="@name"`.
     *
     * @param selector the `sel` attribute.
     * @param name the qualified name of the attribute; a prefix resolves through [namespaces], and no name resolves
     * through the default namespace.
     * @param value the value of the attribute.
     * @param namespaces the namespace declarations in scope on the operation.
     * @throws IllegalArgumentException if [name] is no qualified name.
     * @since 1.0.0
     */
    public class AddAttribute(
        selector: String,
        public val name: String,
        public val value: String,
        namespaces: Map<String, String> = emptyMap(),
    ) : XmlPatchOperation(selector, namespaces) {
        init {
            require(name.split(':').let { parts -> parts.size <= 2 && parts.all(XmlCharacters::isNcName) }) {
                "${SafeText.quoted(name)} is no qualified name"
            }
        }

        override val values: List<Any> get() = listOf(selector, namespaces, name, value)

        override val label: String get() = "AddAttribute"
    }

    /**
     * Declares [prefix] for [uri] on the located element, `type="namespace::prefix"`.
     *
     * @param selector the `sel` attribute.
     * @param prefix the prefix to declare.
     * @param uri the namespace URI.
     * @param namespaces the namespace declarations in scope on the operation.
     * @throws IllegalArgumentException if [prefix] is no NCName or is `xml` or `xmlns`.
     * @since 1.0.0
     */
    public class AddNamespace(
        selector: String,
        public val prefix: String,
        public val uri: String,
        namespaces: Map<String, String> = emptyMap(),
    ) : XmlPatchOperation(selector, namespaces) {
        init {
            val isReserved = prefix == XmlNamespaces.XML_PREFIX || prefix == XmlNamespaces.XMLNS_PREFIX
            require(XmlCharacters.isNcName(prefix) && !isReserved) { "${SafeText.quoted(prefix)} cannot be declared" }
        }

        override val values: List<Any> get() = listOf(selector, namespaces, prefix, uri)

        override val label: String get() = "AddNamespace"
    }

    /**
     * Replaces the located node: an element, a comment or a processing instruction by the one node of [content] of
     * the same kind, the value of an attribute, the text of a text node or the URI of a namespace declaration by the
     * text of [content].
     *
     * @param selector the `sel` attribute.
     * @param content the new node or the new text; empty text removes a text node and empties an attribute.
     * @param namespaces the namespace declarations in scope on the operation.
     * @since 1.0.0
     */
    public class Replace(
        selector: String,
        content: List<XmlNode>,
        namespaces: Map<String, String> = emptyMap(),
    ) : XmlPatchOperation(selector, namespaces) {
        /**
         * The new node or text, which cannot be changed.
         *
         * @since 1.0.0
         */
        public val content: List<XmlNode> = content.immutableList()

        override val values: List<Any> get() = listOf(selector, namespaces, content)

        override val label: String get() = "Replace"
    }

    /**
     * Removes the located node and the white space text nodes [whitespace] names beside it.
     *
     * @param selector the `sel` attribute.
     * @param whitespace the white space text nodes to remove as well; only for elements, comments and processing
     * instructions.
     * @param namespaces the namespace declarations in scope on the operation.
     * @since 1.0.0
     */
    public class Remove(
        selector: String,
        public val whitespace: XmlPatchWhitespace = XmlPatchWhitespace.None,
        namespaces: Map<String, String> = emptyMap(),
    ) : XmlPatchOperation(selector, namespaces) {
        override val values: List<Any> get() = listOf(selector, namespaces, whitespace)

        override val label: String get() = "Remove"
    }
}
