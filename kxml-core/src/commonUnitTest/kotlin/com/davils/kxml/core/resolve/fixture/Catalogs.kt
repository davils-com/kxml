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

package com.davils.kxml.core.resolve.fixture

import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.resolve.XmlCatalog
import com.davils.kxml.core.resolve.XmlCatalogPrefer

internal const val ROOT: String = "https://c.test/catalog.xml"
internal const val OTHER: String = "https://c.test/other.xml"
internal const val THIRD: String = "https://c.test/third.xml"
internal const val CATALOG_NAMESPACE: String = "urn:oasis:names:tc:entity:xmlns:xml:catalog"

internal fun catalog(vararg entries: String, attributes: String = ""): String =
    "<catalog xmlns='$CATALOG_NAMESPACE' $attributes>${entries.joinToString(separator = "")}</catalog>"

internal fun system(systemId: String, uri: String): String = "<system systemId='$systemId' uri='$uri'/>"

internal fun public(publicId: String, uri: String): String = "<public publicId='$publicId' uri='$uri'/>"

internal fun nextCatalog(catalog: String): String = "<nextCatalog catalog='$catalog'/>"

internal fun loaded(
    vararg files: Pair<String, String>,
    prefer: XmlCatalogPrefer = XmlCatalogPrefer.Public,
    limits: XmlLimits = XmlLimits.Untrusted,
): XmlCatalog = XmlCatalog.load(catalogs = listOf(ROOT), loader = MapLoader(*files), prefer = prefer, limits = limits)

internal fun loadedFrom(files: Map<String, String>, limits: XmlLimits): XmlCatalog =
    XmlCatalog.load(catalogs = listOf(ROOT), loader = MapLoader(files), limits = limits)

internal fun single(vararg entries: String, attributes: String = ""): XmlCatalog =
    loaded(ROOT to catalog(*entries, attributes = attributes))
