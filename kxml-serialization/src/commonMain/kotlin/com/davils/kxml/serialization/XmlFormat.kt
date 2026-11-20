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

import com.davils.kore.core.validation.violation.Severity
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.XmlReader
import com.davils.kxml.core.diagnostic.XmlDiagnostic
import com.davils.kxml.core.node.XmlDeclaration
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.syntax.XmlSyntaxTree
import com.davils.kxml.serialization.decode.DecodeContext
import com.davils.kxml.serialization.decode.DecodeFailure
import com.davils.kxml.serialization.encode.EncodeContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.modules.SerializersModule

/**
 * A kotlinx.serialization format that encodes values as XML and decodes them from it.
 *
 * A class becomes an element named after its serial name, a property becomes a child element unless it is annotated
 * with [XmlAsAttribute][com.davils.kxml.serialization.annotation.XmlAsAttribute] or
 * [XmlAsText][com.davils.kxml.serialization.annotation.XmlAsText], a list repeats its element or is wrapped with
 * [XmlWrapped][com.davils.kxml.serialization.annotation.XmlWrapped], and a map writes one entry per key. Documents are
 * read with the hardened reader of [XmlFormatConfiguration.xml], so its limits and its treatment of document type
 * declarations apply. Create an instance with [xmlFormat] or use [Default].
 *
 * An instance holds no mutable state and is safe to share between threads.
 *
 * @property configuration the settings of the format.
 * @since 1.0.0
 */
public class XmlFormat internal constructor(public val configuration: XmlFormatConfiguration) : StringFormat {
    /**
     * The module of contextual and polymorphic serializers, [XmlFormatConfiguration.serializersModule].
     *
     * @since 1.0.0
     */
    override val serializersModule: SerializersModule get() = configuration.serializersModule

