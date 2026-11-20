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

package com.davils.kxml.core.source

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * A document to read, as text or as bytes, with an optional name for messages.
 *
 * Bytes are decoded as UTF-8 or UTF-16, detected from the byte order mark or the first two bytes; a byte order mark is
 * dropped. The bytes are copied when the source is created, so a later change of the array does not reach the reader.
 * An instance is immutable.
 *
 * @since 1.0.0
 */
public class XmlSource private constructor(
    internal val text: String?,
    internal val bytes: ByteArray,
    name: String?,
) {
    /**
     * A name for messages, such as a file name, or `null`; treated as input and escaped wherever it is shown.
     *
     * @since 1.0.0
     */
    public val name: String? = name

    internal val length: Int get() = text?.length ?: bytes.size

    /**
     * Returns the escaped name, such as `XmlSource("order.xml")`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String = "XmlSource(${name?.let(SafeText::quoted) ?: "unnamed"})"

    /**
     * Creates sources.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Creates a source of text that is read as it is; a leading byte order mark is dropped.
         *
         * @param text the complete document.
         * @param name a name for messages, or `null`.
         * @return the source.
         * @since 1.0.0
         */
        public fun of(text: String, name: String? = null): XmlSource =
            XmlSource(text = text, bytes = ByteArray(0), name = name)

        /**
         * Creates a source of bytes in UTF-8 or UTF-16.
         *
         * @param bytes the complete document; copied.
         * @param name a name for messages, or `null`.
         * @return the source.
         * @since 1.0.0
         */
        public fun of(bytes: ByteArray, name: String? = null): XmlSource =
            XmlSource(text = null, bytes = bytes.copyOf(), name = name)
    }
}
