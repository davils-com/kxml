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
package com.davils.kxml.document.redact.rule

import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.syntax.XmlSyntaxKind
import com.davils.kxml.core.syntax.XmlSyntaxNode
import com.davils.kxml.core.syntax.XmlSyntaxToken
import com.davils.kxml.core.syntax.XmlSyntaxTree
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.c14n.XmlCanonicalization
import com.davils.kxml.document.c14n.XmlCanonicalizer
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.navigation.NodeKind
import com.davils.kxml.document.redact.XmlRedactionRefusal
import com.davils.kxml.document.redact.XmlRedactionResult

private val predefined: Set<String> = setOf("lt", "gt", "amp", "apos", "quot")
private val editOrder: Comparator<TextEdit> =
    compareBy<TextEdit> { edit -> edit.range.start }.thenByDescending { edit -> edit.range.end }
private const val INSTRUCTION_CLOSE: Int = 2
private const val COMMENT_CLOSE: Int = 3
private val literalForbidden: Set<Char> = setOf('&', '%', '<', '"', '\'')

internal class SyntaxRedaction(private val rules: RedactionRules) {
    private val canonicalizer: XmlCanonicalizer =
        XmlCanonicalizer(XmlCanonicalization.Canonical10WithComments, limits = rules.limits)

    fun redacted(tree: XmlSyntaxTree): XmlRedactionResult = try {
        attempted(tree)
    } catch (_: XmlDocumentLimitException) {
        XmlRedactionResult.Refused(XmlRedactionRefusal.LimitExceeded)
    } catch (_: UndecidableException) {
        XmlRedactionResult.Refused(XmlRedactionRefusal.Undecidable)
    }

    private fun attempted(tree: XmlSyntaxTree): XmlRedactionResult {
        val parsed = tree.compose() as? XmlParseResult.Parsed
            ?: return XmlRedactionResult.Refused(XmlRedactionRefusal.InvalidDocument)
        val model = DocumentModel.of(parsed.document)
        val plan = rules.planOf(model)
        val edits = EditCollector(tree, model, plan).edits()
        val redacted = tree.edit(edits)
        val composed = (redacted.compose() as? XmlParseResult.Parsed).required()
        if (!isRedacted(composed.document)) throw UndecidableException()
        return XmlRedactionResult.Redacted(edits, redacted)
    }

    private fun isRedacted(document: XmlDocument): Boolean {
        val model = DocumentModel.of(document)
        val again = ModelWriter(model, rules.planOf(model), rules.replacement).documentOf(document)
        return canonicalizer.canonicalize(again).contentEquals(canonicalizer.canonicalize(document))
    }

