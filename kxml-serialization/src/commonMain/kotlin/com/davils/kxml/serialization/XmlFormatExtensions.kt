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

import com.davils.kxml.core.node.XmlNode
import kotlinx.serialization.serializer

/**
 * Decodes a value of type [T] from a document with the serializer the [serializersModule][XmlFormat.serializersModule]
 * finds for it.
 *
 * @param T the type of the value.
 * @param text the complete document.
 * @return [XmlDecoding.Decoded] or [XmlDecoding.Failed], see [XmlFormat.decodeOrFailure].
 * @throws kotlinx.serialization.SerializationException if there is no serializer for [T].
 * @since 1.0.0
 */
public inline fun <reified T> XmlFormat.decodeOrFailure(text: String): XmlDecoding<T> =
    decodeOrFailure(serializersModule.serializer<T>(), text)

/**
 * Decodes a value of type [T] from an element with the serializer the [serializersModule][XmlFormat.serializersModule]
 * finds for it.
 *
 * @param T the type of the value.
 * @param element the root element.
 * @return [XmlDecoding.Decoded] or [XmlDecoding.Failed], see [XmlFormat.decodeOrFailure].
 * @throws kotlinx.serialization.SerializationException if there is no serializer for [T].
 * @since 1.0.0
 */
public inline fun <reified T> XmlFormat.decodeOrFailure(element: XmlNode.Element): XmlDecoding<T> =
    decodeOrFailure(serializersModule.serializer<T>(), element)

/**
 * Encodes a value of type [T] as an element with the serializer the [serializersModule][XmlFormat.serializersModule]
 * finds for it.
 *
 * @param T the type of the value.
 * @param value the value.
 * @return the root element.
 * @throws kotlinx.serialization.SerializationException if there is no serializer for [T] or the value cannot be
 *   encoded.
 * @since 1.0.0
 */
public inline fun <reified T> XmlFormat.encodeToElement(value: T): XmlNode.Element =
    encodeToElement(serializersModule.serializer<T>(), value)

/**
 * Decodes a value of type [T] from an element with the serializer the [serializersModule][XmlFormat.serializersModule]
 * finds for it.
 *
 * @param T the type of the value.
 * @param element the root element.
 * @return the value.
 * @throws XmlDecodingException if the element does not fit the type.
 * @throws kotlinx.serialization.SerializationException if there is no serializer for [T].
 * @since 1.0.0
 */
public inline fun <reified T> XmlFormat.decodeFromElement(element: XmlNode.Element): T =
    decodeFromElement(serializersModule.serializer<T>(), element)
