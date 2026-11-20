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

internal const val TEST_SCHEME: String = "urn:test:"

internal class RecordingResolver(vararg resources: Pair<String, String>) : XmlResolver {
    private val texts: Map<String, String> = resources.toMap()
    private val recorded = mutableListOf<XmlResourceRequest>()

    val requests: List<XmlResourceRequest> get() = recorded.toList()

    override fun resolve(request: XmlResourceRequest): XmlSource? {
        recorded += request
        val text = texts[request.systemId] ?: return null
        return XmlSource.of(text, uri = TEST_SCHEME + request.systemId)
    }
}
