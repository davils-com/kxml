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

package com.davils.kxml.core.name

/**
 * The namespace names and prefixes XML reserves.
 *
 * @since 1.0.0
 */
public object XmlNamespaces {
    /**
     * The namespace of the `xml` prefix, as in `xml:lang`.
     *
     * @since 1.0.0
     */
    public const val XML: String = "http://www.w3.org/XML/1998/namespace"

    /**
     * The namespace of namespace declarations, which no element or attribute may use.
     *
     * @since 1.0.0
     */
    public const val XMLNS: String = "http://www.w3.org/2000/xmlns/"

    /**
     * The prefix bound to [XML] in every document.
     *
     * @since 1.0.0
     */
    public const val XML_PREFIX: String = "xml"

    /**
     * The prefix of namespace declarations, which no name may use.
     *
     * @since 1.0.0
     */
    public const val XMLNS_PREFIX: String = "xmlns"
}
