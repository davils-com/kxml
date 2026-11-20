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
import com.davils.kxml.core.diagnostic.XmlDiagnostic
import com.davils.kxml.core.node.XmlDocument

/**
 * The outcome of [Xml.parse]: a document or the reason there is none.
 *
 * @since 1.0.0
 */
public sealed interface XmlParseResult {
    /**
     * The diagnostics of the reader: warnings for [Parsed], warnings and one error for [Failed].
     *
     * @since 1.0.0
     */
    public val diagnostics: List<XmlDiagnostic>

    /**
     * The document was well-formed.
     *
     * @property document the document.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    public class Parsed internal constructor(
        public val document: XmlDocument,
        diagnostics: List<XmlDiagnostic>,
    ) : XmlParseResult {
        override val diagnostics: List<XmlDiagnostic> = diagnostics.immutableList()

        override fun equals(other: Any?): Boolean =
            other is Parsed && document == other.document && diagnostics == other.diagnostics

        override fun hashCode(): Int = listOf(document, diagnostics).hashCode()

        override fun toString(): String = "Parsed(${document.root}, $diagnostics)"
    }

    /**
     * The document was not well-formed or broke a limit.
     *
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    public class Failed internal constructor(diagnostics: List<XmlDiagnostic>) : XmlParseResult {
        override val diagnostics: List<XmlDiagnostic> = diagnostics.immutableList()

        override fun equals(other: Any?): Boolean = other is Failed && diagnostics == other.diagnostics

        override fun hashCode(): Int = diagnostics.hashCode()

        override fun toString(): String = "Failed($diagnostics)"
    }
}
