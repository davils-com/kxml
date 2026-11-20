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
package com.davils.kxml.document.xpath.evaluate

private const val TRUE: String = "true"
private const val FALSE: String = "false"

internal class Conversions(private val nodes: NodeAccess) {
    fun stringOf(value: Value): String = when (value) {
        is Value.Nodes -> firstValueOf(value.keys)
        is Value.Text -> value.value
        is Value.Number -> textOf(value.value)
        is Value.Truth -> TRUE.takeIf { value.isTrue } ?: FALSE
    }

    fun numberOf(value: Value): Double = when (value) {
        is Value.Nodes -> numberIn(stringOf(value))
        is Value.Text -> numberIn(value.value)
        is Value.Number -> value.value
        is Value.Truth -> 1.0.takeIf { value.isTrue } ?: 0.0
    }

    private fun firstValueOf(keys: LongArray): String {
        val first = keys.firstOrNull() ?: return ""
        return nodes.stringValueOf(first)
    }

    fun truthOf(value: Value): Boolean = when (value) {
        is Value.Nodes -> value.keys.isNotEmpty()
        is Value.Text -> value.value.isNotEmpty()
        is Value.Number -> value.value != 0.0 && !value.value.isNaN()
        is Value.Truth -> value.isTrue
    }
}
