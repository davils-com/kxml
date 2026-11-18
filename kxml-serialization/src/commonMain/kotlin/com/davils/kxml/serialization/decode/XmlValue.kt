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

package com.davils.kxml.serialization.decode

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.node.XmlNode

internal sealed interface XmlValue {
    val range: TextRange?

    data class Element(val element: XmlNode.Element) : XmlValue {
        override val range: TextRange? get() = element.range
    }

    data class Text(val text: String, override val range: TextRange?) : XmlValue

    data class Items(val elements: List<XmlNode.Element>, override val range: TextRange?) : XmlValue

    data class Entries(
        val elements: List<XmlNode.Element>,
        val key: XmlName,
        val value: XmlName,
        override val range: TextRange?,
    ) : XmlValue

    data class Absent(override val range: TextRange?) : XmlValue
}
