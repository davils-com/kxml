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

package com.davils.kxml.core.parser

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.internal.charCountOf
import com.davils.kxml.core.internal.codePointAt
import com.davils.kxml.core.internal.unicodeNameOf

private const val NONE: Int = -1

internal object Sections {
    fun normalized(text: String, from: Int, to: Int, dialect: Dialect, isRaw: Boolean): String {
        val raw = text.substring(startIndex = from, endIndex = to)
        val invalid = invalidOffset(raw, dialect)
        if (invalid != NONE) {
            failure(
                XmlDiagnosticCode.InvalidCharacter,
                TextRange.of(start = from + invalid, length = 1),
                "The character ${unicodeNameOf(raw.codePointAt(invalid))} is not allowed in XML",
            )
        }
        if (!isRaw) return raw
        return dialect.normalizedLineEnds(raw)
    }

    private fun invalidOffset(raw: String, dialect: Dialect): Int {
        var index = 0
        while (index < raw.length) {
            val code = raw.codePointAt(index)
            if (!dialect.isLiteral(code)) return index
            index += charCountOf(code)
        }
        return NONE
    }
}
