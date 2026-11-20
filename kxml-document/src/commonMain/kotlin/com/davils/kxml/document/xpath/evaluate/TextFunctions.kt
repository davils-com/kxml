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

import com.davils.kxml.document.internal.appendCode
import com.davils.kxml.document.internal.codePointCountOf
import com.davils.kxml.document.internal.codePointsOf
import com.davils.kxml.document.xpath.syntax.CoreFunction
import com.davils.kxml.document.xpath.syntax.Expression

internal class TextFunctions(private val evaluator: Evaluator) {
    private val bounds: EvaluationBounds = evaluator.bounds

    val handlers: Map<CoreFunction, FunctionHandler> = mapOf(
        CoreFunction.String to FunctionHandler { arguments, focus -> Value.Text(contextText(arguments, focus)) },
        CoreFunction.Concat to FunctionHandler { arguments, focus -> concat(arguments, focus) },
        CoreFunction.StartsWith to FunctionHandler { arguments, focus ->
            val (text, prefix) = texts(arguments, focus)
            Value.Truth(text.startsWith(prefix))
        },
        CoreFunction.Contains to FunctionHandler { arguments, focus ->
            val (text, pattern) = texts(arguments, focus)
            Value.Truth(indexIn(text, pattern) >= 0)
        },
        CoreFunction.SubstringBefore to FunctionHandler { arguments, focus -> before(arguments, focus) },
        CoreFunction.SubstringAfter to FunctionHandler { arguments, focus -> after(arguments, focus) },
        CoreFunction.Substring to FunctionHandler { arguments, focus -> substring(arguments, focus) },
        CoreFunction.StringLength to FunctionHandler { arguments, focus ->
            val text = contextText(arguments, focus)
            Value.Number(text.codePointCountOf().toDouble())
        },
        CoreFunction.NormalizeSpace to FunctionHandler { arguments, focus ->
            Value.Text(normalizedSpaceOf(contextText(arguments, focus)))
        },
        CoreFunction.Translate to FunctionHandler { arguments, focus -> translate(arguments, focus) },
    )

    private fun contextText(arguments: List<Expression>, focus: Focus): String {
        val value = evaluator.argumentOrContext(arguments, focus)
        val text = evaluator.conversions.stringOf(value)
        bounds.copied(text.length)
        return text
    }

    private fun texts(arguments: List<Expression>, focus: Focus): Pair<String, String> {
        val first = evaluator.stringOf(arguments[0], focus)
        val second = evaluator.stringOf(arguments[1], focus)
        bounds.copied(first.length + second.length)
        return first to second
    }

    private fun concat(arguments: List<Expression>, focus: Focus): Value {
        val result = StringBuilder()
        arguments.forEach { argument ->
            val text = evaluator.stringOf(argument, focus)
            bounds.length(result.length + text.length)
            bounds.copied(text.length)
            result.append(text)
        }
        return Value.Text(result.toString())
    }

    private fun before(arguments: List<Expression>, focus: Focus): Value {
        val (text, pattern) = texts(arguments, focus)
        val index = indexIn(text, pattern)
        if (index < 0) return Value.Text("")
        return Value.Text(text.substring(startIndex = 0, endIndex = index))
    }

    private fun after(arguments: List<Expression>, focus: Focus): Value {
        val (text, pattern) = texts(arguments, focus)
        val index = indexIn(text, pattern)
        if (index < 0) return Value.Text("")
        return Value.Text(text.substring(startIndex = index + pattern.length))
    }

    private fun substring(arguments: List<Expression>, focus: Focus): Value {
        val text = evaluator.stringOf(arguments[0], focus)
        bounds.copied(text.length)
        val codes = text.codePointsOf()
        val start = roundOf(evaluator.numberOf(arguments[1], focus))
        val length = arguments.getOrNull(2)?.let { argument -> roundOf(evaluator.numberOf(argument, focus)) }
        val end = start + (length ?: Double.POSITIVE_INFINITY)
        val from = maxOf(a = start, b = 1.0)
        val until = minOf(a = end, b = codes.size + 1.0)
        if (!(from < until)) return Value.Text("")
        val result = StringBuilder()
        for (index in from.toInt() - 1..<until.toInt() - 1) result.appendCode(codes[index])
        return Value.Text(result.toString())
    }

    private fun translate(arguments: List<Expression>, focus: Focus): Value {
        val text = evaluator.stringOf(arguments[0], focus)
        val from = evaluator.stringOf(arguments[1], focus)
        val to = evaluator.stringOf(arguments[2], focus)
        bounds.copied(text.length + from.length + to.length)
        return Value.Text(translated(text, from = from, to = to))
    }
}
