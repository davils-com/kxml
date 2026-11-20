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
package com.davils.kxml.document.xpath.evaluate

import com.davils.kxml.core.name.XmlName
import com.davils.kxml.document.xpath.syntax.CoreFunction
import com.davils.kxml.document.xpath.syntax.Expression

internal class NodeFunctions(private val evaluator: Evaluator) {
    private val nodes: NodeAccess = evaluator.nodes
    private val bounds: EvaluationBounds = evaluator.bounds

    val handlers: Map<CoreFunction, FunctionHandler> = mapOf(
        CoreFunction.Last to FunctionHandler { _, focus -> Value.Number(focus.size.toDouble()) },
        CoreFunction.Position to FunctionHandler { _, focus -> Value.Number(focus.position.toDouble()) },
        CoreFunction.Count to FunctionHandler { arguments, focus -> count(arguments, focus) },
        CoreFunction.Id to FunctionHandler { arguments, focus -> id(arguments, focus) },
        CoreFunction.LocalName to FunctionHandler { arguments, focus ->
            Value.Text(nameOf(arguments, focus)?.local.orEmpty())
        },
        CoreFunction.NamespaceUri to FunctionHandler { arguments, focus ->
            Value.Text(nameOf(arguments, focus)?.namespace.orEmpty())
        },
        CoreFunction.Name to FunctionHandler { arguments, focus ->
            Value.Text(nameOf(arguments, focus)?.qualified.orEmpty())
        },
        CoreFunction.Lang to FunctionHandler { arguments, focus -> Value.Truth(isLang(arguments, focus)) },
        CoreFunction.Sum to FunctionHandler { arguments, focus -> sum(arguments, focus) },
    )

    private fun count(arguments: List<Expression>, focus: Focus): Value {
        val keys = evaluator.nodesOf(arguments.single(), focus)
        return Value.Number(keys.size.toDouble())
    }

    private fun id(arguments: List<Expression>, focus: Focus): Value {
        val value = evaluator.evaluate(arguments.single(), focus)
        val keys = (value as? Value.Nodes)?.keys
        val texts = keys?.map(nodes::stringValueOf) ?: listOf(evaluator.conversions.stringOf(value))
        val collector = evaluator.collector()
        texts.forEach { text ->
            val tokens = normalizedSpaceOf(text).split(' ')
            tokens.filter(String::isNotEmpty).forEach { token ->
                bounds.charge()
                val element = nodes.elementById(token)
                if (element != null) collector.add(NodeKeys.keyOf(element))
            }
        }
        return Value.Nodes(collector.keys())
    }

    private fun nameOf(arguments: List<Expression>, focus: Focus): XmlName? {
        val argument = arguments.firstOrNull()
        val keys = argument?.let { found -> evaluator.nodesOf(found, focus) } ?: longArrayOf(focus.key)
        return keys.firstOrNull()?.let(nodes::nameOf)
    }

    private fun isLang(arguments: List<Expression>, focus: Focus): Boolean {
        val wanted = evaluator.stringOf(arguments.single(), focus).lowercase()
        val lang = nodes.langOf(focus.key)?.lowercase() ?: return false
        return lang == wanted || lang.startsWith("$wanted-")
    }

    private fun sum(arguments: List<Expression>, focus: Focus): Value {
        val keys = evaluator.nodesOf(arguments.single(), focus)
        val total = keys.sumOf { key ->
            bounds.charge()
            numberIn(nodes.stringValueOf(key))
        }
        return Value.Number(total)
    }
}
