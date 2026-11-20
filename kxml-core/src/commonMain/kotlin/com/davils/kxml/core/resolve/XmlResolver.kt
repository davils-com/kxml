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

import com.davils.kxml.core.source.XmlSource

/**
 * Hands the reader the external resources a document refers to, such as its external subset and its external
 * entities, under [XmlDoctype.External][com.davils.kxml.core.XmlDoctype.External].
 *
 * kxml never loads a resource on its own; it asks the resolver once for every resource and charges what it returns to
 * the limits. A resolver decides what a document may reach, so it should map identifiers to resources it trusts, such
 * as files below one directory or a catalog, and never fetch whatever URI a document names. Returning `null` refuses
 * the resource, which ends reading with an
 * [UnresolvedEntity][com.davils.kxml.core.diagnostic.XmlDiagnosticCode.UnresolvedEntity] diagnostic; to skip a
 * resource, return an empty source. The [XmlSource.uri] of the result is the base URI of the references the resource
 * makes. An exception the resolver throws is not caught and leaves the reader. A resolver may be called from several
 * readers at once when one configuration is shared.
 *
 * @since 1.0.0
 */
public fun interface XmlResolver {
    /**
     * Returns the resource [request] names, or `null` to refuse it.
     *
     * @param request what the document refers to and from where.
     * @return the resource, or `null`.
     * @since 1.0.0
     */
    public fun resolve(request: XmlResourceRequest): XmlSource?
}
