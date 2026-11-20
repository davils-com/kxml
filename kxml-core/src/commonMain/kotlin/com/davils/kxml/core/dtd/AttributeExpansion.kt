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

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.internal.textOf
import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.parser.Dialect
import com.davils.kxml.core.parser.failure

private const val AMPERSAND: Char = '&'
private const val SEMICOLON: Char = ';'
private const val HASH: Char = '#'
private const val HEX: Char = 'x'
private const val LESS_THAN: Char = '<'
private const val SPACE: Char = ' '
private const val WHITESPACE: String = " \t\r\n"
private const val HEX_RADIX: Int = 16
private const val DECIMAL_RADIX: Int = 10
private const val CHARACTER_PREFIX: Int = 2
private val predefined: Map<String, String> = mapOf(
    "lt" to "<",
    "gt" to ">",
    "amp" to "&",
    "apos" to "'",
    "quot" to "\"",
)

internal class AttributeExpansion(private val entities: EntityContext, private val dialect: Dialect) {
    fun expand(name: String, range: TextRange, expanding: List<String>): String {
        val stack = ArrayDeque<Frame>()
        push(stack = stack, name = name, range = range, expanding = expanding)
        return drained(stack, range, expanding)
    }

    fun normalize(raw: String, range: TextRange): String {
        val stack = ArrayDeque<Frame>()
        stack.addLast(Frame(name = "", text = raw))
        return drained(stack, range, expanding = emptyList())
    }

    private fun drained(stack: ArrayDeque<Frame>, range: TextRange, expanding: List<String>): String {
        val output = StringBuilder()
        val walk = Walk(stack = stack, range = range, expanding = expanding)
        while (stack.isNotEmpty()) {
            val frame = stack.last()
            if (frame.index >= frame.text.length) {
                stack.removeLast()
                continue
            }
            step(frame, output, walk)
        }
        return output.toString()
    }

    private fun step(frame: Frame, output: StringBuilder, walk: Walk) {
        val char = frame.text[frame.index]
        if (char == LESS_THAN) failure(XmlDiagnosticCode.InvalidAttribute, walk.range, "An entity puts < into a value")
        if (char == AMPERSAND) return reference(frame, output, walk)
        output.append(SPACE.takeIf { char in WHITESPACE } ?: char)
        frame.moveTo(frame.index + 1)
    }

    private fun reference(frame: Frame, output: StringBuilder, walk: Walk) {
        val end = frame.text.indexOf(SEMICOLON, frame.index)
        if (end < 0) failure(XmlDiagnosticCode.InvalidReference, walk.range, "An entity holds a reference without ;")
        val body = frame.text.substring(startIndex = frame.index + 1, endIndex = end)
        frame.moveTo(end + 1)
        val character = body.takeIf { it.startsWith(HASH) }?.let { reference -> characterOf(reference, walk.range) }
        val known = character ?: predefined[body]
        if (known != null) {
            output.append(known)
            return
        }
        if (!XmlCharacters.isName(body)) {
            failure(XmlDiagnosticCode.InvalidReference, walk.range, "An entity holds a reference that names nothing")
        }
        val outer = walk.expanding + walk.stack.map(Frame::name).filter(String::isNotEmpty)
        push(stack = walk.stack, name = body, range = walk.range, expanding = outer)
    }

    private fun push(stack: ArrayDeque<Frame>, name: String, range: TextRange, expanding: List<String>) {
        val entity = entities.general(name = name, range = range, expanding = expanding, isInAttribute = true)
        val replacement = entity.replacement.orEmpty()
        entities.budget.charge(length = replacement.length, depth = expanding.size + stack.size, range = range)
        stack.addLast(Frame(name, replacement))
    }

    private fun characterOf(body: String, range: TextRange): String {
        val isHex = body.getOrNull(1) == HEX
        val digits = body.substring(CHARACTER_PREFIX.takeIf { isHex } ?: 1)
        val code = digits.toIntOrNull(HEX_RADIX.takeIf { isHex } ?: DECIMAL_RADIX)
        if (code == null || !dialect.isReferable(code)) {
            failure(XmlDiagnosticCode.InvalidReference, range, "An entity refers to a character XML does not allow")
        }
        return textOf(code)
    }

    private class Frame(val name: String, val text: String) {
        var index: Int = 0
            private set

        fun moveTo(target: Int) {
            index = target
        }
    }

    private data class Walk(val stack: ArrayDeque<Frame>, val range: TextRange, val expanding: List<String>)
}
