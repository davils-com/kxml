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
package com.davils.kxml.document.xpath.syntax

internal enum class Relation(val isEquality: Boolean, val isLess: Boolean) {
    Equal(isEquality = true, isLess = false),
    NotEqual(isEquality = true, isLess = false),
    Less(isEquality = false, isLess = true),
    LessOrEqual(isEquality = false, isLess = true),
    Greater(isEquality = false, isLess = false),
    GreaterOrEqual(isEquality = false, isLess = false),
    ;

    val mirrored: Relation
        get() = when (this) {
            Equal -> Equal
            NotEqual -> NotEqual
            Less -> Greater
            LessOrEqual -> GreaterOrEqual
            Greater -> Less
            GreaterOrEqual -> LessOrEqual
        }

    fun holds(left: Double, right: Double): Boolean = when (this) {
        Equal -> left == right
        NotEqual -> left != right
        Less -> left < right
        LessOrEqual -> left <= right
        Greater -> left > right
        GreaterOrEqual -> left >= right
    }
}
