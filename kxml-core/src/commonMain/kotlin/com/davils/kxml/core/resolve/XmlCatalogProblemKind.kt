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

package com.davils.kxml.core.resolve

/**
 * What went wrong with a catalog file or one of its entries.
 *
 * @since 1.0.0
 */
public enum class XmlCatalogProblemKind {
    /**
     * The loader refused the catalog file; it is treated as empty, as section 8 of XML Catalogs 1.1 asks.
     *
     * @since 1.0.0
     */
    Unloaded,

    /**
     * The catalog file is not well-formed XML within the limits; it is treated as empty.
     *
     * @since 1.0.0
     */
    Malformed,

    /**
     * The root of the catalog file is not a `catalog` element of the catalog namespace; it is treated as empty.
     *
     * @since 1.0.0
     */
    NotCatalog,

    /**
     * An element of the catalog namespace is unknown, misses an attribute or names a URI that is no URI reference;
     * it is ignored, together with what it encloses.
     *
     * @since 1.0.0
     */
    InvalidEntry,

    /**
     * The catalog file lies beyond
     * [maximumCatalogFiles][com.davils.kxml.core.limit.XmlLimits.maximumCatalogFiles] or
     * [maximumCatalogDepth][com.davils.kxml.core.limit.XmlLimits.maximumCatalogDepth] and was not loaded; a lookup
     * that reaches it fails instead of skipping it.
     *
     * @since 1.0.0
     */
    LimitExceeded,
}
