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

private val escapedCategories: Set<Int> = setOf(
    Character.CONTROL.toInt(),
    Character.FORMAT.toInt(),
    Character.LINE_SEPARATOR.toInt(),
    Character.PARAGRAPH_SEPARATOR.toInt(),
    Character.PRIVATE_USE.toInt(),
    Character.UNASSIGNED.toInt(),
    Character.SURROGATE.toInt(),
)

private val combiningMarks: Set<Int> = setOf(
    Character.NON_SPACING_MARK.toInt(),
    Character.ENCLOSING_MARK.toInt(),
    Character.COMBINING_SPACING_MARK.toInt(),
)

internal actual fun isEscapedCategory(codePoint: Int): Boolean = Character.getType(codePoint) in escapedCategories

internal actual fun isCombiningMark(codePoint: Int): Boolean = Character.getType(codePoint) in combiningMarks
