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

package com.davils.kxml.serialization.encode

import com.davils.kxml.core.node.XmlAttribute
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.node.xmlText
import com.davils.kxml.serialization.shape.Placement
import com.davils.kxml.serialization.shape.PropertyShape
import kotlinx.serialization.SerializationException

internal class ClassCollector(private val shapes: List<PropertyShape>) : ContentCollector {
    private val attributes = mutableListOf<XmlAttribute>()

    private val children = mutableListOf<XmlNode>()

    override fun add(index: Int, content: Content) {
        val shape = shapes[index]
        when (shape.placement) {
            Placement.Attribute -> singleOf(shape, content)?.let { attributes += XmlAttribute(shape.name, it) }
            Placement.Text -> singleOf(shape, content)?.let { children += xmlText(it) }
            Placement.Element -> children += ElementWriting.elementsOf(shape, content)
        }
    }

    override fun build(): Content = Content.Structure(attributes.toList(), children.toList())

    private fun singleOf(shape: PropertyShape, content: Content): String? {
        if (content == Content.Absent || content == Content.Nil) return null
        val text = content as? Content.Text
            ?: throw SerializationException("The property ${shape.name} must be a single value to stand there")
        return text.value
    }
}
