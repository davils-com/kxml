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
import com.davils.kore.core.collection.immutableList

/**
 * A whole document: the root element, the declaration and the comments and processing instructions around the root.
 *
 * Two documents are equal when their roots, declarations, prologs and epilogs are equal. An instance is immutable.
 *
 * @property root the root element.
 * @property declaration the XML declaration, or `null` for none.
 * @param prolog the comments and processing instructions before the root element.
 * @param epilog the comments and processing instructions after the root element.
 * @throws IllegalArgumentException if [prolog] or [epilog] holds anything but comments and processing instructions.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class XmlDocument(
    public val root: XmlNode.Element,
    public val declaration: XmlDeclaration? = null,
    prolog: List<XmlNode> = emptyList(),
    epilog: List<XmlNode> = emptyList(),
) {
    /**
     * The comments and processing instructions before the root element.
     *
     * @since 1.0.0
     */
    public val prolog: List<XmlNode> = prolog.immutableList()

    /**
     * The comments and processing instructions after the root element.
     *
     * @since 1.0.0
     */
    public val epilog: List<XmlNode> = epilog.immutableList()

    init {
        val isMiscellaneousOnly = this.prolog.all(::isMiscellaneous) && this.epilog.all(::isMiscellaneous)
        require(isMiscellaneousOnly) {
            "Only comments and processing instructions may stand before or after the root element"
        }
    }

    private val values: List<Any?> get() = listOf(root, declaration, prolog, epilog)

    /**
     * Compares the root, the declaration, the prolog and the epilog.
     *
     * @param other the value to compare with.
     * @return whether [other] is an equal document.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is XmlDocument && values == other.values

    /**
     * Returns a hash code derived from the root, the declaration, the prolog and the epilog.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns the root and the declaration, such as `XmlDocument(Element(a, 0 children), no declaration)`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlDocument($root, ${declaration ?: "no declaration"})"
}

private fun isMiscellaneous(node: XmlNode): Boolean = node is XmlNode.Comment || node is XmlNode.ProcessingInstruction