    /**
     * Encodes [value] as a document, with an XML declaration if [XmlFormatConfiguration.isDeclarationWritten].
     *
     * @param T the type of the value.
     * @param serializer the serializer of the value.
     * @param value the value.
     * @return the XML text.
     * @throws kotlinx.serialization.SerializationException if the value cannot be encoded, such as a polymorphic value
     *   that is no class under [XmlPolymorphism.Attribute].
     * @since 1.0.0
     */
    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val root = encodeToElement(serializer, value)
        val declaration = XmlDeclaration.Default.takeIf { configuration.isDeclarationWritten }
        return configuration.xml.emit(XmlDocument(root, declaration))
    }

    /**
     * Encodes [value] as an element without writing it.
     *
     * @param T the type of the value.
     * @param serializer the serializer of the value.
     * @param value the value.
     * @return the root element.
     * @throws kotlinx.serialization.SerializationException if the value cannot be encoded.
     * @since 1.0.0
     */
    public fun <T> encodeToElement(serializer: SerializationStrategy<T>, value: T): XmlNode.Element =
        EncodeContext(configuration).root(serializer, value)

    /**
     * Decodes a value from a document.
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the value.
     * @param string the complete document.
     * @return the value.
     * @throws XmlDecodingException if the document is malformed, breaks a limit or does not fit the type.
     * @since 1.0.0
     */
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T =
        valueOf(decodeOrFailure(deserializer, string), string)

    /**
     * Decodes a value from an element.
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the value.
     * @param element the root element.
     * @return the value.
     * @throws XmlDecodingException if the element does not fit the type; the message has no positions.
     * @since 1.0.0
     */
    public fun <T> decodeFromElement(deserializer: DeserializationStrategy<T>, element: XmlNode.Element): T =
        valueOf(decodeOrFailure(deserializer, element), text = "")

    /**
     * Decodes a value from a document and returns the diagnostics instead of throwing.
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the value.
     * @param text the complete document.
     * @return [XmlDecoding.Decoded] with the value and the warnings, or [XmlDecoding.Failed] with the diagnostics.
     * @since 1.0.0
     */
    public fun <T> decodeOrFailure(deserializer: DeserializationStrategy<T>, text: String): XmlDecoding<T> {
        val result = configuration.xml.parse(text)
        if (result !is XmlParseResult.Parsed) return XmlDecoding.Failed(result.diagnostics)
        return decodeElement(deserializer, result.document.root, result.diagnostics)
    }

    /**
     * Decodes a value from an element and returns the diagnostics instead of throwing.
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the value.
     * @param element the root element.
     * @return [XmlDecoding.Decoded] with the value and the warnings, or [XmlDecoding.Failed] with the diagnostics.
     * @since 1.0.0
     */
    public fun <T> decodeOrFailure(
        deserializer: DeserializationStrategy<T>,
        element: XmlNode.Element,
    ): XmlDecoding<T> = decodeElement(deserializer, element, emptyList())

    /**
     * Decodes a value from the document of a syntax tree and returns the diagnostics instead of throwing; the ranges of
     * the diagnostics refer to [XmlSyntaxTree.text].
     *
     * The tree is composed with its own [XmlSyntaxTree.xml], not with [XmlFormatConfiguration.xml].
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the value.
     * @param tree the syntax tree of the document.
     * @return [XmlDecoding.Decoded] with the value and the warnings, or [XmlDecoding.Failed] with the diagnostics.
     * @since 1.0.0
     */
    public fun <T> decodeOrFailure(deserializer: DeserializationStrategy<T>, tree: XmlSyntaxTree): XmlDecoding<T> {
        val result = tree.compose()
        if (result !is XmlParseResult.Parsed) return XmlDecoding.Failed(result.diagnostics)
        return decodeElement(deserializer, result.document.root, result.diagnostics)
    }

    /**
     * Decodes a value from the next element of [reader], as [XmlReader.nextElement] reads it, so that a large document
     * can be decoded one element at a time; the ranges of the diagnostics refer to the text of the reader.
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the value.
     * @param reader the reader, positioned before the element; it reads on until the element ends.
     * @return [XmlDecoding.Decoded] with the value and the warnings, [XmlDecoding.Failed] with the diagnostics of the
     * reader or of decoding, or `null` when the enclosing element or the document ended before another element.
     * @since 1.0.0
     */
    public fun <T> decodeNextOrNull(deserializer: DeserializationStrategy<T>, reader: XmlReader): XmlDecoding<T>? {
        val element = reader.nextElement()
        if (!reader.isValid) return XmlDecoding.Failed(reader.diagnostics)
        return element?.let { next -> decodeElement(deserializer, next, emptyList()) }
    }

    /**
     * Returns the configuration, such as `XmlFormat(XmlFormatConfiguration(...))`.
     *
     * @return a text for diagnostics.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlFormat($configuration)"

    private fun <T> decodeElement(
        deserializer: DeserializationStrategy<T>,
        root: XmlNode.Element,
        earlier: List<XmlDiagnostic>,
    ): XmlDecoding<T> {
        val context = DecodeContext(configuration)
        return try {
            val value = context.root(deserializer, root)
            outcome(value, earlier + context.problems)
        } catch (failure: DecodeFailure) {
            XmlDecoding.Failed(earlier + context.problems + failure.diagnostic)
        }
    }

    private fun <T> outcome(value: T, problems: List<XmlDiagnostic>): XmlDecoding<T> {
        val isFailed = problems.any { problem -> problem.severity == Severity.Error }
        if (isFailed) return XmlDecoding.Failed(problems)
        return XmlDecoding.Decoded(value, problems)
    }

    private fun <T> valueOf(decoding: XmlDecoding<T>, text: String): T {
        if (decoding is XmlDecoding.Decoded) return decoding.value
        throw XmlDecodingException(decoding.diagnostics, DiagnosticText.render(text, decoding.diagnostics))
    }

    /**
     * Holds the format with the default configuration.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads with [Xml.Default][com.davils.kxml.core.Xml.Default], matches namespaces exactly, refuses unknown
         * elements, encodes default values and writes a declaration.
         *
         * @since 1.0.0
         */
        public val Default: XmlFormat = xmlFormat()
    }
}
