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

import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.node.XmlAttribute
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.serialization.SCHEMA_INSTANCE
import com.davils.kxml.serialization.shape.Placement
import com.davils.kxml.serialization.shape.PropertyShape
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder

internal class ClassDecoder(
    context: DecodeContext,
    descriptor: SerialDescriptor,
    value: XmlValue,
) : StructureDecoder(context) {
    private val element: XmlNode.Element = elementOf(value)

    private val shapes: List<PropertyShape> = context.shapes.of(descriptor)

    private val values: List<XmlValue?> = valuesOf(shapes)

    private val order: ArrayDeque<Int> = ArrayDeque(shapes.filter(::isDecoded).map(PropertyShape::index))

    init {
        if (!context.configuration.isUnknownElementIgnored) checkUnknown()
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int =
        order.removeFirstOrNull() ?: CompositeDecoder.DECODE_DONE

    override fun valueAt(index: Int): XmlValue = values.getOrNull(index) ?: XmlValue.Absent(element.range)

    private fun elementOf(value: XmlValue): XmlNode.Element {
        val holder = value as? XmlValue.Element ?: context.mismatch(value.range, "an element")
        return holder.element
    }

    private fun valuesOf(shapes: List<PropertyShape>): List<XmlValue?> {
        val slots = shapes.maxOfOrNull(PropertyShape::index)?.plus(1) ?: 0
        val found = MutableList<XmlValue?>(slots) { null }
        shapes.forEach { shape -> found[shape.index] = valueOf(shape) }
        return found
    }

    private fun valueOf(shape: PropertyShape): XmlValue? = when (shape.placement) {
        Placement.Attribute -> attributeOf(shape)?.let { XmlValue.Text(it.value, it.range) }
        Placement.Text -> XmlValue.Text(element.text, element.range)
        Placement.Element -> collectionOf(shape) ?: firstChild(shape)?.let(XmlValue::Element)
    }

    private fun collectionOf(shape: PropertyShape): XmlValue? {
        val elements = shape.takeIf { it.isCollection }?.let(::itemsOf) ?: return null
        val entries = XmlValue.Entries(
            elements = elements,
            key = shape.entries.key,
            value = shape.entries.value,
            range = element.range,
        )
        return entries.takeIf { shape.kind == StructureKind.MAP } ?: XmlValue.Items(elements, element.range)
    }

    private fun itemsOf(shape: PropertyShape): List<XmlNode.Element>? {
        val item = shape.item ?: return repeatedOf(shape)
        return firstChild(shape)?.let { wrapper -> itemsIn(wrapper, item) }
    }

    private fun repeatedOf(shape: PropertyShape): List<XmlNode.Element>? {
        val repeated = childrenOf(shape)
        return repeated.takeUnless { repeated.isEmpty() && shape.isOptional }
    }

    private fun itemsIn(wrapper: XmlNode.Element, item: XmlName): List<XmlNode.Element> =
        wrapper.elements.filter { child -> context.matches(actual = child.name, expected = item) }

    private fun attributeOf(shape: PropertyShape): XmlAttribute? = element.attributes.firstOrNull { attribute ->
        context.matches(actual = attribute.name, expected = shape.name, aliases = shape.aliases)
    }

    private fun firstChild(shape: PropertyShape): XmlNode.Element? =
        element.elements.firstOrNull { child -> isNamed(child, shape) }

    private fun childrenOf(shape: PropertyShape): List<XmlNode.Element> =
        element.elements.filter { child -> isNamed(child, shape) }

    private fun isNamed(child: XmlNode.Element, shape: PropertyShape): Boolean =
        context.matches(actual = child.name, expected = shape.name, aliases = shape.aliases)

    private fun isDecoded(shape: PropertyShape): Boolean {
        val isFound = values.getOrNull(shape.index) != null
        return isFound || shape.isImplicitlyNull
    }

    private fun checkUnknown() {
        unknownElements().forEach { child ->
            context.report(XmlDiagnosticCode.UnknownProperty, child.range, "There is no <${child.name}>")
        }
        unknownAttributes().forEach { attribute ->
            context.report(XmlDiagnosticCode.UnknownProperty, attribute.range, "There is no ${attribute.name}")
        }
        if (isTextUnexpected()) {
            context.report(XmlDiagnosticCode.UnknownProperty, element.range, "There is no place for text")
        }
    }

    private fun unknownElements(): List<XmlNode.Element> {
        val known = shapes.filter { shape -> shape.placement == Placement.Element }
        return element.elements.filterNot { child -> known.any { shape -> isNamed(child, shape) } }
    }

    private fun unknownAttributes(): List<XmlAttribute> {
        val known = shapes.filter { shape -> shape.placement == Placement.Attribute }
        val discriminator = context.configuration.classDiscriminator
        return element.attributes.filterNot { attribute ->
            val isDiscriminator = context.matches(actual = attribute.name, expected = discriminator)
            val isSchemaInstance = attribute.name.namespace == SCHEMA_INSTANCE
            val isKnown = known.any { shape ->
                context.matches(actual = attribute.name, expected = shape.name, aliases = shape.aliases)
            }
            isDiscriminator || isKnown || isSchemaInstance
        }
    }

    private fun isTextUnexpected(): Boolean {
        val hasText = shapes.any { shape -> shape.placement == Placement.Text }
        return !hasText && element.text.isNotBlank()
    }
}
