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
import com.davils.kore.core.validation.violation.Severity
import com.davils.kxml.core.diagnostic.XmlDiagnostic
import com.davils.kxml.core.parser.ParseFailure
import com.davils.kxml.core.parser.XmlScanner

/**
 * Reads a document event by event, without building its tree, from [Xml.reader].
 *
 * Each call of [next] reads as much of the source as the next event needs and checks it against the
 * [XmlLimits][com.davils.kxml.core.limit.XmlLimits] of the [Xml] that created the reader, so memory grows with the
 * depth of the document rather than with its length. The first error ends reading: [next] returns `null` from then
 * on and [diagnostics] holds the error, so a caller never sees an event of a document that is not well formed after
 * the error. Malformed input never makes a reader throw.
 *
 * A reader is not thread safe: one thread reads it from start to end.
 *
 * @since 1.0.0
 */
public class XmlReader internal constructor(
    private val scanner: XmlScanner?,
    private val decoding: List<XmlDiagnostic>,
) {
    private var failure: XmlDiagnostic? = null
    private var isDone: Boolean = scanner == null

    /**
     * The warnings found so far and the error that ended reading, if there is one, in the order they were found.
     *
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    public val diagnostics: List<XmlDiagnostic>
        get() {
            val found = decoding + scanner?.warnings.orEmpty() + listOfNotNull(failure)
            return found.immutableList()
        }

    /**
     * Whether no error was found so far; once [next] returned `null`, whether the whole document is well formed.
     *
     * @since 1.0.0
     */
    public val isValid: Boolean get() = diagnostics.none { diagnostic -> diagnostic.severity == Severity.Error }

    /**
     * Reads the next event.
     *
     * @return the next event, or `null` at the end of the document and after an error.
     * @since 1.0.0
     */
    public fun next(): XmlEvent? {
        val active = scanner?.takeUnless { isDone } ?: return null
        val event = try {
            active.next()
        } catch (refused: ParseFailure) {
            failure = refused.diagnostic
            null
        }
        isDone = event == null
        return event
    }

    /**
     * Returns the number of diagnostics and whether reading has ended, such as `XmlReader(reading, 0 diagnostics)`.
     *
     * @return a text for diagnostics.
     * @since 1.0.0
     */
    override fun toString(): String {
        val state = "done".takeIf { isDone } ?: "reading"
        return "XmlReader($state, ${diagnostics.size} diagnostics)"
    }
}
