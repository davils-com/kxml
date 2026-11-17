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

package com.davils.kxml.core.fixture

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import java.util.Locale

private const val LONGEST_HOSTILE_TEXT: Int = 64
private const val TAB_CODE: Int = '\t'.code
private const val HEX: Int = 16
private const val RANGE_SEPARATOR: String = ".."
private const val DEFAULT_IGNORABLE_CODE_POINTS: String = "00AD 034F 061C 115F..1160 17B4..17B5 180B..180F " +
    "200B..200F 202A..202E 2060..206F 3164 FE00..FE0F FEFF FFA0 FFF0..FFF8 1BCA0..1BCA3 1D173..1D17A E0000..E0FFF"
private const val VARIATION_SELECTORS: String = "180B..180D 180F FE00..FE0F E0100..E01EF"

internal const val ESCAPE: String = "\u001B"
internal const val RIGHT_TO_LEFT_OVERRIDE: String = "\u202E"
internal const val LINE_SEPARATOR: String = "\u2028"
internal const val SOFT_HYPHEN: String = "\u00AD"
internal const val LONE_HIGH_SURROGATE: String = "\uD800"
internal const val LONE_LOW_SURROGATE: String = "\uDC00"
internal const val BYTE_ORDER_MARK: String = "\uFEFF"
internal const val TAG_LATIN_A: String = "\uDB40\uDC41"
internal const val VARIATION_SELECTOR: String = "\uFE0F"
internal const val CLEAR_SCREEN: String = "$ESCAPE[2J"
internal const val CLIPBOARD_WRITE: String = "$ESCAPE]52;c;aGVsbG8=\u0007"
internal const val FORGED_LOG_LINE: String = "\n2026-11-16 INFO auth ok user=admin"
internal const val COMBINING_GRAPHEME_JOINER: String = "\u034F"
internal const val HANGUL_FILLER: String = "\u3164"
internal const val HALFWIDTH_HANGUL_FILLER: String = "\uFFA0"
internal const val HANGUL_CHOSEONG_FILLER: String = "\u115F"
internal const val KHMER_VOWEL_INHERENT_AQ: String = "\u17B4"
internal const val MONGOLIAN_VARIATION_SELECTOR: String = "\u180B"
internal const val MONGOLIAN_VARIATION_SELECTOR_FOUR: String = "\u180F"
internal const val COMBINING_LONG_SOLIDUS: String = "\u0338"
internal const val HEBREW_ALEF: String = "\u05D0"

private val fragments: List<String> = listOf(
    "a", "Z", "7", ".", "[", "]", "\"", "\\", " ", "ä", "日", "\uD83D\uDE00",
    ESCAPE, RIGHT_TO_LEFT_OVERRIDE, LINE_SEPARATOR, SOFT_HYPHEN, LONE_HIGH_SURROGATE, LONE_LOW_SURROGATE,
    BYTE_ORDER_MARK, TAG_LATIN_A, VARIATION_SELECTOR, "\r", "\n", "\t", "\u0000", "\u007F", "\u0085", "\u200B",
    CLEAR_SCREEN, CLIPBOARD_WRITE, FORGED_LOG_LINE, COMBINING_GRAPHEME_JOINER, HANGUL_FILLER, HALFWIDTH_HANGUL_FILLER,
    HANGUL_CHOSEONG_FILLER, KHMER_VOWEL_INHERENT_AQ, MONGOLIAN_VARIATION_SELECTOR, MONGOLIAN_VARIATION_SELECTOR_FOUR,
    COMBINING_LONG_SOLIDUS, HEBREW_ALEF,
)

private val hostilePieces: Arb<List<String>> = Arb.list(Arb.element(fragments), 0..LONGEST_HOSTILE_TEXT)

internal val hostileText: Arb<String> = hostilePieces.map { pieces -> pieces.joinToString(separator = "") }

internal val defaultIgnorableCodePoints: List<IntRange> = codePointRangesOf(DEFAULT_IGNORABLE_CODE_POINTS)

internal val variationSelectorCodePoints: List<IntRange> = codePointRangesOf(VARIATION_SELECTORS)

private val invisibleCodePoints: List<IntRange> = defaultIgnorableCodePoints + variationSelectorCodePoints

private val unsafeCategories: Set<Int> = setOf(
    Character.CONTROL.toInt(),
    Character.FORMAT.toInt(),
    Character.LINE_SEPARATOR.toInt(),
    Character.PARAGRAPH_SEPARATOR.toInt(),
    Character.PRIVATE_USE.toInt(),
    Character.UNASSIGNED.toInt(),
    Character.SURROGATE.toInt(),
)

internal fun unsafeCodePointsIn(text: String, allowTabs: Boolean = false): List<String> {
    val codePoints = text.codePoints().toArray()
    val unsafe = codePoints.filter { codePoint -> isUnsafe(codePoint, allowTabs) }
    return unsafe.map { codePoint -> "U+%04X".format(Locale.ROOT, codePoint) }
}

internal fun String.shouldBeSafeToShow(allowTabs: Boolean = false) {
    withClue("text shown to a person: $this".take(LONGEST_HOSTILE_TEXT)) {
        unsafeCodePointsIn(this, allowTabs).shouldBeEmpty()
    }
}

internal fun Any.shouldBeShownSafely() {
    toString().shouldBeSafeToShow()
}

private fun isUnsafe(codePoint: Int, allowTabs: Boolean): Boolean {
    val isAllowedTab = allowTabs && codePoint == TAB_CODE
    return !isAllowedTab && isUnsafeToShow(codePoint)
}

private fun isUnsafeToShow(codePoint: Int): Boolean {
    val isInvisible = invisibleCodePoints.any { range -> codePoint in range }
    return isInvisible || Character.getType(codePoint) in unsafeCategories
}

private fun codePointRangesOf(listing: String): List<IntRange> = listing.split(" ").map { entry ->
    val bounds = entry.split(RANGE_SEPARATOR).map { bound -> bound.toInt(HEX) }
    bounds.first()..bounds.last()
}
