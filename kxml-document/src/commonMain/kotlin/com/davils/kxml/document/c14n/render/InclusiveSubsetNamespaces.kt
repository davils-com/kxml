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
package com.davils.kxml.document.c14n.render

import com.davils.kxml.core.name.XmlNamespaces

private const val DEFAULT_PREFIX: String = ""

internal class InclusiveSubsetNamespaces(private val membership: Membership) : NamespacePolicy {
    private val outputs: ArrayDeque<Map<String, String>> = ArrayDeque()
    private val pushed: ArrayDeque<Boolean> = ArrayDeque()

    override fun open(element: Int, isIncluded: Boolean, attributes: List<Rendered>): List<Declaration> {
        val own = membership.namespacesOf(element)
        val ancestor = outputs.lastOrNull().orEmpty()
        val declarations = mutableListOf<Declaration>()
        val isUndeclared = isIncluded && DEFAULT_PREFIX !in own && ancestor[DEFAULT_PREFIX] != null
        if (isUndeclared) declarations += Declaration(prefix = DEFAULT_PREFIX, uri = "")
        own.forEach { (prefix, uri) ->
            val isXml = prefix == XmlNamespaces.XML_PREFIX && uri == XmlNamespaces.XML
            if (!isXml && ancestor[prefix] != uri) declarations += Declaration(prefix, uri)
        }
        if (isIncluded) outputs.addLast(own)
        pushed.addLast(isIncluded)
        return declarations
    }

    override fun close() {
        if (pushed.removeLast()) outputs.removeLast()
    }
}
