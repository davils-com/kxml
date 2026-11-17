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

package com.davils.kxml.core.parser

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.limit.XmlLimits

internal class ParseBudget(private val limits: XmlLimits) {
    private var elements: Long = 0

    fun enter(depth: Int, range: TextRange) {
        elements++
        if (depth > limits.maximumDepth) {
            failure(
                XmlDiagnosticCode.TooDeep,
                range,
                "The document nests deeper than ${limits.maximumDepth} elements",
            )
        }
        if (elements > limits.maximumElements) {
            failure(
                XmlDiagnosticCode.TooManyElements,
                range,
                "The document has more than ${limits.maximumElements} elements",
            )
        }
    }

    fun attributes(count: Int, range: TextRange) {
        if (count <= limits.maximumAttributes) return
        failure(
            XmlDiagnosticCode.TooManyAttributes,
            range,
            "An element carries more than ${limits.maximumAttributes} attributes",
        )
    }

    fun text(length: Int, range: TextRange) {
        if (length <= limits.maximumTextLength) return
        failure(
            XmlDiagnosticCode.TextTooLong,
            range,
            "A text is longer than ${limits.maximumTextLength} characters",
        )
    }

    fun name(length: Int, range: TextRange) {
        if (length <= limits.maximumNameLength) return
        failure(
            XmlDiagnosticCode.NameTooLong,
            range,
            "A name is longer than ${limits.maximumNameLength} characters",
        )
    }
}
