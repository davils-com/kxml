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

internal class CatalogWalk(private val catalogs: CatalogSet, start: CatalogQuery) {
    private var query: CatalogQuery = start
    private val files = ArrayDeque(catalogs.roots)
    private val visited = OrderedSet<String>()
    private var delegations = 0

    fun outcome(): CatalogOutcome {
        var outcome: CatalogOutcome? = null
        while (outcome == null) outcome = step()
        return outcome
    }

    private fun step(): CatalogOutcome? {
        val uri = files.removeFirstOrNull() ?: return CatalogOutcome.Missing
        val match = uri.takeIf(visited::add)?.let(::matchIn)
        return follow(match ?: CatalogMatch.None)
    }

    private fun matchIn(uri: String): CatalogMatch = when (val slot = catalogs.slotOf(uri)) {
        is CatalogSlot.Loaded -> query.matchIn(slot.file) ?: CatalogMatch.Next(nextOf(slot.file))
        CatalogSlot.Unavailable -> CatalogMatch.None
        CatalogSlot.BeyondLimits -> CatalogMatch.Stop
    }

    private fun follow(match: CatalogMatch): CatalogOutcome? = when (match) {
        is CatalogMatch.Found -> CatalogOutcome.Found(match.uri)
        is CatalogMatch.Delegate -> delegate(match)
        is CatalogMatch.Next -> next(match.catalogs)
        CatalogMatch.Stop -> CatalogOutcome.Stopped
        CatalogMatch.None -> null
    }

    private fun delegate(match: CatalogMatch.Delegate): CatalogOutcome? {
        delegations++
        if (delegations > catalogs.maximumDelegations) return CatalogOutcome.Stopped
        query = match.query
        files.clear()
        files.addAll(match.catalogs)
        visited.clear()
        return null
    }

    private fun next(catalogs: List<String>): CatalogOutcome? {
        files.addAll(index = 0, elements = catalogs)
        return null
    }

    private fun nextOf(file: CatalogFile): List<String> = file.nextCatalogs().mapNotNull(::catalogKeyOf)
}
