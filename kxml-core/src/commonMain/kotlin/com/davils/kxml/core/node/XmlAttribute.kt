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

package com.davils.kxml.core.node

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaces

/**
 * An attribute of an element.
 *
 * Two attributes are equal when their [name]s and [value]s are equal; the [range] takes no part in equality. An
 * instance is immutable. Use [XmlElementBuilder.attribute] to add one to an element, which also checks the value.
 * A namespace declaration is no attribute: a name `xmlns` without a namespace and every name in the namespace
 * [XmlNamespaces.XMLNS] are refused with an [IllegalArgumentException], so no attribute can declare a namespace when
 * it is written; declare one with [XmlElementBuilder.namespace].
 *
 * @property name the resolved name.
 * @property value the normalized value, with every reference replaced.
 * @property range the characters of the source text the attribute was read from, or `null` for one built in code.
 * @since 1.0.0
 */
public class XmlAttribute(
    public val name: XmlName,
    public val value: String,
    public val range: TextRange? = null,
) {
    private val values: List<Any> get() = listOf(name, value)

    init {
        val isUndeclaredXmlns = name.namespace == null && name.local == XmlNamespaces.XMLNS_PREFIX
        require(!isUndeclaredXmlns && name.namespace != XmlNamespaces.XMLNS) {
            "The attribute $name is a namespace declaration"
        }
    }

    /**
     * Compares the name and the value.
     *
     * @param other the value to compare with.
     * @return whether [other] is an attribute with an equal name and value.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is XmlAttribute && values == other.values

    /**
     * Returns a hash code derived from the name and the value.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns the name and the escaped value, such as `id="a1"`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String = "$name=${SafeText.quoted(value)}"
}
