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

package com.davils.kxml.document.navigation

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap

internal class PrefixScope private constructor(
    private val own: OrderedMap<String, String>,
    private val parent: PrefixScope?,
) : AbstractMap<String, String>() {
    private val pairs: List<Pair<String, String>> by lazy(::flattened)

    override val size: Int get() = pairs.size

    override val entries: Set<Map.Entry<String, String>> get() = EntrySet(pairs)

    override fun get(key: String): String? {
        val uri = own[key] ?: parent?.get(key)
        return uri?.takeIf(String::isNotEmpty)
    }

    override fun containsKey(key: String): Boolean = get(key) != null

    fun with(added: Iterable<Pair<String, String>>): PrefixScope {
        val layer = OrderedMap<String, String>()
        added.forEach { (prefix, uri) -> layer[prefix] = uri }
        return PrefixScope(layer, parent = this)
    }

    private fun flattened(): List<Pair<String, String>> {
        val layers = generateSequence(this) { scope -> scope.parent }.toList()
        val merged = OrderedMap<String, String>()
        layers.asReversed().forEach { scope -> scope.own.entries.forEach { (prefix, uri) -> merged[prefix] = uri } }
        return merged.entries.filter { (_, uri) -> uri.isNotEmpty() }
    }

    companion object {
        val EMPTY: PrefixScope = PrefixScope(OrderedMap(), parent = null)

        fun of(map: Map<String, String>): PrefixScope =
            map as? PrefixScope ?: EMPTY.with(map.entries.map { entry -> entry.key to entry.value })
    }

    private class EntrySet(pairs: List<Pair<String, String>>) : AbstractSet<Map.Entry<String, String>>() {
        private val members: List<Map.Entry<String, String>> = pairs.map { (prefix, uri) ->
            Entry(
                key = prefix,
                value = uri,
            )
        }

        override val size: Int get() = members.size

        override fun iterator(): Iterator<Map.Entry<String, String>> = members.iterator()
    }

    private class Entry(override val key: String, override val value: String) : Map.Entry<String, String> {
        override fun equals(other: Any?): Boolean = other is Map.Entry<*, *> && key == other.key && value == other.value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()
    }
}
