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
package com.davils.kxml.document.merge.trace

import com.davils.kxml.core.node.XmlNode

private const val TEXT_STEP: String = "text()"
private const val COMMENT_STEP: String = "comment()"
private const val INSTRUCTION_STEP: String = "processing-instruction()"

internal object OriginWriter {
    fun originsOf(root: TracedNode.Element): Map<String, String> {
        val origins = linkedMapOf<String, String>()
        written(root, "/${root.name.qualified}[1]", origins)
        return origins
    }

    private fun written(element: TracedNode.Element, path: String, origins: MutableMap<String, String>) {
        origins[path] = element.origin
        element.attributes.forEach { attribute -> origins["$path/@${attribute.name.qualified}"] = attribute.origin }
        val counts = mutableMapOf<String, Int>()
        element.children.forEach { child ->
            val step = stepOf(child)
            val position = counts.getOrElse(step) { 0 } + 1
            counts[step] = position
            val childPath = "$path/$step[$position]"
            val nested = child as? TracedNode.Element
            if (nested != null) written(nested, childPath, origins)
            if (nested == null) origins[childPath] = child.origin
        }
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
