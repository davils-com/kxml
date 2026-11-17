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

import com.davils.kore.core.validation.violation.ViolationCode

/**
 * The codes of the violations an invalid configuration of kxml-serialization reports.
 *
 * @since 1.0.0
 */
public object XmlFormatViolationCodes {
    /**
     * The default namespace is empty; leave it out to write names in no namespace.
     *
     * @since 1.0.0
     */
    public val EMPTY_NAMESPACE: ViolationCode = ViolationCode("kxml.format.empty-namespace")
}
