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
package com.davils.kxml.document.diff.align

internal object Alignment {
    fun matches(old: List<String>, new: List<String>): List<Pair<Int, Int>> {
        val prefix = commonPrefix(old, new)
        val suffix = commonSuffix(old, new, prefix)
        val middle = longestCommon(
            old = old.subList(fromIndex = prefix, toIndex = old.size - suffix),
            new = new.subList(fromIndex = prefix, toIndex = new.size - suffix),
        )
        val head = (0..<prefix).map { index -> index to index }
        val shifted = middle.map { (left, right) -> left + prefix to right + prefix }
        val tail = (0..<suffix).map { offset -> old.size - suffix + offset to new.size - suffix + offset }
        return head + shifted + tail
    }

    fun middleCells(old: List<String>, new: List<String>): Long {
        val prefix = commonPrefix(old, new)
        val suffix = commonSuffix(old, new, prefix)
        return (old.size - prefix - suffix).toLong() * (new.size - prefix - suffix).toLong()
    }

    private fun commonPrefix(old: List<String>, new: List<String>): Int {
        var length = 0
        while (length < old.size && length < new.size && old[length] == new[length]) length += 1
        return length
    }

    private fun commonSuffix(old: List<String>, new: List<String>, prefix: Int): Int {
        var length = 0
        while (length < old.size - prefix && length < new.size - prefix && isSameFromEnd(old, new, length)) length += 1
        return length
    }

    private fun isSameFromEnd(old: List<String>, new: List<String>, offset: Int): Boolean =
        old[old.size - 1 - offset] == new[new.size - 1 - offset]

    private fun longestCommon(old: List<String>, new: List<String>): List<Pair<Int, Int>> {
        val width = new.size + 1
        val lengths = IntArray((old.size + 1) * width)
        for (left in old.indices.reversed()) {
            for (right in new.indices.reversed()) {
                val cell = left * width + right
                val isEqual = old[left] == new[right]
                lengths[cell] = (lengths[cell + width + 1] + 1).takeIf { isEqual }
                    ?: maxOf(a = lengths[cell + width], b = lengths[cell + 1])
            }
        }
        val pairs = mutableListOf<Pair<Int, Int>>()
        var left = 0
        var right = 0
        while (left < old.size && right < new.size) {
            val cell = left * width + right
            val isEqual = old[left] == new[right]
            if (isEqual) pairs += left to right
            val isDown = !isEqual && lengths[cell + width] >= lengths[cell + 1]
            left += 1.takeIf { isEqual || isDown } ?: 0
            right += 1.takeIf { isEqual || !isDown } ?: 0
        }
        return pairs
    }
}
