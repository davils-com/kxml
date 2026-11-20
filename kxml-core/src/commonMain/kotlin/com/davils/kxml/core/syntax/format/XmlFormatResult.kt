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

package com.davils.kxml.core.syntax.format

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.TextEdit
import com.davils.kxml.core.syntax.XmlSyntaxTree

/**
 * The result of an [XmlFormatter]: the formatted tree or the reason it refused.
 *
 * @since 1.0.0
 */
public sealed interface XmlFormatResult {
    /**
     * The tree was formatted; a tree that was formatted already gives no edits and an equal text.
     *
     * @property tree the tree of the formatted text, parsed with the configuration of the original tree.
     * @since 1.0.0
     */
    public class Formatted internal constructor(edits: List<TextEdit>, public val tree: XmlSyntaxTree) :
        XmlFormatResult {
        /**
         * The text edits that turn the original text into the text of [tree], in the order of their ranges; each
         * replaces whitespace only.
         *
         * @since 1.0.0
         */
        public val edits: List<TextEdit> = edits.immutableList()

        /**
         * Returns how many text edits were made, such as `Formatted(3 edits)`.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        override fun toString(): String = "Formatted(${edits.size} edits)"
    }

    /**
     * The tree was not formatted.
     *
     * @since 1.0.0
     */
    public class Refused internal constructor(reason: XmlFormatRefusal) : XmlFormatResult {
        /**
         * Why the tree was not formatted.
         *
         * @since 1.0.0
         */
        public val reason: XmlFormatRefusal = reason

        /**
         * Compares the reason.
         *
         * @param other the value to compare with.
         * @return whether [other] is a refusal for the same reason.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Refused && reason == other.reason

        /**
         * Returns the hash code of the reason.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = reason.hashCode()

        /**
         * Returns the reason, such as `Refused(MeaningChanged)`.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        override fun toString(): String = "Refused($reason)"
    }
}
