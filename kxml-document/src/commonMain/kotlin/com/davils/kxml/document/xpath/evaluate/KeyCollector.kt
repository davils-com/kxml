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
import com.davils.kore.core.collection.OrderedSet
import com.davils.kxml.document.navigation.NodeSet

private const val NODES_PER_WORD_SHIFT: Int = 6

internal class KeyCollector(private val capacity: Int, private val bounds: EvaluationBounds) {
    private val densest: Int = (capacity ushr NODES_PER_WORD_SHIFT) + 1
    private val sparse: OrderedSet<Int> = OrderedSet()
    private var dense: NodeSet? = null
    private val namespaces: OrderedSet<Long> = OrderedSet()
    private var count: Int = 0

    fun add(key: Long) {
        val isNew = addNew(key)
        if (!isNew) return
        count += 1
        bounds.results(count)
    }

    fun addAll(keys: LongArray) {
        keys.forEach(::add)
    }

    fun keys(): LongArray {
        val keys = LongArray(count)
        var filled = 0
        val pending = namespaces.sorted().iterator()
        var namespace = pending.nextOrNull()
        val ordinals = sparse.sorted().asSequence() + dense?.nodes().orEmpty()
        ordinals.forEach { ordinal ->
            val key = NodeKeys.keyOf(ordinal)
            while (namespace != null && namespace < key) {
                keys[filled] = namespace
                filled += 1
                namespace = pending.nextOrNull()
            }
            keys[filled] = key
            filled += 1
        }
        while (namespace != null) {
            keys[filled] = namespace
            filled += 1
            namespace = pending.nextOrNull()
        }
        return keys
    }

    private fun addNew(key: Long): Boolean {
        if (NodeKeys.isNamespace(key)) return namespaces.add(key)
        val ordinal = NodeKeys.ordinalOf(key)
        return dense?.let { nodes -> addedDense(nodes, ordinal) } ?: addedSparse(ordinal)
    }

    private fun addedDense(nodes: NodeSet, ordinal: Int): Boolean {
        val isNew = ordinal !in nodes
        nodes.add(ordinal)
        return isNew
    }

    private fun addedSparse(ordinal: Int): Boolean {
        val isNew = sparse.add(ordinal)
        if (sparse.size > densest) densified()
        return isNew
    }

    private fun densified() {
        val nodes = NodeSet(capacity)
        sparse.forEach(nodes::add)
        sparse.clear()
        dense = nodes
    }

    private fun Iterator<Long>.nextOrNull(): Long? {
        if (!hasNext()) return null
        return next()
    }
}
