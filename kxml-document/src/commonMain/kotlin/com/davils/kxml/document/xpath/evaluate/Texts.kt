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

package com.davils.kxml.document.xpath.evaluate

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet
import com.davils.kxml.document.internal.appendCode
import com.davils.kxml.document.internal.charCountOf
import com.davils.kxml.document.internal.codePointAt

internal fun normalizedSpaceOf(text: String): String {
    val words = text.split(' ', '\t', '\n', '\r')
    return words.filter(String::isNotEmpty).joinToString(separator = " ")
}

internal fun indexIn(text: String, pattern: String): Int {
    val fallbacks = fallbacksOf(pattern)
    var matched = 0
    var index = 0
    while (index < text.length && matched < pattern.length) {
        while (matched > 0 && text[index] != pattern[matched]) matched = fallbacks[matched - 1]
        if (text[index] == pattern[matched]) matched += 1
        index += 1
    }
    if (matched < pattern.length) return -1
    return index - pattern.length
}

internal fun translated(text: String, from: String, to: String): String {
    val sources = mutableListOf<Int>()
    val targets = mutableListOf<Int?>()
    val seen = OrderedSet<Int>()
    val toCodes = codePointsIn(to)
    var index = 0
    while (index < from.length) {
        val code = from.codePointAt(index)
        if (seen.add(code)) {
            targets += toCodes.getOrNull(sources.size)
            sources += code
        }
        index += charCountOf(code)
    }
    return mapped(text, mapping = CodeMapping(sources, targets))
}

private fun mapped(text: String, mapping: CodeMapping): String {
    val result = StringBuilder(text.length)
    var index = 0
    while (index < text.length) {
        val code = text.codePointAt(index)
        index += charCountOf(code)
        val target = mapping.targetOf(code)
        if (target != null) result.appendCode(target)
    }
    return result.toString()
}

private fun codePointsIn(text: String): List<Int> {
    val codes = mutableListOf<Int>()
    var index = 0
    while (index < text.length) {
        val code = text.codePointAt(index)
        codes += code
        index += charCountOf(code)
    }
    return codes
}

private fun fallbacksOf(pattern: String): IntArray {
    val fallbacks = IntArray(pattern.length)
    var matched = 0
    for (index in 1..<pattern.length) {
        while (matched > 0 && pattern[index] != pattern[matched]) matched = fallbacks[matched - 1]
        if (pattern[index] == pattern[matched]) matched += 1
        fallbacks[index] = matched
    }
    return fallbacks
}
