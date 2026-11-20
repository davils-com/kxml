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
package com.davils.kxml.document.patch.apply

import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.navigation.DocumentModel

internal fun DocumentModel.requireShapeWithin(limits: XmlLimits) {
    val depths = IntArray(size)
    var elements = 0L
    for (node in 0..<size) {
        if (!isElement(node)) continue
        val depth = depths[parent(node)] + 1
        depths[node] = depth
        elements += 1
        requireWithin(depth <= limits.maximumDepth, limit = "maximumDepth")
        requireWithin(attributes(node).count() <= limits.maximumAttributes, limit = "maximumAttributes")
    }
    requireWithin(elements <= limits.maximumElements, limit = "maximumElements")
}

private fun requireWithin(isWithin: Boolean, limit: String) {
    if (!isWithin) throw XmlDocumentLimitException(limit)
}
