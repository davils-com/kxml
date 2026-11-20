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

package com.davils.kxml.document.c14n.render

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.document.navigation.DocumentModel

private const val DEFAULT_PREFIX: String = ""

internal class ExclusiveNamespaces(
    private val model: DocumentModel,
    private val membership: Membership,
    private val inclusivePrefixes: Set<String>,
) : NamespacePolicy {
    private val scope: ScopedMap = ScopedMap()
    private val rendered: ScopedMap = ScopedMap()

    override fun open(element: Int, isIncluded: Boolean, attributes: List<Rendered>): List<Declaration> {
        scope.open()
        rendered.open()
        model.declaredNamespaces(element).forEach { binding ->
            scope.set(key = binding.prefix ?: DEFAULT_PREFIX, value = binding.uri)
        }
        if (!isIncluded) return emptyList()
        val own = membership.namespacesOf(element)
        val declarations = mutableListOf<Declaration>()
        prefixesOf(element, attributes).forEach { prefix ->
            val uri = uriOf(prefix, own)
            val isDeclared = uri != null && rendered[prefix] != uri
            val isUndeclared = uri == null && prefix == DEFAULT_PREFIX && !rendered[prefix].isNullOrEmpty()
            if (isDeclared || isUndeclared) {
                val value = uri.orEmpty()
                declarations += Declaration(prefix, value)
                rendered.set(prefix, value)
            }
        }
        return declarations
    }

    override fun close() {
        rendered.close()
        scope.close()
    }

    private fun prefixesOf(element: Int, attributes: List<Rendered>): List<String> {
        val prefixes = OrderedSet<String>()
        prefixes.add(model.name(element)?.prefix ?: DEFAULT_PREFIX)
        attributes.forEach { attribute -> attribute.name.prefix?.let(prefixes::add) }
        inclusivePrefixes.forEach(prefixes::add)
        prefixes.remove(XmlNamespaces.XML_PREFIX)
        return prefixes.sortedWith(CodePointOrder)
    }

    private fun uriOf(prefix: String, own: Map<String, String>): String? {
        val uri = own[prefix].takeIf { membership.isSubset } ?: scope[prefix].takeUnless { membership.isSubset }
        return uri?.takeIf(String::isNotEmpty)
    }
}
