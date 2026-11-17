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

/**
 * Creates an [Xml] with the settings of [configure], starting from the safe defaults of [XmlBuilder].
 *
 * @param configure the changes to the defaults.
 * @return an instance with the validated configuration.
 * @throws com.davils.kore.core.validation.report.ValidationException if a setting is invalid; the exception lists
 *   every violation.
 * @since 1.0.0
 */
public fun xml(configure: XmlBuilder.() -> Unit = {}): Xml {
    val builder = XmlBuilder()
    builder.configure()
    return Xml(builder.build())
}
