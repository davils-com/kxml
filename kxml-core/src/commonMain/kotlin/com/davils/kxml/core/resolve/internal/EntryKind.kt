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

private const val URI: String = "uri"
private const val CATALOG: String = "catalog"
private const val REWRITE_PREFIX: String = "rewritePrefix"
private const val SYSTEM_ID_START: String = "systemIdStartString"
private const val URI_START: String = "uriStartString"

internal enum class EntryKind(val element: String, val keyAttribute: String?, val targetAttribute: String) {
    Public(element = "public", keyAttribute = "publicId", targetAttribute = URI),
    System(element = "system", keyAttribute = "systemId", targetAttribute = URI),
    RewriteSystem(element = "rewriteSystem", keyAttribute = SYSTEM_ID_START, targetAttribute = REWRITE_PREFIX),
    SystemSuffix(element = "systemSuffix", keyAttribute = "systemIdSuffix", targetAttribute = URI),
    DelegatePublic(element = "delegatePublic", keyAttribute = "publicIdStartString", targetAttribute = CATALOG),
    DelegateSystem(element = "delegateSystem", keyAttribute = SYSTEM_ID_START, targetAttribute = CATALOG),
    Uri(element = URI, keyAttribute = "name", targetAttribute = URI),
    RewriteUri(element = "rewriteURI", keyAttribute = URI_START, targetAttribute = REWRITE_PREFIX),
    UriSuffix(element = "uriSuffix", keyAttribute = "uriSuffix", targetAttribute = URI),
    DelegateUri(element = "delegateURI", keyAttribute = URI_START, targetAttribute = CATALOG),
    NextCatalog(element = "nextCatalog", keyAttribute = null, targetAttribute = CATALOG),
    ;

    val isPublicKey: Boolean get() = this == Public || this == DelegatePublic

    val isReferenceToCatalog: Boolean get() = targetAttribute == CATALOG

    companion object {
        fun of(element: String): EntryKind? = entries.firstOrNull { kind -> kind.element == element }
    }
}
