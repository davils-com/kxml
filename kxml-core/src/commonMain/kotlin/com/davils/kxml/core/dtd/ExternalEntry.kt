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
import com.davils.kxml.core.parser.Cursor
import com.davils.kxml.core.parser.DeclarationReader
import com.davils.kxml.core.parser.Dialect
import com.davils.kxml.core.parser.failure
import com.davils.kxml.core.resolve.XmlResourceKind
import com.davils.kxml.core.resolve.XmlResourceRequest

private const val SPACE: String = " "
private const val VERSION_11: String = "1.1"

internal class ExternalEntry(
    private val cursor: Cursor,
    private val declarations: DeclarationReader,
    private val resources: ExternalResources,
    private val budget: ExpansionBudget,
    private val dialect: Dialect,
) {
    val isEnabled: Boolean get() = resources.isEnabled

    fun enter(request: XmlResourceRequest, range: TextRange, isPadded: Boolean) {
        val resource = resources.load(request, range)
        val isEntity = request.kind != XmlResourceKind.ExternalSubset
        if (isEntity) budget.charge(length = resource.text.length, depth = cursor.depth, range = range)
        val text = resource.text + SPACE.takeIf { isPadded }.orEmpty()
        cursor.pushExternal(text = text, anchor = range, name = request.name, baseUri = resource.uri)
        val version = versionOf(resource)
        if (version == VERSION_11 && !dialect.isVersion11) {
            failure(XmlDiagnosticCode.UnsupportedVersion, range, "An entity of XML 1.1 cannot stand in XML 1.0")
        }
        if (isPadded) cursor.push(text = SPACE, anchor = range, name = request.name.orEmpty(), isReplacement = true)
    }

    private fun versionOf(resource: LoadedResource): String? {
        if (!declarations.isAhead()) return null
        return declarations.readText(resource.encoding)
    }

    fun request(kind: XmlResourceKind, entity: EntityDefinition): XmlResourceRequest = XmlResourceRequest(
        kind = kind,
        name = entity.name,
        publicId = entity.publicId,
        systemId = entity.systemId.orEmpty(),
        baseUri = entity.baseUri,
    )
}
