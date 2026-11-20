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

package com.davils.kxml.core.syntax.format

import com.davils.kore.core.text.TextEdit
import com.davils.kxml.core.emit.XmlEmitOptions
import com.davils.kxml.core.syntax.XmlSyntaxTree

/**
 * Formats the layout of XML documents without changing what they mean.
 *
 * The formatter puts every child of an element with element-only content on a line of its own, indented by
 * [XmlEmitOptions.indent] spaces per level and separated by [XmlEmitOptions.lineBreak], puts the declarations, comments
 * and processing instructions around the root element on lines of their own and ends the document with one line
 * break; an indent of 0 writes them without any whitespace. It changes only whitespace between the elements, comments
 * and processing instructions of element-only content and around the root element. It never touches an element whose
 * content holds text, a reference or a CDATA section, mixed content, nor anything under `xml:space="preserve"`, whose
 * whitespace is significant, and it never changes tags, attributes or the order of anything.
 * [XmlEmitOptions.isEmptyElementCollapsed] does not apply.
 *
 * Before it returns, it reads the original and the formatted text into documents and refuses with
 * [XmlFormatRefusal.MeaningChanged] unless both are the same apart from whitespace between the elements of
 * element-only content, so formatting fails closed. A tree with errors is refused with [XmlFormatRefusal.SyntaxErrors].
 * The formatted text is parsed again through [XmlSyntaxTree.edit] with the configuration of the tree, and a result
 * longer than `XmlLimits.maximumInputLength` is refused with [XmlFormatRefusal.OutputTooLong] before it is built.
 *
 * A formatter is immutable and safe to share between threads. Its work grows linearly with the length of the text.
 *
 * @property options how the formatter lays out documents.
 * @since 1.0.0
 */
public class XmlFormatter(public val options: XmlEmitOptions = XmlEmitOptions.Default) {
    /**
     * Formats [tree].
     *
     * @param tree the tree to format.
     * @return the formatted tree with the edits that made it, or why the formatter refused.
     * @since 1.0.0
     */
    public fun format(tree: XmlSyntaxTree): XmlFormatResult {
        val edits = tree.takeIf(XmlSyntaxTree::isValid)?.let { valid -> Layout(valid, options).edits() }
        val length = tree.text.length + edits.orEmpty().sumOf(TextEdit::delta)
        val isFitting = length <= tree.xml.configuration.limits.maximumInputLength
        val refusal = XmlFormatRefusal.SyntaxErrors.takeIf { edits == null }
            ?: XmlFormatRefusal.OutputTooLong.takeUnless { isFitting }
        return refusal?.let(XmlFormatResult::Refused) ?: formatted(tree, edits.orEmpty())
    }

    /**
     * Returns the options, such as `XmlFormatter(XmlEmitOptions(indent=2, ...))`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlFormatter($options)"

    private fun formatted(tree: XmlSyntaxTree, edits: List<TextEdit>): XmlFormatResult {
        val formatted = tree.edit(edits)
        if (!isSameInfoset(tree, formatted)) return XmlFormatResult.Refused(XmlFormatRefusal.MeaningChanged)
        return XmlFormatResult.Formatted(edits, formatted)
    }
}
