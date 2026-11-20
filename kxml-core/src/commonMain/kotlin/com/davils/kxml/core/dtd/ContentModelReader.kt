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

package com.davils.kxml.core.dtd

private const val OPEN: Char = '('
private const val CLOSE: Char = ')'
private const val CHOICE: Char = '|'
private const val SEQUENCE: Char = ','
private const val PCDATA: String = "#PCDATA"
private const val ZERO_OR_MORE: Char = '*'
private const val INDICATORS: String = "?*+"

internal class ContentModelReader(private val lexer: DtdLexer, private val maximumDepth: Int) {
    private val cursor = lexer.cursor

    fun read(start: Int) {
        val keyword = listOf(EMPTY, ANY).firstOrNull(lexer::isKeyword)
        if (keyword != null) return lexer.keyword(keyword)
        if (!cursor.startsWith(OPEN.toString())) lexer.invalid(start, "A content model, EMPTY or ANY was expected")
        group(start)
    }

    private fun group(start: Int) {
        lexer.punctuation(OPEN)
        lexer.space()
        if (cursor.startsWith(PCDATA)) {
            mixed(start)
            return
        }
        children(start)
    }

    private fun mixed(start: Int) {
        lexer.keyword(PCDATA)
        var names = 0
        lexer.space()
        while (!cursor.isAtEnd && cursor.peek() == CHOICE) {
            lexer.punctuation(CHOICE)
            lexer.space()
            lexer.name(isQualified = true)
            names++
            lexer.space()
        }
        lexer.punctuation(CLOSE)
        val hasStar = !cursor.isAtEnd && cursor.peek() == ZERO_OR_MORE
        if (names > 0 && !hasStar) lexer.invalid(start, "Mixed content with names must end with )*")
        if (hasStar) lexer.punctuation(ZERO_OR_MORE)
    }

    private fun children(start: Int) {
        val groups = ArrayDeque<Group>()
        groups.addLast(Group())
        while (groups.isNotEmpty()) {
            if (groups.size > maximumDepth) lexer.invalid(start, "The content model nests too deep")
            lexer.space()
            if (!cursor.isAtEnd && cursor.peek() == OPEN) {
                lexer.punctuation(OPEN)
                groups.addLast(Group())
                continue
            }
            lexer.name(isQualified = true)
            indicator()
            closeGroups(groups, start)
        }
    }

    private fun closeGroups(groups: ArrayDeque<Group>, start: Int) {
        var isOpen = true
        while (isOpen && groups.isNotEmpty()) {
            val group = groups.last()
            group.particles++
            lexer.space()
            val next = cursor.text.getOrNull(cursor.offset)
            if (next == CHOICE || next == SEQUENCE) {
                separate(group, next, start)
                isOpen = false
                continue
            }
            if (next != CLOSE) lexer.invalid(start, "A content model expects |, , or )")
            if (group.separator == CHOICE && group.particles < 2) lexer.invalid(start, "A choice needs two parts")
            lexer.punctuation(CLOSE)
            indicator()
            groups.removeLast()
        }
    }

    private fun separate(group: Group, separator: Char, start: Int) {
        val chosen = group.separator ?: separator
        if (chosen != separator) lexer.invalid(start, "A content model group may not mix | and ,")
        group.separator = chosen
        lexer.punctuation(separator)
    }

    private fun indicator() {
        val next = cursor.text.getOrNull(cursor.offset) ?: return
        if (next in INDICATORS) lexer.punctuation(next)
    }

    private class Group {
        var separator: Char? = null
        var particles: Int = 0
    }

    private companion object {
        const val EMPTY: String = "EMPTY"
        const val ANY: String = "ANY"
    }
}
