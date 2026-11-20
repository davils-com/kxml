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

package com.davils.kxml.core.resolve

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * A catalog file that could not be used, or an entry of it that was ignored, while an [XmlCatalog] was loaded.
 *
 * The [uri] comes from a catalog or the caller and is input. An instance is immutable.
 *
 * @since 1.0.0
 */
public class XmlCatalogProblem internal constructor(uri: String, kind: XmlCatalogProblemKind, detail: String) {
    /**
     * The URI of the catalog file.
     *
     * @since 1.0.0
     */
    public val uri: String = uri

    /**
     * What went wrong.
     *
     * @since 1.0.0
     */
    public val kind: XmlCatalogProblemKind = kind

    /**
     * What went wrong in words, such as `misses the attribute uri`; it names elements and attributes, never values.
     *
     * @since 1.0.0
     */
    public val detail: String = detail

    private val values: List<Any> get() = listOf(uri, kind, detail)

    /**
     * Compares every property.
     *
     * @param other the value to compare with.
     * @return whether [other] is a problem with equal properties.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is XmlCatalogProblem && values == other.values

    /**
     * Returns a hash code derived from every property.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns the kind, the escaped URI and the escaped detail, such as
     * `XmlCatalogProblem(Unloaded, uri="file:///etc/xml/catalog", detail="was refused by the loader")`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String =
        "XmlCatalogProblem($kind, uri=${SafeText.quoted(uri)}, detail=${SafeText.quoted(detail)})"
}
