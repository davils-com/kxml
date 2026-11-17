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

/**
 * How a polymorphic value names its concrete type.
 *
 * @since 1.0.0
 */
public enum class XmlPolymorphism {
    /**
     * An attribute named [XmlFormatConfiguration.classDiscriminator] on the element of the value holds the serial name
     * of the type; only classes can carry it.
     *
     * @since 1.0.0
     */
    Attribute,

    /**
     * The value is wrapped in an element named after the serial name of its type.
     *
     * @since 1.0.0
     */
    Element,
}
