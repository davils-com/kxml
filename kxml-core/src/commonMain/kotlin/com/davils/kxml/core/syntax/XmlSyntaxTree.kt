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

import com.davils.kore.core.Limits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.LineMap
import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.validation.violation.Severity
import com.davils.kxml.core.Xml
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.diagnostic.XmlDiagnostic
import com.davils.kxml.core.syntax.internal.GreenNode
import com.davils.kxml.core.syntax.internal.Reparse
import com.davils.kxml.core.syntax.internal.SyntaxParsing

/**
 * The lossless syntax tree of a text: every character of [text], whitespace, line breaks, references and quotes
 * included, belongs to exactly one [XmlSyntaxToken], so the [XmlSyntaxNode.text] of [root] is [text] again.
 *
 * The tree is read by the same rules as [Xml.parse] with the [XmlLimits][com.davils.kxml.core.limit.XmlLimits] of
 * [xml]. Where the text stops being well formed, the rest of it becomes one [XmlSyntaxKind.ERROR] token and
 * [diagnostics] holds the error, so the tree of a broken text is still complete and its text is unchanged. A text
 * longer than [XmlLimits.maximumInputLength][com.davils.kxml.core.limit.XmlLimits.maximumInputLength] is refused
 * with one ERROR token before it is read.
 *
 * A tree is immutable and safe to share between threads; the nodes and tokens of its views are created when they are
 * first asked for.
 *
 * @since 1.0.0
 */
public class XmlSyntaxTree internal constructor(
    /**
     * The text the tree was read from.
     *
     * @since 1.0.0
     */
    public val text: String,
    internal val green: GreenNode,
    diagnostics: List<XmlDiagnostic>,
    /**
     * The reader whose configuration and limits the tree was read with.
     *
     * @since 1.0.0
     */
    public val xml: Xml,
) {
    /**
     * The warnings of the text and the error where it stops being well formed, if there is one.
     *
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    public val diagnostics: List<XmlDiagnostic> = diagnostics.immutableList()

    /**
     * Whether the text is well formed: [diagnostics] holds no error.
     *
     * @since 1.0.0
     */
    public val isValid: Boolean get() = diagnostics.none { diagnostic -> diagnostic.severity == Severity.Error }

    /**
     * The node of kind [XmlSyntaxKind.DOCUMENT] that covers the whole text.
     *
     * @since 1.0.0
     */
    public val root: XmlSyntaxNode by lazy { XmlSyntaxNode(green, 0, null) }

    /**
     * The lines of [text], to turn the ranges of the tree into lines and columns; they cover the first
     * [Limits.MAX_LINE_MAP_LENGTH] characters, as many as the line map of Kore indexes.
     *
     * @since 1.0.0
     */
    public val lines: LineMap by lazy { LineMap.of(text.take(Limits.MAX_LINE_MAP_LENGTH)) }

    /**
     * Reads [text] into a document with [xml], as [Xml.parse] does.
     *
     * @return the document or the diagnostics that refuse it.
     * @since 1.0.0
     */
    public fun compose(): XmlParseResult = xml.parse(text)

    /**
     * Applies [edits] to the text and returns the tree of the edited text.
     *
     * When the edits lie inside one element of a well-formed document without a document type declaration, only the
     * smallest element that encloses them is parsed again, in the context of the XML declaration and the start tags
     * around it, so its namespaces and its version are those of the document; the rest of the tree is shared with
     * this one. In every other case, such as an edit that reaches a tag of the enclosing element, a document with
     * errors or with a document type declaration, or an element that is no longer a single element afterwards, the
     * edited text is parsed as a whole. Either way the result has the same text, tokens and diagnostics as a [parse]
     * of the edited text with [xml]; the limits apply to the whole edited document, and an edited text longer than
     * `XmlLimits.maximumInputLength` ends with an `InputTooLong` diagnostic, like [parse].
     *
     * @param edits the edits to apply, in any order, with ranges into [text] that do not overlap.
     * @return the tree of the edited text, or this tree when [edits] is empty.
     * @throws IllegalArgumentException if two edits overlap, an edit reaches beyond [text], or the edited text would
     * be too long to be held in a string.
     * @since 1.0.0
     */
    public fun edit(edits: List<TextEdit>): XmlSyntaxTree = Reparse(this).apply(edits)

    /**
     * Returns the length of the text and the number of diagnostics, such as `XmlSyntaxTree(120 characters, 0
     * diagnostics)`; the text itself is never shown.
     *
     * @return a text for diagnostics.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlSyntaxTree(${text.length} characters, ${diagnostics.size} diagnostics)"

    /**
     * Creates syntax trees.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads [text] into its lossless syntax tree; never throws for malformed text.
         *
         * @param text the complete document; a leading byte order mark becomes a [XmlSyntaxKind.BYTE_ORDER_MARK]
         * token.
         * @param xml the reader whose configuration and limits apply, [Xml.Default] if none is given.
         * @return the syntax tree with its diagnostics.
         * @since 1.0.0
         */
        public fun parse(text: String, xml: Xml = Xml.Default): XmlSyntaxTree {
            val parsed = SyntaxParsing.parse(text, xml.configuration)
            return XmlSyntaxTree(text = text, green = parsed.root, diagnostics = parsed.diagnostics, xml = xml)
        }
    }
}
