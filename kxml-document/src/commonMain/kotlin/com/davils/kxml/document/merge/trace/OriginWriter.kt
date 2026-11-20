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

package com.davils.kxml.document.merge.trace

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget
import com.davils.kore.core.collection.OrderedMap
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.document.XmlDocumentLimitException

private const val TEXT_STEP: String = "text()"
private const val COMMENT_STEP: String = "comment()"
private const val INSTRUCTION_STEP: String = "processing-instruction()"
private const val OUTPUT_LIMIT: String = "maximumOutputLength"

internal class OriginWriter(limits: XmlLimits) {
    private val budget: WorkBudget = WorkBudget(maximum = limits.maximumOutputLength.toLong(), limit = OUTPUT_LIMIT)
    private val origins: OrderedMap<String, String> = OrderedMap()

    fun originsOf(root: TracedNode.Element): OrderedMap<String, String> {
        written(root, "/${root.name.qualified}[1]")
        return origins
    }

    private fun written(element: TracedNode.Element, path: String) {
        recorded(path, element.origin)
        element.attributes.forEach { attribute ->
            recorded(
                path = "$path/@${attribute.name.qualified}",
                origin = attribute.origin,
            )
        }
        val counts = OrderedMap<String, Int>()
        element.children.forEach { child ->
            val step = stepOf(child)
            val position = (counts[step] ?: 0) + 1
            counts[step] = position
            val childPath = "$path/$step[$position]"
            val nested = child as? TracedNode.Element
            if (nested != null) written(nested, childPath)
            if (nested == null) recorded(path = childPath, origin = child.origin)
        }
    }

    private fun recorded(path: String, origin: String) {
        if (!budget.tryCharge(path.length.toLong())) throw XmlDocumentLimitException(OUTPUT_LIMIT)
        origins[path] = origin
    }

    private fun stepOf(node: TracedNode): String {
        val element = node as? TracedNode.Element
        if (element != null) return element.name.qualified
        val leaf = (node as TracedNode.Leaf).node
        return COMMENT_STEP.takeIf { leaf is XmlNode.Comment }
            ?: INSTRUCTION_STEP.takeIf { leaf is XmlNode.ProcessingInstruction }
            ?: TEXT_STEP
    }
}
