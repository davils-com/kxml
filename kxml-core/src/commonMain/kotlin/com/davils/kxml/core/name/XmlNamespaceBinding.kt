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

package com.davils.kxml.core.name

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kxml.core.internal.SafeDisplay

/**
 * A namespace declaration, `xmlns="uri"` or `xmlns:prefix="uri"`.
 *
 * An instance is immutable.
 *
 * @property prefix the declared prefix, or `null` for the default namespace.
 * @property uri the namespace name; empty only for a default namespace, where it undeclares the default.
 * @throws IllegalArgumentException if [prefix] is not a name without a colon, or binds a prefix to an empty [uri].
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class XmlNamespaceBinding(public val prefix: String?, public val uri: String) {
    private val values: List<String?> get() = listOf(prefix, uri)

    init {
        prefix?.let { declared ->
            require(XmlCharacters.isNcName(declared)) {
                "A namespace prefix must be a name, but was ${SafeDisplay.quoted(declared)}"
            }
            require(uri.isNotEmpty()) { "The prefix $declared cannot be bound to nothing" }
        }
    }

    /**
     * Compares the prefix and the namespace name.
     *
     * @param other the value to compare with.
     * @return whether [other] declares the same prefix for the same namespace name.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is XmlNamespaceBinding && values == other.values

    /**
     * Returns a hash code derived from the prefix and the namespace name.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns the declaration as written, such as `xmlns:a="urn:a"`, with the namespace name escaped.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String {
        val shown = SafeDisplay.quoted(uri)
        val declared = prefix ?: return "xmlns=$shown"
        return "xmlns:$declared=$shown"
    }
}
