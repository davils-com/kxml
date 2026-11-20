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

import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.document.navigation.DocumentModel

internal class OperationContext(val model: DocumentModel, val editor: TreeEditor, val charge: (Int) -> Unit) {
    fun scopeOf(element: Int): Map<String, String> {
        val scope = mutableMapOf(XmlNamespaces.XML_PREFIX to XmlNamespaces.XML)
        var current = element
        while (current > DocumentModel.ROOT) {
            model.declaredNamespaces(current).forEach { binding ->
                charge(1)
                scope.getOrPut(prefixOf(binding)) { binding.uri }
            }
            current = model.parent(current)
        }
        return scope.filterValues(String::isNotEmpty)
    }

    fun isDeclaredOn(element: Int, prefix: String): Boolean =
        model.declaredNamespaces(element).any { binding -> prefixOf(binding) == prefix }
}
