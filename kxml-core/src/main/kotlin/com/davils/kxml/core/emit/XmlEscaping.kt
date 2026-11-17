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

package com.davils.kxml.core.emit

import com.davils.kxml.core.name.XmlCharacters

private val textEscapes: Map<Char, String> = mapOf(
    '&' to "&amp;",
    '<' to "&lt;",
    '>' to "&gt;",
    '\r' to "&#13;",
)

private val attributeOnlyEscapes: Map<Char, String> = mapOf(
    '"' to "&quot;",
    '\t' to "&#9;",
    '\n' to "&#10;",
)

private val attributeEscapes: Map<Char, String> = textEscapes + attributeOnlyEscapes

internal object XmlEscaping {
    fun text(value: String): String = escaped(value, textEscapes)

    fun attribute(value: String): String = escaped(value, attributeEscapes)

    private fun escaped(value: String, escapes: Map<Char, String>): String {
        require(XmlCharacters.isText(value)) { "The value holds characters XML cannot carry" }
        if (value.none { char -> char in escapes }) return value
        val output = StringBuilder(value.length)
        value.forEach { char -> output.append(escapes[char] ?: char.toString()) }
        return output.toString()
    }
}
