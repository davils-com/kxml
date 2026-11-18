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

package com.davils.kxml.core.internal

import com.davils.kore.core.Limits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kxml.core.fixture.CLEAR_SCREEN
import com.davils.kxml.core.fixture.COMBINING_GRAPHEME_JOINER
import com.davils.kxml.core.fixture.COMBINING_LONG_SOLIDUS
import com.davils.kxml.core.fixture.ESCAPE
import com.davils.kxml.core.fixture.FORGED_LOG_LINE
import com.davils.kxml.core.fixture.HALFWIDTH_HANGUL_FILLER
import com.davils.kxml.core.fixture.HANGUL_CHOSEONG_FILLER
import com.davils.kxml.core.fixture.HANGUL_FILLER
import com.davils.kxml.core.fixture.KHMER_VOWEL_INHERENT_AQ
import com.davils.kxml.core.fixture.LINE_SEPARATOR
import com.davils.kxml.core.fixture.LONE_HIGH_SURROGATE
import com.davils.kxml.core.fixture.MONGOLIAN_VARIATION_SELECTOR
import com.davils.kxml.core.fixture.MONGOLIAN_VARIATION_SELECTOR_FOUR
import com.davils.kxml.core.fixture.RIGHT_TO_LEFT_OVERRIDE
import com.davils.kxml.core.fixture.SOFT_HYPHEN
import com.davils.kxml.core.fixture.TAG_LATIN_A
import com.davils.kxml.core.fixture.VARIATION_SELECTOR
import com.davils.kxml.core.fixture.codePointsOf
import com.davils.kxml.core.fixture.defaultIgnorableCodePoints
import com.davils.kxml.core.fixture.hostileText
import com.davils.kxml.core.fixture.shouldBeSafeToShow
import com.davils.kxml.core.fixture.variationSelectorCodePoints
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.checkAll

private const val CASES: Int = 500
private const val INSIDE_QUOTES: Int = 3
private const val SHORT: Int = 8
private const val SUFFIX_ROOM: Int = 24
private const val PRINTABLE: String = """port 8080 äöü 日本 😀 "quoted" C:\path"""
private const val DECOMPOSED_E_ACUTE: String = "e\u0301"

private val invisibleCodePoints: List<Int> =
    (defaultIgnorableCodePoints + variationSelectorCodePoints).flatMap { range -> range.toList() }

