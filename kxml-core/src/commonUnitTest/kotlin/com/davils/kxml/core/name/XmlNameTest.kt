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

package com.davils.kxml.core.name

import com.davils.kxml.core.fixture.hostileText
import com.davils.kxml.core.fixture.shouldBeSafeToShow
import com.davils.kxml.core.fixture.shouldBeShownSafely
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.arbitrary.filter
import io.kotest.property.checkAll

private const val CASES: Int = 200
private const val NAMESPACE: String = "urn:kxml"

class XmlNameTest : FunSpec({
    context("names") {
        test("are equal by namespace and local name") {
            val spelledWithQ = XmlName(local = "a", namespace = NAMESPACE, prefix = "q")
            XmlName(local = "a", namespace = NAMESPACE, prefix = "p") shouldBe spelledWithQ
            XmlName(local = "a", namespace = NAMESPACE).hashCode() shouldBe spelledWithQ.hashCode()
            XmlName(local = "a", namespace = NAMESPACE) shouldNotBe XmlName("a")
            XmlName("a") shouldNotBe "a"
        }

        test("describe themselves") {
            XmlName(local = "a", namespace = NAMESPACE, prefix = "p").qualified shouldBe "p:a"
            XmlName("a").qualified shouldBe "a"
            XmlName(local = "a", namespace = NAMESPACE).toString() shouldBe "{$NAMESPACE}a"
            XmlName("a").toString() shouldBe "a"
        }

        test("refuse what XML cannot name") {
            shouldThrow<IllegalArgumentException> { XmlName("1a") }
            shouldThrow<IllegalArgumentException> { XmlName("a:b") }
            shouldThrow<IllegalArgumentException> { XmlName("") }
            shouldThrow<IllegalArgumentException> { XmlName(local = "a", namespace = NAMESPACE, prefix = "1p") }
            shouldThrow<IllegalArgumentException> { XmlName("a", prefix = "p") }
            shouldThrow<IllegalArgumentException> { XmlName("a", namespace = "") }
            shouldThrow<IllegalArgumentException> {
                XmlName(local = "a", namespace = NAMESPACE, prefix = XmlNamespaces.XMLNS_PREFIX)
            }
            shouldThrow<IllegalArgumentException> {
                XmlName(local = "a", namespace = NAMESPACE, prefix = XmlNamespaces.XML_PREFIX)
            }
        }

        test("accept the xml prefix in its own namespace") {
            val lang = XmlName(local = "lang", namespace = XmlNamespaces.XML, prefix = XmlNamespaces.XML_PREFIX)
            lang.qualified shouldBe "xml:lang"
        }

        test("escape the namespace when they are shown") {
            checkAll(CASES, hostileText.filter { text -> text.isNotEmpty() }) { text ->
                XmlName(local = "a", namespace = text).shouldBeShownSafely()
            }
        }

        test("escape the refused input in their messages") {
            checkAll(CASES, hostileText) { text ->
                val refusal = shouldThrow<IllegalArgumentException> { XmlName("1$text") }
                refusal.message.orEmpty().shouldBeSafeToShow()
            }
        }

        test("accept letters beyond ASCII") {
            XmlName("über-name.·x").local shouldBe "über-name.·x"
            XmlName("𐀀").local shouldBe "𐀀"
        }
    }

    context("bindings") {
        test("are equal by prefix and namespace") {
            val binding = XmlNamespaceBinding(prefix = "p", uri = NAMESPACE)

            binding shouldBe XmlNamespaceBinding(prefix = "p", uri = NAMESPACE)
            val same = XmlNamespaceBinding(prefix = "p", uri = NAMESPACE)
            binding.hashCode() shouldBe same.hashCode()
            binding shouldNotBe XmlNamespaceBinding(prefix = null, uri = NAMESPACE)
            binding shouldNotBe NAMESPACE
        }

        test("escape the namespace when they are shown") {
            checkAll(CASES, hostileText) { text ->
                XmlNamespaceBinding(prefix = null, uri = text).shouldBeShownSafely()
            }
        }

        test("describe themselves") {
            XmlNamespaceBinding(prefix = "p", uri = NAMESPACE).toString() shouldBe "xmlns:p=\"$NAMESPACE\""
            XmlNamespaceBinding(null, NAMESPACE).toString() shouldBe "xmlns=\"$NAMESPACE\""
        }

        test("refuse a prefix that is no name") {
            shouldThrow<IllegalArgumentException> { XmlNamespaceBinding(prefix = "a:b", uri = NAMESPACE) }
        }

        test("undeclare a prefix with an empty namespace name, as Namespaces in XML 1.1 allows") {
            XmlNamespaceBinding(prefix = "p", uri = "").uri shouldBe ""
        }
    }

    context("characters") {
        test("know whitespace and qualified names") {
            XmlCharacters.isWhitespace(" \t\r\n") shouldBe true
            XmlCharacters.isWhitespace(" x") shouldBe false
            XmlCharacters.isQualifiedName("a:b") shouldBe true
            XmlCharacters.isQualifiedName("a:b:c") shouldBe false
            XmlCharacters.isName("") shouldBe false
        }
    }
})
