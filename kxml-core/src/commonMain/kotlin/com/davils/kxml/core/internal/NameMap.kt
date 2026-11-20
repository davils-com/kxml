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

package com.davils.kxml.core.internal

internal class NameMap<V : Any> {
    private var index = OrderedIndex<String>()
    private val keys = ArrayList<String>()
    private val values = ArrayList<V?>()

    var size: Int = 0
        private set

    val names: List<String>
        get() = keys.filterIndexed { ordinal, _ -> values[ordinal] != null }

    val entries: List<Pair<String, V>>
        get() = keys.indices.mapNotNull { ordinal -> values[ordinal]?.let { value -> keys[ordinal] to value } }

    operator fun get(name: String): V? = index.ordinalOf(name)?.let(values::get)

    operator fun contains(name: String): Boolean = get(name) != null

    operator fun set(name: String, value: V) {
        val ordinal = index.ordinalFor(name)
        if (ordinal == keys.size) {
            keys.add(name)
            values.add(null)
        }
        if (values[ordinal] == null) size++
        values[ordinal] = value
    }

    fun getOrPut(name: String, value: () -> V): V {
        val found = get(name)
        if (found != null) return found
        val created = value()
        set(name, created)
        return created
    }

    fun remove(name: String) {
        val ordinal = index.ordinalOf(name) ?: return
        if (values[ordinal] != null) size--
        values[ordinal] = null
    }

    fun clear() {
        index = OrderedIndex()
        keys.clear()
        values.clear()
        size = 0
    }
}
