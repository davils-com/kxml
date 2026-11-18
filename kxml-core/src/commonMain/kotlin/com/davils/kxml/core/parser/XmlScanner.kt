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
import com.davils.kxml.core.XmlConfiguration
import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.XmlEvent
import com.davils.kxml.core.diagnostic.XmlDiagnostic
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.source.XmlEncoding

private const val END_TAG: String = "</"
private const val COMMENT: String = "<!--"
private const val CDATA: String = "<![CDATA["
private const val INSTRUCTION: String = "<?"
private const val DOCTYPE: String = "<!DOCTYPE"
private const val MARKUP: String = "<!"
private const val START: String = "<"
private const val ANYTHING: String = ""

internal class XmlScanner(
    text: String,
    private val configuration: XmlConfiguration,
    private val encoding: XmlEncoding?,
) {
    private val cursor = Cursor(text)
    private val budget = ParseBudget(configuration.limits)
    private val characters = CharacterReader(cursor, budget)
    private val tags = TagReader(cursor, budget, characters)
    private val markup = MarkupReader(cursor, budget, tags)
    private val declarations = DeclarationReader(cursor, characters)
    private val resolver = NamespaceResolver()
    private val open = ArrayDeque<OpenElement>()
    private val pending = ArrayDeque<XmlEvent>()
    private val scanned = mutableListOf<XmlEvent>()
    private val noticed = mutableListOf<XmlDiagnostic>()
    private var hasRoot = false
    private var hasDoctype = false

    private val contentReaders: List<Pair<String, () -> XmlEvent>> = listOf(
        END_TAG to ::endTag,
        COMMENT to markup::comment,
        CDATA to markup::cdata,
        INSTRUCTION to markup::instruction,
        MARKUP to ::strayMarkup,
        START to ::startTag,
        ANYTHING to characters::text,
    )

    private val outsideReaders: List<Pair<String, () -> XmlEvent?>> = listOf(
        COMMENT to markup::comment,
        INSTRUCTION to markup::instruction,
        DOCTYPE to ::doctype,
        MARKUP to ::strayMarkup,
        START to ::root,
        ANYTHING to ::strayText,
    )

    val events: List<XmlEvent> get() = scanned.toList()

    val warnings: List<XmlDiagnostic> get() = noticed.toList()

    fun scan() {
        if (declarations.isAhead()) scanned += declarations.read(encoding)
        generateSequence(::next).forEach(scanned::add)
    }

    private fun next(): XmlEvent? = pending.removeFirstOrNull() ?: produce()

    private fun produce(): XmlEvent? {
        if (open.isNotEmpty()) return content()
        return outside()
    }

    private fun content(): XmlEvent {
        if (cursor.isAtEnd) {
            failure(
                XmlDiagnosticCode.UnclosedElement,
                TextRange.at(cursor.offset),
                "The element <${open.last().qualified}> is not closed",
            )
        }
        val (_, reader) = contentReaders.first { (prefix, _) -> cursor.startsWith(prefix) }
        return reader()
    }

    private fun outside(): XmlEvent? {
        cursor.skipWhitespace()
        if (cursor.isAtEnd) return finish()
        val (_, reader) = outsideReaders.first { (prefix, _) -> cursor.startsWith(prefix) }
        return reader()
    }

    private fun finish(): XmlEvent? {
        if (!hasRoot) {
            failure(
                XmlDiagnosticCode.MissingRoot,
                TextRange.at(cursor.offset),
                "There is no root element",
            )
        }
        return null
    }

    private fun strayText(): XmlEvent = failure(
        XmlDiagnosticCode.ContentAfterRoot.takeIf { hasRoot } ?: XmlDiagnosticCode.UnexpectedContent,
        TextRange.at(cursor.offset),
        "Text may only stand inside the root element",
    )

    private fun strayMarkup(): XmlEvent = failure(
        XmlDiagnosticCode.UnexpectedContent,
        TextRange.at(cursor.offset),
        "Markup declarations may only stand inside a document type declaration",
    )

    private fun root(): XmlEvent {
        if (hasRoot) {
            failure(
                XmlDiagnosticCode.ContentAfterRoot,
                TextRange.at(cursor.offset),
                "There is a second root element",
            )
        }
        hasRoot = true
        return startTag()
    }

    private fun doctype(): XmlEvent? {
        val start = cursor.offset
        val range = TextRange.of(start, DOCTYPE.length)
        if (configuration.doctype == XmlDoctype.Refuse) {
            failure(XmlDiagnosticCode.RefusedDoctype, range, "Document type declarations are refused")
        }
        if (hasDoctype) failure(XmlDiagnosticCode.MisplacedDoctype, range, "There is a second document type")
        markup.skipDoctype()
        hasDoctype = true
        noticed += XmlDiagnostic(
            XmlDiagnosticCode.IgnoredDoctype,
            cursor.rangeFrom(start),
            "The document type declaration was ignored",
        )
        return next()
    }

    private fun startTag(): XmlEvent {
        val tag = tags.startTag()
        budget.enter(open.size + 1, tag.range)
        val resolved = resolver.open(tag)
        val event = XmlEvent.StartElement(
            name = resolved.name,
            attributes = resolved.attributes,
            namespaces = resolved.namespaces,
            isEmpty = tag.isEmpty,
            range = tag.range,
        )
        enter(tag, resolved)
        return event
    }

    private fun enter(tag: RawTag, resolved: ResolvedTag) {
        if (!tag.isEmpty) {
            open.addLast(OpenElement(tag.qualified, resolved.name))
            return
        }
        resolver.close()
        pending += XmlEvent.EndElement(resolved.name, tag.range)
    }

    private fun endTag(): XmlEvent {
        val tag = tags.endTag()
        val top = open.removeLast()
        if (tag.qualified != top.qualified) {
            failure(
                XmlDiagnosticCode.MismatchedTag,
                tag.range,
                "The element <${top.qualified}> is closed by </${tag.qualified}>",
            )
        }
        resolver.close()
        return XmlEvent.EndElement(top.name, tag.range)
    }
}
