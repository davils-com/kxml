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

import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.navigation.NodeKind
import com.davils.kxml.document.navigation.NodeSet
import com.davils.kxml.document.xpath.evaluate.Evaluator
import com.davils.kxml.document.xpath.evaluate.Focus
import com.davils.kxml.document.xpath.evaluate.NodeKeys
import com.davils.kxml.document.xpath.syntax.Expression

private const val STRING_LIMIT: String = "maximumQueryStringLength"
private const val PREDICATE_FAILED: String = "A predicate of the redaction failed"

internal class RedactionRules(
    val replacement: String,
    val limits: XmlLimits,
    private val paths: List<Expression>,
    private val elements: List<(XmlName) -> Boolean>,
    private val attributes: List<(XmlName) -> Boolean>,
    private val values: List<Regex>,
    private val comments: List<Regex>,
) {
    fun planOf(model: DocumentModel): RedactionPlan {
        val whole = NodeSet(model.size)
        paths.forEach { path -> selected(model, path).forEach(whole::add) }
        for (node in 0..<model.size) {
            val name = model.name(node)
            val kind = model.kind(node)
            val predicates =
                elements.takeIf { kind == NodeKind.Element } ?: attributes.takeIf { kind == NodeKind.Attribute }
            if (name != null && predicates.orEmpty().any { predicate -> accepts(predicate, name) }) whole.add(node)
        }
        val unpatterned = RedactionPlan(model, whole, emptyMap())
        val patterned = mutableMapOf<Int, String>()
        for (node in 0..<model.size) {
            val value = model.value(node)
            val patterns = patternsOf(model.kind(node))
            val isOpen = value != null && patterns.isNotEmpty() && !unpatterned.isRedacted(node)
            val replaced = value?.takeIf { isOpen }?.let { text -> replaced(text, patterns) }
            if (replaced != null && replaced != value) patterned[node] = replaced
        }
        return RedactionPlan(model, whole, patterned)
    }

    private fun selected(model: DocumentModel, path: Expression): List<Int> {
        val evaluator = Evaluator(model = model, limits = limits, variables = emptyMap())
        val focus = Focus(key = NodeKeys.keyOf(DocumentModel.ROOT), position = 1, size = 1)
        val keys = evaluator.nodesOf(path, focus).filterNot(NodeKeys::isNamespace)
        val ordinals = keys.map(NodeKeys::ordinalOf)
        val root = model.children(DocumentModel.ROOT).first(model::isElement)
        return ordinals.map { ordinal -> root.takeIf { ordinal == DocumentModel.ROOT } ?: ordinal }
    }

    private fun patternsOf(kind: NodeKind): List<Regex> {
        val isValue = kind == NodeKind.Text || kind == NodeKind.Attribute
        return values.takeIf { isValue } ?: comments.takeIf { kind == NodeKind.Comment }.orEmpty()
    }

    private fun replaced(value: String, patterns: List<Regex>): String {
        if (value.length > limits.maximumQueryStringLength) throw XmlDocumentLimitException(STRING_LIMIT)
        val escaped = Regex.escapeReplacement(replacement)
        var replaced = value
        patterns.forEach { pattern -> replaced = pattern.replace(input = replaced, replacement = escaped) }
        return replaced
    }

    private fun accepts(predicate: (XmlName) -> Boolean, name: XmlName): Boolean =
        runCatching { predicate(name) }.getOrElse { throw IllegalStateException(PREDICATE_FAILED) }
}
