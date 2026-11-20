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
import com.davils.kore.core.collection.immutableList
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.resolve.internal.CatalogLookup
import com.davils.kxml.core.resolve.internal.CatalogOutcome
import com.davils.kxml.core.resolve.internal.CatalogSet

/**
 * A set of OASIS XML Catalogs 1.1, which maps public identifiers, system identifiers and URIs to the URIs of
 * resources.
 *
 * A catalog is loaded once with [load]: every catalog file the given ones reach through `nextCatalog`,
 * `delegatePublic`, `delegateSystem` and `delegateURI` entries is loaded through the loader, read with
 * [XmlDoctype.Internal][com.davils.kxml.core.XmlDoctype.Internal] within the limits and kept. A catalog file is loaded
 * at most once, so catalogs that refer to each other in a circle cannot make loading or a lookup endless. A catalog
 * file the loader refuses, that is not well-formed or whose root is not a `catalog` element is treated as empty, and
 * an entry that misses an attribute or names no URI reference is ignored, as section 8 of the specification asks;
 * [problems] lists all of them. A catalog file beyond [XmlLimits.maximumCatalogFiles] or
 * [XmlLimits.maximumCatalogDepth] is not loaded, and a lookup that reaches it finds nothing instead of skipping it, so
 * the limits never change which entry matches.
 *
 * [resolveExternalId] and [resolveUri] follow sections 7.1 and 7.2 of the specification: `system`, `rewriteSystem`,
 * `systemSuffix` and `delegateSystem` entries before `public` and `delegatePublic` entries, the longest prefix or
 * suffix among `rewrite*`, `*Suffix` and `delegate*` entries, delegation without return to the first list,
 * `nextCatalog` entries after the other entries of their file and `urn:publicid:` URNs unwrapped. Identifiers are
 * compared after the normalization of section 6, as the document writes them; a relative system identifier is not
 * made absolute. Every URI a catalog maps to is made absolute against the base URI of its entry, which `xml:base`
 * changes. A lookup works in time linear in the size of the catalogs it walks and follows at most
 * [XmlLimits.maximumCatalogDepth] delegations.
 *
 * A catalog is immutable after it was loaded and safe to use from several threads at once.
 *
 * @since 1.0.0
 */
public class XmlCatalog private constructor(private val catalogs: CatalogSet) {
    private val lookup = CatalogLookup(catalogs)

    /**
     * The catalog files that could not be used and the entries that were ignored, in the order they were found.
     *
     * @since 1.0.0
     */
    public val problems: List<XmlCatalogProblem> = catalogs.problems.immutableList()

    /**
     * Returns the URI a catalog maps an external identifier to, or `null` if no entry matches.
     *
     * @param publicId the public identifier, or `null`; normalized before it is compared.
     * @param systemId the system identifier as the document writes it, or `null`; normalized before it is compared.
     * @return the absolute URI of the resource, or `null`.
     * @since 1.0.0
     */
    public fun resolveExternalId(publicId: String?, systemId: String?): String? =
        outcomeOf(publicId = publicId, systemId = systemId).uri

    /**
     * Returns the URI a catalog maps a URI reference to, such as the location of a schema, or `null` if no entry
     * matches.
     *
     * @param uri the URI reference as it is written; normalized before it is compared.
     * @return the absolute URI of the resource, or `null`.
     * @since 1.0.0
     */
    public fun resolveUri(uri: String): String? = lookup.resolveUri(uri).uri

    internal fun outcomeOf(publicId: String?, systemId: String?): CatalogOutcome =
        lookup.resolveExternalId(publicId = publicId, systemId = systemId)

    /**
     * Returns how many catalog files were loaded and how many problems were found, such as
     * `XmlCatalog(files=2, problems=0)`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlCatalog(files=${catalogs.fileCount}, problems=${problems.size})"

    /**
     * Loads catalogs.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Loads the catalog files [catalogs] name and every catalog file they reach, in this order.
         *
         * @param catalogs the absolute URIs of the catalog files to search first; a relative one is reported as
         * [Unloaded][XmlCatalogProblemKind.Unloaded].
         * @param loader loads every catalog file; its exceptions are not caught.
         * @param prefer the `prefer` setting where no `catalog` or `group` element sets one; [XmlCatalogPrefer.Public]
         * by default.
         * @param limits the limits of reading each catalog file and of the catalog files to load;
         * [XmlLimits.Untrusted] by default.
         * @return the catalog.
         * @since 1.0.0
         */
        public fun load(
            catalogs: List<String>,
            loader: XmlResourceLoader,
            prefer: XmlCatalogPrefer = XmlCatalogPrefer.Public,
            limits: XmlLimits = XmlLimits.Untrusted,
        ): XmlCatalog = XmlCatalog(CatalogSet(catalogs = catalogs, loader = loader, prefer = prefer, limits = limits))
    }
}
