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

import com.davils.kxml.core.name.XmlName

internal const val SCHEMA_INSTANCE: String = "http://www.w3.org/2001/XMLSchema-instance"

internal object SchemaInstance {
    val nil: XmlName = XmlName(local = "nil", namespace = SCHEMA_INSTANCE, prefix = "xsi")

    val type: XmlName = XmlName(local = "type", namespace = SCHEMA_INSTANCE, prefix = "xsi")

    private val trueValues: Set<String> = setOf("true", "1")

    fun isNil(value: String?): Boolean = value?.trim() in trueValues
}
