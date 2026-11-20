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

package com.davils.kxml.core.syntax.internal

internal object GreenWalk {
    private const val HASH_FACTOR: Int = 31

    fun forEachToken(root: GreenElement, action: (GreenToken) -> Unit) {
        val pending = ArrayList<GreenElement>()
        pending.add(root)
        while (pending.isNotEmpty()) {
            val element = pending.removeAt(pending.lastIndex)
            if (element is GreenToken) action(element)
            pending.addAll(element.children.asReversed())
        }
    }

    fun isSame(first: GreenElement, second: GreenElement): Boolean {
        val pending = ArrayList<Pair<GreenElement, GreenElement>>()
        pending.add(first to second)
        var isSame = true
        while (isSame && pending.isNotEmpty()) {
            val (left, right) = pending.removeAt(pending.lastIndex)
            val isShared = left === right
            isSame = isShared || isShallowSame(left, right)
            val isDescending = isSame && !isShared
            if (isDescending) pending.addAll(left.children.zip(right.children))
        }
        return isSame
    }

    fun hashOf(root: GreenElement): Int {
        var hash = 0
        val pending = ArrayList<GreenElement>()
        pending.add(root)
        while (pending.isNotEmpty()) {
            val element = pending.removeAt(pending.lastIndex)
            hash = HASH_FACTOR * hash + shallowHashOf(element)
            pending.addAll(element.children.asReversed())
        }
        return hash
    }

    private fun isShallowSame(left: GreenElement, right: GreenElement): Boolean {
        val isSameShape = left.kind == right.kind && left.length == right.length
        val isSameSize = left.children.size == right.children.size
        val isSameText = !left.kind.isToken || left.text == right.text
        return isSameShape && isSameSize && isSameText
    }

    private fun shallowHashOf(element: GreenElement): Int {
        val token = element as? GreenToken
        val content = token?.run { text.hashCode() } ?: element.children.size
        return HASH_FACTOR * element.kind.ordinal + content
    }
}
