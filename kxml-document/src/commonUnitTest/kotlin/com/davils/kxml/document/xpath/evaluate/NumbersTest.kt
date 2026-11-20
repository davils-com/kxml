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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val TWELVE: Double = 12.0
private const val TOO_LARGE: Int = 350
private const val FRACTION: Double = 123.456
private const val MINUS_TWO: Double = -2.0
private const val TINY: Double = 1e-7
private const val NEGATIVE_ZERO: Double = -0.0
private const val HALF: Double = 0.5
private const val EXACT: Double = 4_503_599_627_370_497.0
private const val MANY: Int = 500
private const val LONG: Int = 900
private const val ONE_AND_A_HALF: Double = 1.5

class NumbersTest : FunSpec({
    context("numberIn") {
        test("reads the Number production of XPath with an optional minus sign") {
            numberIn("12") shouldBe TWELVE
            numberIn(" \t-1.5\n") shouldBe -ONE_AND_A_HALF
            numberIn("-0").toString() shouldBe "-0.0"
            numberIn("0.000") shouldBe 0.0
            numberIn("1x").isNaN() shouldBe true
            numberIn("1.2.3").isNaN() shouldBe true
        }

        test("reads long numbers without a limit on their digits") {
            numberIn("0." + "0".repeat(MANY) + "1") shouldBe 0.0
            numberIn("1" + "0".repeat(MANY)) shouldBe Double.POSITIVE_INFINITY
            numberIn("1" + "0".repeat(TOO_LARGE)) shouldBe Double.POSITIVE_INFINITY
            numberIn("0".repeat(MANY) + "1.5") shouldBe ONE_AND_A_HALF
            numberIn("1." + "4".repeat(LONG)) shouldBe numberIn("1.4444444444444444")
            numberIn("1." + "0".repeat(LONG) + "1") shouldBe 1.0
            numberIn("-" + "9".repeat(MANY)) shouldBe Double.NEGATIVE_INFINITY
        }
    }

    context("textOf") {
        test("writes numbers without an exponent") {
            textOf(FRACTION) shouldBe "123.456"
            textOf(MINUS_TWO) shouldBe "-2"
            textOf(TINY) shouldBe "0.0000001"
            textOf(NEGATIVE_ZERO) shouldBe "0"
            textOf(Double.NaN) shouldBe "NaN"
        }
    }

    context("roundOf") {
        test("rounds half up and keeps the sign of zero") {
            roundOf(HALF) shouldBe 1.0
            roundOf(-HALF).toString() shouldBe "-0.0"
            roundOf(EXACT) shouldBe EXACT
        }
    }
})
