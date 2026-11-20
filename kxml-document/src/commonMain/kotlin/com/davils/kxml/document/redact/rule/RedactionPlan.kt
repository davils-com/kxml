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

import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.navigation.NodeSet

internal class RedactionPlan(
    private val model: DocumentModel,
    private val whole: NodeSet,
    val values: Map<Int, String>,
) {
    private val inside: BooleanArray = BooleanArray(model.size)

    init {
        val ends = ArrayDeque<Int>()
        for (node in 0..<model.size) {
            while (ends.isNotEmpty() && ends.last() <= node) ends.removeLast()
            inside[node] = ends.isNotEmpty()
            if (node in whole && model.isElement(node)) ends.addLast(model.end(node))
        }
    }

    fun isWhole(node: Int): Boolean = node in whole

    fun isInside(node: Int): Boolean = inside[node]

    fun isRedacted(node: Int): Boolean = isWhole(node) || isInside(node)
}
