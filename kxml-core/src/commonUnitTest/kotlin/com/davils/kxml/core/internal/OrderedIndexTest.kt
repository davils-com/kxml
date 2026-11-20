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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val MANY: Int = 200
private const val WIDTH: Int = 4
private const val STRIDE: Int = 7
private const val FIRST: Int = 1
private const val SECOND: Int = 2
private const val REPLACED: Int = 3
private const val DEFAULT: Int = 9
private const val NAMES: Int = 3

private val ascending: List<String> = (0 until MANY).map { index ->
    index.toString().padStart(length = WIDTH, padChar = '0')
}
private val everyOther: List<String> = ascending.filterIndexed { index, _ -> index % 2 == 0 }
private val zigzag: List<String> = ascending.indices.map { index -> ascending[index * STRIDE % ascending.size] }

class OrderedIndexTest : FunSpec({
    context("an ordered index") {
        test("hands out ordinals in insertion order and finds them again in any order of insertion") {
            val orders = listOf(ascending, ascending.asReversed(), everyOther, zigzag.distinct())

            orders.forEach { keys ->
                val index = OrderedIndex<String>()
                val ordinals = keys.map(index::ordinalFor)

                ordinals shouldBe keys.indices.toList()
                keys.map(index::ordinalOf) shouldBe ordinals
                keys.map(index::ordinalFor) shouldBe ordinals
                index.size shouldBe keys.size
                index.ordinalOf("absent").shouldBeNull()
            }
        }
    }

    context("a name map") {
        test("keeps the order of first insertion and replaces values") {
            val map = NameMap<Int>()
            map["b"] = FIRST
            map["a"] = SECOND
            map["b"] = REPLACED

            map.entries shouldContainExactly listOf("b" to REPLACED, "a" to SECOND)
            map.names shouldContainExactly listOf("b", "a")
            map.size shouldBe 2
            ("a" in map) shouldBe true
            map["c"].shouldBeNull()
            map.getOrPut("a") { DEFAULT } shouldBe SECOND
            map.getOrPut("c") { DEFAULT } shouldBe DEFAULT
        }
    }

    context("a name set") {
        test("reports whether a name is new and iterates in insertion order") {
            val set = NameSet(listOf("x", "y"))

            set.add("z") shouldBe true
            set.add("x") shouldBe false
            set.toList() shouldContainExactly listOf("x", "y", "z")
            set.size shouldBe NAMES
            ("y" in set) shouldBe true
        }
    }
})
