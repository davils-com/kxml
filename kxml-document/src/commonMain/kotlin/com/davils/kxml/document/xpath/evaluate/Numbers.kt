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
@file:OptIn(DavilsInternal::class)

package com.davils.kxml.document.xpath.evaluate

import com.davils.kore.core.ShortestDecimal
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.parseDecimalOrNull
import com.davils.kxml.core.name.XmlCharacters
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.withSign

private const val SIGNIFICANT_DIGITS: Int = 800
private const val MOST_EXPONENT: Int = 400
private const val EXACT_LIMIT: Double = 4_503_599_627_370_496.0
private const val HALF: Double = 0.5
private const val ZERO: String = "0"

private val specialTexts: Map<Double, String> = mapOf(
    Double.NaN to "NaN",
    Double.POSITIVE_INFINITY to "Infinity",
    Double.NEGATIVE_INFINITY to "-Infinity",
    0.0 to ZERO,
    -0.0 to ZERO,
)

internal fun numberIn(text: String): Double {
    val trimmed = text.trim(XmlCharacters::isWhitespace)
    val isNegative = trimmed.startsWith('-')
    val unsigned = trimmed.removePrefix("-")
    val integer = unsigned.substringBefore('.')
    val fraction = unsigned.substringAfter('.', missingDelimiterValue = "")
    val isDigits = integer.all(Char::isAsciiDigit) && fraction.all(Char::isAsciiDigit)
    val hasDigits = integer.isNotEmpty() || fraction.isNotEmpty()
    if (!isDigits || !hasDigits) return Double.NaN
    return magnitudeOf(integer, fraction).signed(isNegative)
}

internal fun textOf(number: Double): String = specialTexts[number] ?: ShortestDecimal.of(number).toPlainString()

internal fun roundOf(number: Double): Double {
    val isExact = number.isNaN() || abs(number) >= EXACT_LIMIT
    if (isExact) return number
    return floor(number + HALF).withSign(number)
}

private fun magnitudeOf(integer: String, fraction: String): Double {
    val digits = integer + fraction
    val zeros = digits.length - digits.trimStart('0').length
    val significant = digits.substring(zeros).trimEnd('0')
    val exponent = integer.length - zeros
    val isZero = significant.isEmpty() || exponent < -MOST_EXPONENT
    if (isZero) return 0.0
    val kept = significant.take(SIGNIFICANT_DIGITS)
    val sticky = "1".takeIf { significant.length > SIGNIFICANT_DIGITS }.orEmpty()
    val decimal = "0.$kept${sticky}e$exponent".takeIf { exponent <= MOST_EXPONENT }
    return decimal?.let(::parseDecimalOrNull) ?: Double.POSITIVE_INFINITY
}

private fun Double.signed(isNegative: Boolean): Double {
    if (isNegative) return -this
    return this
}

private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
