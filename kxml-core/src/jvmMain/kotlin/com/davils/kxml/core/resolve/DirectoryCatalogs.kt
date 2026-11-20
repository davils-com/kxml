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

import com.davils.kxml.core.limit.XmlLimits
import java.nio.file.Path

private const val DEFAULT_CATALOG_FILE: String = "catalog.xml"

/**
 * Loads the catalog file [catalogFile] below [root] and every catalog file it reaches through an
 * [XmlDirectoryLoader] of [root], so the catalog never loads a file outside the directory.
 *
 * @param root the directory of the catalog files; it must exist.
 * @param catalogFile the first catalog file, relative to [root]; `catalog.xml` by default.
 * @param prefer the `prefer` setting where no element sets one; [XmlCatalogPrefer.Public] by default.
 * @param limits the limits of [XmlCatalog.load]; [XmlLimits.Untrusted] by default.
 * @return the catalog.
 * @throws java.io.IOException if [root] does not exist or cannot be read.
 * @throws IllegalArgumentException if [root] is no directory.
 * @since 1.0.0
 */
public fun XmlCatalog.Companion.fromDirectory(
    root: Path,
    catalogFile: String = DEFAULT_CATALOG_FILE,
    prefer: XmlCatalogPrefer = XmlCatalogPrefer.Public,
    limits: XmlLimits = XmlLimits.Untrusted,
): XmlCatalog {
    val loader = XmlDirectoryLoader(root = root, maximumLength = limits.maximumExternalLength)
    return catalogOf(loader = loader, catalogFile = catalogFile, prefer = prefer, limits = limits)
}

/**
 * Returns a resolver whose catalog and resources come from [root] only: the catalog of
 * [fromDirectory][XmlCatalog.Companion.fromDirectory] and an [XmlDirectoryLoader] of [root] for every resource it
 * maps to. A resource the catalog maps to a URI outside [root] is refused.
 *
 * @param root the directory of the catalog files and of the resources; it must exist.
 * @param catalogFile the first catalog file, relative to [root]; `catalog.xml` by default.
 * @param prefer the `prefer` setting where no element sets one; [XmlCatalogPrefer.Public] by default.
 * @param limits the limits of [XmlCatalog.load] and the longest resource; [XmlLimits.Untrusted] by default.
 * @param fallback resolves what the catalog does not map, or `null` to refuse it, the default.
 * @return the resolver.
 * @throws java.io.IOException if [root] does not exist or cannot be read.
 * @throws IllegalArgumentException if [root] is no directory.
 * @since 1.0.0
 */
public fun XmlCatalogResolver.Companion.fromDirectory(
    root: Path,
    catalogFile: String = DEFAULT_CATALOG_FILE,
    prefer: XmlCatalogPrefer = XmlCatalogPrefer.Public,
    limits: XmlLimits = XmlLimits.Untrusted,
    fallback: XmlResolver? = null,
): XmlCatalogResolver {
    val loader = XmlDirectoryLoader(root = root, maximumLength = limits.maximumExternalLength)
    val catalog = catalogOf(loader = loader, catalogFile = catalogFile, prefer = prefer, limits = limits)
    return XmlCatalogResolver(catalog = catalog, loader = loader, fallback = fallback)
}

private fun catalogOf(
    loader: XmlDirectoryLoader,
    catalogFile: String,
    prefer: XmlCatalogPrefer,
    limits: XmlLimits,
): XmlCatalog {
    val first = loader.uriOf(catalogFile) ?: catalogFile
    return XmlCatalog.load(catalogs = listOf(first), loader = loader, prefer = prefer, limits = limits)
}
