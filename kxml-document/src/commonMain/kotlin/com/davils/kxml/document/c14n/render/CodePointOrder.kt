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
package com.davils.kxml.document.c14n.render

import com.davils.kxml.document.internal.charCountOf
import com.davils.kxml.document.internal.codePointAt

internal object CodePointOrder : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        var left = 0
        var right = 0
        var order = 0
        while (order == 0 && left < a.length && right < b.length) {
            val leftCode = a.codePointAt(left)
            val rightCode = b.codePointAt(right)
            order = leftCode.compareTo(rightCode)
            left += charCountOf(leftCode)
            right += charCountOf(rightCode)
        }
        if (order != 0) return order
        return (a.length - left).compareTo(b.length - right)
    }
}
