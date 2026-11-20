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
import com.davils.kore.core.text.SafeText
import com.davils.kxml.core.node.XmlDocument

/**
 * One layer of a merge: a document and the name that marks the nodes it contributes.
 *
 * @property name the name of the layer, such as `defaults` or `production`, which the origins of a merge name.
 * @param document the document of the layer.
 * @since 1.0.0
 */
public class XmlLayer(public val name: String, document: XmlDocument) {
    /**
     * The document of the layer.
     *
     * @since 1.0.0
     */
    public val document: XmlDocument = document

    /**
     * Returns whether [other] is a layer with the same name and an equal document.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an equal layer.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is XmlLayer && name == other.name && document == other.document

    /**
     * Returns a hash code of the name and the document.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * name.hashCode() + document.hashCode()

    /**
     * Returns the name escaped, such as `XmlLayer("defaults")`, without the document.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlLayer(${SafeText.quoted(name)})"

    private companion object {
        const val HASH_FACTOR: Int = 31
    }
}
