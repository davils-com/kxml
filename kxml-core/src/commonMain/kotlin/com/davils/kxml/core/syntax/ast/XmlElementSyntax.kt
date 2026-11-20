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

package com.davils.kxml.core.syntax.ast

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.syntax.XmlSyntaxElement
import com.davils.kxml.core.syntax.XmlSyntaxKind
import com.davils.kxml.core.syntax.XmlSyntaxNode

private const val PREFIX_SEPARATOR: Char = ':'
private const val XML_PREFIX: String = "xml"
private const val DEFAULT_NAMESPACE: String = "xmlns"

private val tagKinds: Set<XmlSyntaxKind> = setOf(XmlSyntaxKind.START_TAG, XmlSyntaxKind.END_TAG)

/**
 * The typed view of an element of an [XmlSyntaxTree][com.davils.kxml.core.syntax.XmlSyntaxTree]: its tags, its
 * attributes, its child elements and its character data.
 *
 * The view reads the syntax tree as it is, without composing it: it expands no entity of a document type declaration,
 * adds no default attribute and reports no diagnostics, so it also shows an element of a document with errors as far
 * as it could be parsed. Namespaces are resolved from the `xmlns` attributes of the element and its ancestors. It is
 * immutable and safe to share between threads. Everything it returns comes from the input and must be escaped before
 * it is shown.
 *
 * @property node the syntax node of the element, of kind [XmlSyntaxKind.ELEMENT].
 * @since 1.0.0
 */
public class XmlElementSyntax internal constructor(public val node: XmlSyntaxNode) {
    /**
     * The start tag, `<name ...>` or `<name .../>`.
     *
     * @since 1.0.0
     */
    public val startTag: XmlSyntaxNode get() = node.childNodes.first()

    /**
     * The end tag, `</name>`, or `null` for an element written as an empty-element tag or cut short by an error.
     *
     * @since 1.0.0
     */
    public val endTag: XmlSyntaxNode? get() = node.child(XmlSyntaxKind.END_TAG)

    /**
     * Whether the element is written as an empty-element tag, `<name/>`.
     *
     * @since 1.0.0
     */
    public val isEmptyTag: Boolean get() = startTag.token(XmlSyntaxKind.EMPTY_TAG_CLOSE) != null

    /**
     * The qualified name as the start tag writes it, such as `p:item`.
     *
     * @since 1.0.0
     */
    public val name: String get() = startTag.token(XmlSyntaxKind.NAME)?.text.orEmpty()

    /**
     * The prefix of [name], or `null` when it has none.
     *
     * @since 1.0.0
     */
    public val prefix: String? get() = name.substringBefore(PREFIX_SEPARATOR).takeIf { PREFIX_SEPARATOR in name }

    /**
     * The local part of [name], after its prefix.
     *
     * @since 1.0.0
     */
    public val localName: String get() = name.substringAfter(PREFIX_SEPARATOR)

    /**
     * The namespace of [name], as [namespaceFor] resolves its prefix.
     *
     * @since 1.0.0
     */
    public val namespace: String? get() = namespaceFor(prefix)

    /**
     * The attributes of the start tag in their order, namespace declarations included.
     *
     * @since 1.0.0
     */
    public val attributes: List<XmlAttributeSyntax>
        get() = startTag.childNodes.map(::XmlAttributeSyntax)

    /**
     * The child elements in their order.
     *
     * @since 1.0.0
     */
    public val elements: List<XmlElementSyntax>
        get() = node.childNodes.filter { child -> child.kind == XmlSyntaxKind.ELEMENT }.map(::XmlElementSyntax)

    /**
     * Everything between the start tag and the end tag: text, references, CDATA sections, comments, processing
     * instructions and child elements, in their order.
     *
     * @since 1.0.0
     */
    public val content: List<XmlSyntaxElement> get() = node.children.filter { child -> child.kind !in tagKinds }

    /**
     * The character data of the element itself, without that of its child elements: text, CDATA sections, character
     * references and the five predefined entities, with line ends normalized as the version of the document asks.
     * `null` when the content refers to another entity, whose replacement only the document type declaration knows.
     *
     * @since 1.0.0
     */
    public val text: String?
        get() {
            val characters = content.filter { part -> part.kind != XmlSyntaxKind.ELEMENT && isCharacterData(part) }
            return decodedText(characters, versionOf(node), isAttribute = false)
        }

    /**
     * The element that encloses this one, or `null` for the root element.
     *
     * @since 1.0.0
     */
    public val parent: XmlElementSyntax?
        get() {
            val enclosing = node.ancestors.firstOrNull { ancestor -> ancestor.kind == XmlSyntaxKind.ELEMENT }
            return enclosing?.let(::XmlElementSyntax)
        }

    /**
     * Returns the first attribute with the qualified name [name], as written.
     *
     * @param name the qualified name, such as `xml:lang`.
     * @return the attribute, or `null`.
     * @since 1.0.0
     */
    public fun attribute(name: String): XmlAttributeSyntax? =
        attributes.firstOrNull { attribute -> attribute.name == name }

    /**
     * Returns the child elements with the qualified name [name], as written.
     *
     * @param name the qualified name, such as `p:item`.
     * @return the child elements, in their order.
     * @since 1.0.0
     */
    public fun elements(name: String): List<XmlElementSyntax> = elements.filter { element -> element.name == name }

    /**
     * Returns the namespace [prefix] is bound to in the scope of this element: by the nearest `xmlns:prefix`
     * attribute of this element or an ancestor, or by the nearest `xmlns` attribute for `null`. The prefix `xml` is
     * always bound to [XmlNamespaces.XML].
     *
     * @param prefix the prefix, or `null` for the default namespace.
     * @return the namespace, or `null` when the prefix is unbound, undeclared with an empty value or bound through a
     * reference to an entity of the document type declaration.
     * @since 1.0.0
     */
    public fun namespaceFor(prefix: String?): String? {
        if (prefix == XML_PREFIX) return XmlNamespaces.XML
        val declaration = DEFAULT_NAMESPACE + prefix?.let { bound -> PREFIX_SEPARATOR + bound }.orEmpty()
        val scope = generateSequence(this, XmlElementSyntax::parent)
        val declared = scope.firstNotNullOfOrNull { element -> element.attribute(declaration) }
        return declared?.value?.takeIf(String::isNotEmpty)
    }

    /**
     * Returns the escaped name and range, such as `XmlElementSyntax("p:item", 12..40)`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlElementSyntax(${SafeText.quoted(name)}, ${node.range})"

    private fun isCharacterData(part: XmlSyntaxElement): Boolean =
        part.kind != XmlSyntaxKind.COMMENT && part.kind != XmlSyntaxKind.PROCESSING_INSTRUCTION
}