    private inner class EditCollector(
        tree: XmlSyntaxTree,
        private val model: DocumentModel,
        private val plan: RedactionPlan,
    ) {
        private val root: XmlSyntaxNode = tree.root
        private val edits: MutableList<TextEdit> = mutableListOf()

        fun edits(): List<TextEdit> {
            for (node in 0..<model.size) {
                valueAt(node)?.let { value -> edited(node, value) }
            }
            return withoutNested(edits.sortedWith(editOrder))
        }

        private fun valueAt(node: Int): String? {
            if (plan.isInside(node)) return null
            return plan.values[node] ?: rules.replacement.takeIf { plan.isWhole(node) }
        }

        private fun edited(node: Int, value: String) {
            val range = model.range(node).required()
            when (model.kind(node)) {
                NodeKind.Element, NodeKind.Root -> {
                    elementEdited(node, range)
                }

                NodeKind.Attribute -> {
                    attributeEdited(range, value)
                }

                NodeKind.Text -> {
                    textEdited(range, value)
                }

                NodeKind.Comment -> {
                    tokenEdited(
                        range = range,
                        kind = XmlSyntaxKind.COMMENT,
                        tokenKind = XmlSyntaxKind.COMMENT_TEXT,
                        value = value,
                    )
                }

                NodeKind.ProcessingInstruction -> {
                    tokenEdited(
                        range = range,
                        kind = XmlSyntaxKind.PROCESSING_INSTRUCTION,
                        tokenKind = XmlSyntaxKind.INSTRUCTION_TEXT,
                        value = value,
                    )
                }
            }
        }

        private fun elementEdited(node: Int, range: TextRange) {
            val element = syntaxAt(range, XmlSyntaxKind.ELEMENT)
            model.attributes(node).forEach { attribute ->
                attributeEdited(model.range(attribute).required(), rules.replacement)
            }
            val start = element.child(XmlSyntaxKind.START_TAG).required()
            val end = element.child(XmlSyntaxKind.END_TAG)
            val escaped = escapedText(rules.replacement)
            referencesIn(element)
            if (end != null) {
                edits += TextEdit(TextRange(start = start.range.end, end = end.range.start), escaped)
                return
            }
            val close = start.token(XmlSyntaxKind.EMPTY_TAG_CLOSE).required()
            val name = start.token(XmlSyntaxKind.NAME).required()
            edits += TextEdit(close.range, ">$escaped</${name.text}>")
        }

        private fun attributeEdited(range: TextRange, value: String) {
            val attribute = syntaxAt(range, XmlSyntaxKind.ATTRIBUTE)
            val literal = attribute.child(XmlSyntaxKind.ATTRIBUTE_VALUE).required()
            val quotes = literal.children.filterIsInstance<XmlSyntaxToken>().filter { it.kind == XmlSyntaxKind.QUOTE }
            val inner = TextRange(start = quotes.first().range.end, end = quotes.last().range.start)
            referencesIn(literal)
            edits += TextEdit(inner, escapedAttribute(value, quotes.first().text.single()))
        }

        private fun textEdited(range: TextRange, value: String) {
            val covering = root.coveringNode(range)
            referencesIn(covering, range)
            edits += TextEdit(range, escapedText(value))
        }

        private fun tokenEdited(range: TextRange, kind: XmlSyntaxKind, tokenKind: XmlSyntaxKind, value: String) {
            val token = syntaxAt(range, kind).token(tokenKind)
            val isInstruction = kind == XmlSyntaxKind.PROCESSING_INSTRUCTION
            val point = range.end - (INSTRUCTION_CLOSE.takeIf { isInstruction } ?: COMMENT_CLOSE)
            val missing = TextEdit(TextRange(start = point, end = point), " $value".takeIf { isInstruction } ?: value)
            edits += token?.let { found -> TextEdit(found.range, value) } ?: missing
        }

        private fun referencesIn(node: XmlSyntaxNode, range: TextRange = node.range) {
            val references = node.tokens.filter { token ->
                token.kind == XmlSyntaxKind.ENTITY_REFERENCE &&
                    token.range.start >= range.start &&
                    token.range.end <= range.end
            }
            val names = references.map { token -> token.text.removePrefix("&").removeSuffix(";") }
            names.filter { name -> name !in predefined }.forEach(::declarationEdited)
        }

        private fun declarationEdited(name: String) {
            val found = root.descendants.firstOrNull { node ->
                node.kind == XmlSyntaxKind.ENTITY_DECLARATION &&
                    node.token(XmlSyntaxKind.PERCENT) == null &&
                    node.token(XmlSyntaxKind.NAME)?.text == name
            }
            val declaration = found.required()
            val literal = declaration.token(XmlSyntaxKind.LITERAL).required()
            val content = literal.text.drop(1).dropLast(1)
            val isPlain =
                content.none { char -> char in literalForbidden } && rules.replacement.none { it in literalForbidden }
            if (!isPlain) throw UndecidableException()
            val inner = TextRange(start = literal.range.start + 1, end = literal.range.end - 1)
            edits += TextEdit(inner, rules.replacement)
        }

        private fun syntaxAt(range: TextRange, kind: XmlSyntaxKind): XmlSyntaxNode {
            val covering = root.coveringNode(range)
            val sameRange = (sequenceOf(covering) + covering.ancestors).takeWhile { node -> node.range == range }
            return sameRange.firstOrNull { node -> node.kind == kind }.required()
        }

        private fun withoutNested(sorted: List<TextEdit>): List<TextEdit> {
            val kept = mutableListOf<TextEdit>()
            var end = 0
            var previous: TextRange? = null
            sorted.forEach { edit ->
                val isAfter = edit.range.start >= end && edit.range != previous
                if (isAfter) kept += edit
                if (isAfter) end = edit.range.end
                previous = edit.range
            }
            return kept
        }
    }

    private fun <T : Any> T?.required(): T = this ?: throw UndecidableException()

    private class UndecidableException : Exception()
}
