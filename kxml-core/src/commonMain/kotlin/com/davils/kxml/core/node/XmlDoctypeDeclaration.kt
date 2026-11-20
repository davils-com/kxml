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

package com.davils.kxml.core.node

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.SafeText

private const val NONE: String = "none"

/**
 * The document type declaration of a document, `<!DOCTYPE name SYSTEM "uri" [...]>`, as the information set of a
 * document reports it.
 *
 * Entities and attribute defaults the declaration holds are applied by the reader where they are used; the
 * declaration itself only keeps the name, the external identifiers, the notations and the unparsed entities. An
 * instance is immutable and only the reader creates one.
 *
 * @property name the name the declaration gives the root element.
 * @property publicId the public identifier of the external subset, or `null`.
 * @property systemId the system identifier of the external subset, or `null`.
 * @param notations the notations, in the order they were declared.
 * @param unparsedEntities the unparsed entities, in the order they were declared.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class XmlDoctypeDeclaration internal constructor(
    public val name: String,
    public val publicId: String?,
    public val systemId: String?,
    notations: List<XmlNotation>,
    unparsedEntities: List<XmlUnparsedEntity>,
) {
    /**
     * the notations, in the order they were declared.
     *
     * @since 1.0.0
     */
    public val notations: List<XmlNotation> = notations.immutableList()

    /**
     * the unparsed entities, in the order they were declared.
     *
     * @since 1.0.0
     */
    public val unparsedEntities: List<XmlUnparsedEntity> = unparsedEntities.immutableList()
    private val values: List<Any?> get() = listOf(name, publicId, systemId, notations, unparsedEntities)

    /**
     * Compares every property.
     *
     * @param other the value to compare with.
     * @return whether [other] holds equal properties.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is XmlDoctypeDeclaration && values == other.values

    /**
     * Returns a hash code derived from every property.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns the name, the escaped identifiers and the numbers of notations and unparsed entities.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String {
        val shownPublic = publicId?.let(SafeText::quoted) ?: NONE
        val shownSystem = systemId?.let(SafeText::quoted) ?: NONE
        return "XmlDoctypeDeclaration($name, public=$shownPublic, system=$shownSystem, ${notations.size} notations, " +
            "${unparsedEntities.size} unparsed entities)"
    }
}
