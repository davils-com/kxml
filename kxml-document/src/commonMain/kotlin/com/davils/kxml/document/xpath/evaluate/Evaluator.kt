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

package com.davils.kxml.document.xpath.evaluate

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.navigation.NodeKind
import com.davils.kxml.document.xpath.XPathNodeKind
import com.davils.kxml.document.xpath.XPathValue
import com.davils.kxml.document.xpath.syntax.Axis
import com.davils.kxml.document.xpath.syntax.Expression
import com.davils.kxml.document.xpath.syntax.NodeTest
import com.davils.kxml.document.xpath.syntax.Step

private val principals: Map<Axis, XPathNodeKind> = mapOf(
    Axis.Attribute to XPathNodeKind.Attribute,
    Axis.Namespace to XPathNodeKind.Namespace,
)

internal class Evaluator(
    private val model: DocumentModel,
    limits: XmlLimits,
    private val variables: Map<XmlName, XPathValue>,
    val bounds: EvaluationBounds = EvaluationBounds(limits),
) {
    val nodes: NodeAccess = NodeAccess(model, bounds)
    val conversions: Conversions = Conversions(nodes)
    private val walker: AxisWalker = AxisWalker(nodes, bounds)
    private val comparisons: Comparisons = Comparisons(conversions, nodes, bounds)
    private val functions: Functions = Functions(this)

    fun evaluate(expression: Expression, focus: Focus): Value {
        bounds.charge()
        return when (expression) {
            is Expression.Or -> Value.Truth(expression.operands.any { operand -> truthOf(operand, focus) })
            is Expression.And -> Value.Truth(expression.operands.all { operand -> truthOf(operand, focus) })
            is Expression.Comparison -> comparison(expression, focus)
            is Expression.Arithmetic -> arithmetic(expression, focus)
            is Expression.Negation -> negation(expression, focus)
            is Expression.Union -> union(expression, focus)
            is Expression.Path -> Value.Nodes(path(expression, focus))
            is Expression.Filter -> Value.Nodes(filtered(nodesOf(expression.primary, focus), expression.predicates))
            is Expression.Literal -> Value.Text(expression.value)
            is Expression.Number -> Value.Number(expression.value)
            is Expression.Variable -> variable(expression)
            is Expression.Call -> functions.call(expression, focus)
        }
    }

    fun nodesOf(expression: Expression, focus: Focus): LongArray {
        val value = evaluate(expression, focus) as Value.Nodes
        return value.keys
    }

    fun stringOf(expression: Expression, focus: Focus): String = conversions.stringOf(evaluate(expression, focus))

    fun numberOf(expression: Expression, focus: Focus): Double = conversions.numberOf(evaluate(expression, focus))

    fun truthOf(expression: Expression, focus: Focus): Boolean = conversions.truthOf(evaluate(expression, focus))

    fun collector(): KeyCollector = KeyCollector(capacity = model.size, bounds = bounds)

    fun argumentOrContext(arguments: List<Expression>, focus: Focus): Value {
        val argument = arguments.firstOrNull() ?: return Value.Nodes(longArrayOf(focus.key))
        return evaluate(argument, focus)
    }

    private fun comparison(comparison: Expression.Comparison, focus: Focus): Value {
        var result = evaluate(comparison.first, focus)
        comparison.links.forEach { link ->
            val right = evaluate(link.operand, focus)
            result = Value.Truth(comparisons.holds(result, link.operator, right))
        }
        return result
    }

    private fun arithmetic(arithmetic: Expression.Arithmetic, focus: Focus): Value {
        var result = numberOf(arithmetic.first, focus)
        arithmetic.links.forEach { link ->
            result = link.operator.applied(left = result, right = numberOf(link.operand, focus))
        }
        return Value.Number(result)
    }

    private fun negation(negation: Expression.Negation, focus: Focus): Value {
        val number = numberOf(negation.operand, focus)
        if (negation.isNegated) return Value.Number(-number)
        return Value.Number(number)
    }

    private fun union(union: Expression.Union, focus: Focus): Value {
        val collector = collector()
        union.operands.forEach { operand -> collector.addAll(nodesOf(operand, focus)) }
        return Value.Nodes(collector.keys())
    }

    private fun path(path: Expression.Path, focus: Focus): LongArray {
        var context = startOf(path, focus)
        path.steps.forEach { step -> context = step(context, step) }
        return context
    }

    private fun startOf(path: Expression.Path, focus: Focus): LongArray {
        val base = path.base ?: return contextOf(path, focus)
        return nodesOf(base, focus)
    }

    private fun contextOf(path: Expression.Path, focus: Focus): LongArray {
        if (path.isAbsolute) return longArrayOf(NodeKeys.keyOf(DocumentModel.ROOT))
        return longArrayOf(focus.key)
    }

    private fun step(context: LongArray, step: Step): LongArray {
        val collector = collector()
        if (step.predicates.isEmpty()) {
            unfiltered(context, step, collector)
            return collector.keys()
        }
        context.forEach { key -> collector.addAll(filtered(matchesOf(key, step), step.predicates)) }
        return collector.keys()
    }

    private fun unfiltered(context: LongArray, step: Step, collector: KeyCollector) {
        val isDescending = step.axis == Axis.Descendant || step.axis == Axis.DescendantOrSelf
        var coveredEnd = -1
        context.forEach { key ->
            val ordinal = NodeKeys.ordinalOf(key)
            val isTreeNode = !NodeKeys.isNamespace(key) && model.kind(ordinal) != NodeKind.Attribute
            val isSpanning = isDescending && isTreeNode
            if (isSpanning && ordinal < coveredEnd) return@forEach
            walker.walk(step.axis, key) { candidate -> if (matches(candidate, step)) collector.add(candidate) }
            if (isSpanning) coveredEnd = maxOf(a = coveredEnd, b = model.end(ordinal))
        }
    }

    private fun matchesOf(key: Long, step: Step): LongArray {
        val found = mutableListOf<Long>()
        walker.walk(step.axis, key) { candidate -> if (matches(candidate, step)) found += candidate }
        bounds.results(found.size)
        return found.toLongArray()
    }

    private fun filtered(candidates: LongArray, predicates: List<Expression>): LongArray {
        var kept = candidates
        predicates.forEach { predicate -> kept = filtered(kept, predicate) }
        return kept
    }

    private fun filtered(candidates: LongArray, predicate: Expression): LongArray {
        val kept = mutableListOf<Long>()
        candidates.forEachIndexed { index, key ->
            val focus = Focus(key = key, position = index + 1, size = candidates.size)
            if (accepts(evaluate(predicate, focus), focus.position)) kept += key
        }
        return kept.toLongArray()
    }

    private fun accepts(value: Value, position: Int): Boolean {
        val number = value as? Value.Number ?: return conversions.truthOf(value)
        return number.value == position.toDouble()
    }

    private fun matches(key: Long, step: Step): Boolean {
        val kind = nodes.kindOf(key)
        val isPrincipal = kind == principalOf(step.axis)
        return when (val test = step.test) {
            NodeTest.AnyNode -> true
            NodeTest.Text -> kind == XPathNodeKind.Text
            NodeTest.Comment -> kind == XPathNodeKind.Comment
            is NodeTest.Instruction -> kind == XPathNodeKind.ProcessingInstruction && targets(key, test.target)
            NodeTest.AnyName -> isPrincipal
            is NodeTest.AnyLocal -> isPrincipal && nodes.nameOf(key)?.namespace == test.namespace
            is NodeTest.Name -> isPrincipal && named(key, test)
        }
    }

    private fun targets(key: Long, target: String?): Boolean = target == null || nodes.nameOf(key)?.local == target

    private fun named(key: Long, test: NodeTest.Name): Boolean {
        val name = nodes.nameOf(key) ?: return false
        return name.local == test.local && name.namespace == test.namespace
    }

    private fun principalOf(axis: Axis): XPathNodeKind = principals[axis] ?: XPathNodeKind.Element

    private fun variable(variable: Expression.Variable): Value {
        val bound = variables[variable.name]
            ?: throw IllegalArgumentException("The variable ${SafeText.quoted(variable.name.local)} is not bound")
        return when (bound) {
            is XPathValue.Text -> Value.Text(bound.value)
            is XPathValue.Number -> Value.Number(bound.value)
            is XPathValue.Truth -> Value.Truth(bound.isTrue)
            is XPathValue.NodeSet -> throw IllegalArgumentException("A variable holds a string, a number or a boolean")
        }
    }
}
