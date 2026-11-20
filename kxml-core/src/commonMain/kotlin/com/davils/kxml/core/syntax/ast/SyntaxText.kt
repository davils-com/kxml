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

package com.davils.kxml.core.syntax.ast

import com.davils.kxml.core.internal.textOf
import com.davils.kxml.core.syntax.XmlSyntaxElement
import com.davils.kxml.core.syntax.XmlSyntaxKind
import com.davils.kxml.core.syntax.XmlSyntaxNode

private const val CR: Char = '\r'
private const val LF: Char = '\n'
private const val NEL: Char = '\u0085'
private const val LINE_SEPARATOR: Char = '\u2028'
private const val SPACE: Char = ' '
private const val TAB: Char = '\t'
private const val HEX_PREFIX: String = "&#x"
private const val DECIMAL_PREFIX: String = "&#"
private const val HEX_RADIX: Int = 16
private const val DECIMAL_RADIX: Int = 10
private const val VERSION_11: String = "1.1"

private val predefined: Map<String, String> = mapOf(
    "&lt;" to "<",
    "&gt;" to ">",
    "&amp;" to "&",
    "&apos;" to "'",
    "&quot;" to "\"",
)

private val literalKinds: Set<XmlSyntaxKind> = setOf(
    XmlSyntaxKind.TEXT,
    XmlSyntaxKind.WHITESPACE,
    XmlSyntaxKind.CDATA_TEXT,
    XmlSyntaxKind.ATTRIBUTE_TEXT,
)

internal fun decodedText(parts: List<XmlSyntaxElement>, version: String, isAttribute: Boolean): String? {
    val decoded = StringBuilder()
    val isComplete = parts.all { part ->
        appendDecoded(decoded = decoded, part = part, version = version, isAttribute = isAttribute)
    }
    return decoded.toString().takeIf { isComplete }
}

internal fun versionOf(node: XmlSyntaxNode): String = XmlDocumentSyntax(node.ancestors.last()).version

private fun appendDecoded(
    decoded: StringBuilder,
    part: XmlSyntaxElement,
    version: String,
    isAttribute: Boolean,
): Boolean {
    if (part.kind == XmlSyntaxKind.CDATA_SECTION) {
        val inner = (part as XmlSyntaxNode).token(XmlSyntaxKind.CDATA_TEXT)?.text.orEmpty()
        decoded.append(normalizedLines(inner, version))
        return true
    }
    val text = part.text
    if (part.kind in literalKinds) decoded.append(literal(text, version, isAttribute))
    val reference = referenceOf(part)
    reference?.let(decoded::append)
    return part.kind in literalKinds || reference != null
}

private fun referenceOf(part: XmlSyntaxElement): String? {
    val text = part.text
    if (part.kind == XmlSyntaxKind.ENTITY_REFERENCE) return predefined[text]
    val isCharacter = part.kind == XmlSyntaxKind.CHARACTER_REFERENCE
    return text.takeIf { isCharacter }?.let(::characterOf)
}

private fun characterOf(reference: String): String? {
    val isHex = reference.startsWith(HEX_PREFIX)
    val digits = reference.removePrefix(HEX_PREFIX.takeIf { isHex } ?: DECIMAL_PREFIX).removeSuffix(";")
    val codePoint = digits.toIntOrNull(HEX_RADIX.takeIf { isHex } ?: DECIMAL_RADIX)
    return codePoint?.let(::textOf)
}

private fun literal(text: String, version: String, isAttribute: Boolean): String {
    val lines = normalizedLines(text, version)
    if (!isAttribute) return lines
    val spaced = lines.map { character -> SPACE.takeIf { character == LF || character == TAB } ?: character }
    return spaced.joinToString(separator = "")
}

private fun normalizedLines(text: String, version: String): String {
    val isVersion11 = version == VERSION_11
    val normalized = StringBuilder(text.length)
    var at = 0
    while (at < text.length) {
        val character = text[at]
        val next = text.getOrNull(at + 1)
        val isNelPair = isVersion11 && next == NEL
        val isPair = character == CR && (next == LF || isNelPair)
        val isLine11End = isVersion11 && (character == NEL || character == LINE_SEPARATOR)
        val isLineEnd = character == CR || isLine11End
        normalized.append(LF.takeIf { isLineEnd } ?: character)
        at += 2.takeIf { isPair } ?: 1
    }
    return normalized.toString()
}
