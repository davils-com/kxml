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

package com.davils.kxml.core.dtd

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.validation.violation.Severity
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.parser.failure
import com.davils.kxml.core.resolve.XmlResolver
import com.davils.kxml.core.resolve.XmlResourceRequest
import com.davils.kxml.core.source.SourceDecoding

internal class ExternalResources(private val resolver: XmlResolver?, private val limits: XmlLimits) {
    private val loaded = OrderedMap<String, LoadedResource>()
    private var count = 0

    val isEnabled: Boolean get() = resolver != null

    fun load(request: XmlResourceRequest, range: TextRange): LoadedResource {
        val key = "${request.kind.ordinal}${request.name.orEmpty()}"
        return loaded[key] ?: fetched(request, range).also { resource -> loaded[key] = resource }
    }

    private fun fetched(request: XmlResourceRequest, range: TextRange): LoadedResource {
        count++
        if (count > limits.maximumExternalResources) {
            failure(
                XmlDiagnosticCode.TooManyExternalResources,
                range,
                "The document refers to more than ${limits.maximumExternalResources} external resources",
            )
        }
        val source = resolver?.resolve(request) ?: failure(
            XmlDiagnosticCode.UnresolvedEntity,
            range,
            "No resolver hands over the external resource ${request.systemId}",
        )
        if (source.length > limits.maximumExternalLength) {
            failure(
                XmlDiagnosticCode.ExternalResourceTooLong,
                range,
                "The external resource is longer than ${limits.maximumExternalLength}",
            )
        }
        val decoded = SourceDecoding.decode(source)
        val error = decoded.diagnostics.firstOrNull { diagnostic -> diagnostic.severity == Severity.Error }
        if (error != null) failure(error.code, range, error.message)
        return LoadedResource(text = decoded.text, encoding = decoded.encoding, uri = source.uri)
    }
}
