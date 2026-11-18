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

package com.davils.kxml.serialization.shape

import com.davils.kxml.core.name.XmlName
import com.davils.kxml.serialization.XmlFormatConfiguration
import com.davils.kxml.serialization.annotation.XmlAsAttribute
import com.davils.kxml.serialization.annotation.XmlAsText
import com.davils.kxml.serialization.annotation.XmlEntry
import com.davils.kxml.serialization.annotation.XmlNames
import com.davils.kxml.serialization.annotation.XmlNamespace
import com.davils.kxml.serialization.annotation.XmlWrapped
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind

private const val NULLABLE_SUFFIX: String = "?"
private const val PACKAGE_SEPARATOR: Char = '.'

private val binary: SerialDescriptor = ByteArraySerializer().descriptor

private inline fun <reified A : Annotation> List<Annotation>.firstOfType(): A? = filterIsInstance<A>().firstOrNull()

@OptIn(ExperimentalSerializationApi::class)
internal class Shapes(private val configuration: XmlFormatConfiguration) {
    private val cache = HashMap<SerialDescriptor, List<PropertyShape>>()

    private val fallback: Namespace? = configuration.namespace?.let { uri -> Namespace(uri, prefix = "") }

    fun of(descriptor: SerialDescriptor): List<PropertyShape> = cache.getOrPut(descriptor) {
        (0 until descriptor.elementsCount).map { index -> shapeOf(descriptor, index) }
    }

    fun rootName(descriptor: SerialDescriptor): XmlName {
        val serialName = descriptor.serialName.removeSuffix(NULLABLE_SUFFIX)
        val local = serialName.substringAfterLast(PACKAGE_SEPARATOR)
        return nameIn(local, namespaceOf(descriptor.annotations))
    }

    fun typeName(descriptor: SerialDescriptor): XmlName =
        nameIn(descriptor.serialName, namespaceOf(descriptor.annotations))

    private fun shapeOf(descriptor: SerialDescriptor, index: Int): PropertyShape {
        val annotations = descriptor.getElementAnnotations(index)
        val placement = placementOf(annotations)
        val namespace = propertyNamespaceOf(descriptor, annotations, placement)
        val wrapped = annotations.firstOfType<XmlWrapped>()
        val element = descriptor.getElementDescriptor(index)
        return PropertyShape(
            index = index,
            placement = placement,
            name = nameIn(localOf(descriptor, index), namespace),
            aliases = aliasesOf(annotations),
            kind = kindOf(element),
            item = wrapped?.let { nameIn(it.item, namespace) },
            entries = entriesOf(annotations.firstOfType<XmlEntry>(), namespace),
            isImplicitlyNull = element.isNullable && !descriptor.isElementOptional(index),
            isOptional = descriptor.isElementOptional(index),
        )
    }

    private fun entriesOf(entry: XmlEntry?, namespace: Namespace?): EntryNames {
        val declared = entry
            ?: return namesOf(key = XmlEntry.DEFAULT_KEY, value = XmlEntry.DEFAULT_VALUE, namespace = namespace)
        return namesOf(key = declared.key, value = declared.value, namespace = namespace)
    }

    private fun namesOf(key: String, value: String, namespace: Namespace?): EntryNames =
        EntryNames(key = nameIn(key, namespace), value = nameIn(value, namespace))

    private fun localOf(descriptor: SerialDescriptor, index: Int): String =
        configuration.namingStrategy.nameOf(descriptor, index, descriptor.getElementName(index))

    private fun placementOf(annotations: List<Annotation>): Placement {
        val isAttribute = annotations.any { annotation -> annotation is XmlAsAttribute }
        val isText = annotations.any { annotation -> annotation is XmlAsText }
        val attribute = Placement.Attribute.takeIf { isAttribute }
        val text = Placement.Text.takeIf { isText }
        return attribute ?: text ?: Placement.Element
    }

    private fun propertyNamespaceOf(
        descriptor: SerialDescriptor,
        annotations: List<Annotation>,
        placement: Placement,
    ): Namespace? {
        val owned = annotations.firstOfType<XmlNamespace>()?.let(::Namespace)
        val inherited = namespaceOf(descriptor.annotations).takeIf { placement == Placement.Element }
        return owned ?: inherited
    }

    private fun aliasesOf(annotations: List<Annotation>): List<String> {
        val declared = annotations.filterIsInstance<XmlNames>()
        return declared.flatMap { annotation -> annotation.names.asList() }
    }

    private fun kindOf(descriptor: SerialDescriptor): SerialKind {
        val inner = innerOf(descriptor)
        val isBinary = inner == binary
        return PrimitiveKind.STRING.takeIf { isBinary } ?: inner.kind
    }

    private fun innerOf(descriptor: SerialDescriptor): SerialDescriptor {
        if (!descriptor.isInline) return descriptor
        return descriptor.getElementDescriptor(0)
    }

    private fun namespaceOf(annotations: List<Annotation>): Namespace? {
        val annotated = annotations.firstOfType<XmlNamespace>() ?: return fallback
        return Namespace(annotated)
    }

    private fun nameIn(local: String, namespace: Namespace?): XmlName {
        val owner = namespace ?: return XmlName(local)
        return owner.nameOf(local)
    }
}
