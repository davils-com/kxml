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
package com.davils.kxml.document.xpath

import com.davils.kxml.document.fixture.namesOf
import com.davils.kxml.document.fixture.numberOf
import com.davils.kxml.document.fixture.truthOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private const val NUMBERS: Int = 3

class XPathComparisonTest : FunSpec({
    context("two node-sets") {
        test("are equal when any two of their nodes have equal string-values") {
            truthOf("//d:n = //d:n") shouldBe true
            truthOf("//d:n = //@n") shouldBe false
            truthOf("//d:none = //d:n") shouldBe false
            truthOf("//d:n = //d:none") shouldBe false
        }

        test("differ when any two of their nodes have different string-values") {
            truthOf("//d:n != //d:n") shouldBe true
            truthOf("/d:r/d:e != /d:r/d:e") shouldBe false
            truthOf("/d:r/d:e != //d:n") shouldBe true
            truthOf("//d:none != //d:n") shouldBe false
        }

        test("compare by the numbers of their string-values") {
            truthOf("//d:n < //d:n") shouldBe true
            truthOf("//d:n > //@n") shouldBe true
            truthOf("//d:n <= //@n") shouldBe true
            truthOf("//@n >= //d:n") shouldBe true
            truthOf("//@n > //@a") shouldBe true
            truthOf("//@a > //@n") shouldBe false
            truthOf("//d:n[. = 'abc'] < //d:n") shouldBe false
            truthOf("//d:n < //d:n[. = 'abc']") shouldBe false
            truthOf("//d:none < //d:n") shouldBe false
        }
    }

    context("a node-set and another value") {
        test("compare each node with a number, a string or a boolean") {
            truthOf("//d:n = 3.5") shouldBe true
            truthOf("//d:n = 'abc'") shouldBe true
            truthOf("//d:n != 'abc'") shouldBe true
            truthOf("//d:n < 0") shouldBe true
            truthOf("//d:n > 3.5") shouldBe false
            truthOf("//d:n >= 3.5") shouldBe true
            truthOf("//d:n = true()") shouldBe true
            truthOf("//d:none = false()") shouldBe true
            truthOf("//d:none < true()") shouldBe true
        }

        test("hold with the node-set on the right as on the left") {
            truthOf("3.5 = //d:n") shouldBe true
            truthOf("0 > //d:n") shouldBe true
            truthOf("3 < //d:n") shouldBe true
            truthOf("4 <= //d:n") shouldBe false
            truthOf("-1 >= //d:n") shouldBe true
            truthOf("'abc' = //d:n") shouldBe true
            truthOf("'x' != //d:n") shouldBe true
            truthOf("false() = //d:none") shouldBe true
        }
    }

    context("two other values") {
        test("compare as booleans, then as numbers, then as strings") {
            truthOf("1 = '1'") shouldBe true
            truthOf("true() = 1") shouldBe true
            truthOf("true() = 'x'") shouldBe true
            truthOf("'1.0' = 1") shouldBe true
            truthOf("'1.0' = '1'") shouldBe false
            truthOf("'a' != 'b'") shouldBe true
            truthOf("'2' < '10'") shouldBe true
            truthOf("number('x') != number('x')") shouldBe true
            truthOf("number('x') = number('x')") shouldBe false
            truthOf("1 < 2 = true()") shouldBe true
            truthOf("2 > 1 > 0") shouldBe true
        }
    }

    context("predicates") {
        test("select by position when a number and by truth otherwise") {
            namesOf("//d:n[2]") shouldContainExactly listOf("n")
            namesOf("//d:n[2.5]").size shouldBe 0
            namesOf("//d:n[. > 0][1]") shouldContainExactly listOf("n")
            namesOf("//*[@n][1]") shouldContainExactly listOf("i")
            numberOf("count(//d:n['x'])") shouldBe NUMBERS.toDouble()
            numberOf("count(//d:n[''])") shouldBe 0.0
        }

        test("filter node-sets in document order") {
            namesOf("(//d:n | //p:i)[1]") shouldContainExactly listOf("p:i")
            namesOf("(//d:n)[last()]/text()") shouldContainExactly listOf("#text")
            namesOf("(//d:n)//text()").size shouldBe NUMBERS
            namesOf("(//d:n)/..") shouldContainExactly listOf("r")
            namesOf("(/d:r/@a | /d:r/p:i)") shouldContainExactly listOf("a", "p:i")
        }
    }
})
