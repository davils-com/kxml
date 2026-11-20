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

import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.document.navigation.DocumentModel

private const val BASE: String = "base"
private val simpleInheritable: Set<String> = setOf("lang", "space")
private val namespaceOrder: Comparator<Rendered> =
    compareBy(CodePointOrder) { attribute -> attribute.name.namespace.orEmpty() }
private val attributeOrder: Comparator<Rendered> =
    namespaceOrder.thenBy(CodePointOrder) { attribute -> attribute.name.local }

internal class AttributeScope(
    private val model: DocumentModel,
    private val membership: Membership,
    private val inheritance: Inheritance,
    private val charge: (Int) -> Unit,
) {
    private val inherited: ScopedMap = ScopedMap()
    private val chain: ArrayDeque<Link> = ArrayDeque()

    fun open(element: Int, isIncluded: Boolean): List<Rendered> {
        val ordinals = model.attributes(element).toList()
        val own = ordinals.map(::renderedOf)
        val included = ordinals.filter(membership::isIncluded).map(::renderedOf)
        val isOrphan = isIncluded && !membership.isIncluded(model.parent(element))
        val isImporting = isOrphan && inheritance != Inheritance.None
        val rendered = inheritedInto(own, included).takeIf { isImporting } ?: included
        inherited.open()
        val xmlAttributes = own.filter(::isXml)
        xmlAttributes.forEach { attribute -> inherited.set(key = attribute.name.local, value = attribute.value) }
        chain.addLast(Link(isIncluded = isIncluded, base = own.firstOrNull(::isBase)?.value))
        return rendered.sortedWith(attributeOrder)
    }

    fun close() {
        inherited.close()
        chain.removeLast()
    }

    private fun inheritedInto(own: List<Rendered>, included: List<Rendered>): List<Rendered> {
        val ownNames = own.filter(::isXml).mapTo(mutableSetOf()) { attribute -> attribute.name.local }
        val entries = inherited.entries()
        charge(entries.size)
        val isSimple = inheritance == Inheritance.SimpleXml
        val candidates = entries.filter { (local, _) -> !isSimple || local in simpleInheritable }
        val missing = candidates.filter { (local, _) -> local !in ownNames }
        val added = missing.map { (local, value) -> xmlAttribute(local = local, value = value) }
        if (!isSimple) return included + added
        return fixedBase(included) + added
    }

    private fun fixedBase(included: List<Rendered>): List<Rendered> {
        val omitted = chain.asReversed().takeWhile { link -> !link.isIncluded }
        charge(omitted.size)
        val bases = omitted.mapNotNull(Link::base)
        if (bases.isEmpty()) return included
        return included.filterNot(::isBase) + listOfNotNull(joinedBase(included, bases))
    }

    private fun joinedBase(included: List<Rendered>, bases: List<String>): Rendered? {
        var value = included.firstOrNull(::isBase)?.value
        bases.forEach { base ->
            value = value?.let { inner -> BaseJoin.joined(base = base, reference = inner) } ?: base
        }
        val fixed = value?.takeIf(String::isNotEmpty)
        return fixed?.let { joined -> xmlAttribute(local = BASE, value = joined) }
    }

    private fun renderedOf(attribute: Int): Rendered =
        Rendered(name = model.attributeName(attribute), value = model.value(attribute).orEmpty())

    private fun isXml(attribute: Rendered): Boolean = attribute.name.namespace == XmlNamespaces.XML

    private fun isBase(attribute: Rendered): Boolean = isXml(attribute) && attribute.name.local == BASE

    private fun xmlAttribute(local: String, value: String): Rendered {
        val name = XmlName(local = local, namespace = XmlNamespaces.XML, prefix = XmlNamespaces.XML_PREFIX)
        return Rendered(name, value)
    }

    private data class Link(val isIncluded: Boolean, val base: String?)
}
