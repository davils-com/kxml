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

package com.davils.kxml.serialization

import com.davils.kxml.core.emit.XmlEmitOptions
import com.davils.kxml.serialization.fixture.GROWTH_FACTOR
import com.davils.kxml.serialization.fixture.Nillable
import com.davils.kxml.serialization.fixture.shouldGrowLinearly
import io.kotest.core.spec.style.FunSpec
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private const val SMALL_ITEMS: Int = 500

private val compact: XmlFormat = xmlFormat {
    isDeclarationWritten = false
    xml { emitter = XmlEmitOptions.Compact }
}

private fun tagged(count: Int): Nillable = Nillable(tags = List(count) { index -> "t$index" })

private fun nested(depth: Int): Nillable =
    (1..depth).fold(Nillable(name = "leaf")) { child, level -> Nillable(count = level, child = child) }

class SerializationGrowthTest : FunSpec({
    context("the work of the format") {
        test("grows linearly with the items it decodes") {
            shouldGrowLinearly(
                small = compact.encodeToString(tagged(SMALL_ITEMS)),
                large = compact.encodeToString(tagged(SMALL_ITEMS * GROWTH_FACTOR)),
            ) { text -> compact.decodeFromString<Nillable>(text) }
        }

        test("grows linearly with the items it encodes") {
            shouldGrowLinearly(small = "$SMALL_ITEMS", large = "${SMALL_ITEMS * GROWTH_FACTOR}") { count ->
                compact.encodeToString(tagged(count.toInt()))
            }
        }

        test("grows linearly with the depth of nested values") {
            shouldGrowLinearly(small = "10", large = "${10 * GROWTH_FACTOR}") { depth ->
                compact.decodeFromString<Nillable>(compact.encodeToString(nested(depth.toInt())))
            }
        }
    }
})
