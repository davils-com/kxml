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
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.fixture.refusal
import com.davils.kxml.core.fixture.root
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

private const val ALL_ATTRIBUTES: Int = 10
private const val HALF_ATTRIBUTES: Int = 9
private const val PREFIXES: Int = 8

private fun collidingNames(pieces: Int): List<String> =
    (1..pieces).fold(listOf("")) { names, _ -> names.flatMap { name -> listOf("${name}Aa", "${name}BB") } }

private fun attributesOf(names: List<String>): String = names.joinToString(separator = " ") { name -> "$name='v'" }

class CollidingNamesTest : FunSpec({
    context("names that share a hash code") {
        test("are distinct attributes of one element") {
            val names = collidingNames(ALL_ATTRIBUTES)
            val hashes = names.map(String::hashCode)

            hashes.distinct() shouldHaveSize 1
            Xml.Default.root("<a ${attributesOf(names)}/>").attributes shouldHaveSize names.size
        }

        test("still find the attribute that is set twice") {
            val names = collidingNames(HALF_ATTRIBUTES)
            val twice = names + names.last()

            Xml.Default.refusal("<a ${attributesOf(twice)}/>") shouldBe XmlDiagnosticCode.DuplicateAttribute
        }

        test("are distinct prefixes in scope") {
            val prefixes = collidingNames(PREFIXES)
            val declarations = prefixes.joinToString(separator = " ") { prefix -> "xmlns:$prefix='urn:$prefix'" }
            val attributes = prefixes.joinToString(separator = " ") { prefix -> "$prefix:x='v'" }
            val root = Xml.Default.root("<a $declarations><b $attributes/></a>")
            val child = root.elements.single()
            val namespaces = child.attributes.map { attribute -> attribute.name.namespace }

            namespaces shouldBe prefixes.map { prefix -> "urn:$prefix" }
        }

        test("compare equal in any order") {
            val names = collidingNames(PREFIXES)
            val forward = Xml.Default.root("<a ${attributesOf(names)}/>")
            val backward = Xml.Default.root("<a ${attributesOf(names.asReversed())}/>")

            forward shouldBe backward
            forward.hashCode() shouldBe backward.hashCode()
        }
    }
})
