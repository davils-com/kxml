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
package com.davils.kxml.document.patch.apply

import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.patch.XmlPatchError
import com.davils.kxml.document.patch.XmlPatchErrorCode
import com.davils.kxml.document.patch.XmlPatchOperation
import com.davils.kxml.document.patch.XmlPatchResult
import com.davils.kxml.document.xpath.evaluate.EvaluationBounds
import com.davils.kxml.document.xpath.evaluate.Evaluator
import com.davils.kxml.document.xpath.evaluate.Focus
import com.davils.kxml.document.xpath.evaluate.NodeKeys
import com.davils.kxml.document.xpath.parse.QueryParsing
import com.davils.kxml.document.xpath.parse.QueryReading
import com.davils.kxml.document.xpath.syntax.ValueType

private const val DEFAULT_PREFIX: String = ""
private const val UNDECLARED: String = "the prefix is not declared"

internal class PatchApplier(private val limits: XmlLimits) {
    private val bounds: EvaluationBounds = EvaluationBounds(limits)

    fun applied(document: XmlDocument, operations: List<XmlPatchOperation>): XmlPatchResult {
        var current = document
        var index = 0
        return try {
            operations.forEach { operation ->
                current = appliedOne(current, operation)
                index += 1
            }
            DocumentModel.of(current).requireShapeWithin(limits)
            XmlPatchResult.Applied(current)
        } catch (failure: PatchFailure) {
            XmlPatchResult.Failed(XmlPatchError(code = failure.code, operation = index, message = failure.reason))
        }
    }

    private fun appliedOne(document: XmlDocument, operation: XmlPatchOperation): XmlDocument {
        val model = DocumentModel.of(document)
        bounds.charge(model.size.toLong())
        model.requireShapeWithin(limits)
        val evaluator = Evaluator(model = model, limits = limits, variables = emptyMap(), bounds = bounds)
        val target = located(evaluator, operation)
        val context = OperationContext(model = model, editor = TreeEditor(document, model, bounds), charge = ::charged)
        return when (operation) {
            is XmlPatchOperation.Add -> Additions(context).added(target, operation)
            is XmlPatchOperation.AddAttribute -> Additions(context).attributeAdded(target, operation)
            is XmlPatchOperation.AddNamespace -> Additions(context).namespaceAdded(target, operation)
            is XmlPatchOperation.Replace -> Replacements(context).replaced(target, operation)
            is XmlPatchOperation.Remove -> Removals(context).removed(target, operation)
        }
    }

    private fun located(evaluator: Evaluator, operation: XmlPatchOperation): Target {
        val namespaces = operation.namespaces
        val reading = QueryParsing.read(
            expression = operation.selector,
            namespaces = namespaces::get,
            limits = limits,
            elementNamespace = namespaces[DEFAULT_PREFIX],
        )
        val parsed = (reading as? QueryReading.Read)?.expression
        val expression = parsed?.takeIf { found -> found.type == ValueType.NodeSet } ?: throw refusal(reading)
        val focus = Focus(key = NodeKeys.keyOf(DocumentModel.ROOT), position = 1, size = 1)
        val keys = evaluator.nodesOf(expression, focus)
        val key = keys.singleOrNull() ?: throw PatchFailure(XmlPatchErrorCode.UnlocatedNode, NOT_ONE)
        if (!NodeKeys.isNamespace(key)) return Target.Node(NodeKeys.ordinalOf(key))
        val binding = evaluator.nodes.namespaceOf(key)
        return Target.Namespace(element = NodeKeys.ordinalOf(key), prefix = binding.prefix ?: DEFAULT_PREFIX)
    }

    private fun refusal(reading: QueryReading): PatchFailure {
        val isUndeclared = (reading as? QueryReading.Refused)?.reason == UNDECLARED
        val code = XmlPatchErrorCode.InvalidNamespacePrefix.takeIf { isUndeclared }
            ?: XmlPatchErrorCode.InvalidAttributeValue
        return PatchFailure(code, "the selector is no XPath 1.0 expression of a node-set")
    }

    private fun charged(steps: Int) {
        bounds.charge(steps.toLong())
    }

    private companion object {
        const val NOT_ONE: String = "the selector locates no node or more than one"
    }
}
