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

import com.davils.kxml.core.Xml
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.fixture.GROWTH_FACTOR
import com.davils.kxml.core.fixture.shouldGrowLinearly
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.syntax.XmlSyntaxTree
import com.davils.kxml.core.xml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf

private const val SMALL_PIECES: Int = 9
private const val LARGE_PIECES: Int = 13
private const val SMALL_DEPTH: Int = 50
private const val SMALL_COUNT: Int = 2_000

private val relaxed: Xml = xml { limits = XmlLimits.Relaxed }

private fun collidingNames(pieces: Int): List<String> =
    (1..pieces).fold(listOf("")) { names, _ -> names.flatMap { name -> listOf("${name}Aa", "${name}BB") } }

private fun collidingAttributes(pieces: Int): String {
    val attributes = collidingNames(pieces).joinToString(separator = " ") { name -> "$name='v'" }
    return "<a $attributes/>"
}

private fun prefixesOnLevels(depth: Int): String {
    val opening = (0 until depth).joinToString(separator = "") { level -> "<e xmlns:p$level='urn:$level'>" }
    val used = (0 until depth).joinToString(separator = " ") { level -> "p$level:x='v'" }
    return "$opening<f $used/>" + "</e>".repeat(depth)
}

private fun references(count: Int): String = "<a>" + "&amp;&#x41;".repeat(count) + "</a>"

private fun read(text: String) {
    relaxed.parse(text).shouldBeInstanceOf<XmlParseResult.Parsed>()
}

class GrowthTest : FunSpec({
    context("the work of reading") {
        test("grows linearly with attributes whose names share a hash code") {
            shouldGrowLinearly(
                small = collidingAttributes(SMALL_PIECES),
                large = collidingAttributes(LARGE_PIECES),
                read = ::read,
            )
        }

        test("grows linearly with prefixes declared on many levels and used at once") {
            shouldGrowLinearly(
                small = prefixesOnLevels(SMALL_DEPTH),
                large = prefixesOnLevels(SMALL_DEPTH * GROWTH_FACTOR),
                read = ::read,
            )
        }

        test("grows linearly with references") {
            shouldGrowLinearly(
                small = references(SMALL_COUNT),
                large = references(SMALL_COUNT * GROWTH_FACTOR),
                read = ::read,
            )
        }

        test("of a syntax tree grows linearly with the text") {
            shouldGrowLinearly(
                small = references(SMALL_COUNT),
                large = references(SMALL_COUNT * GROWTH_FACTOR),
            ) { text -> XmlSyntaxTree.parse(text, relaxed) }
        }
    }
})
