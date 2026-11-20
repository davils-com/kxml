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

internal class ExternalQuery(
    private val publicId: String? = null,
    private val systemId: String? = null,
) : CatalogQuery {
    override fun matchIn(file: CatalogFile): CatalogMatch? {
        val bySystem = systemId?.let { system -> systemMatch(file, system) }
        return bySystem ?: publicId?.let { public -> publicMatch(file, public) }
    }

    private fun systemMatch(file: CatalogFile, system: String): CatalogMatch? =
        exactMatch(file.entriesOf(EntryKind.System), system)
            ?: rewrittenMatch(file.entriesOf(EntryKind.RewriteSystem), system)
            ?: suffixMatch(file.entriesOf(EntryKind.SystemSuffix), system)
            ?: delegatedMatch(file.entriesOf(EntryKind.DelegateSystem), system, ExternalQuery(systemId = system))

    private fun publicMatch(file: CatalogFile, public: String): CatalogMatch? {
        val publics = preferred(file.entriesOf(EntryKind.Public))
        val delegates = preferred(file.entriesOf(EntryKind.DelegatePublic))
        return exactMatch(publics, public) ?: delegatedMatch(delegates, public, ExternalQuery(publicId = public))
    }

    private fun preferred(entries: List<CatalogEntry>): List<CatalogEntry> {
        val hasSystemId = systemId != null
        return entries.filter { entry -> !hasSystemId || entry.isPreferPublic }
    }
}
