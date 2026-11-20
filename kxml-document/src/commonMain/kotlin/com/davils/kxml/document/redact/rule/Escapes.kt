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
package com.davils.kxml.document.redact.rule

private val textEscapes: Map<Char, String> = mapOf('&' to "&amp;", '<' to "&lt;", '>' to "&gt;", '\r' to "&#xD;")

internal fun escapedText(value: String): String = escaped(value, textEscapes)

internal fun escapedAttribute(value: String, quote: Char): String {
    val escapes = mapOf(
        '&' to "&amp;",
        '<' to "&lt;",
        quote to "&#${quote.code};",
        '\t' to "&#x9;",
        '\n' to "&#xA;",
        '\r' to "&#xD;",
    )
    return escaped(value, escapes)
}

private fun escaped(value: String, escapes: Map<Char, String>): String {
    val written = StringBuilder(value.length)
    value.forEach { char -> written.append(escapes[char] ?: char) }
    return written.toString()
}
