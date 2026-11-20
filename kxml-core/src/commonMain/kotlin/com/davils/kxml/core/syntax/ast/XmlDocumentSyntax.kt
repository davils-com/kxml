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

package com.davils.kxml.core.syntax.ast

import com.davils.kxml.core.syntax.XmlSyntaxKind
import com.davils.kxml.core.syntax.XmlSyntaxNode

private const val DEFAULT_VERSION: String = "1.0"
private const val VERSION: String = "version"

private val miscKinds: Set<XmlSyntaxKind> = setOf(XmlSyntaxKind.COMMENT, XmlSyntaxKind.PROCESSING_INSTRUCTION)

/**
 * The typed view of a document of an [XmlSyntaxTree][com.davils.kxml.core.syntax.XmlSyntaxTree]: its XML declaration,
 * its document type declaration, its root element and the comments and processing instructions around it.
 *
 * The view reads the syntax tree as it is, so it also shows a document with errors as far as it could be parsed. It is
 * immutable and safe to share between threads. Everything it returns comes from the input and must be escaped before
 * it is shown.
 *
 * @property node the syntax node of the document, of kind [XmlSyntaxKind.DOCUMENT].
 * @since 1.0.0
 */
public class XmlDocumentSyntax internal constructor(public val node: XmlSyntaxNode) {
    /**
     * The XML declaration, `<?xml ...?>`, or `null` when the document has none.
     *
     * @since 1.0.0
     */
    public val declaration: XmlSyntaxNode? get() = node.child(XmlSyntaxKind.XML_DECLARATION)

    /**
     * The version the XML declaration names, or `1.0` when there is none.
     *
     * @since 1.0.0
     */
    public val version: String
        get() {
            val attributes = declaration?.childNodes.orEmpty().filter { child -> child.kind == XmlSyntaxKind.ATTRIBUTE }
            val declared = attributes.map(::XmlAttributeSyntax).firstOrNull { attribute -> attribute.name == VERSION }
            return declared?.rawValue ?: DEFAULT_VERSION
        }

    /**
     * The document type declaration, `<!DOCTYPE ...>`, or `null` when the document has none.
     *
     * @since 1.0.0
     */
    public val doctype: XmlSyntaxNode? get() = node.child(XmlSyntaxKind.DOCTYPE)

    /**
     * The root element, or `null` when the document has none, such as an empty text.
     *
     * @since 1.0.0
     */
    public val root: XmlElementSyntax? get() = node.child(XmlSyntaxKind.ELEMENT)?.let(::XmlElementSyntax)

    /**
     * The comments and processing instructions before the root element, in their order.
     *
     * @since 1.0.0
     */
    public val prolog: List<XmlSyntaxNode> get() = misc(isBeforeRoot = true)

    /**
     * The comments and processing instructions after the root element, in their order.
     *
     * @since 1.0.0
     */
    public val epilog: List<XmlSyntaxNode> get() = misc(isBeforeRoot = false)

    /**
     * Returns the range of the document, such as `XmlDocumentSyntax(0..120)`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlDocumentSyntax(${node.range})"

    private fun misc(isBeforeRoot: Boolean): List<XmlSyntaxNode> {
        val nodes = node.childNodes
        val found = nodes.indexOfFirst { child -> child.kind == XmlSyntaxKind.ELEMENT }
        val rootIndex = found.takeIf { index -> index >= 0 } ?: nodes.size
        return nodes.filterIndexed { index, child ->
            val isBefore = index < rootIndex
            child.kind in miscKinds && isBefore == isBeforeRoot
        }
    }
}
