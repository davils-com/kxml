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

package com.davils.kxml.core.dtd

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.parser.failure

internal class ExpansionBudget(private val limits: XmlLimits) {
    private var expanded: Long = 0L

    fun charge(length: Int, depth: Int, range: TextRange) {
        expanded += maxOf(a = length.toLong(), b = 1L)
        if (expanded > limits.maximumEntityExpansion) {
            failure(
                XmlDiagnosticCode.EntityExpansionTooLarge,
                range,
                "The entities of the document expand to more than ${limits.maximumEntityExpansion} characters",
            )
        }
        if (depth >= limits.maximumEntityDepth) {
            failure(
                XmlDiagnosticCode.EntityTooDeep,
                range,
                "The entities of the document nest deeper than ${limits.maximumEntityDepth} levels",
            )
        }
    }
}
