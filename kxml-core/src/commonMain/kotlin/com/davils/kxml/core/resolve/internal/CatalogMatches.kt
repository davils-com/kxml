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

package com.davils.kxml.core.resolve.internal

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet

internal fun exactMatch(entries: List<CatalogEntry>, identifier: String): CatalogMatch? {
    val entry = entries.firstOrNull { candidate -> candidate.key == identifier }
    return entry?.let { found -> CatalogMatch.Found(found.target) }
}

internal fun rewrittenMatch(entries: List<CatalogEntry>, identifier: String): CatalogMatch? {
    val matching = entries.filter { entry -> identifier.startsWith(entry.key) }
    val longest = matching.maxByOrNull { entry -> entry.key.length }
    return longest?.let { entry -> CatalogMatch.Found(entry.target + identifier.substring(entry.key.length)) }
}

internal fun suffixMatch(entries: List<CatalogEntry>, identifier: String): CatalogMatch? {
    val matching = entries.filter { entry -> identifier.endsWith(entry.key) }
    val longest = matching.maxByOrNull { entry -> entry.key.length }
    return longest?.let { entry -> CatalogMatch.Found(entry.target) }
}

internal fun delegatedMatch(entries: List<CatalogEntry>, identifier: String, query: CatalogQuery): CatalogMatch? {
    val matching = entries.filter { entry -> identifier.startsWith(entry.key) }
    if (matching.isEmpty()) return null
    val ordered = matching.sortedByDescending { entry -> entry.key.length }
    val keys = ordered.mapNotNull { entry -> catalogKeyOf(entry.target) }
    return CatalogMatch.Delegate(query = query, catalogs = OrderedSet(keys).toList())
}
