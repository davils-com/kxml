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

package com.davils.kxml.core

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding
import com.davils.kxml.core.node.XmlAttribute
import com.davils.kxml.core.node.XmlDeclaration

/**
 * One step of a document as the reader meets it; [Xml.events] returns them in document order.
 *
 * Every event is immutable and compares equal to another of the same kind with equal values and an equal [range].
 * A `toString` escapes and shortens the text it shows.
 *
 * @since 1.0.0
 */
public sealed interface XmlEvent {
    /**
     * The characters of the source text the event was read from.
     *
     * @since 1.0.0
     */
    public val range: TextRange

    /**
     * The XML declaration, `<?xml ...?>`.
     *
     * @property declaration the version, encoding and standalone flag.
     * @property range the characters of the declaration.
     * @since 1.0.0
     */
    public class Declaration internal constructor(
        public val declaration: XmlDeclaration,
        override val range: TextRange,
    ) : XmlEvent {
        private val values: List<Any> get() = listOf(declaration, range)

        override fun equals(other: Any?): Boolean = other is Declaration && values == other.values

        override fun hashCode(): Int = values.hashCode()

        override fun toString(): String = "Declaration($declaration)"
    }

    /**
     * A start tag or an empty-element tag.
     *
     * @property name the resolved name of the element.
     * @param attributes the attributes in the order of the source, without namespace declarations.
     * @param namespaces the namespace declarations of the tag.
     * @property isEmpty whether the tag closes itself, `<a/>`; an [EndElement] follows it anyway.
     * @property range the characters of the tag.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    public class StartElement internal constructor(
        public val name: XmlName,
        attributes: List<XmlAttribute>,
        namespaces: List<XmlNamespaceBinding>,
        public val isEmpty: Boolean,
        override val range: TextRange,
    ) : XmlEvent {
        /**
         * The attributes in the order of the source, without namespace declarations.
         *
         * @since 1.0.0
         */
        public val attributes: List<XmlAttribute> = attributes.immutableList()

        /**
         * The namespace declarations of the tag.
         *
         * @since 1.0.0
         */
        public val namespaces: List<XmlNamespaceBinding> = namespaces.immutableList()

        private val values: List<Any> get() = listOf(name, attributes, namespaces, isEmpty, range)

        override fun equals(other: Any?): Boolean = other is StartElement && values == other.values

        override fun hashCode(): Int = values.hashCode()

        override fun toString(): String = "StartElement($name)"
    }

    /**
     * An end tag, or the end of an empty-element tag.
     *
     * @property name the resolved name of the element.
     * @property range the characters of the end tag; empty at the end of an empty-element tag.
     * @since 1.0.0
     */
    public class EndElement internal constructor(public val name: XmlName, override val range: TextRange) : XmlEvent {
        private val values: List<Any> get() = listOf(name, range)

        override fun equals(other: Any?): Boolean = other is EndElement && values == other.values

        override fun hashCode(): Int = values.hashCode()

        override fun toString(): String = "EndElement($name)"
    }

    /**
     * Character data with every reference replaced and every line break normalized to a line feed.
     *
     * @property value the text.
     * @property range the characters it was read from.
     * @since 1.0.0
     */
    public class Text internal constructor(public val value: String, override val range: TextRange) : XmlEvent {
        private val values: List<Any> get() = listOf(value, range)

        override fun equals(other: Any?): Boolean = other is Text && values == other.values

        override fun hashCode(): Int = values.hashCode()

        @OptIn(DavilsInternal::class)
        override fun toString(): String = "Text(${SafeText.quoted(value)})"
    }

    /**
     * A CDATA section, `<![CDATA[...]]>`.
     *
     * @property value the content of the section.
     * @property range the characters it was read from.
     * @since 1.0.0
     */
    public class CData internal constructor(public val value: String, override val range: TextRange) : XmlEvent {
        private val values: List<Any> get() = listOf(value, range)

        override fun equals(other: Any?): Boolean = other is CData && values == other.values

        override fun hashCode(): Int = values.hashCode()

        @OptIn(DavilsInternal::class)
        override fun toString(): String = "CData(${SafeText.quoted(value)})"
    }

    /**
     * A comment, `<!--...-->`.
     *
     * @property value the content of the comment.
     * @property range the characters it was read from.
     * @since 1.0.0
     */
    public class Comment internal constructor(public val value: String, override val range: TextRange) : XmlEvent {
        private val values: List<Any> get() = listOf(value, range)

        override fun equals(other: Any?): Boolean = other is Comment && values == other.values

        override fun hashCode(): Int = values.hashCode()

        @OptIn(DavilsInternal::class)
        override fun toString(): String = "Comment(${SafeText.quoted(value)})"
    }

    /**
     * A processing instruction, `<?target data?>`.
     *
     * @property target the name the instruction is addressed to.
     * @property data everything after the target and its whitespace; empty if there is nothing.
     * @property range the characters of the instruction.
     * @since 1.0.0
     */
    public class ProcessingInstruction internal constructor(
        public val target: String,
        public val data: String,
        override val range: TextRange,
    ) : XmlEvent {
        private val values: List<Any> get() = listOf(target, data, range)

        override fun equals(other: Any?): Boolean = other is ProcessingInstruction && values == other.values

        override fun hashCode(): Int = values.hashCode()

        @OptIn(DavilsInternal::class)
        override fun toString(): String = "ProcessingInstruction(${SafeText.text(target)})"
    }
}
