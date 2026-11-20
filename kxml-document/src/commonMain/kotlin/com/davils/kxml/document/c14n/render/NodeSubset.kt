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
import com.davils.kore.core.collection.OrderedMap
import com.davils.kxml.document.navigation.NodeSet
import com.davils.kxml.document.navigation.PrefixScope
import com.davils.kxml.document.xpath.evaluate.NodeAccess
import com.davils.kxml.document.xpath.evaluate.NodeKeys

private const val DEFAULT_PREFIX: String = ""

internal class NodeSubset(keys: LongArray, nodes: NodeAccess) : Membership {
    private val members: NodeSet = NodeSet(nodes.model.size)
    private val namespaces: MutableMap<Int, OrderedMap<String, String>> = mutableMapOf()

    init {
        keys.forEach { key ->
            val ordinal = NodeKeys.ordinalOf(key)
            if (!NodeKeys.isNamespace(key)) members.add(ordinal)
            if (NodeKeys.isNamespace(key)) {
                val binding = nodes.namespaceOf(key)
                val bindings = namespaces.getOrPut(ordinal) { OrderedMap() }
                bindings[binding.prefix ?: DEFAULT_PREFIX] = binding.uri
            }
        }
    }

    override val isSubset: Boolean get() = true

    override fun isIncluded(node: Int): Boolean = node in members

    override fun namespacesOf(element: Int): Map<String, String> {
        val bindings = namespaces[element] ?: return PrefixScope.EMPTY
        val entries = bindings.entries.sortedWith(compareBy(CodePointOrder) { entry -> entry.first })
        return PrefixScope.EMPTY.with(entries)
    }
}
