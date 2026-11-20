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

package com.davils.kxml.serialization.fixture

import io.kotest.matchers.doubles.shouldBeLessThan
import kotlin.time.TimeSource

internal const val GROWTH_FACTOR: Int = 16

private const val RUNS: Int = 5
private const val ATTEMPTS: Int = 3
private const val SLACK: Int = 3
private const val LEAST_MICROSECONDS: Long = 20_000L

internal fun shouldGrowLinearly(small: String, large: String, read: (String) -> Unit) {
    val bound = (GROWTH_FACTOR * SLACK).toDouble()
    var ratio = timeRatio(small, large, read)
    var attempt = 1
    while (ratio >= bound && attempt < ATTEMPTS) {
        ratio = minOf(a = ratio, b = timeRatio(small, large, read))
        attempt++
    }
    ratio shouldBeLessThan bound
}

private fun timeRatio(small: String, large: String, read: (String) -> Unit): Double {
    read(large)
    read(small)
    var fastestSmall = Double.MAX_VALUE
    var fastestLarge = Double.MAX_VALUE
    repeat(RUNS) {
        fastestSmall = minOf(a = fastestSmall, b = timeOf(small, read))
        fastestLarge = minOf(a = fastestLarge, b = timeOf(large, read))
    }
    return fastestLarge / fastestSmall
}

private fun timeOf(text: String, read: (String) -> Unit): Double {
    val start = TimeSource.Monotonic.markNow()
    var repeats = 0
    var elapsed = 0L
    while (repeats == 0 || elapsed < LEAST_MICROSECONDS) {
        read(text)
        repeats++
        elapsed = start.elapsedNow().inWholeMicroseconds
    }
    return maxOf(a = elapsed, b = 1L).toDouble() / repeats
}
