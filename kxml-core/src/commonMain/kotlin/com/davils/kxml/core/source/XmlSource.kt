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
 * Bytes are decoded strictly, as Appendix F of XML 1.0 detects them: UTF-8, UTF-16 and UTF-32 in either byte order,
 * with or without a byte order mark, and, for bytes without a mark that start like ASCII, US-ASCII and ISO-8859-1 when
 * the XML declaration names them. UTF-16 and UTF-32 without a byte order mark are only detected when the document
 * starts with `<?`. The declaration is read from the first 1,024 bytes; a declared encoding that contradicts the
 * bytes, an encoding kxml does not read, such as EBCDIC or Windows-1252, and bytes that are not valid in their
 * encoding end reading with a diagnostic. Nothing is ever replaced by U+FFFD. A byte order mark is dropped. The bytes
 * are copied when the source is created, so a later change of the array does not reach the reader. An instance is
 * immutable.
 *
 * @since 1.0.0
 */
public class XmlSource private constructor(
    internal val text: String?,
    internal val bytes: ByteArray,
    name: String?,
    internal val charset: String?,
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
    override fun toString(): String {
        val shown = name ?: return "XmlSource(unnamed)"
        return "XmlSource(${SafeText.quoted(shown)})"
    }

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
            XmlSource(text = text, bytes = ByteArray(0), name = name, charset = null)

        /**
         * Creates a source of bytes, decoded as the class describes.
         *
         * [charset] is the `charset` parameter of the media type the bytes came with, such as from a `Content-Type`
         * header. As RFC 7303 orders it, a byte order mark takes precedence over it and it takes precedence over the
         * encoding the document declares; when it overrides a declaration, the result carries an
         * [EncodingOverridden][com.davils.kxml.core.diagnostic.XmlDiagnosticCode.EncodingOverridden] warning. A charset
         * kxml does not read ends reading with an
         * [UnsupportedEncoding][com.davils.kxml.core.diagnostic.XmlDiagnosticCode.UnsupportedEncoding] diagnostic. The
         * charset is matched without regard to case and may come from input.
         *
         * @param bytes the complete document; copied.
         * @param name a name for messages, or `null`.
         * @param charset the charset the bytes were labelled with outside the document, or `null`.
         * @return the source.
         * @since 1.0.0
         */
        public fun of(bytes: ByteArray, name: String? = null, charset: String? = null): XmlSource =
            XmlSource(text = null, bytes = bytes.copyOf(), name = name, charset = charset)
    }
}
