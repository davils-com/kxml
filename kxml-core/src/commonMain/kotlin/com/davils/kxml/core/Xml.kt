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

package com.davils.kxml.core

import com.davils.kxml.core.emit.XmlWriter
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.parser.XmlPipeline
import com.davils.kxml.core.source.XmlSource

/**
 * Reads and writes XML 1.0 with namespaces under one [XmlConfiguration].
 *
 * The reader is hardened for untrusted input: it never resolves external entities, refuses or skips a document type
 * declaration as [XmlConfiguration.doctype] says, and checks every input against [XmlConfiguration.limits] before the
 * work grows with it. A malformed or oversized document never throws; it yields diagnostics with the code, the range
 * and a message for each problem. Create an instance with [xml] or use [Default].
 *
 * An instance holds no mutable state and is safe to share between threads.
 *
 * @property configuration the settings this instance reads and writes with.
 * @since 1.0.0
 */
public class Xml internal constructor(public val configuration: XmlConfiguration) {
    private val pipeline: XmlPipeline = XmlPipeline(configuration)

    private val writer: XmlWriter = XmlWriter(configuration.emitter, configuration.limits)

    /**
     * Parses [text] into a document.
     *
     * @param text the complete document.
     * @return [XmlParseResult.Parsed] with the document and its warnings, or [XmlParseResult.Failed] with the
     *   diagnostics that stopped the reader.
     * @since 1.0.0
     */
    public fun parse(text: String): XmlParseResult = parse(XmlSource.of(text))

    /**
     * Parses [source] into a document, decoding its bytes first if it holds any.
     *
     * @param source the complete document as text or bytes.
     * @return [XmlParseResult.Parsed] with the document and its warnings, or [XmlParseResult.Failed] with the
     *   diagnostics that stopped the reader.
     * @since 1.0.0
     */
    public fun parse(source: XmlSource): XmlParseResult = pipeline.parse(source)

    /**
     * Creates a reader that reads [source] event by event, without building its tree, with the limits of
     * [configuration].
     *
     * @param source the document to read.
     * @return a reader that is not thread safe; it never throws for malformed input.
     * @since 1.0.0
     */
    public fun reader(source: XmlSource): XmlReader = pipeline.reader(source)

    /**
     * Reads [source] into a flat list of events without building a tree.
     *
     * The reader stops at the first error; the result then holds the events read so far and the diagnostic of the
     * error.
     *
     * @param source the complete document as text or bytes.
     * @return the events and diagnostics of the document.
     * @since 1.0.0
     */
    public fun events(source: XmlSource): XmlEventResult = pipeline.events(source)

    /**
     * Writes [document] as XML text with the options of [XmlConfiguration.emitter].
     *
     * Text and attribute values are escaped for the version the declaration of [document] names: in XML 1.1, the
     * characters that version only allows as references, such as C0 and C1 controls, and the line ends NEL and U+2028
     * are written as character references. A CDATA section that contains `]]>` is split into two sections. Names,
     * comments and processing instructions were checked when the nodes were created.
     *
     * @param document the document to write.
     * @return the XML text.
     * @throws IllegalArgumentException if a value holds a character the version cannot carry, or a CDATA section, a
     * comment or a processing instruction holds one it could only carry as a reference.
     * @throws IllegalStateException if the text would be longer than `XmlLimits.maximumOutputLength` of the
     * configuration; nothing longer is built.
     * @since 1.0.0
     */
    public fun emit(document: XmlDocument): String = writer.write(document)

    /**
     * Writes [element] as a document without a declaration.
     *
     * @param element the root element to write.
     * @return the XML text.
     * @throws IllegalArgumentException if a value holds a character XML 1.0 cannot carry.
     * @throws IllegalStateException if the text would be longer than `XmlLimits.maximumOutputLength` of the
     * configuration.
     * @since 1.0.0
     */
    public fun emit(element: XmlNode.Element): String = emit(XmlDocument(element))

    /**
     * Returns the configuration this instance works with, such as `Xml(XmlConfiguration(...))`.
     *
     * @return a text for diagnostics.
     * @since 1.0.0
     */
    override fun toString(): String = "Xml($configuration)"

    /**
     * Holds the instance with the default configuration.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads with [XmlLimits.Untrusted][com.davils.kxml.core.limit.XmlLimits.Untrusted], refuses document type
         * declarations, keeps all whitespace and writes with
         * [XmlEmitOptions.Default][com.davils.kxml.core.emit.XmlEmitOptions.Default].
         *
         * @since 1.0.0
         */
        public val Default: Xml = xml()
    }
}