@OptIn(DavilsInternal::class)
class SafeDisplayTest : FunSpec({
    context("text") {
        test("keeps plain text, spaces and every printable script as it is") {
            SafeDisplay.text(PRINTABLE) shouldBe PRINTABLE
        }

        test("writes line breaks and tabs as their escapes, so a value can never start a line of its own") {
            SafeDisplay.text("a${FORGED_LOG_LINE}\r\tb") shouldBe """a\n2026-11-16 INFO auth ok user=admin\r\tb"""
        }

        test("writes terminal control sequences as escapes, so they cannot reach a terminal") {
            SafeDisplay.text(CLEAR_SCREEN) shouldBe "\\u{001B}[2J"
        }

        test("escapes bidirectional overrides, separators, soft hyphens, tags and variation selectors") {
            val shown = SafeDisplay.text(
                "$RIGHT_TO_LEFT_OVERRIDE$LINE_SEPARATOR$SOFT_HYPHEN$TAG_LATIN_A$VARIATION_SELECTOR",
            )

            shown shouldBe """\u{202E}\u{2028}\u{00AD}\u{E0041}\u{FE0F}"""
        }

        test("escapes a lone surrogate instead of passing broken text on") {
            SafeDisplay.text("a${LONE_HIGH_SURROGATE}b") shouldBe "a\\u{D800}b"
        }

        test("escapes the invisible fillers, joiners and selectors that are neither format nor unassigned characters") {
            val joined = "a$COMBINING_GRAPHEME_JOINER$HANGUL_CHOSEONG_FILLER$KHMER_VOWEL_INHERENT_AQ"
            val selected = "$MONGOLIAN_VARIATION_SELECTOR$MONGOLIAN_VARIATION_SELECTOR_FOUR"
            val shown = SafeDisplay.text("$joined$selected$HANGUL_FILLER$HALFWIDTH_HANGUL_FILLER")

            shown shouldBe """a\u{034F}\u{115F}\u{17B4}\u{180B}\u{180F}\u{3164}\u{FFA0}"""
        }

        test("treats every code point that Unicode ignores by default and every variation selector as unsafe") {
            val missed = invisibleCodePoints.filter { codePoint ->
                val raw = textOf(codePoint)
                SafeDisplay.text("a$raw") == "a$raw"
            }

            withClue("code points shown as they are") { missed.shouldBeEmpty() }
        }

        test("writes a combining mark at the start of a value as an escape, so it cannot change the text before it") {
            val message = "limit=" + SafeDisplay.text("$COMBINING_LONG_SOLIDUS 5")

            message shouldBe "limit=\\u{0338} 5"
        }

        test("writes a combining mark after an escape as an escape, so it cannot change how the escape looks") {
            SafeDisplay.text("$ESCAPE$COMBINING_LONG_SOLIDUS") shouldBe "\\u{001B}\\u{0338}"
        }

        test("keeps a combining mark that follows a character of the value shown as it is") {
            SafeDisplay.text("caf$DECOMPOSED_E_ACUTE$COMBINING_LONG_SOLIDUS") shouldBe
                "caf$DECOMPOSED_E_ACUTE$COMBINING_LONG_SOLIDUS"
        }

        test("never starts with a combining mark, whatever the input") {
            checkAll(CASES, hostileText) { text ->
                val codePoints = codePointsOf(SafeDisplay.text(text, Int.MAX_VALUE))
                val first = codePoints.firstOrNull() ?: 'a'.code

                isCombiningMark(first) shouldBe false
            }
        }

        test("never lets an unsafe character through, whatever the input") {
            checkAll(CASES, hostileText) { text ->
                SafeDisplay.text(text, Int.MAX_VALUE).shouldBeSafeToShow()
            }
        }

        test("changes nothing when it is applied to its own output") {
            checkAll(CASES, hostileText) { text ->
                val once = SafeDisplay.text(text, Int.MAX_VALUE)

                SafeDisplay.text(once, Int.MAX_VALUE) shouldBe once
            }
        }

        test("shortens long text at the limit and says how much it left out") {
            val shown = SafeDisplay.text("abcdefghijkl", SHORT)

            shown shouldBe "abcdefgh…(+4 chars)"
        }

        test("never cuts an escape in half when it shortens") {
            val shown = SafeDisplay.text("abcdef$ESCAPE", SHORT)

            shown shouldBe "abcdef…(+1 chars)"
        }

        test("stays within the limit plus the note about what it left out") {
            checkAll(CASES, hostileText) { text ->
                SafeDisplay.text(text, SHORT).length shouldBeLessThanOrEqual SHORT + SUFFIX_ROOM
            }
        }

        test("shortens to the display limit by default") {
            val shown = SafeDisplay.text("x".repeat(Limits.MAX_DISPLAY_LENGTH + 1))

            shown shouldEndWith "…(+1 chars)"
        }

        test("refuses a negative limit") {
            shouldThrow<IllegalArgumentException> { SafeDisplay.text("a", -1) }
        }
    }

    context("quoted") {
        test("puts the value in quotes and escapes quotes and backslashes like JSON") {
            SafeDisplay.quoted("""a"].admin["b\""") shouldBe """"a\"].admin[\"b\\""""
        }

        test("puts the note about what it left out after the closing quote, so it cannot be mistaken for the value") {
            val shortened = SafeDisplay.quoted("abcdef", INSIDE_QUOTES)

            shortened shouldBe "\"abc\"…(+3 chars)"
            shortened shouldNotBe SafeDisplay.quoted("abc…(+3 chars)")
        }

        test("writes a combining mark at the start of the value as an escape, so it cannot change the quote") {
            SafeDisplay.quoted("${COMBINING_LONG_SOLIDUS}x") shouldBe """"\u{0338}x""""
        }

        test("never lets an unsafe character through and never ends the quote early") {
            checkAll(CASES, hostileText) { text ->
                val shown = SafeDisplay.quoted(text, Int.MAX_VALUE)
                val body = shown.substring(startIndex = 1, endIndex = shown.length - 1)

                shown.shouldBeSafeToShow()
                shown shouldStartWith "\""
                val stripped = body.replace(oldValue = "\\\\", newValue = "").replace(oldValue = "\\\"", newValue = "")
                stripped shouldNotContain "\""
            }
        }
    }
})
