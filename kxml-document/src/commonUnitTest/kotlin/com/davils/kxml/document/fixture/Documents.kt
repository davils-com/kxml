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

package com.davils.kxml.document.fixture

import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.xml
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map

private const val MOST_STEPS: Int = 40
private const val CLOSING: Int = 1

private val pieces: List<String> = listOf(
    "<a x='1' y='2'>", "", "t &amp; u", "<![CDATA[<c>]]>", "<!-- c -->", "<?pi d?>", "<c z='3'/>",
    "<p:d xmlns:p='urn:p' p:w='4'>", "\n", "&#x41;", "<b>",
)
private val openings: Set<Int> = setOf(0, 7, 10)

private val codeLists: Arb<List<Int>> = Arb.list(Arb.int(pieces.indices), 0..MOST_STEPS)

internal val documentTexts: Arb<String> = codeLists.map(::documentOf)

internal fun documentOf(text: String): XmlDocument {
    val result = xml { }.parse(text)
    val parsed = result.shouldBeInstanceOf<XmlParseResult.Parsed>()
    return parsed.document
}

private fun documentOf(codes: List<Int>): String {
    val open = ArrayDeque<String>()
    val body = StringBuilder()
    codes.forEach { code ->
        val piece = pieces[code]
        if (code == CLOSING && open.isNotEmpty()) body.append("</${open.removeLast()}>")
        body.append(piece)
        val opened = piece.removePrefix("<")
        if (code in openings) open.addLast(opened.substringBefore(' ').substringBefore('>'))
    }
    while (open.isNotEmpty()) body.append("</${open.removeLast()}>")
    return "<!--p--><r>$body</r><?e?>"
}
