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

package com.davils.kxml.document.redact

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.TextEdit
import com.davils.kxml.core.syntax.XmlSyntaxTree

/**
 * The outcome of redacting a syntax tree in place.
 *
 * @since 1.0.0
 */
public sealed interface XmlRedactionResult {
    /**
     * The tree was redacted.
     *
     * @param edits the edits of the text, in the order of their ranges.
     * @param tree the redacted tree.
     * @since 1.0.0
     */
    public class Redacted internal constructor(edits: List<TextEdit>, tree: XmlSyntaxTree) : XmlRedactionResult {
        /**
         * The edits of the text, in the order of their ranges, which cannot be changed.
         *
         * @since 1.0.0
         */
        public val edits: List<TextEdit> = edits.immutableList()

        /**
         * The redacted tree.
         *
         * @since 1.0.0
         */
        public val tree: XmlSyntaxTree = tree

        /**
         * Returns the number of edits, such as `Redacted(edits=2)`, without the text.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        override fun toString(): String = "Redacted(edits=${edits.size})"
    }

    /**
     * The tree was not redacted.
     *
     * @param reason why the redaction refused.
     * @since 1.0.0
     */
    public class Refused internal constructor(reason: XmlRedactionRefusal) : XmlRedactionResult {
        /**
         * Why the redaction refused.
         *
         * @since 1.0.0
         */
        public val reason: XmlRedactionRefusal = reason

        /**
         * Returns the reason, such as `Refused(Undecidable)`.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        override fun toString(): String = "Refused($reason)"
    }
}
