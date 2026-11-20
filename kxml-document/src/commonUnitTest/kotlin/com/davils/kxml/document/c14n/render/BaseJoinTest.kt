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
package com.davils.kxml.document.c14n.render

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val ROOT_G: String = "http://a/g"
private const val RFC_BASE: String = "http://a/b/c/d;p?q"

class BaseJoinTest : FunSpec({
    context("joined") {
        test("keeps leading parent segments of relative references, as Canonical XML 1.1 requires") {
            BaseJoin.joined(base = "abc/", reference = "../") shouldBe ""
            BaseJoin.joined(base = "../", reference = "../") shouldBe "../../"
            BaseJoin.joined(base = "..", reference = "..") shouldBe "../../"
            BaseJoin.joined(base = "..", reference = "x") shouldBe "../x"
            BaseJoin.joined(base = "..", reference = "../x") shouldBe "../../x"
            BaseJoin.joined(base = "bar/", reference = "foo") shouldBe "bar/foo"
            BaseJoin.joined(base = "foo", reference = "x") shouldBe "x"
            BaseJoin.joined(base = "a//b/", reference = "./c/.") shouldBe "a/b/c/"
        }

        test("resolves against absolute bases as RFC 3986 does") {
            BaseJoin.joined(base = RFC_BASE, reference = "../g") shouldBe "http://a/b/g"
            BaseJoin.joined(base = RFC_BASE, reference = "") shouldBe RFC_BASE
            BaseJoin.joined(base = RFC_BASE, reference = "?y") shouldBe "http://a/b/c/d;p?y"
            BaseJoin.joined(base = RFC_BASE, reference = "/../g") shouldBe ROOT_G
            BaseJoin.joined(base = RFC_BASE, reference = "//g/x") shouldBe "http://g/x"
            BaseJoin.joined(base = RFC_BASE, reference = "urn:x#f") shouldBe "urn:x"
            BaseJoin.joined(base = "http://a", reference = "g") shouldBe ROOT_G
            BaseJoin.joined(base = "http://a/b/..", reference = "g") shouldBe ROOT_G
        }
    }
})
