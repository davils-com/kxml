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
import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.syntax.XmlSyntaxElement
import com.davils.kxml.core.syntax.XmlSyntaxKind
import com.davils.kxml.core.syntax.XmlSyntaxNode

private const val NAMESPACE_ATTRIBUTE: String = "xmlns"
private const val PREFIX_SEPARATOR: Char = ':'

/**
 * The typed view of an attribute of a start tag or of the XML declaration in an
 * [XmlSyntaxTree][com.davils.kxml.core.syntax.XmlSyntaxTree]: its name and its value, as written and as a reader
 * reports it.
 *
 * The view reads the syntax tree as it is, so it also shows an attribute of a document with errors as far as it
 * could be parsed. It is immutable and safe to share between threads. Everything it returns comes from the input
 * and must be escaped before it is shown.
 *
 * @property node the syntax node of the attribute, of kind [XmlSyntaxKind.ATTRIBUTE].
 * @since 1.0.0
 */
public class XmlAttributeSyntax internal constructor(public val node: XmlSyntaxNode) {
    private val valueNode: XmlSyntaxNode? get() = node.child(XmlSyntaxKind.ATTRIBUTE_VALUE)

    private val valueParts: List<XmlSyntaxElement>
        get() = valueNode?.children.orEmpty().filter { part -> part.kind != XmlSyntaxKind.QUOTE }

    /**
     * The qualified name as written, such as `xml:lang`.
     *
     * @since 1.0.0
     */
    public val name: String get() = node.children.first().text

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
     * Whether the attribute declares a namespace, as `xmlns` or `xmlns:prefix`.
     *
     * @since 1.0.0
     */
    public val isNamespaceDeclaration: Boolean get() = name == NAMESPACE_ATTRIBUTE || prefix == NAMESPACE_ATTRIBUTE

    /**
     * The value between its quotes as written, with every reference unexpanded.
     *
     * @since 1.0.0
     */
    public val rawValue: String get() = valueParts.joinToString(separator = "", transform = XmlSyntaxElement::text)

    /**
     * The range of [rawValue] in the text, between the quotes.
     *
     * @since 1.0.0
     */
    public val valueRange: TextRange
        get() {
            val opening = valueNode?.let { value -> value.range.start + 1 }
            val start = opening ?: node.range.end
            return TextRange(start = start, end = start + rawValue.length)
        }

    /**
     * The value as a reader without a document type declaration reports it: character references and the five
     * predefined entities are replaced, line ends are normalized and every tab and line end becomes a space, as
     * section 3.3.3 of XML 1.0 describes it for a value of type CDATA. `null` when the value refers to another entity,
     * whose replacement only the document type declaration knows.
     *
     * @since 1.0.0
     */
    public val value: String? get() = decodedText(valueParts, versionOf(node), isAttribute = true)

    /**
     * Returns the escaped name and range, such as `XmlAttributeSyntax("xml:lang", 3..15)`.
     *
     * @return a text that is safe to log; it never shows the value.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlAttributeSyntax(${SafeText.quoted(name)}, ${node.range})"
}
