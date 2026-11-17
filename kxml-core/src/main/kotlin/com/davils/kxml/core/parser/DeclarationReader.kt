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
import com.davils.kxml.core.XmlEvent
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.node.XmlDeclaration
import com.davils.kxml.core.source.XmlEncoding

private const val OPEN: String = "<?xml"
private const val CLOSE: String = "?>"
private const val VERSION: String = "version"
private const val ENCODING: String = "encoding"
private const val STANDALONE: String = "standalone"
private const val EQUALS: Char = '='
private const val YES: String = "yes"
private const val NO: String = "no"

private val versionPattern: Regex = Regex("1\\.[0-9]+")
private val encodingPattern: Regex = Regex("[A-Za-z][A-Za-z0-9._-]*")
private val compatible: Map<XmlEncoding, Set<String>> = mapOf(
    XmlEncoding.Utf8 to setOf("UTF-8", "UTF8", "US-ASCII", "ASCII"),
    XmlEncoding.Utf16LittleEndian to setOf("UTF-16", "UTF-16LE"),
    XmlEncoding.Utf16BigEndian to setOf("UTF-16", "UTF-16BE"),
)

internal class DeclarationReader(private val cursor: Cursor, private val characters: CharacterReader) {
    fun isAhead(): Boolean {
        val next = cursor.text.getOrNull(OPEN.length)
        val isSeparated = next != null && XmlCharacters.isWhitespace(next)
        return cursor.startsWith(OPEN) && isSeparated
    }

    fun read(encoding: XmlEncoding?): XmlEvent.Declaration {
        val start = cursor.offset
        cursor.advance(OPEN.length)
        val version = pseudoAttribute(VERSION, start) ?: invalid(start, "The declaration needs a version")
        if (!versionPattern.matches(version)) {
            failure(
                XmlDiagnosticCode.UnsupportedVersion,
                cursor.rangeFrom(start),
                "kxml reads XML 1.x, but the document declares $version",
            )
        }
        val declared = pseudoAttribute(ENCODING, start)
        if (declared != null && !encodingPattern.matches(declared)) invalid(start, "The encoding $declared is no name")
        checkEncoding(declared, encoding, start)
        val standalone = pseudoAttribute(STANDALONE, start)
        if (standalone != null && standalone != YES && standalone != NO) invalid(start, "standalone is yes or no")
        cursor.skipWhitespace()
        if (!cursor.startsWith(CLOSE)) invalid(start, "The declaration must end with $CLOSE")
        cursor.advance(CLOSE.length)
        val declaration = XmlDeclaration(version, declared, standalone?.let { value -> value == YES })
        return XmlEvent.Declaration(declaration, cursor.rangeFrom(start))
    }

    private fun pseudoAttribute(name: String, start: Int): String? {
        val mark = cursor.offset
        val hasSpace = cursor.skipWhitespace()
        val isPresent = hasSpace && cursor.startsWith(name)
        if (!isPresent) cursor.moveTo(mark)
        return name.takeIf { isPresent }?.let { valueOf(it, start) }
    }

    private fun valueOf(name: String, start: Int): String {
        cursor.advance(name.length)
        cursor.skipWhitespace()
        if (cursor.isAtEnd || cursor.peek() != EQUALS) invalid(start, "The pseudo attribute $name needs =")
        cursor.advance()
        cursor.skipWhitespace()
        return characters.attributeValue()
    }

    private fun checkEncoding(declared: String?, detected: XmlEncoding?, start: Int) {
        val isCompatible = declared == null || detected == null || declared.uppercase() in compatible.getValue(detected)
        if (isCompatible) return
        failure(
            XmlDiagnosticCode.UnsupportedEncoding,
            cursor.rangeFrom(start),
            "The document declares $declared, but is $detected; kxml reads UTF-8 and UTF-16",
        )
    }

    private fun invalid(start: Int, message: String): Nothing =
        failure(XmlDiagnosticCode.InvalidDeclaration, TextRange(start, cursor.offset), message)
}
