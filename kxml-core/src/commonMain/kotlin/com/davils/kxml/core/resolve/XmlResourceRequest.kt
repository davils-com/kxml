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

package com.davils.kxml.core.resolve

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kxml.core.resolve.internal.referenceOf

private const val NONE: String = "none"

/**
 * An external resource a document refers to, as the reader asks an [XmlResolver] for it.
 *
 * Every value comes from the document or from the [uri][com.davils.kxml.core.source.XmlSource.uri] of a source and
 * is input: a resolver must not hand it to a file system or a network client unchecked. An instance is immutable.
 *
 * @since 1.0.0
 */
public class XmlResourceRequest internal constructor(
    kind: XmlResourceKind,
    name: String?,
    publicId: String?,
    systemId: String,
    baseUri: String?,
) {
    /**
     * What the resource is to the document.
     *
     * @since 1.0.0
     */
    public val kind: XmlResourceKind = kind

    /**
     * The name of the entity, without `&` or `%`, or `null` for the external subset.
     *
     * @since 1.0.0
     */
    public val name: String? = name

    /**
     * The public identifier, normalized as section 4.2.2 of XML 1.0 describes it, or `null`.
     *
     * @since 1.0.0
     */
    public val publicId: String? = publicId

    /**
     * The system identifier as the declaration writes it, which may be relative to [baseUri].
     *
     * @since 1.0.0
     */
    public val systemId: String = systemId

    /**
     * The URI of the resource that declares the reference, the document or another external resource, or `null` when
     * that resource has none.
     *
     * @since 1.0.0
     */
    public val baseUri: String? = baseUri

    /**
     * The system identifier made absolute: escaped as section 4.2.2 of XML 1.0 describes it, which writes every
     * character outside printable ASCII and every space as `%HH` of its UTF-8 bytes, and resolved against [baseUri] by
     * section 5.2 of RFC 3986. `null` when the system identifier or the base URI is no URI reference, or when the
     * system identifier is relative and there is no absolute base URI.
     *
     * It is computed from input; a resolver must still decide whether it may load it.
     *
     * @since 1.0.0
     */
    public val absoluteSystemId: String?
        get() {
            val reference = referenceOf(systemId) ?: return null
            if (reference.isAbsolute) return reference.toString()
            val base = baseUri?.let(::referenceOf)
            return base?.resolve(reference)?.toString()
        }

    private val values: List<Any?> get() = listOf(kind, name, publicId, systemId, baseUri)

    /**
     * Compares every property.
     *
     * @param other the value to compare with.
     * @return whether [other] is a request with equal properties.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is XmlResourceRequest && values == other.values

    /**
     * Returns a hash code derived from every property.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns the kind and the escaped identifiers, such as
     * `XmlResourceRequest(ExternalEntity, name="chapter", public=none, system="ch1.xml", base=none)`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String {
        val shownName = name?.let(SafeText::quoted) ?: NONE
        val shownPublic = publicId?.let(SafeText::quoted) ?: NONE
        val shownBase = baseUri?.let(SafeText::quoted) ?: NONE
        return "XmlResourceRequest($kind, name=$shownName, public=$shownPublic, " +
            "system=${SafeText.quoted(systemId)}, base=$shownBase)"
    }
}
