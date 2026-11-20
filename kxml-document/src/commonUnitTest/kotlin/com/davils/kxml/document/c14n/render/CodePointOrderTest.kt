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
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe

class CodePointOrderTest : FunSpec({
    context("the order of code points") {
        test("puts U+FFFD before supplementary characters, unlike the order of code units") {
            CodePointOrder.compare(a = "\uFFFD", b = "\uD800\uDC00") shouldBeLessThan 0
            CodePointOrder.compare(a = "\uD800\uDC00", b = "\uFFFD") shouldBeGreaterThan 0
            CodePointOrder.compare(a = "a", b = "ab") shouldBeLessThan 0
            CodePointOrder.compare(a = "ab", b = "ab") shouldBe 0
        }
    }
})
