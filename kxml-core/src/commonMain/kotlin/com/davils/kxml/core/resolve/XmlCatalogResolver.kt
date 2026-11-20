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

import com.davils.kxml.core.resolve.internal.CatalogOutcome
import com.davils.kxml.core.source.XmlSource

/**
 * Resolves the external resources of a document through an [XmlCatalog]: it maps the identifiers of a request to a
 * URI and loads that URI through [loader].
 *
 * The system identifier is looked up as the document writes it first and, when that finds nothing, in its absolute
 * form, [XmlResourceRequest.absoluteSystemId], as section 7.1.1 of XML Catalogs 1.1 allows. When the catalog maps
 * neither, the request goes to [fallback], and without one it is refused, so a document reaches nothing the catalog
 * does not name. A lookup that reaches a catalog file beyond the limits is refused and never goes to [fallback].
 * When the catalog maps it, the resource comes from [loader] and nowhere else; its [uri][XmlSource.uri] becomes the
 * URI the catalog mapped to, the base of the references the resource makes, and a resource the loader refuses is
 * refused. The resolver keeps no state and may be shared between readers on several threads when [loader] and
 * [fallback] may.
 *
 * @property catalog the catalog that maps identifiers.
 * @property loader loads what the catalog maps to; its exceptions are not caught.
 * @property fallback resolves what the catalog does not map, or `null` to refuse it.
 * @since 1.0.0
 */
public class XmlCatalogResolver(
    public val catalog: XmlCatalog,
    public val loader: XmlResourceLoader,
    public val fallback: XmlResolver? = null,
) : XmlResolver {
    /**
     * Returns the resource the catalog maps [request] to, the result of [fallback] when it maps nothing, or `null`.
     *
     * @param request what the document refers to and from where.
     * @return the resource, or `null`.
     * @since 1.0.0
     */
    override fun resolve(request: XmlResourceRequest): XmlSource? {
        val outcome = outcomeOf(request)
        if (outcome is CatalogOutcome.Missing) return fallback?.resolve(request)
        return outcome.uri?.let { target -> loader.load(target)?.withUri(target) }
    }

    /**
     * Returns the catalog and whether a fallback is set, such as `XmlCatalogResolver(XmlCatalog(files=1,
     * problems=0), fallback=none)`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String {
        val shownFallback = FALLBACK_SET.takeIf { fallback != null } ?: FALLBACK_NONE
        return "XmlCatalogResolver($catalog, fallback=$shownFallback)"
    }

    private fun outcomeOf(request: XmlResourceRequest): CatalogOutcome {
        val asWritten = catalog.outcomeOf(publicId = request.publicId, systemId = request.systemId)
        val absolute = request.absoluteSystemId?.takeIf { system -> system != request.systemId }
        if (asWritten !is CatalogOutcome.Missing || absolute == null) return asWritten
        return catalog.outcomeOf(publicId = request.publicId, systemId = absolute)
    }

    /**
     * Holds the ways to build a resolver on a platform.
     *
     * @since 1.0.0
     */
    public companion object {
        private const val FALLBACK_SET: String = "set"
        private const val FALLBACK_NONE: String = "none"
    }
}
