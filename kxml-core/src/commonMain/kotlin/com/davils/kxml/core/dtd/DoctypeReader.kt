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

@file:OptIn(DavilsInternal::class)

package com.davils.kxml.core.dtd

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.node.XmlNotation
import com.davils.kxml.core.parser.MarkupReader
import com.davils.kxml.core.parser.failure
import com.davils.kxml.core.syntax.XmlSyntaxKind

private const val DOCTYPE: String = "<!DOCTYPE"
private const val ENTITY: String = "<!ENTITY"
private const val ATTLIST: String = "<!ATTLIST"
private const val ELEMENT: String = "<!ELEMENT"
private const val NOTATION: String = "<!NOTATION"
private const val COMMENT: String = "<!--"
private const val INSTRUCTION: String = "<?"
private const val SYSTEM: String = "SYSTEM"
private const val PUBLIC: String = "PUBLIC"
private const val NDATA: String = "NDATA"
private const val REQUIRED: String = "#REQUIRED"
private const val IMPLIED: String = "#IMPLIED"
private const val FIXED: String = "#FIXED"
private const val SUBSET_OPEN: String = "["
private const val SUBSET_CLOSE: String = "]"
private const val PERCENT: String = "%"
private const val SEMICOLON: Char = ';'
private const val SPACE: Char = ' '
private const val ENUMERATION_OPEN: Char = '('
private const val ENUMERATION_CLOSE: Char = ')'
private const val ALTERNATIVE: Char = '|'
private val predefined: Set<String> = setOf("lt", "gt", "amp", "apos", "quot")

