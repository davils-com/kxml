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
import com.davils.kxml.core.dtd.AttributeDefaults
import com.davils.kxml.core.dtd.AttributeExpansion
import com.davils.kxml.core.dtd.DoctypeReader
import com.davils.kxml.core.dtd.Dtd
import com.davils.kxml.core.dtd.DtdLexer
import com.davils.kxml.core.dtd.EntityContext
import com.davils.kxml.core.dtd.ExpansionBudget
import com.davils.kxml.core.source.XmlEncoding
import com.davils.kxml.core.syntax.XmlSyntaxKind

private const val END_TAG: String = "</"
private const val COMMENT: String = "<!--"
private const val CDATA: String = "<![CDATA["
private const val INSTRUCTION: String = "<?"
private const val DOCTYPE: String = "<!DOCTYPE"
private const val MARKUP: String = "<!"
private const val START: String = "<"
private const val ANYTHING: String = ""
private const val BYTE_ORDER_MARK: String = "\uFEFF"
private const val REFERENCE_DELIMITERS: Int = 2

internal class XmlScanner(
    text: String,
    private val configuration: XmlConfiguration,
    private val encoding: XmlEncoding?,
    sink: SyntaxSink,
) {
    private val cursor = Cursor(text)
    private val syntax = FramedSink(sink, cursor)
    private val dialect = Dialect()
    private val budget = ParseBudget(configuration.limits)
    private val expansion = ExpansionBudget(configuration.limits)
    private val entities = EntityContext(expansion)
    private val characters = CharacterReader(
        cursor = cursor,
        budget = budget,
        sink = syntax,
        dialect = dialect,
        entities = entities,
    )
    private val entered = ArrayDeque<Int>()
    private val tags = TagReader(cursor = cursor, budget = budget, characters = characters, sink = syntax)
    private val markup = MarkupReader(cursor = cursor, budget = budget, tags = tags, sink = syntax, dialect = dialect)
    private val declarations = DeclarationReader(cursor, characters, syntax)
    private val resolver = NamespaceResolver(dialect)
    private val open = ArrayDeque<OpenElement>()
    private val pending = ArrayDeque<XmlEvent>()
    private val noticed = mutableListOf<XmlDiagnostic>()
    private var hasRoot = false
    private var hasDoctype = false
    private var isStarted = false
    private var isStandalone = false
    private var dtd: Dtd? = null

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

    val warnings: List<XmlDiagnostic> get() = noticed.toList()

    fun next(): XmlEvent? {
        if (!isStarted) return start()
        return pending.removeFirstOrNull() ?: produce()
    }

    private fun start(): XmlEvent? {
        isStarted = true
        syntax.startNode(XmlSyntaxKind.DOCUMENT)
        if (cursor.startsWith(BYTE_ORDER_MARK)) {
            cursor.advance(BYTE_ORDER_MARK.length)
            syntax.token(kind = XmlSyntaxKind.BYTE_ORDER_MARK, start = 0, end = cursor.offset)
        }
        if (!declarations.isAhead()) return next()
        val declaration = declarations.read(encoding)
        dialect.adopt(declaration.declaration.version)
        cursor.isLineSeparatorSpace = dialect.isVersion11
        isStandalone = declaration.declaration.isStandalone == true
        entities.isStandalone = isStandalone
        return declaration
    }

    private fun produce(): XmlEvent? {
        var event: XmlEvent? = null
        while (event == null && open.isNotEmpty()) event = content()
        return event ?: outside()
    }

    private fun content(): XmlEvent? {
        while (cursor.isAtEnd && cursor.depth > 0) leaveEntity()
        if (cursor.isAtEnd) {
            failure(
                XmlDiagnosticCode.UnclosedElement,
                TextRange.at(cursor.offset),
                "The element <${open.last().qualified}> is not closed",
            )
        }
        if (characters.isExpandableReference()) return enterEntity()
        val (_, reader) = contentReaders.first { (prefix, _) -> cursor.startsWith(prefix) }
        return reader()
    }

    private fun enterEntity(): XmlEvent? {
        val start = cursor.offset
        val name = characters.referencedName().orEmpty()
        cursor.advance(name.length + REFERENCE_DELIMITERS)
        syntax.token(kind = XmlSyntaxKind.ENTITY_REFERENCE, start = start, end = cursor.offset)
        val range = cursor.rangeFrom(start)
        val entity = entities.general(name = name, range = range, expanding = cursor.names, isInAttribute = false)
        val replacement = entity.replacement.orEmpty()
        expansion.charge(length = replacement.length, depth = cursor.depth, range = range)
        entered.addLast(open.size)
        cursor.push(text = replacement, anchor = range, name = name, isReplacement = true)
        return null
    }

    private fun leaveEntity() {
        val depth = entered.removeLast()
        if (open.size != depth) {
            failure(
                XmlDiagnosticCode.UnbalancedEntity,
                cursor.rangeFrom(cursor.offset),
                "The entity &${cursor.names.last()}; leaves an element open",
            )
        }
        cursor.pop()
    }

    private fun outside(): XmlEvent? {
        cursor.skipWhitespace(syntax)
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
        syntax.finishNode()
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
        if (hasDoctype || hasRoot) {
            failure(
                XmlDiagnosticCode.MisplacedDoctype,
                range,
                "A document type may only stand once, before the root element",
            )
        }
        hasDoctype = true
        val lexer = DtdLexer(cursor = cursor, sink = syntax, dialect = dialect, budget = budget)
        val reader = DoctypeReader(
            lexer = lexer,
            markup = markup,
            limits = configuration.limits,
            entities = entities,
            expansion = AttributeExpansion(entities, dialect),
            isStandalone = isStandalone,
        )
        val read = reader.read()
        dtd = read
        entities.dtd = read
        return XmlEvent.DocumentType(read.declaration(), cursor.rangeFrom(start))
    }

    private fun startTag(): XmlEvent {
        val read = tags.startTag()
        val tag = dtd?.let { declared -> AttributeDefaults.applied(read, declared) } ?: read
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
        if (cursor.depth > 0 && open.size <= entered.last()) {
            failure(
                XmlDiagnosticCode.UnbalancedEntity,
                tag.range,
                "The entity &${cursor.names.last()}; closes an element it did not open",
            )
        }
        val top = open.removeLast()
        if (tag.qualified != top.qualified) {
            failure(
                XmlDiagnosticCode.MismatchedTag,
                tag.range,
                "The element <${top.qualified}> is closed by </${tag.qualified}>",
            )
        }
        resolver.close()
        syntax.finishNode()
        return XmlEvent.EndElement(top.name, tag.range)
    }
}
