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
import com.davils.kxml.core.name.XmlCharacters

private const val CRLF: String = "\r\n"
private const val CARRIAGE_RETURN: Char = '\r'
private const val LINE_FEED: Char = '\n'
private const val NONE: Int = -1

internal object Sections {
    fun normalized(text: String, from: Int, to: Int): String {
        val raw = text.substring(startIndex = from, endIndex = to)
        val invalid = invalidOffset(raw)
        if (invalid != NONE) {
            failure(
                XmlDiagnosticCode.InvalidCharacter,
                TextRange.of(start = from + invalid, length = 1),
                "The character ${unicodeNameOf(raw.codePointAt(invalid))} is not allowed in XML",
            )
        }
        val joined = raw.replace(oldValue = CRLF, newValue = LINE_FEED.toString())
        return joined.replace(oldChar = CARRIAGE_RETURN, newChar = LINE_FEED)
    }

    private fun invalidOffset(raw: String): Int {
        var index = 0
        while (index < raw.length) {
            val code = raw.codePointAt(index)
            if (!XmlCharacters.isCharacter(code)) return index
            index += charCountOf(code)
        }
        return NONE
    }
}
