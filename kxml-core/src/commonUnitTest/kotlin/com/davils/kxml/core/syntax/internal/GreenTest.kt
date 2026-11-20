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

import com.davils.kxml.core.parser.TokenTable
import com.davils.kxml.core.syntax.XmlSyntaxKind
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val MANY: Int = 600
private const val COLLIDING_PIECES: Int = 6

private fun name(text: String): GreenToken = GreenToken(XmlSyntaxKind.NAME, text)

private fun collidingTexts(pieces: Int): List<String> =
    (1..pieces).fold(listOf("")) { texts, _ -> texts.flatMap { text -> listOf("${text}Aa", "${text}BB") } }

class GreenTest : FunSpec({
    context("green elements") {
        test("refuse a kind of the other sort") {
            shouldThrow<IllegalArgumentException> { GreenNode(XmlSyntaxKind.NAME, emptyList()) }
            shouldThrow<IllegalArgumentException> { GreenToken(XmlSyntaxKind.ELEMENT, "x") }
        }

        test("compare by kind, length, children and text") {
            val node = GreenNode(XmlSyntaxKind.ATTRIBUTE, listOf(name("a"), name("b")))

            node shouldBe node
            node shouldBe GreenNode(XmlSyntaxKind.ATTRIBUTE, listOf(name("a"), name("b")))
            node shouldNotBe GreenNode(XmlSyntaxKind.ATTRIBUTE, listOf(name("a"), name("c")))
            node shouldNotBe GreenNode(XmlSyntaxKind.ATTRIBUTE, listOf(name("a")))
            node shouldNotBe GreenNode(XmlSyntaxKind.START_TAG, listOf(name("a"), name("b")))
            node shouldNotBe name("ab")
            name("a") shouldNotBe GreenToken(XmlSyntaxKind.TEXT, "a")
            node.text shouldBe "ab"
            node.toString() shouldBe "ATTRIBUTE(2 children, 2)"
            name("ab").toString() shouldBe "NAME(2)"
        }
    }

    context("a token table") {
        test("returns the first of equal tokens and grows with distinct ones") {
            val table = TokenTable()
            val first = name("a")

            table.intern(first) shouldBeSameInstanceAs first
            table.intern(name("a")) shouldBeSameInstanceAs first
            (0 until MANY).map { index -> table.intern(name("n$index")) }.size shouldBe MANY
            table.intern(name("a")) shouldBeSameInstanceAs first
        }

        test("keeps tokens it cannot place after a few probes uninterned") {
            val table = TokenTable()
            val colliding = collidingTexts(COLLIDING_PIECES).map(::name)
            val interned = colliding.map(table::intern)

            interned.zip(colliding).all { (kept, given) -> kept === given } shouldBe true
        }
    }
})
