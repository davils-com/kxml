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
 * Puts the element of a class, or the element or attribute of a property, into a namespace.
 *
 * On a class it applies to the element of the class and to the elements of its properties that have no namespace of
 * their own; it replaces
 * [XmlFormatConfiguration.namespace][com.davils.kxml.serialization.XmlFormatConfiguration.namespace].
 *
 * @property uri the namespace name; must not be empty.
 * @property prefix the prefix the writer uses, or empty for the default namespace.
 * @since 1.0.0
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
public annotation class XmlNamespace(val uri: String, val prefix: String = "")
