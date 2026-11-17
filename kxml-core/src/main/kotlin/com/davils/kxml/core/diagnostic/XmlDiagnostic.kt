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

package com.davils.kxml.core.diagnostic

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.TextRange
import com.davils.kore.core.validation.violation.Severity
import com.davils.kxml.core.internal.SafeDisplay

/**
 * A problem the reader or a decoder found in a document.
 *
 * The [message] may quote parts of the document, so it is input: show it through [toString], which escapes and
 * shortens it, or escape it yourself. An instance is immutable.
 *
 * @property code the stable code of the problem.
 * @property range the characters of the source text the problem is about.
 * @property message a description in English; may quote the document.
 * @property severity whether the problem makes the document unusable; the severity of [code] by default.
 * @since 1.0.0
 */
public class XmlDiagnostic(
    public val code: XmlDiagnosticCode,
    public val range: TextRange,
    public val message: String,
    public val severity: Severity = code.severity,
) {
    private val values: List<Any> get() = listOf(code, range, message, severity)

    /**
     * Compares every property.
     *
     * @param other the value to compare with.
     * @return whether [other] is a diagnostic with equal properties.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is XmlDiagnostic && values == other.values

    /**
     * Returns a hash code derived from every property.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns the severity, the code, the range and the escaped message, such as
     * `Error kxml.syntax.mismatched-tag at 4..10: "..."`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String = "$severity $code at $range: ${SafeDisplay.quoted(message)}"
}
