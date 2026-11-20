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

package com.davils.kxml.core.parser

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.collection.OrderedSet
import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.node.XmlAttribute

private const val DEFAULT: String = ""
private const val COLON: Char = ':'
private const val DECLARATION_PREFIX: String = "xmlns:"

internal class NamespaceResolver(private val dialect: Dialect) {
    private val scopes = OrderedMap<String, ArrayDeque<String>>()
    private val frames = ArrayDeque<List<String>>()

    init {
        bind(prefix = XmlNamespaces.XML_PREFIX, uri = XmlNamespaces.XML)
    }

    fun open(tag: RawTag): ResolvedTag {
        val bindings = tag.attributes.mapNotNull(::bindingOf)
        val prefixes = bindings.map { binding -> binding.prefix ?: DEFAULT }
        frames.addLast(prefixes)
        bindings.forEach { binding -> bind(prefix = binding.prefix ?: DEFAULT, uri = binding.uri) }
        val name = elementName(tag)
        val plain = tag.attributes.filter { attribute -> declaredPrefixOf(attribute.qualified) == null }
        val attributes = plain.map { raw -> raw to attributeOf(raw) }
        checkDuplicates(attributes)
        return ResolvedTag(name, attributes.map { (_, attribute) -> attribute }, bindings)
    }

    fun close() {
        val prefixes = frames.removeLast()
        prefixes.forEach { prefix -> scopes[prefix]?.removeLast() }
    }

    private fun bind(prefix: String, uri: String) {
        val stack = scopes.getOrPut(prefix) { ArrayDeque() }
        stack.add(uri)
    }

    private fun bindingOf(attribute: RawAttribute): XmlNamespaceBinding? {
        val prefix = declaredPrefixOf(attribute.qualified) ?: return null
        checkBinding(prefix, attribute)
        return XmlNamespaceBinding(prefix = prefix.takeIf { it.isNotEmpty() }, uri = attribute.value)
    }

    private fun declaredPrefixOf(qualified: String): String? {
        val isDefault = qualified == XmlNamespaces.XMLNS_PREFIX
        val prefixed = qualified.takeIf { it.startsWith(DECLARATION_PREFIX) }?.removePrefix(DECLARATION_PREFIX)
        return DEFAULT.takeIf { isDefault } ?: prefixed
    }

    private fun checkBinding(prefix: String, attribute: RawAttribute) {
        val uri = attribute.value
        val isXmlPrefix = prefix == XmlNamespaces.XML_PREFIX
        val isReservedUri = uri == XmlNamespaces.XML || uri == XmlNamespaces.XMLNS
        if (prefix == XmlNamespaces.XMLNS_PREFIX) reserved(attribute, "The prefix xmlns cannot be declared")
        if (isXmlPrefix && uri != XmlNamespaces.XML) {
            reserved(
                attribute,
                "The prefix xml is bound to its own namespace",
            )
        }
        if (!isXmlPrefix && isReservedUri) reserved(attribute, "The namespace $uri is reserved by XML")
        if (prefix.isNotEmpty() && uri.isEmpty() && !dialect.isVersion11) {
            failure(XmlDiagnosticCode.EmptyPrefixBinding, attribute.range, "The prefix $prefix cannot be unbound")
        }
    }

    private fun reserved(attribute: RawAttribute, message: String): Nothing =
        failure(XmlDiagnosticCode.ReservedPrefix, attribute.range, message)

    private fun elementName(tag: RawTag): XmlName {
        val prefix = prefixOf(tag.qualified)
        if (prefix == XmlNamespaces.XMLNS_PREFIX) {
            failure(XmlDiagnosticCode.ReservedPrefix, tag.range, "Elements cannot use the prefix xmlns")
        }
        val namespace = resolve(prefix ?: DEFAULT)
        if (prefix != null && namespace == null) undeclared(prefix, tag.range)
        return XmlName(tag.qualified.substringAfter(COLON), namespace, prefix)
    }

    private fun attributeOf(attribute: RawAttribute): XmlAttribute {
        val prefix = prefixOf(attribute.qualified)
        val namespace = prefix?.let { resolve(it) ?: undeclared(it, attribute.range) }
        val name = XmlName(attribute.qualified.substringAfter(COLON), namespace, prefix)
        return XmlAttribute(name, attribute.value, attribute.range)
    }

    private fun checkDuplicates(attributes: List<Pair<RawAttribute, XmlAttribute>>) {
        val seen = OrderedSet<String>()
        val duplicate = attributes.firstOrNull { (_, attribute) -> !seen.add(keyOf(attribute.name)) } ?: return
        val (raw, attribute) = duplicate
        failure(
            XmlDiagnosticCode.DuplicateAttribute,
            raw.range,
            "The attribute ${attribute.name} is set twice",
        )
    }

    private fun keyOf(name: XmlName): String {
        val namespace = name.namespace ?: return name.local
        return "${name.local}$COLON$namespace"
    }

    private fun prefixOf(qualified: String): String? =
        qualified.substringBefore(COLON, missingDelimiterValue = DEFAULT).takeIf { it.isNotEmpty() }

    private fun resolve(prefix: String): String? {
        val found = scopes[prefix]?.lastOrNull()
        return found?.takeIf { it.isNotEmpty() }
    }

    private fun undeclared(prefix: String, range: TextRange): Nothing =
        failure(XmlDiagnosticCode.UndeclaredPrefix, range, "The prefix $prefix is not declared")
}
