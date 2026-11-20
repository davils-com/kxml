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

package com.davils.kxml.document.merge

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.collection.immutableMap
import com.davils.kxml.core.name.XmlName

/**
 * How a merge matches the elements of a layer with those below it.
 *
 * An element of a layer matches an element below it with the same expanded name and parent when [keys] names key
 * attributes for that name and both carry them with equal values. Without key attributes, it matches when the name
 * occurs once among the children of both parents. An element that matches nothing is appended after the last element
 * of its parent, and one that carries [deletionMarker] with the value `true` removes the element it matches instead.
 *
 * @param keys the key attributes of elements, by the expanded name of the element.
 * @property deletionMarker the attribute that marks an element of a layer as a deletion, or `null` for none.
 * @throws IllegalArgumentException if [keys] names an element without key attributes.
 * @since 1.0.0
 */
public class XmlMergeOptions(
    keys: Map<XmlName, List<XmlName>> = emptyMap(),
    public val deletionMarker: XmlName? = null,
) {
    /**
     * The key attributes of elements, by the expanded name of the element, which cannot be changed.
     *
     * @since 1.0.0
     */
    public val keys: Map<XmlName, List<XmlName>> = immutableKeys(keys)

    init {
        require(keys.values.none(List<XmlName>::isEmpty)) { "Every keyed element needs at least one key attribute" }
    }

    /**
     * Returns whether [other] is options with equal keys and deletion marker.
     *
     * @param other the object to compare with.
     * @return `true` if [other] matches elements the same way.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is XmlMergeOptions && keys == other.keys && deletionMarker == other.deletionMarker

    /**
     * Returns a hash code of the keys and the deletion marker.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * keys.hashCode() + deletionMarker.hashCode()

    /**
     * Returns the number of keyed elements and the deletion marker, such as `XmlMergeOptions(keys=2, deletion=delete)`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String {
        val deletion = deletionMarker?.qualified ?: NONE
        return "XmlMergeOptions(keys=${keys.size}, deletion=$deletion)"
    }

    private companion object {
        const val HASH_FACTOR: Int = 31
        const val NONE: String = "none"

        fun immutableKeys(keys: Map<XmlName, List<XmlName>>): Map<XmlName, List<XmlName>> {
            val copied = keys.mapValues { (_, attributes) -> attributes.immutableList() }
            return copied.immutableMap()
        }
    }
}
