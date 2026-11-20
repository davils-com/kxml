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
import com.davils.kore.core.text.SafeText

private const val HASH_FACTOR: Int = 31

/**
 * A namespace-aware XML name: a local name, the namespace it belongs to and the prefix it was written with.
 *
 * Two names are equal when their [local] names and [namespace]s are equal; the [prefix] is only how a name is spelled
 * and takes no part in equality. An instance is immutable.
 *
 * @property local the local name; an XML name without a colon.
 * @property namespace the namespace name, or `null` for a name in no namespace; never empty.
 * @property prefix the prefix the name is written with, or `null` for none; needs a [namespace].
 * @throws IllegalArgumentException if [local] or [prefix] is not a name without a colon, if [prefix] is set without
 *   a [namespace], if [namespace] is empty, if [prefix] is `xmlns`, or if [prefix] is `xml` with another namespace than
 *   [XmlNamespaces.XML].
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class XmlName(
    public val local: String,
    public val namespace: String? = null,
    public val prefix: String? = null,
) {
    init {
        require(XmlCharacters.isNcName(local)) {
            "A local XML name must be a name without a colon, but was ${SafeText.quoted(local)}"
        }
        prefix?.let { declared ->
            require(XmlCharacters.isNcName(declared)) {
                "An XML prefix must be a name, but was ${SafeText.quoted(declared)}"
            }
            requireNotNull(namespace) { "The prefix $declared of $local needs a namespace" }
        }
        require(namespace == null || namespace.isNotEmpty()) { "The namespace of $local cannot be empty, leave it out" }
        require(prefix != XmlNamespaces.XMLNS_PREFIX) { "The prefix xmlns is reserved for declarations" }
        require(prefix != XmlNamespaces.XML_PREFIX || namespace == XmlNamespaces.XML) {
            "The prefix xml is bound to ${XmlNamespaces.XML}"
        }
    }

    /**
     * The name as written in a document, `prefix:local` or just `local`.
     *
     * @since 1.0.0
     */
    public val qualified: String
        get() {
            val declared = prefix ?: return local
            return "$declared:$local"
        }

    /**
     * Compares the local name and the namespace.
     *
     * @param other the value to compare with.
     * @return whether [other] is a name with the same local name and namespace, whatever its prefix.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        val isSame = other is XmlName && local == other.local && namespace == other.namespace
        return this === other || isSame
    }

    /**
     * Returns a hash code derived from the local name and the namespace.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * local.hashCode() + namespace.hashCode()

    /**
     * Returns the name in Clark notation, `{namespace}local`, or just `local`; the namespace is escaped.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String {
        val uri = namespace ?: return local
        return "{${SafeText.text(uri)}}$local"
    }
}
