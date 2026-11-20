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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
internal open class ValueEncoder(
    protected val context: EncodeContext,
    private val sink: (Content) -> Unit,
) : AbstractEncoder() {
    override val serializersModule: SerializersModule get() = context.configuration.serializersModule

    protected open fun emit(content: Content) {
        sink(content)
    }

    override fun encodeValue(value: Any) {
        emit(Content.Text(value.toString()))
    }

    override fun encodeNull() {
        emit(Content.Nil.takeIf { context.configuration.isNilWritten } ?: Content.Absent)
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        emit(Content.Text(enumDescriptor.getElementName(index)))
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder = this

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder =
        StructureEncoder(context, context.collector(descriptor), ::emit)

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        context.encode(serializer, value, ::emit)
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean =
        context.configuration.isDefaultEncoded
}
