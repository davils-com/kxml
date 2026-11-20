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

import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.unsafeCodePointsIn
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map

private const val MOST_FRAGMENTS: Int = 64

internal val hostileText: Arb<String> =
    Arb.list(Arb.element(HostileFragments.all), 0..MOST_FRAGMENTS).map { fragments -> fragments.joinToString("") }

internal fun String.shouldBeSafeToShow() {
    unsafeCodePointsIn(this).shouldBeEmpty()
}

internal fun Any.shouldBeShownSafely() {
    toString().shouldBeSafeToShow()
}
