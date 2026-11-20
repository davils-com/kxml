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

package com.davils.kxml.core.emit

import com.davils.kxml.core.internal.charCountOf
import com.davils.kxml.core.internal.codePointAt
import com.davils.kxml.core.internal.textOf
import com.davils.kxml.core.parser.Dialect

private const val DOUBLE_QUOTE: Char = '"'
private const val CR: Int = 0x0D
private const val LF: Int = 0x0A
private const val TAB: Int = 0x09
private const val SURROGATES_START: Int = 0xD800
private const val SURROGATES_END: Int = 0xDFFF
private const val VERSION_11: String = "1.1"
private const val CDATA_START: String = "<![CDATA["
private const val CDATA_END: String = "]]>"
private const val CDATA_SPLIT: String = "]]]]><![CDATA[>"
private const val UNREPRESENTABLE: String = "The value holds characters this version of XML cannot carry"

private val markup: Map<Int, String> = mapOf(
    '&'.code to "&amp;",
    '<'.code to "&lt;",
    '>'.code to "&gt;",
)

internal object XmlEscaping {
    fun text(value: String, version: String): String =
        requireNotNull(escapedOrNull(value, version, quote = null)) { UNREPRESENTABLE }

    fun attribute(value: String, version: String): String =
        requireNotNull(escapedOrNull(value, version, quote = DOUBLE_QUOTE)) { UNREPRESENTABLE }

    fun escapedOrNull(value: String, version: String, quote: Char?): String? {
        val dialect = dialectOf(version)
        val target = StringBuilder(value.length)
        var at = 0
        var isRepresentable = true
        while (at < value.length && isRepresentable) {
            val code = value.codePointAt(at)
            val piece = pieceOf(code, dialect, quote)
            isRepresentable = piece != null
            piece?.let(target::append)
            at += charCountOf(code)
        }
        return target.toString().takeIf { isRepresentable }
    }

    fun isLiteral(value: String, version: String): Boolean {
        val dialect = dialectOf(version)
        var at = 0
        var isLiteral = true
        while (at < value.length && isLiteral) {
            val code = value.codePointAt(at)
            isLiteral = isSafeLiteral(code, dialect)
            at += charCountOf(code)
        }
        return isLiteral
    }

    fun cdata(value: String, version: String): String {
        val dialect = dialectOf(version)
        val target = StringBuilder(CDATA_START)
        val run = StringBuilder()
        var at = 0
        while (at < value.length) {
            val code = value.codePointAt(at)
            if (isSafeLiteral(code, dialect)) run.append(textOf(code))
            if (!isSafeLiteral(code, dialect)) {
                target.append(split(run))
                run.clear()
                target.append(CDATA_END)
                target.append(text(value = textOf(code), version = version))
                target.append(CDATA_START)
            }
            at += charCountOf(code)
        }
        target.append(split(run))
        target.append(CDATA_END)
        return target.toString()
    }

    private fun split(run: CharSequence): String = run.toString().replace(oldValue = CDATA_END, newValue = CDATA_SPLIT)

    private fun dialectOf(version: String): Dialect {
        val dialect = Dialect()
        dialect.adopt(version.takeIf { it == VERSION_11 }.orEmpty())
        return dialect
    }

    private fun isSafeLiteral(code: Int, dialect: Dialect): Boolean {
        val isSurrogate = code in SURROGATES_START..SURROGATES_END
        val isLineSeparator = code <= Char.MAX_VALUE.code && dialect.isLineSeparator(code.toChar())
        return code != CR && !isSurrogate && !isLineSeparator && dialect.isLiteral(code)
    }

    private fun pieceOf(code: Int, dialect: Dialect, quote: Char?): String? {
        val isRepresentable = code !in SURROGATES_START..SURROGATES_END && dialect.isReferable(code)
        if (!isRepresentable) return null
        val isQuote = quote != null && code == quote.code
        val isWhitespaceInValue = quote != null && (code == TAB || code == LF)
        val isReference = code == CR || isWhitespaceInValue || !isSafeLiteral(code, dialect)
        val reference = "&#$code;".takeIf { isReference }
        val quoted = ("&quot;".takeIf { quote == DOUBLE_QUOTE } ?: "&apos;").takeIf { isQuote }
        return markup[code] ?: quoted ?: reference ?: textOf(code)
    }
}