internal class DoctypeReader(
    private val lexer: DtdLexer,
    markup: MarkupReader,
    private val limits: XmlLimits,
    private val entities: EntityContext,
    private val expansion: AttributeExpansion,
    private val isStandalone: Boolean,
) {
    private val budget = entities.budget
    private val cursor = lexer.cursor
    private val sink = lexer.sink
    private val models = ContentModelReader(lexer, limits.maximumDepth)
    private val literals = DtdLiterals(lexer)
    private val declarations: List<Pair<String, (Dtd) -> Unit>> = listOf(
        ENTITY to ::entity,
        ATTLIST to ::attributeList,
        ELEMENT to { _ -> element() },
        NOTATION to ::notation,
        COMMENT to { _ -> markup.comment() },
        INSTRUCTION to { _ -> markup.instruction() },
        PERCENT to ::parameterReference,
        "" to { _ -> lexer.invalid(cursor.offset, "A markup declaration was expected") },
    )

    fun read(): Dtd {
        val start = cursor.offset
        sink.startNode(XmlSyntaxKind.DOCTYPE)
        lexer.keyword(DOCTYPE, XmlSyntaxKind.DOCTYPE_OPEN)
        lexer.requiredSpace("before the name of a document type")
        val dtd = Dtd(lexer.name(isQualified = true))
        entities.dtd = dtd
        val hasSpace = lexer.space()
        val hasExternalId = hasSpace && (lexer.isKeyword(SYSTEM) || lexer.isKeyword(PUBLIC))
        if (hasExternalId) {
            val (publicId, systemId) = externalId(isSystemOptional = false)
            dtd.publicId = publicId
            dtd.systemId = systemId
            lexer.space()
        }
        if (cursor.startsWith(SUBSET_OPEN)) internalSubset(dtd)
        lexer.close(start)
        sink.finishNode()
        return dtd
    }

    private fun internalSubset(dtd: Dtd) {
        sink.startNode(XmlSyntaxKind.INTERNAL_SUBSET)
        lexer.keyword(SUBSET_OPEN, XmlSyntaxKind.SUBSET_OPEN)
        val depth = cursor.depth
        while (isInSubset(depth)) {
            val (_, read) = declarations.first { (prefix, _) -> cursor.startsWith(prefix) }
            read(dtd)
        }
        lexer.keyword(SUBSET_CLOSE, XmlSyntaxKind.SUBSET_CLOSE)
        sink.finishNode()
    }

    private fun isInSubset(depth: Int): Boolean {
        lexer.space()
        while (cursor.isAtEnd && cursor.depth > depth) {
            cursor.pop()
            lexer.space()
        }
        if (cursor.isAtEnd) {
            failure(XmlDiagnosticCode.UnexpectedEnd, cursor.rest(cursor.offset), "The document type is not closed")
        }
        return cursor.depth > depth || !cursor.startsWith(SUBSET_CLOSE)
    }

    private fun externalId(isSystemOptional: Boolean): Pair<String?, String?> {
        if (!lexer.isKeyword(SYSTEM)) return publicId(isSystemOptional)
        lexer.keyword(SYSTEM)
        lexer.requiredSpace("after SYSTEM")
        return null to lexer.systemLiteral()
    }

    private fun publicId(isSystemOptional: Boolean): Pair<String?, String?> {
        val start = cursor.offset
        if (!lexer.isKeyword(PUBLIC)) lexer.invalid(start, "An external identifier starts with SYSTEM or PUBLIC")
        lexer.keyword(PUBLIC)
        lexer.requiredSpace("after PUBLIC")
        val publicId = lexer.publicLiteral()
        val hasSpace = lexer.space()
        val hasSystem = hasSpace && lexer.isQuoteAhead()
        if (!hasSystem && isSystemOptional) return publicId to null
        if (!hasSpace) lexer.invalid(start, "Whitespace was expected before the system identifier")
        return publicId to lexer.systemLiteral()
    }

    private fun entity(dtd: Dtd) {
        val start = cursor.offset
        sink.startNode(XmlSyntaxKind.ENTITY_DECLARATION)
        lexer.keyword(ENTITY, XmlSyntaxKind.DECLARATION_OPEN)
        lexer.requiredSpace("after <!ENTITY")
        val isParameter = cursor.startsWith(PERCENT)
        if (isParameter) {
            lexer.keyword(PERCENT, XmlSyntaxKind.PERCENT)
            lexer.requiredSpace("after %")
        }
        val name = lexer.name(isQualified = false)
        lexer.requiredSpace("after the name of an entity")
        val definition = definitionOf(name, isParameter)
        lexer.close(start)
        sink.finishNode()
        record(dtd, definition, start)
    }

    private fun definitionOf(name: String, isParameter: Boolean): EntityDefinition {
        if (lexer.isQuoteAhead()) {
            val value = literals.entityValue()
            return EntityDefinition(
                name = name,
                isParameter = isParameter,
                replacement = value,
                publicId = null,
                systemId = null,
                notation = null,
            )
        }
        val (publicId, systemId) = externalId(isSystemOptional = false)
        return EntityDefinition(
            name = name,
            isParameter = isParameter,
            replacement = null,
            publicId = publicId,
            systemId = systemId,
            notation = notationOf(isParameter),
        )
    }

    private fun notationOf(isParameter: Boolean): String? {
        val hasSpace = lexer.space()
        if (isParameter || !hasSpace || !lexer.isKeyword(NDATA)) return null
        lexer.keyword(NDATA)
        lexer.requiredSpace("after NDATA")
        return lexer.name(isQualified = false)
    }

    private fun attributeList(dtd: Dtd) {
        val start = cursor.offset
        sink.startNode(XmlSyntaxKind.ATTLIST_DECLARATION)
        lexer.keyword(ATTLIST, XmlSyntaxKind.DECLARATION_OPEN)
        lexer.requiredSpace("after <!ATTLIST")
        val element = lexer.name(isQualified = true)
        var hasSpace = lexer.space()
        while (!cursor.isAtEnd && cursor.peek() != '>') {
            if (!hasSpace) lexer.invalid(start, "Attribute definitions must be separated by whitespace")
            val definition = attributeDefinition(start)
            record(dtd = dtd, element = element, definition = definition, start = start)
            hasSpace = lexer.space()
        }
        lexer.close(start)
        sink.finishNode()
    }

    private fun attributeDefinition(start: Int): AttributeDefinition {
        sink.startNode(XmlSyntaxKind.ATTRIBUTE_DEFINITION)
        val name = lexer.name(isQualified = true)
        lexer.requiredSpace("after the name of an attribute")
        val type = attributeType(start)
        lexer.requiredSpace("after the type of an attribute")
        val definition = defaultOf(name, type, start)
        sink.finishNode()
        return definition
    }

    private fun attributeType(start: Int): AttributeType {
        val keyword = AttributeType.entries.firstOrNull { type ->
            type != AttributeType.Enumeration && lexer.isKeyword(type.keyword)
        }
        if (keyword == null) {
            val isEnumeration = !cursor.isAtEnd && cursor.peek() == ENUMERATION_OPEN
            if (!isEnumeration) lexer.invalid(start, "An attribute type was expected")
            enumeration(isNotation = false)
            return AttributeType.Enumeration
        }
        lexer.keyword(keyword.keyword)
        if (keyword == AttributeType.Notation) {
            lexer.requiredSpace("after NOTATION")
            enumeration(isNotation = true)
        }
        return keyword
    }

    private fun enumeration(isNotation: Boolean) {
        lexer.punctuation(ENUMERATION_OPEN)
        lexer.space()
        enumerated(isNotation)
        lexer.space()
        while (!cursor.isAtEnd && cursor.peek() == ALTERNATIVE) {
            lexer.punctuation(ALTERNATIVE)
            lexer.space()
            enumerated(isNotation)
            lexer.space()
        }
        lexer.punctuation(ENUMERATION_CLOSE)
    }

    private fun enumerated(isNotation: Boolean) {
        if (isNotation) lexer.name(isQualified = false)
        if (!isNotation) lexer.nameToken()
    }

    private fun defaultOf(name: String, type: AttributeType, start: Int): AttributeDefinition {
        val keyword = listOf(REQUIRED, IMPLIED).firstOrNull(lexer::isKeyword)
        if (keyword != null) {
            lexer.keyword(keyword)
            return AttributeDefinition(name = name, type = type, defaultValue = null, isFixed = false)
        }
        val isFixed = lexer.isKeyword(FIXED)
        if (isFixed) {
            lexer.keyword(FIXED)
            lexer.requiredSpace("after #FIXED")
        }
        if (!lexer.isQuoteAhead()) lexer.invalid(start, "A default value or #REQUIRED, #IMPLIED or #FIXED was expected")
        val literalStart = cursor.offset
        val raw = literals.defaultValue()
        val normalized = expansion.normalize(raw, cursor.rangeFrom(literalStart))
        val value = AttributeDefaults.collapsed(normalized).takeIf { type.isTokenized } ?: normalized
        return AttributeDefinition(name = name, type = type, defaultValue = value, isFixed = isFixed)
    }

    private fun element() {
        val start = cursor.offset
        sink.startNode(XmlSyntaxKind.ELEMENT_DECLARATION)
        lexer.keyword(ELEMENT, XmlSyntaxKind.DECLARATION_OPEN)
        lexer.requiredSpace("after <!ELEMENT")
        lexer.name(isQualified = true)
        lexer.requiredSpace("after the name of an element type")
        models.read(start)
        lexer.close(start)
        sink.finishNode()
    }

    private fun notation(dtd: Dtd) {
        val start = cursor.offset
        sink.startNode(XmlSyntaxKind.NOTATION_DECLARATION)
        lexer.keyword(NOTATION, XmlSyntaxKind.DECLARATION_OPEN)
        lexer.requiredSpace("after <!NOTATION")
        val name = lexer.name(isQualified = false)
        lexer.requiredSpace("after the name of a notation")
        val (publicId, systemId) = externalId(isSystemOptional = true)
        lexer.close(start)
        sink.finishNode()
        val notation = XmlNotation(name = name, publicId = publicId, systemId = systemId)
        if (name !in dtd.notations) dtd.notations[name] = notation
    }

    private fun parameterReference(dtd: Dtd) {
        val start = cursor.offset
        cursor.advance()
        val name = lexer.scanName()
        if (cursor.isAtEnd || cursor.peek() != SEMICOLON) {
            failure(XmlDiagnosticCode.InvalidReference, cursor.rangeFrom(start), "A reference must end with ;")
        }
        cursor.advance()
        sink.token(kind = XmlSyntaxKind.PARAMETER_REFERENCE, start = start, end = cursor.offset)
        dtd.hasParameterReferences = true
        val entity = dtd.parameterEntities[name]
        val replacement = entity?.replacement
        if (replacement == null) {
            if (entity == null && isStandalone) {
                failure(XmlDiagnosticCode.UndefinedEntity, cursor.rangeFrom(start), "The entity %$name; is undeclared")
            }
            dtd.isProcessing = false
            return
        }
        if (name in cursor.names) {
            failure(XmlDiagnosticCode.RecursiveEntity, cursor.rangeFrom(start), "The entity %$name; refers to itself")
        }
        budget.charge(length = replacement.length, depth = cursor.depth, range = cursor.rangeFrom(start))
        val padded = "$SPACE$replacement$SPACE"
        cursor.push(text = padded, anchor = cursor.rangeFrom(start), name = name, isReplacement = true)
    }

    private fun record(dtd: Dtd, definition: EntityDefinition, start: Int) {
        dtd.entityDeclarations++
        if (dtd.entityDeclarations > limits.maximumEntityDeclarations) {
            failure(
                XmlDiagnosticCode.TooManyEntityDeclarations,
                cursor.rangeFrom(start),
                "The document type declares more than ${limits.maximumEntityDeclarations} entities",
            )
        }
        val table = dtd.parameterEntities.takeIf { definition.isParameter } ?: dtd.generalEntities
        val isPredefined = !definition.isParameter && definition.name in predefined
        val isFirst = definition.name !in table && !isPredefined
        if (dtd.isProcessing && isFirst) table[definition.name] = definition
    }

    private fun record(dtd: Dtd, element: String, definition: AttributeDefinition, start: Int) {
        dtd.attributeDeclarations++
        if (dtd.attributeDeclarations > limits.maximumAttributeDeclarations) {
            failure(
                XmlDiagnosticCode.TooManyAttributeDeclarations,
                cursor.rangeFrom(start),
                "The document type declares more than ${limits.maximumAttributeDeclarations} attributes",
            )
        }
        if (!dtd.isProcessing) return
        val definitions = dtd.attributes.getOrPut(element) { OrderedMap() }
        if (definition.name !in definitions) definitions[definition.name] = definition
    }
}
