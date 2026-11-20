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

package com.davils.kxml.document.navigation

private const val BITS: Int = 64
private const val SHIFT: Int = 6

internal class NodeSet(private val capacity: Int) {
    private val words = LongArray(capacity + BITS - 1 ushr SHIFT)

    val size: Int get() = words.sumOf { word -> word.countOneBits() }

    val isEmpty: Boolean get() = words.all { word -> word == 0L }

    fun add(node: Int) {
        words[node ushr SHIFT] = words[node ushr SHIFT] or (1L shl node)
    }

    operator fun contains(node: Int): Boolean = words[node ushr SHIFT] and (1L shl node) != 0L

    fun addAll(other: NodeSet) {
        for (index in words.indices) words[index] = words[index] or other.words[index]
    }

    fun first(): Int? = nodes().firstOrNull()

    fun nodes(): Sequence<Int> = sequence {
        for (index in words.indices) {
            var word = words[index]
            while (word != 0L) {
                val bit = word.countTrailingZeroBits()
                yield((index shl SHIFT) + bit)
                word = word and word - 1
            }
        }
    }

    fun copy(): NodeSet {
        val copy = NodeSet(capacity)
        words.copyInto(copy.words)
        return copy
    }
}
