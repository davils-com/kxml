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

package com.davils.kxml.serialization

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kxml.core.diagnostic.XmlDiagnostic

/**
 * The outcome of [XmlFormat.decodeOrFailure]: a value or the diagnostics that explain why there is none.
 *
 * @param T the type of the decoded value.
 * @since 1.0.0
 */
public sealed interface XmlDecoding<out T> {
    /**
     * The diagnostics of the reader and the decoder: warnings for [Decoded], warnings and errors for [Failed].
     *
     * @since 1.0.0
     */
    public val diagnostics: List<XmlDiagnostic>

    /**
     * The document was decoded.
     *
     * @param T the type of the decoded value.
     * @property value the decoded value.
     * @param diagnostics the warnings of the reader and the decoder.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    public class Decoded<out T> internal constructor(
        public val value: T,
        diagnostics: List<XmlDiagnostic>,
    ) : XmlDecoding<T> {
        override val diagnostics: List<XmlDiagnostic> = diagnostics.immutableList()

        override fun equals(other: Any?): Boolean =
            other is Decoded<*> && value == other.value && diagnostics == other.diagnostics

        override fun hashCode(): Int = listOf(value, diagnostics).hashCode()

        override fun toString(): String = "Decoded($diagnostics)"
    }

    /**
     * The document could not be read or did not fit the type.
     *
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    public class Failed internal constructor(diagnostics: List<XmlDiagnostic>) : XmlDecoding<Nothing> {
        override val diagnostics: List<XmlDiagnostic> = diagnostics.immutableList()

        override fun equals(other: Any?): Boolean = other is Failed && diagnostics == other.diagnostics

        override fun hashCode(): Int = diagnostics.hashCode()

        override fun toString(): String = "Failed($diagnostics)"
    }
}
