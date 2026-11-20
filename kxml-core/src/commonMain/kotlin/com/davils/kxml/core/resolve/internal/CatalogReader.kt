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

package com.davils.kxml.core.resolve.internal

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.uri.UriReference
import com.davils.kxml.core.Xml
import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.resolve.XmlCatalogPrefer
import com.davils.kxml.core.resolve.XmlCatalogProblem
import com.davils.kxml.core.resolve.XmlCatalogProblemKind
import com.davils.kxml.core.source.XmlSource
import com.davils.kxml.core.xml

internal const val CATALOG_NAMESPACE: String = "urn:oasis:names:tc:entity:xmlns:xml:catalog"

private const val CATALOG: String = "catalog"
private const val GROUP: String = "group"
private const val PREFER_PUBLIC: String = "public"
private const val PREFER_SYSTEM: String = "system"

private val catalogName: XmlName = XmlName(local = CATALOG, namespace = CATALOG_NAMESPACE)
private val baseName: XmlName = XmlName(local = "base", namespace = XmlNamespaces.XML)
private val preferName: XmlName = XmlName(local = "prefer")

internal class CatalogReader(
    private val prefer: XmlCatalogPrefer,
    limits: XmlLimits,
    private val problems: MutableList<XmlCatalogProblem>,
) {
    private val reader: Xml = xml {
        doctype = XmlDoctype.Internal
        this.limits = limits
    }

    fun read(source: XmlSource, uri: UriReference): CatalogFile? {
        val root = rootOf(source, uri) ?: return null
        val entries = Walk(uri).entriesOf(root)
        return entries?.let { found -> CatalogFile(uri.toString(), found) }
    }

    private fun rootOf(source: XmlSource, uri: UriReference): XmlNode.Element? {
        val parsed = reader.parse(source) as? XmlParseResult.Parsed
        val document = parsed?.document
        val root = document?.root
        val isCatalog = root?.name == catalogName
        if (root == null) report(uri, XmlCatalogProblemKind.Malformed, "is not well-formed XML")
        if (root != null && !isCatalog) {
            report(uri, XmlCatalogProblemKind.NotCatalog, "has no catalog element of the catalog namespace")
        }
        return root.takeIf { isCatalog }
    }

    private fun report(uri: UriReference, kind: XmlCatalogProblemKind, detail: String) {
        problems += XmlCatalogProblem(uri.toString(), kind, detail)
    }

    private inner class Walk(private val uri: UriReference) {
        private val entries = mutableListOf<CatalogEntry>()
        private val frames = ArrayDeque<Frame>()

        fun entriesOf(root: XmlNode.Element): List<CatalogEntry>? {
            val rootScope = Scope(base = uri, isPreferPublic = prefer == XmlCatalogPrefer.Public)
            val scope = scopeOf(root, rootScope) ?: return null
            frames.addLast(Frame(scope, root.elements.iterator()))
            while (frames.isNotEmpty()) {
                val frame = frames.last()
                val element = frame.next()
                if (element == null) frames.removeLast()
                element?.let { child -> visit(child, frame.scope) }
            }
            return entries.toList()
        }

        private fun visit(element: XmlNode.Element, scope: Scope) {
            val isCatalogElement = element.name.namespace == CATALOG_NAMESPACE
            val inner = scope.takeIf { isCatalogElement }?.let { outer -> scopeOf(element, outer) } ?: return
            val local = element.name.local
            if (local == GROUP) frames.addLast(Frame(inner, element.elements.iterator()))
            if (local != GROUP) entryIn(element, local, inner)
        }

        private fun entryIn(element: XmlNode.Element, local: String, scope: Scope) {
            val kind = EntryKind.of(local)
            if (kind == null) report("has the unknown element $local")
            val entry = kind?.let { known -> entryOf(element, known, scope) }
            entry?.let(entries::add)
        }

        private fun scopeOf(element: XmlNode.Element, outer: Scope): Scope? {
            val base = baseOf(element, outer.base) ?: return null
            val isGroupOrCatalog = element.name.local == GROUP || element.name.local == CATALOG
            val preferAttribute = element.attribute(preferName).takeIf { isGroupOrCatalog }
            val isPreferPublic = preferAttribute?.let { found -> preferenceOf(found.value, outer.isPreferPublic) }
            return Scope(base = base, isPreferPublic = isPreferPublic ?: outer.isPreferPublic)
        }

        private fun baseOf(element: XmlNode.Element, outer: UriReference): UriReference? {
            val written = element.attribute(baseName)?.value ?: return outer
            val base = referenceOf(written)?.let(outer::resolve)
            if (base == null) report("has an xml:base on ${element.name.local} that is no URI reference")
            return base
        }

        private fun preferenceOf(value: String, outer: Boolean): Boolean {
            val isKnown = value == PREFER_PUBLIC || value == PREFER_SYSTEM
            if (!isKnown) report("has a prefer attribute that is neither public nor system")
            if (value == PREFER_PUBLIC) return true
            return value != PREFER_SYSTEM && outer
        }

        private fun entryOf(element: XmlNode.Element, kind: EntryKind, scope: Scope): CatalogEntry? {
            val key = kind.keyAttribute?.let { attribute -> keyOf(element, kind, attribute) }
            val isKeyMissing = kind.keyAttribute != null && key == null
            val written = kind.targetAttribute.takeUnless { isKeyMissing }?.let { name -> attributeOf(element, name) }
            val target = written?.let { value -> targetOf(value, kind, scope) }
            return target?.let { reference ->
                CatalogEntry(
                    kind = kind,
                    key = key.orEmpty(),
                    target = reference.toString(),
                    isPreferPublic = scope.isPreferPublic,
                )
            }
        }

        private fun targetOf(written: String, kind: EntryKind, scope: Scope): UriReference? {
            val target = referenceOf(written)?.let(scope.base::resolve)
            if (target == null) report("has a ${kind.targetAttribute} on ${kind.element} that is no URI reference")
            return target
        }

        private fun keyOf(element: XmlNode.Element, kind: EntryKind, attribute: String): String? {
            val written = attributeOf(element, attribute)
            return written?.let { value -> normalizedKey(value, kind) }
        }

        private fun normalizedKey(value: String, kind: EntryKind): String? {
            if (kind.isPublicKey) return normalizedPublicId(value)
            return normalizedSystemId(value)
        }

        private fun attributeOf(element: XmlNode.Element, attribute: String): String? {
            val value = element.attribute(XmlName(local = attribute))?.value
            if (value == null) report("misses the attribute $attribute on ${element.name.local}")
            return value
        }

        private fun report(detail: String) {
            problems += XmlCatalogProblem(uri.toString(), XmlCatalogProblemKind.InvalidEntry, detail)
        }
    }

    private data class Scope(val base: UriReference, val isPreferPublic: Boolean)

    private class Frame(val scope: Scope, private val children: Iterator<XmlNode.Element>) {
        fun next(): XmlNode.Element? = children.takeIf { iterator -> iterator.hasNext() }?.next()
    }
}

internal fun referenceOf(written: String): UriReference? = normalizedSystemId(written)?.let(UriReference::parseOrNull)
