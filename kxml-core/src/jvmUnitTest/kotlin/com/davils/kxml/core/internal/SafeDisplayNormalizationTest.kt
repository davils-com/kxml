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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kxml.core.fixture.COMBINING_LONG_SOLIDUS
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldNotContain
import java.text.Normalizer

private const val NOT_EQUAL_TO: String = "\u2260"

@OptIn(DavilsInternal::class)
class SafeDisplayNormalizationTest : FunSpec({
    context("text") {
        test("keeps an escaped combining mark from composing with the text before it under NFC") {
            val message = "limit=" + SafeDisplay.text("$COMBINING_LONG_SOLIDUS 5")

            Normalizer.normalize(message, Normalizer.Form.NFC) shouldNotContain NOT_EQUAL_TO
        }
    }
})
