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
import com.davils.kxml.core.node.XmlDoctypeDeclaration
import com.davils.kxml.core.node.XmlNotation
import com.davils.kxml.core.node.XmlUnparsedEntity

internal class Dtd(val name: String) {
    val generalEntities: OrderedMap<String, EntityDefinition> = OrderedMap()
    val parameterEntities: OrderedMap<String, EntityDefinition> = OrderedMap()
    val attributes: OrderedMap<String, OrderedMap<String, AttributeDefinition>> = OrderedMap()
    val notations: OrderedMap<String, XmlNotation> = OrderedMap()
    var publicId: String? = null
    var systemId: String? = null
    var hasParameterReferences: Boolean = false
    var isProcessing: Boolean = true
    var entityDeclarations: Int = 0
    var attributeDeclarations: Int = 0

    val isEntityDeclarationRequired: Boolean get() = systemId == null && !hasParameterReferences

    fun declaration(): XmlDoctypeDeclaration {
        val unparsed = generalEntities.entries.mapNotNull { (_, entity) -> unparsedOf(entity) }
        return XmlDoctypeDeclaration(
            name = name,
            publicId = publicId,
            systemId = systemId,
            notations = notations.entries.map { (_, notation) -> notation },
            unparsedEntities = unparsed,
        )
    }

    private fun unparsedOf(entity: EntityDefinition): XmlUnparsedEntity? {
        val notation = entity.notation ?: return null
        return XmlUnparsedEntity(
            name = entity.name,
            publicId = entity.publicId,
            systemId = entity.systemId.orEmpty(),
            notation = notation,
        )
    }
}
