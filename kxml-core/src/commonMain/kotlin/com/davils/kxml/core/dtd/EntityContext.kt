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

internal class EntityContext(val budget: ExpansionBudget, private val resources: ExternalResources) {
    var dtd: Dtd? = null
    var isStandalone: Boolean = false

    fun general(name: String, range: TextRange, expanding: List<String>, isInAttribute: Boolean): EntityDefinition {
        val entity = dtd?.run { generalEntities[name] } ?: undeclared(name, range)
        if (entity.isUnparsed) {
            failure(XmlDiagnosticCode.MisusedEntity, range, "The unparsed entity &$name; cannot be referenced here")
        }
        if (entity.isExternal && isInAttribute) {
            failure(XmlDiagnosticCode.MisusedEntity, range, "An attribute value cannot refer to the external &$name;")
        }
        if (entity.isExternal && !resources.isEnabled) {
            failure(XmlDiagnosticCode.UnresolvedEntity, range, "No resolver reads the external entity &$name;")
        }
        if (entity.isExternallyDeclared && isStandalone) {
            failure(XmlDiagnosticCode.UndefinedEntity, range, "A standalone document cannot use the external &$name;")
        }
        if (name in expanding) failure(XmlDiagnosticCode.RecursiveEntity, range, "The entity &$name; refers to itself")
        return entity
    }

    private fun undeclared(name: String, range: TextRange): Nothing {
        val declared = dtd
        val isRequired = declared == null || declared.isEntityDeclarationRequired || isStandalone
        if (isRequired) failure(XmlDiagnosticCode.UndefinedEntity, range, "The entity &$name; is not declared")
        failure(XmlDiagnosticCode.UnresolvedEntity, range, "The entity &$name; is not declared where kxml read")
    }
}
