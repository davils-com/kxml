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
package com.davils.kxml.core.fixture

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

internal const val REFUSED: String = "refused"

private const val DISALLOW_DOCTYPE: String = "http://apache.org/xml/features/disallow-doctype-decl"

internal class SaxOutline : DefaultHandler() {
    private val written = StringBuilder()
    private val pending = StringBuilder()

    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        flushed()
        val shown = (0..<attributes.length).map { index ->
            "@{${attributes.getURI(index)}}${attributes.getLocalName(index)}=${attributes.getValue(index)}"
        }
        written.append("<{$uri}$localName${shown.sorted()}")
    }

    override fun endElement(uri: String, localName: String, qName: String) {
        flushed()
        written.append(">")
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        pending.appendRange(value = ch, startIndex = start, endIndex = start + length)
    }

    override fun processingInstruction(target: String, data: String) {
        flushed()
        written.append("?$target $data|")
    }

    private fun text(): String {
        flushed()
        return written.toString()
    }

    private fun flushed() {
        if (pending.isNotEmpty()) written.append("'$pending'")
        pending.clear()
    }

    companion object {
        fun of(text: String): String {
            val factory = SAXParserFactory.newInstance()
            factory.isNamespaceAware = true
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            factory.setFeature(DISALLOW_DOCTYPE, true)
            val outline = SaxOutline()
            return try {
                val parser = factory.newSAXParser()
                StringReader(text).use { reader -> parser.parse(InputSource(reader), outline) }
                outline.text()
            } catch (_: SAXException) {
                REFUSED
            }
        }
    }
}
