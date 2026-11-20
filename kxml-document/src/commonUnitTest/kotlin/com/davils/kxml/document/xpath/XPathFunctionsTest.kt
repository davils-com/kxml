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
import com.davils.kxml.document.fixture.textOf
import com.davils.kxml.document.fixture.truthOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private const val ITEM_LENGTH: Double = 6.0
private const val DOCUMENT_LENGTH: Double = 17.0
private const val MINUS_ONE_AND_A_HALF: Double = -1.5
private const val TEXTS: Double = 5.0
private const val THREE_AND_A_HALF: Double = 3.5
private const val TWO_AND_A_HALF: Double = 2.5
private const val FOUR: Double = 4.0
private const val THREE: Double = 3.0
private const val SEVEN: Double = 7.0
private const val MINUS_TWO: Double = -2.0

class XPathFunctionsTest : FunSpec({
    context("node-set functions") {
        test("count, position and last see the context") {
            numberOf("count(//d:n)") shouldBe THREE
            textOf("string(//d:n[last()])") shouldBe "abc"
            textOf("string(//d:n[position() = 2])") shouldBe "-1"
            textOf("string((//d:n)[2])") shouldBe "-1"
            namesOf("/d:r/*[last() - 1]") shouldContainExactly listOf("n")
            numberOf("last()") shouldBe 1.0
            numberOf("position()") shouldBe 1.0
        }

        test("id finds elements by xml:id") {
            namesOf("id('x1')") shouldContainExactly listOf("p:i")
            namesOf("id(' x1  missing ')") shouldContainExactly listOf("p:i")
            namesOf("id(//@xml:id)") shouldContainExactly listOf("p:i")
            namesOf("id('')").shouldBeEmpty()
            namesOf("id('i1')").shouldBeEmpty()
            val twice = "<r><a xml:id=' k '/><b xml:id='k'/></r>"
            namesOf("id('k')", text = twice) shouldContainExactly listOf("a")
        }

        test("name, local-name and namespace-uri describe the first node") {
            textOf("name(/d:r/p:i)") shouldBe "p:i"
            textOf("local-name(/d:r/p:i)") shouldBe "i"
            textOf("namespace-uri(/d:r/p:i)") shouldBe "urn:p"
            textOf("name(//@xml:lang)") shouldBe "xml:lang"
            textOf("name(//processing-instruction())") shouldBe "pi"
            textOf("name(/)") shouldBe ""
            textOf("name(//d:none)") shouldBe ""
            textOf("local-name()") shouldBe ""
            textOf("namespace-uri(/)") shouldBe ""
            textOf("namespace-uri(//d:none)") shouldBe ""
            textOf("string(//d:n[local-name() = 'n'][1])") shouldBe "3.5"
        }

        test("lang looks at xml:lang on the node and its ancestors") {
            numberOf("count(//*[lang('en')])") shouldBe SEVEN
            numberOf("count(//*[lang('EN-us')])") shouldBe SEVEN
            numberOf("count(//*[lang('de')])") shouldBe 0.0
            numberOf("count(//text()[lang('en')])") shouldBe TEXTS
            truthOf("lang('en')") shouldBe false
            numberOf("count(//comment()[lang('en')])") shouldBe 1.0
            numberOf("count(/d:r/namespace::*[lang('en')])") shouldBe THREE
            numberOf("count(/d:r/@a[lang('en')])") shouldBe 1.0
        }
    }

    context("string functions") {
        test("convert values to strings as XPath writes them") {
            textOf("string(1)") shouldBe "1"
            textOf("string(0.5)") shouldBe "0.5"
            textOf("string(-0)") shouldBe "0"
            textOf("string(1 div 0)") shouldBe "Infinity"
            textOf("string(-1 div 0)") shouldBe "-Infinity"
            textOf("string(0 div 0)") shouldBe "NaN"
            textOf("string(0.1 + 0.2)") shouldBe "0.30000000000000004"
            textOf("string(1000000000000000000000)") shouldBe "1000000000000000000000"
            textOf("string(0.0000001)") shouldBe "0.0000001"
            textOf("string(true())") shouldBe "true"
            textOf("string(false())") shouldBe "false"
            textOf("string(/d:r/d:i)") shouldBe "two cd"
            textOf("string(/)") shouldBe "onetwo cd3.5-1abc"
            textOf("string(//d:none)") shouldBe ""
            textOf("string(//comment())") shouldBe "c0"
            textOf("string(//@n)") shouldBe "2"
        }

        test("join, search and cut strings") {
            textOf("concat('a', 1, true())") shouldBe "a1true"
            truthOf("starts-with('abc', 'ab')") shouldBe true
            truthOf("starts-with('abc', 'b')") shouldBe false
            truthOf("contains('aaab', 'aab')") shouldBe true
            truthOf("contains('abababc', 'ababc')") shouldBe true
            truthOf("contains('abc', 'abd')") shouldBe false
            truthOf("contains('abc', '')") shouldBe true
            textOf("substring-before('1999/04/01', '/')") shouldBe "1999"
            textOf("substring-after('1999/04/01', '/')") shouldBe "04/01"
            textOf("substring-after('1999/04/01', '19')") shouldBe "99/04/01"
            textOf("substring-before('abc', 'x')") shouldBe ""
            textOf("substring-after('abc', 'x')") shouldBe ""
        }

        test("cut substrings by rounded positions of code points") {
            textOf("substring('12345', 2, 3)") shouldBe "234"
            textOf("substring('12345', 2)") shouldBe "2345"
            textOf("substring('12345', 1.5, 2.6)") shouldBe "234"
            textOf("substring('12345', 0, 3)") shouldBe "12"
            textOf("substring('12345', 0 div 0, 3)") shouldBe ""
            textOf("substring('12345', 1, 0 div 0)") shouldBe ""
            textOf("substring('12345', -42, 1 div 0)") shouldBe "12345"
            textOf("substring('12345', -1 div 0, 1 div 0)") shouldBe ""
            textOf("substring('a𝄞b', 2, 1)") shouldBe "𝄞"
            numberOf("string-length('a𝄞b')") shouldBe THREE
        }

        test("measure, normalize and translate strings") {
            numberOf("string-length(/d:r/d:i)") shouldBe ITEM_LENGTH
            numberOf("string-length()") shouldBe DOCUMENT_LENGTH
            textOf("normalize-space('  a \t b\n ')") shouldBe "a b"
            textOf("normalize-space(/d:r/d:i)") shouldBe "two cd"
            textOf("normalize-space()") shouldBe "onetwo cd3.5-1abc"
            textOf("translate('bar', 'abc', 'ABC')") shouldBe "BAr"
            textOf("translate('--aaa--', 'abc-', 'ABC')") shouldBe "AAA"
            textOf("translate('aba', 'aa', 'xy')") shouldBe "xbx"
            textOf("translate('a𝄞', '𝄞', 'b')") shouldBe "ab"
        }
    }

    context("boolean functions") {
        test("convert values to booleans") {
            truthOf("boolean(//d:n)") shouldBe true
            truthOf("boolean(//d:none)") shouldBe false
            truthOf("boolean('')") shouldBe false
            truthOf("boolean('false')") shouldBe true
            truthOf("boolean(0)") shouldBe false
            truthOf("boolean(0 div 0)") shouldBe false
            truthOf("boolean(2)") shouldBe true
            truthOf("not(true())") shouldBe false
            truthOf("false() or true() and not(false())") shouldBe true
            truthOf("1 and 0") shouldBe false
        }
    }

    context("number functions") {
        test("convert and round numbers") {
            numberOf("number(' -1.5 ')") shouldBe MINUS_ONE_AND_A_HALF
            numberOf("number('1.')") shouldBe 1.0
            numberOf("number('.5')") shouldBe HALF
            numberOf("number(true())") shouldBe 1.0
            numberOf("number(false())") shouldBe 0.0
            numberOf("number(//d:n)") shouldBe THREE_AND_A_HALF
            numberOf("number(/d:r/@a)") shouldBe 1.0
            numberOf("number()").isNaN() shouldBe true
            listOf("'.'", "'-'", "'1e3'", "'+1'", "''", "'1 2'", "'- 1'").forEach { text ->
                numberOf("number($text)").isNaN() shouldBe true
            }
            numberOf("sum(//d:n[number(.) = number(.)])") shouldBe TWO_AND_A_HALF
            numberOf("sum(//d:n)").isNaN() shouldBe true
            numberOf("floor(-1.5)") shouldBe MINUS_TWO
            numberOf("ceiling(-1.5)") shouldBe -1.0
            numberOf("round(2.5)") shouldBe THREE
            numberOf("round(-2.5)") shouldBe MINUS_TWO
            numberOf("1 div round(-0.4)") shouldBe Double.NEGATIVE_INFINITY
            numberOf("round(0 div 0)").isNaN() shouldBe true
            numberOf("round(1 div 0)") shouldBe Double.POSITIVE_INFINITY
        }

        test("compute with the operators of XPath") {
            numberOf("5 mod 2") shouldBe 1.0
            numberOf("5 mod -2") shouldBe 1.0
            numberOf("-5 mod 2") shouldBe -1.0
            numberOf("5 div 2") shouldBe TWO_AND_A_HALF
            numberOf("2*3 - 2") shouldBe FOUR
            numberOf("//d:n * 2") shouldBe SEVEN
            numberOf("--2") shouldBe 2.0
            numberOf("- - -'2'") shouldBe MINUS_TWO
            numberOf("-'a'").isNaN() shouldBe true
            numberOf("1 + 2 + 1") shouldBe FOUR
        }
    }
})

private const val HALF: Double = 0.5
