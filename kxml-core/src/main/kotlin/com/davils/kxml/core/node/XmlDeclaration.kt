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
import com.davils.kxml.core.internal.SafeDisplay

private const val NOT_DECLARED: String = "none"

/**
 * The XML declaration at the start of a document, `<?xml version="1.0" encoding="UTF-8"?>`.
 *
 * The values are written as they are; the reader only accepts versions `1.x` and the encodings it can read. An
 * instance is immutable.
 *
 * @property version the XML version; [VERSION] by default.
 * @property encoding the declared encoding, or `null` to leave it out; [ENCODING] by default.
 * @property isStandalone the standalone flag, or `null` to leave it out.
 * @since 1.0.0
 */
public class XmlDeclaration(
    public val version: String = VERSION,
    public val encoding: String? = ENCODING,
    public val isStandalone: Boolean? = null,
) {
    private val values: List<Any?> get() = listOf(version, encoding, isStandalone)

    /**
     * Compares every property.
     *
     * @param other the value to compare with.
     * @return whether [other] is a declaration with equal properties.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is XmlDeclaration && values == other.values

    /**
     * Returns a hash code derived from every property.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns every property escaped, such as `XmlDeclaration(version="1.0", encoding="UTF-8", standalone=none)`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String {
        val shownEncoding = encoding?.let(SafeDisplay::quoted) ?: NOT_DECLARED
        val shownStandalone = isStandalone?.toString() ?: NOT_DECLARED
        return "XmlDeclaration(version=${SafeDisplay.quoted(version)}, encoding=$shownEncoding, " +
            "standalone=$shownStandalone)"
    }

    /**
     * Holds the default values.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The version kxml writes, `1.0`.
         *
         * @since 1.0.0
         */
        public const val VERSION: String = "1.0"

        /**
         * The encoding kxml writes, `UTF-8`.
         *
         * @since 1.0.0
         */
        public const val ENCODING: String = "UTF-8"

        /**
         * Version [VERSION], encoding [ENCODING] and no standalone flag.
         *
         * @since 1.0.0
         */
        public val Default: XmlDeclaration = XmlDeclaration()
    }
}
