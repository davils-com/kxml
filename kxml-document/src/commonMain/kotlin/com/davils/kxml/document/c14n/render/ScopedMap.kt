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

package com.davils.kxml.document.c14n.render

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap

internal class ScopedMap {
    private val values: OrderedMap<String, String> = OrderedMap()
    private val changes: ArrayDeque<Change> = ArrayDeque()

    operator fun get(key: String): String? = values[key]

    fun open() {
        changes.addLast(Change.Mark)
    }

    fun set(key: String, value: String) {
        changes.addLast(Change.Set(key, values[key]))
        values[key] = value
    }

    fun entries(): List<Pair<String, String>> = values.entries

    fun close() {
        var change = changes.removeLast()
        while (change is Change.Set) {
            restore(change)
            change = changes.removeLast()
        }
    }

    private fun restore(change: Change.Set) {
        val previous = change.previous
        if (previous == null) values.remove(change.key)
        previous?.let { value -> values[change.key] = value }
    }

    private sealed interface Change {
        data object Mark : Change

        data class Set(val key: String, val previous: String?) : Change
    }
}
