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
package com.davils.kxml.document.c14n

/**
 * The canonicalization methods of XML: Canonical XML 1.0 and 1.1 and Exclusive XML Canonicalization 1.0, each with
 * and without comments, by the URI that names it in XML Signature and XML Encryption.
 *
 * @property uri the identifier of the method, such as `http://www.w3.org/2006/12/xml-c14n11`.
 * @property isExclusive whether the method is Exclusive XML Canonicalization, which renders a namespace declaration
 * only where it is visibly utilized and imports no attribute of the XML namespace into an orphan element.
 * @property hasComments whether the canonical form keeps the comments of the document.
 * @since 1.0.0
 */
public enum class XmlCanonicalization(
    public val uri: String,
    public val isExclusive: Boolean,
    public val hasComments: Boolean,
) {
    /**
     * Canonical XML 1.0 without comments.
     *
     * @since 1.0.0
     */
    Canonical10(uri = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315", isExclusive = false, hasComments = false),

    /**
     * Canonical XML 1.0 with comments.
     *
     * @since 1.0.0
     */
    Canonical10WithComments(
        uri = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments",
        isExclusive = false,
        hasComments = true,
    ),

    /**
     * Canonical XML 1.1 without comments.
     *
     * @since 1.0.0
     */
    Canonical11(uri = "http://www.w3.org/2006/12/xml-c14n11", isExclusive = false, hasComments = false),

    /**
     * Canonical XML 1.1 with comments.
     *
     * @since 1.0.0
     */
    Canonical11WithComments(
        uri = "http://www.w3.org/2006/12/xml-c14n11#WithComments",
        isExclusive = false,
        hasComments = true,
    ),

    /**
     * Exclusive XML Canonicalization 1.0 without comments.
     *
     * @since 1.0.0
     */
    Exclusive10(uri = "http://www.w3.org/2001/10/xml-exc-c14n#", isExclusive = true, hasComments = false),

    /**
     * Exclusive XML Canonicalization 1.0 with comments.
     *
     * @since 1.0.0
     */
    Exclusive10WithComments(
        uri = "http://www.w3.org/2001/10/xml-exc-c14n#WithComments",
        isExclusive = true,
        hasComments = true,
    ),
    ;

    /**
     * Finds a canonicalization method by its identifier.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns the method [uri] names, or `null` if it names none of them.
         *
         * @param uri the identifier, compared exactly.
         * @return the method, or `null`.
         * @since 1.0.0
         */
        public fun of(uri: String): XmlCanonicalization? = entries.firstOrNull { method -> method.uri == uri }
    }
}
