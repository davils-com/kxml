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

package com.davils.kxml.core.syntax.fixture

import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list

private const val MOST_STEPS: Int = 40
private const val NEWLINE: String = "\n"
private const val CLOSING: Int = 1

private val pieces: List<String> = listOf(
    "<a x='1'>", "", "t &amp; u", "<![CDATA[<c>]]>", "<!-- c -->", "<?pi d?>", "<c y=\"&lt;\"/>",
    "<p:d p:z='2'>", "\n  ", "&#x41;", "<b>", "\r\n", "<s xml:space='preserve'>", "<t xml:space='default'>", " ",
)
private val openings: Set<Int> = setOf(0, 7, 10, 12, 13)

internal val documents: Arb<String> =
    Arb.bind(Arb.list(Arb.int(0..<pieces.size), 0..MOST_STEPS), Arb.int(0..2)) { codes, version ->
        documentOf(codes, version)
    }

private fun StringBuilder.close(name: String) {
    append("</")
    append(name)
    append('>')
}

private fun documentOf(codes: List<Int>, version: Int): String {
    val open = ArrayDeque<String>()
    val body = StringBuilder()
    codes.forEach { code -> step(code, open, body) }
    while (open.isNotEmpty()) body.close(open.removeLast())
    val declaration = listOf("", "<?xml version='1.0'?>", """<?xml version="1.1"?>""" + NEWLINE)[version]
    return "$declaration<r xmlns:p='urn:p'>$body</r>"
}

private fun step(code: Int, open: ArrayDeque<String>, body: StringBuilder) {
    val piece = pieces[code]
    val opened = piece.removePrefix("<")
    val name = opened.substringBefore(' ').substringBefore('>')
    val isClosing = code == CLOSING && open.isNotEmpty()
    if (isClosing) body.close(open.removeLast())
    body.append(piece)
    if (code in openings) open.addLast(name)
}
