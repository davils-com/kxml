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
import com.davils.kore.core.validation.violation.Severity
import com.davils.kxml.core.diagnostic.XmlDiagnostic
import com.davils.kxml.core.internal.immutableList

/**
 * The events [Xml.events] read and the diagnostics it raised.
 *
 * An instance is immutable.
 *
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class XmlEventResult internal constructor(events: List<XmlEvent>, diagnostics: List<XmlDiagnostic>) {
    /**
     * The events in document order; after an error, the events before it.
     *
     * @since 1.0.0
     */
    public val events: List<XmlEvent> = events.immutableList()

    /**
     * The warnings and at most one error, the one that stopped the reader.
     *
     * @since 1.0.0
     */
    public val diagnostics: List<XmlDiagnostic> = diagnostics.immutableList()

    /**
     * Whether no diagnostic is an error, so that [events] describe the whole document.
     *
     * @since 1.0.0
     */
    public val isValid: Boolean get() = diagnostics.none { diagnostic -> diagnostic.severity == Severity.Error }

    /**
     * Compares the events and the diagnostics.
     *
     * @param other the value to compare with.
     * @return whether [other] is a result with equal events and diagnostics.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is XmlEventResult && events == other.events && diagnostics == other.diagnostics

    /**
     * Returns a hash code derived from the events and the diagnostics.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = listOf(events, diagnostics).hashCode()

    /**
     * Returns the number of events and the diagnostics, such as `XmlEventResult(3 events, [])`.
     *
     * @return a text for diagnostics.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlEventResult(${events.size} events, $diagnostics)"
}
