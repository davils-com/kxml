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
import com.davils.kxml.core.diagnostic.XmlDiagnostic
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.serialization.XmlFormatConfiguration
import com.davils.kxml.serialization.XmlNamespaceMatching
import com.davils.kxml.serialization.shape.Shapes
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlin.io.encoding.Base64

@OptIn(ExperimentalSerializationApi::class)
internal class DecodeContext(val configuration: XmlFormatConfiguration) {
    private val collected = ArrayList<XmlDiagnostic>()

    private val polymorphism = PolymorphicReading(this)

    private val collections: Map<SerialKind, (XmlValue) -> CompositeDecoder> = mapOf(
        StructureKind.LIST to { value -> ListDecoder(this, value) },
        StructureKind.MAP to { value -> MapDecoder(this, value) },
    )

    val shapes: Shapes = Shapes(configuration)

    val problems: List<XmlDiagnostic> get() = collected.toList()

    fun <T> root(deserializer: DeserializationStrategy<T>, root: XmlNode.Element): T {
        val descriptor = deserializer.descriptor
        val expected = shapes.rootName(descriptor)
        val isNamed = descriptor.kind is PolymorphicKind || matches(root.name, expected)
        if (!isNamed) {
            fail(XmlDiagnosticCode.UnexpectedRoot, root.range, "Expected <$expected>, but found <${root.name}>")
        }
        return NodeDecoder(this, XmlValue.Element(root)).decodeSerializableValue(deserializer)
    }

    fun matches(actual: XmlName, expected: XmlName, aliases: List<String> = emptyList()): Boolean {
        val isNamed = actual.local == expected.local || actual.local in aliases
        val isExact = configuration.namespaceMatching == XmlNamespaceMatching.Exact
        val isSameNamespace = actual.namespace == expected.namespace
        return isNamed && (!isExact || isSameNamespace)
    }

    fun report(code: XmlDiagnosticCode, range: TextRange?, message: String) {
        collected += diagnostic(code, range, message)
    }

    fun fail(code: XmlDiagnosticCode, range: TextRange?, message: String, cause: Throwable? = null): Nothing =
        throw DecodeFailure(diagnostic(code, range, message), cause)

    fun mismatch(range: TextRange?, expected: String): Nothing =
        fail(XmlDiagnosticCode.TypeMismatch, range, "Expected $expected")

    fun structure(descriptor: SerialDescriptor, value: XmlValue): CompositeDecoder {
        val collection = collections[descriptor.kind] ?: return ClassDecoder(this, descriptor, value)
        return collection(value)
    }

    fun <T> decode(deserializer: DeserializationStrategy<T>, decoder: NodeDecoder): T {
        val isBinary = deserializer.descriptor == ByteArraySerializer().descriptor

        @Suppress("UNCHECKED_CAST")
        if (isBinary) return binary(decoder) as T
        return guarded(decoder.value.range) { polymorphism.decode(deserializer, decoder) }
    }

    private fun binary(decoder: NodeDecoder): ByteArray {
        val compact = decoder.decodeString().filterNot(Char::isWhitespace)
        return runCatching { Base64.decode(compact) }.getOrNull() ?: mismatch(decoder.value.range, "base64 data")
    }

    private fun <T> guarded(range: TextRange?, block: () -> T): T = try {
        block()
    } catch (failure: MissingFieldException) {
        fail(
            code = XmlDiagnosticCode.MissingProperty,
            range = range,
            message = "Missing ${failure.missingFields.joinToString { "<$it>" }}",
            cause = failure,
        )
    } catch (failure: IllegalArgumentException) {
        fail(
            code = XmlDiagnosticCode.TypeMismatch,
            range = range,
            message = "The value is refused by its type",
            cause = failure,
        )
    }

    private fun diagnostic(code: XmlDiagnosticCode, range: TextRange?, message: String): XmlDiagnostic =
        XmlDiagnostic(code = code, range = range ?: TextRange.at(0), message = message)
}
