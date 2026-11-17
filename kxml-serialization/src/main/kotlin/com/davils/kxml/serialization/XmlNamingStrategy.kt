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

package com.davils.kxml.serialization

import kotlinx.serialization.descriptors.SerialDescriptor

private const val KEBAB_SEPARATOR: Char = '-'

/**
 * Derives the XML name of a property from its serial name.
 *
 * A strategy applies to properties without an explicit [SerialName][kotlinx.serialization.SerialName] of their own as
 * much as to those with one; the name it returns must be an XML name without a colon, or encoding and decoding throw.
 *
 * @since 1.0.0
 */
public fun interface XmlNamingStrategy {
    /**
     * Returns the local name of a property.
     *
     * @param descriptor the descriptor of the class that owns the property.
     * @param index the index of the property in [descriptor].
     * @param serialName the serial name of the property.
     * @return the local name of the element or attribute.
     * @since 1.0.0
     */
    public fun nameOf(descriptor: SerialDescriptor, index: Int, serialName: String): String

    /**
     * Holds the predefined strategies.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Uses the serial name as it is.
         *
         * @since 1.0.0
         */
        public val Identity: XmlNamingStrategy = XmlNamingStrategy { _, _, serialName -> serialName }

        /**
         * Upper-cases the first character, such as `creationDate` to `CreationDate`.
         *
         * @since 1.0.0
         */
        public val PascalCase: XmlNamingStrategy = XmlNamingStrategy { _, _, serialName ->
            serialName.replaceFirstChar(Char::uppercaseChar)
        }

        /**
         * Lower-cases the name and puts a hyphen before each start of a word, such as `creationDate` to
         * `creation-date`.
         *
         * @since 1.0.0
         */
        public val KebabCase: XmlNamingStrategy = XmlNamingStrategy { _, _, serialName ->
            words(serialName, KEBAB_SEPARATOR)
        }

        private fun words(name: String, separator: Char): String {
            val result = StringBuilder(name.length + name.length / 2)
            name.forEachIndexed { index, character ->
                val isBoundary = character.isUpperCase() && index > 0 && !name[index - 1].isUpperCase()
                if (isBoundary) result.append(separator)
                result.append(character.lowercaseChar())
            }
            return result.toString()
        }
    }
}
