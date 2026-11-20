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
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.uri.UriReference
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.resolve.XmlCatalogPrefer
import com.davils.kxml.core.resolve.XmlCatalogProblem
import com.davils.kxml.core.resolve.XmlCatalogProblemKind
import com.davils.kxml.core.resolve.XmlResourceLoader

internal class CatalogSet(
    catalogs: List<String>,
    private val loader: XmlResourceLoader,
    prefer: XmlCatalogPrefer,
    private val limits: XmlLimits,
) {
    private val reported = mutableListOf<XmlCatalogProblem>()
    private val reader = CatalogReader(prefer = prefer, limits = limits, problems = reported)
    private val slots = OrderedMap<String, CatalogSlot>()

    val roots: List<String> = catalogs.mapNotNull(::rootOf)

    val problems: List<XmlCatalogProblem>

    val maximumDelegations: Int = limits.maximumCatalogDepth

    init {
        load()
        problems = reported.toList()
    }

    val fileCount: Int get() = slots.entries.count { (_, slot) -> slot is CatalogSlot.Loaded }

    fun slotOf(uri: String): CatalogSlot = slots[uri] ?: CatalogSlot.BeyondLimits

    private fun rootOf(written: String): String? {
        val reference = referenceOf(written)?.takeIf(UriReference::isAbsolute)
        if (reference == null) reported += XmlCatalogProblem(written, XmlCatalogProblemKind.Unloaded, NOT_ABSOLUTE)
        return reference?.let(::keyOf)
    }

    private fun load() {
        val pending = ArrayDeque<Pair<String, Int>>()
        roots.forEach { root -> pending.addLast(root to 0) }
        while (pending.isNotEmpty()) {
            val (uri, depth) = pending.removeFirst()
            val loaded = uri.takeUnless { uri in slots }?.let { fresh -> loadOne(fresh, depth) }
            loaded?.let { file -> enqueue(file, depth + 1, pending) }
        }
    }

    private fun enqueue(file: CatalogFile, depth: Int, pending: ArrayDeque<Pair<String, Int>>) {
        val keys = file.catalogReferences.mapNotNull(::catalogKeyOf)
        keys.forEach { next -> pending.addLast(next to depth) }
    }

    private fun loadOne(uri: String, depth: Int): CatalogFile? {
        val isWithinLimits = slots.keys.size < limits.maximumCatalogFiles && depth <= limits.maximumCatalogDepth
        if (!isWithinLimits) {
            slots[uri] = CatalogSlot.BeyondLimits
            reported += XmlCatalogProblem(uri, XmlCatalogProblemKind.LimitExceeded, BEYOND_LIMITS)
            return null
        }
        val file = readOne(uri)
        slots[uri] = file?.let(CatalogSlot::Loaded) ?: CatalogSlot.Unavailable
        return file
    }

    private fun readOne(uri: String): CatalogFile? {
        val source = loader.load(uri)
        if (source == null) reported += XmlCatalogProblem(uri, XmlCatalogProblemKind.Unloaded, REFUSED)
        val reference = UriReference.parseOrNull(uri)
        if (source == null || reference == null) return null
        return reader.read(source, reference)
    }

    private companion object {
        const val NOT_ABSOLUTE: String = "is no absolute URI"
        const val REFUSED: String = "was refused by the loader"
        const val BEYOND_LIMITS: String = "lies beyond the most catalog files or the deepest nesting of catalogs"
    }
}

internal fun keyOf(reference: UriReference): String = reference.normalize().toString()

internal fun catalogKeyOf(target: String): String? = UriReference.parseOrNull(target)?.let(::keyOf)
