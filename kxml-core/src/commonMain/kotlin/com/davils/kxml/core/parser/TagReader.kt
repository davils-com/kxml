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
import com.davils.kxml.core.internal.NameSet
import com.davils.kxml.core.internal.charCountOf
import com.davils.kxml.core.name.XmlCharacters

private const val END_TAG_OPEN: String = "</"
private const val TAG_CLOSE: Char = '>'
private const val EMPTY_CLOSE: String = "/>"
private const val EQUALS: Char = '='

internal class TagReader(
    private val cursor: Cursor,
    private val budget: ParseBudget,
    private val characters: CharacterReader,
) {
    fun name(): String {
        val start = cursor.offset
        if (cursor.isAtEnd || !XmlCharacters.isNameStart(cursor.codePoint())) {
            failure(XmlDiagnosticCode.InvalidName, TextRange.at(start), "A name was expected")
        }
        while (!cursor.isAtEnd && XmlCharacters.isNameCharacter(cursor.codePoint())) {
            cursor.advance(charCountOf(cursor.codePoint()))
        }
        budget.name(cursor.offset - start, cursor.rangeFrom(start))
        return cursor.text.substring(startIndex = start, endIndex = cursor.offset)
    }

    fun startTag(): RawTag {
        val start = cursor.offset
        cursor.advance()
        val name = qualifiedName()
        val attributes = mutableListOf<RawAttribute>()
        val names = NameSet()
        while (!isTagEnd(start)) {
            attributes += attribute(names)
            budget.attributes(attributes.size, cursor.rangeFrom(start))
        }
        val isEmpty = cursor.startsWith(EMPTY_CLOSE)
        cursor.advance(EMPTY_CLOSE.length.takeIf { isEmpty } ?: 1)
        return RawTag(
            qualified = name,
            attributes = attributes.toList(),
            isEmpty = isEmpty,
            range = cursor.rangeFrom(start),
        )
    }

    fun endTag(): RawTag {
        val start = cursor.offset
        cursor.advance(END_TAG_OPEN.length)
        val name = qualifiedName()
        cursor.skipWhitespace()
        if (cursor.isAtEnd || cursor.peek() != TAG_CLOSE) {
            failure(
                XmlDiagnosticCode.UnexpectedContent,
                cursor.rangeFrom(start),
                "The end tag of $name must close with >",
            )
        }
        cursor.advance()
        return RawTag(qualified = name, attributes = emptyList(), isEmpty = false, range = cursor.rangeFrom(start))
    }

    private fun qualifiedName(): String {
        val start = cursor.offset
        val name = name()
        if (!XmlCharacters.isQualifiedName(name)) {
            failure(
                XmlDiagnosticCode.InvalidName,
                cursor.rangeFrom(start),
                "The name $name is not a qualified name",
            )
        }
        return name
    }

    private fun isTagEnd(start: Int): Boolean {
        val hasSpace = cursor.skipWhitespace()
        if (cursor.isAtEnd) {
            failure(
                XmlDiagnosticCode.UnexpectedEnd,
                cursor.rangeFrom(start),
                "The tag is not closed",
            )
        }
        val isEnd = cursor.startsWith(EMPTY_CLOSE) || cursor.peek() == TAG_CLOSE
        if (!isEnd && !hasSpace) {
            failure(
                XmlDiagnosticCode.UnexpectedContent,
                TextRange.at(cursor.offset),
                "Attributes must be separated by whitespace",
            )
        }
        return isEnd
    }

    private fun attribute(names: NameSet): RawAttribute {
        val start = cursor.offset
        val name = qualifiedName()
        if (!names.add(name)) {
            failure(
                XmlDiagnosticCode.DuplicateAttribute,
                cursor.rangeFrom(start),
                "The attribute $name is set twice",
            )
        }
        cursor.skipWhitespace()
        if (cursor.isAtEnd || cursor.peek() != EQUALS) {
            failure(
                XmlDiagnosticCode.InvalidAttribute,
                TextRange.at(cursor.offset),
                "The attribute $name needs =",
            )
        }
        cursor.advance()
        cursor.skipWhitespace()
        val value = characters.attributeValue()
        return RawAttribute(name, value, cursor.rangeFrom(start))
    }
}
