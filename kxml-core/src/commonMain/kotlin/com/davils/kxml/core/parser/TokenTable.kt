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

package com.davils.kxml.core.parser

import com.davils.kxml.core.syntax.internal.GreenToken

internal class TokenTable {
    private var slots: Array<GreenToken?> = arrayOfNulls(INITIAL_CAPACITY)
    private var count: Int = 0

    fun intern(token: GreenToken): GreenToken {
        val slot = slotOf(token)
        val known = slots.getOrNull(slot)
        if (known != null || slot < 0) return known ?: token
        slots[slot] = token
        count++
        if (count * 2 > slots.size) grow()
        return token
    }

    private fun slotOf(token: GreenToken): Int {
        val mask = slots.size - 1
        val start = spread(token.hashCode()) and mask
        var probe = 0
        var slot = NONE
        while (slot == NONE && probe < MOST_PROBES) {
            val index = start + probe and mask
            val candidate = slots[index]
            if (candidate == null || candidate == token) slot = index
            probe++
        }
        return slot
    }

    private fun grow() {
        val old = slots
        slots = arrayOfNulls(old.size * 2)
        count = 0
        old.forEach { token -> token?.let(::reinsert) }
    }

    private fun reinsert(token: GreenToken) {
        val slot = slotOf(token)
        if (slot < 0) return
        slots[slot] = token
        count++
    }

    private fun spread(hash: Int): Int = hash xor (hash ushr SPREAD_SHIFT)

    private companion object {
        const val INITIAL_CAPACITY: Int = 256
        const val MOST_PROBES: Int = 8
        const val SPREAD_SHIFT: Int = 16
        const val NONE: Int = -1
    }
}
