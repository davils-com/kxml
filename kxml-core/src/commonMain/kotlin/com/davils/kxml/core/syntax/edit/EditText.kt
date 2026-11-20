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

package com.davils.kxml.core.syntax.edit

import com.davils.kxml.core.internal.charCountOf
import com.davils.kxml.core.internal.codePointAt
import com.davils.kxml.core.internal.textOf
import com.davils.kxml.core.parser.Dialect

private const val HEX_RADIX: Int = 16
private const val DOUBLE_QUOTE: Char = '"'
private const val CR: Int = 0x0D
private const val LF: Int = 0x0A
private const val TAB: Int = 0x09
private const val SURROGATES_START: Int = 0xD800
private const val SURROGATES_END: Int = 0xDFFF

private val markup: Map<Int, String> = mapOf(
    '&'.code to "&amp;",
    '<'.code to "&lt;",
    '>'.code to "&gt;",
)

internal fun escapedText(text: String, version: String): String? = escaped(text, version, quote = null)

internal fun escapedValue(value: String, version: String, quote: Char): String? = escaped(value, version, quote)

private fun escaped(text: String, version: String, quote: Char?): String? {
    val dialect = Dialect()
    dialect.adopt(version)
    val target = StringBuilder(text.length)
    var at = 0
    var isRepresentable = true
    while (at < text.length && isRepresentable) {
        val code = text.codePointAt(at)
        val piece = pieceOf(code, dialect, quote)
        isRepresentable = piece != null
        piece?.let(target::append)
        at += charCountOf(code)
    }
    return target.toString().takeIf { isRepresentable }
}

private fun pieceOf(code: Int, dialect: Dialect, quote: Char?): String? {
    val isRepresentable = code !in SURROGATES_START..SURROGATES_END && dialect.isReferable(code)
    if (!isRepresentable) return null
    val isQuote = quote != null && code == quote.code
    val isWhitespaceInValue = quote != null && (code == TAB || code == LF)
    val isLineSeparator = code < Char.MAX_VALUE.code && dialect.isLineSeparator(code.toChar())
    val isReference = code == CR || isWhitespaceInValue || isLineSeparator || !dialect.isLiteral(code)
    val reference = "&#x${code.toString(HEX_RADIX).uppercase()};".takeIf { isReference }
    val quoted = ("&quot;".takeIf { quote == DOUBLE_QUOTE } ?: "&apos;").takeIf { isQuote }
    return markup[code] ?: quoted ?: reference ?: textOf(code)
}
