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

package com.davils.kxml.serialization.annotation

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Names the key and value elements of the entries of a map property.
 *
 * Each entry is written as an element named after the property that holds a key element and a value element.
 *
 * @property key the local name of the key element; [DEFAULT_KEY] by default.
 * @property value the local name of the value element; [DEFAULT_VALUE] by default.
 * @since 1.0.0
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class XmlEntry(val key: String = DEFAULT_KEY, val value: String = DEFAULT_VALUE) {
    /**
     * Holds the default names.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * `Key`, the name of the key element when a map has no [XmlEntry].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_KEY: String = "Key"

        /**
         * `Value`, the name of the value element when a map has no [XmlEntry].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_VALUE: String = "Value"
    }
}
