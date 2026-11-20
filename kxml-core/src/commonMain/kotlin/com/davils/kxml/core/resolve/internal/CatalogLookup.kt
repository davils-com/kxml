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

package com.davils.kxml.core.resolve.internal

internal class CatalogLookup(private val catalogs: CatalogSet) {
    fun resolveExternalId(publicId: String?, systemId: String?): CatalogOutcome {
        val written = publicId?.let(::publicIdOf)
        val system = systemId?.let(::normalizedSystemId)
        if (systemId != null && system == null) return CatalogOutcome.Missing
        val isSystemUrn = system != null && isPublicIdUrn(system)
        val unwrapped = system?.takeIf { isSystemUrn }?.let(::unwrappedPublicIdUrn)
        val query = ExternalQuery(publicId = written ?: unwrapped, systemId = system.takeUnless { isSystemUrn })
        return CatalogWalk(catalogs = catalogs, start = query).outcome()
    }

    fun resolveUri(uri: String): CatalogOutcome {
        val normalized = normalizedSystemId(uri) ?: return CatalogOutcome.Missing
        val urn = normalized.takeIf(::isPublicIdUrn)
        val query = urn?.let { publicIdUrn -> ExternalQuery(publicId = unwrappedPublicIdUrn(publicIdUrn)) }
        return CatalogWalk(catalogs = catalogs, start = query ?: UriQuery(normalized)).outcome()
    }

    private fun publicIdOf(publicId: String): String {
        if (isPublicIdUrn(publicId)) return unwrappedPublicIdUrn(publicId)
        return normalizedPublicId(publicId)
    }
}
