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
import com.davils.kore.core.text.SafeText

private const val NONE: String = "none"

/**
 * A notation the document type declaration declares, `<!NOTATION name SYSTEM "uri">`.
 *
 * An instance is immutable and only the reader creates one.
 *
 * @property name the name of the notation.
 * @property publicId the public identifier, or `null`.
 * @property systemId the system identifier, or `null`.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class XmlNotation internal constructor(
    public val name: String,
    public val publicId: String?,
    public val systemId: String?,
) {
    private val values: List<Any?> get() = listOf(name, publicId, systemId)

    /**
     * Compares every property.
     *
     * @param other the value to compare with.
     * @return whether [other] holds equal properties.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is XmlNotation && values == other.values

    /**
     * Returns a hash code derived from every property.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns the name and the escaped identifiers, such as `XmlNotation(gif, public=none, system="gif.exe")`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlNotation($name, public=${publicId?.let(SafeText::quoted) ?: NONE}, " +
        "system=${systemId?.let(SafeText::quoted) ?: NONE})"
}
