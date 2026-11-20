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
package com.davils.kxml.document

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations the builders of kxml-document report, such as the one of
 * [com.davils.kxml.document.redact.xmlRedaction].
 *
 * @since 1.0.0
 */
public object XmlDocumentViolationCodes {
    /**
     * A configured XPath expression is not valid, gives no node-set or exceeds its limits.
     *
     * @since 1.0.0
     */
    public val INVALID_PATH: ViolationCode = ViolationCode("kxml.document.invalid-path")

    /**
     * A configured replacement holds what a text, an attribute, a comment or a processing instruction cannot carry.
     *
     * @since 1.0.0
     */
    public val INVALID_REPLACEMENT: ViolationCode = ViolationCode("kxml.document.invalid-replacement")

    /**
     * A redaction holds more rules than [com.davils.kxml.core.limit.XmlLimits.maximumRedactionRules] allows.
     *
     * @since 1.0.0
     */
    public val TOO_MANY_RULES: ViolationCode = ViolationCode("kxml.document.too-many-rules")
}
