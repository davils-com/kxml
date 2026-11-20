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
package com.davils.kxml.document.redact

import com.davils.kore.core.validation.report.ValidationException

/**
 * Builds an [XmlRedaction].
 *
 * @param configure sets what is secret and what replaces it.
 * @return the redaction.
 * @throws ValidationException if a path is not a valid XPath expression of a node-set within the limits, the
 * replacement cannot stand in a text, a comment or a processing instruction, or there are more rules than
 * [com.davils.kxml.core.limit.XmlLimits.maximumRedactionRules]; the report lists every problem at once and never
 * repeats the configured text.
 * @since 1.0.0
 */
public fun xmlRedaction(configure: XmlRedactionBuilder.() -> Unit): XmlRedaction {
    val builder = XmlRedactionBuilder()
    builder.configure()
    return builder.build()
}
