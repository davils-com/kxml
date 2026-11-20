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
import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.parser.failure
import com.davils.kxml.core.resolve.XmlResourceKind
import com.davils.kxml.core.syntax.XmlSyntaxKind

private const val SEMICOLON: Char = ';'
private const val SPACE: Char = ' '

internal class ParameterReferences(
    private val lexer: DtdLexer,
    private val entry: ExternalEntry,
    private val budget: ExpansionBudget,
    private val isStandalone: Boolean,
) {
    private val cursor = lexer.cursor

    fun expand(dtd: Dtd, context: ReferenceContext): Boolean {
        val start = cursor.offset
        cursor.advance()
        val name = lexer.scanName()
        if (cursor.isAtEnd || cursor.peek() != SEMICOLON) {
            failure(XmlDiagnosticCode.InvalidReference, cursor.rangeFrom(start), "A reference must end with ;")
        }
        cursor.advance()
        lexer.sink.token(kind = XmlSyntaxKind.PARAMETER_REFERENCE, start = start, end = cursor.offset)
        dtd.hasParameterReferences = true
        val range = cursor.rangeFrom(start)
        val entity = dtd.parameterEntities[name]
        val isStrict = isStandalone || context != ReferenceContext.BetweenDeclarations
        if (entity == null && isStrict) {
            failure(XmlDiagnosticCode.UndefinedEntity, range, "The entity %$name; is undeclared")
        }
        if (entity == null) return false
        if (name in cursor.names) {
            failure(XmlDiagnosticCode.RecursiveEntity, range, "The entity %$name; refers to itself")
        }
        return replaced(entity = entity, range = range, context = context)
    }

    private fun replaced(entity: EntityDefinition, range: TextRange, context: ReferenceContext): Boolean {
        val replacement = entity.replacement ?: return external(entity, range, context)
        budget.charge(length = replacement.length, depth = cursor.depth, range = range)
        val text = "$SPACE$replacement$SPACE".takeIf { context.isPadded } ?: replacement
        cursor.push(text = text, anchor = range, name = entity.name, isReplacement = true)
        return true
    }

    private fun external(entity: EntityDefinition, range: TextRange, context: ReferenceContext): Boolean {
        if (!entry.isEnabled && context == ReferenceContext.BetweenDeclarations) return false
        val request = entry.request(XmlResourceKind.ParameterEntity, entity)
        entry.enter(request = request, range = range, isPadded = context.isPadded)
        return true
    }
}
