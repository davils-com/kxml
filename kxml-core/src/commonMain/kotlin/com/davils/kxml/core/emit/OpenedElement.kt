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

package com.davils.kxml.core.emit

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.collection.OrderedSet
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding

private const val DEFAULT: String = ""

internal class OpenedElement {
    private val declared = OrderedSet<String>()
    private val bindings = mutableListOf<XmlNamespaceBinding>()
    private val attributePrefixes = OrderedMap<String, String>()

    val declarations: List<XmlNamespaceBinding> get() = bindings.toList()

    val prefixes: List<String> get() = bindings.map { binding -> binding.prefix ?: DEFAULT }

    fun declare(prefix: String, uri: String) {
        declared.add(prefix)
        bindings += XmlNamespaceBinding(prefix.takeIf { it.isNotEmpty() }, uri)
    }

    fun isDeclared(prefix: String): Boolean = prefix in declared

    fun hasAttributePrefix(namespace: String): Boolean = namespace in attributePrefixes

    fun useAttributePrefix(namespace: String, prefix: String) {
        attributePrefixes[namespace] = prefix
    }

    fun attributeName(name: XmlName): String {
        val namespace = name.namespace ?: return name.local
        val prefix = checkNotNull(attributePrefixes[namespace]) { "The namespace of an attribute has no prefix" }
        return "$prefix:${name.local}"
    }
}
