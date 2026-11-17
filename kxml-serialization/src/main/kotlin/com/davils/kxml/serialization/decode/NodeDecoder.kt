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
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule

private val booleans: Map<String, Boolean> = mapOf("true" to true, "1" to true, "false" to false, "0" to false)

@OptIn(ExperimentalSerializationApi::class)
internal class NodeDecoder(private val context: DecodeContext, val value: XmlValue) : AbstractDecoder() {
    override val serializersModule: SerializersModule get() = context.configuration.serializersModule

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = CompositeDecoder.DECODE_DONE

    override fun decodeNotNullMark(): Boolean = value !is XmlValue.Absent

    override fun decodeNull(): Nothing? = null

    override fun decodeBoolean(): Boolean = booleans[token()] ?: context.mismatch(value.range, "a boolean")

    override fun decodeByte(): Byte = integer(Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()).toByte()

    override fun decodeShort(): Short = integer(Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()).toShort()

    override fun decodeInt(): Int = integer(Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()).toInt()

    override fun decodeLong(): Long = token().toLongOrNull() ?: context.mismatch(value.range, "an integer")

    override fun decodeFloat(): Float = real().toFloat()

    override fun decodeDouble(): Double = real()

    override fun decodeChar(): Char = text().singleOrNull() ?: context.mismatch(value.range, "a single character")

    override fun decodeString(): String = text()

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val name = token()
        val index = enumDescriptor.getElementIndex(name)
        if (index == CompositeDecoder.UNKNOWN_NAME) {
            context.fail(
                XmlDiagnosticCode.UnknownEnumValue,
                value.range,
                "\"$name\" is none of ${enumDescriptor.elementNames.joinToString()}",
            )
        }
        return index
    }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder = this

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = context.structure(descriptor, value)

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T =
        context.decode(deserializer, this)

    private fun integer(range: LongRange): Long {
        val number = decodeLong()
        if (number !in range) {
            context.fail(XmlDiagnosticCode.NumberOutOfRange, value.range, "$number is outside $range")
        }
        return number
    }

    private fun real(): Double = token().toDoubleOrNull() ?: context.mismatch(value.range, "a number")

    private fun token(): String = text().trim()

    private fun text(): String {
        val current = value
        if (current is XmlValue.Text) return current.text
        val holder = current as? XmlValue.Element ?: context.mismatch(current.range, "a single value")
        return textOf(holder)
    }

    private fun textOf(holder: XmlValue.Element): String {
        val element = holder.element
        if (element.elements.isNotEmpty()) context.mismatch(element.range, "text, but found elements")
        return element.text
    }
}
