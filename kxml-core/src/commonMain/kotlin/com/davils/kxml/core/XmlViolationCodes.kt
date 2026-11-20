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

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations an invalid configuration of kxml-core reports.
 *
 * @since 1.0.0
 */
public object XmlViolationCodes {
    /**
     * A limit is zero or negative.
     *
     * @since 1.0.0
     */
    public val NOT_POSITIVE: ViolationCode = ViolationCode("kxml.config.not-positive")

    /**
     * A limit is above its hard ceiling, a `HARD_*` constant of [com.davils.kxml.core.limit.XmlLimits], such as
     * [XmlLimits.HARD_DEPTH][com.davils.kxml.core.limit.XmlLimits.HARD_DEPTH].
     *
     * @since 1.0.0
     */
    public val ABOVE_HARD_LIMIT: ViolationCode = ViolationCode("kxml.config.above-hard-limit")

    /**
     * The indentation of the writer is outside its range.
     *
     * @since 1.0.0
     */
    public val INVALID_INDENT: ViolationCode = ViolationCode("kxml.config.invalid-indent")
}
