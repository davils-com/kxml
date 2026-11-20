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

import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.node.XmlAttribute
import com.davils.kxml.core.node.XmlNode

private const val DEFAULT_PREFIX: String = ""

internal class NamespaceUse(private val prefix: String, private val charge: (Int) -> Unit) {
    fun isUsed(element: XmlNode.Element, isOwner: Boolean = true): Boolean {
        charge(1)
        if (!isOwner && isShadowed(element)) return false
        val isNamed = uses(element.name, isElement = true) ||
            element.attributes.any { attribute -> uses(attribute.name, isElement = false) }
        return isNamed || element.elements.any { child -> isUsed(child, isOwner = false) }
    }

    fun rewritten(element: XmlNode.Element, uri: String, isOwner: Boolean = true): XmlNode.Element {
        charge(1)
        if (!isOwner && isShadowed(element)) return element
        val namespaces = element.namespaces.map { binding -> rebound(binding, uri, isOwner) }
        val attributes = element.attributes.map { attribute ->
            val name = renamed(attribute.name, uri, isElement = false)
            XmlAttribute(name, attribute.value)
        }
        val children = element.children.map { child ->
            (child as? XmlNode.Element)?.let { found -> rewritten(found, uri, isOwner = false) } ?: child
        }
        val name = renamed(element.name, uri, isElement = true)
        return element.rebuilt(name = name, attributes = attributes, namespaces = namespaces, children = children)
    }

    private fun isShadowed(element: XmlNode.Element): Boolean =
        element.namespaces.any { binding -> prefixOf(binding) == prefix }

    private fun rebound(binding: XmlNamespaceBinding, uri: String, isOwner: Boolean): XmlNamespaceBinding {
        val isRebound = isOwner && prefixOf(binding) == prefix
        if (!isRebound) return binding
        return XmlNamespaceBinding(prefix = binding.prefix, uri = uri)
    }

    private fun renamed(name: XmlName, uri: String, isElement: Boolean): XmlName {
        if (!uses(name, isElement)) return name
        return XmlName(local = name.local, namespace = uri, prefix = name.prefix)
    }

    private fun uses(name: XmlName, isElement: Boolean): Boolean {
        val written = name.prefix ?: DEFAULT_PREFIX
        val isDefault = written == DEFAULT_PREFIX
        if (isDefault) return isElement && prefix == DEFAULT_PREFIX && name.namespace != null
        return written == prefix
    }
}
