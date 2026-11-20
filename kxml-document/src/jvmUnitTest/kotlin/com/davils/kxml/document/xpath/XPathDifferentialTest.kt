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
package com.davils.kxml.document.xpath

import com.davils.kxml.document.fixture.SAMPLE
import com.davils.kxml.document.fixture.sampleNamespaces
import com.davils.kxml.document.fixture.textOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

private const val DISALLOW_DOCTYPE: String = "http://apache.org/xml/features/disallow-doctype-decl"

private val expressions: List<String> = listOf(
    "count(//node())",
    "count(//*)",
    "count(//@*)",
    "count(//text())",
    "count(//comment())",
    "count(//processing-instruction())",
    "name(/*)",
    "local-name(//p:i)",
    "namespace-uri(//p:i)",
    "string(//d:n[2])",
    "string(//d:i)",
    "string(/)",
    "normalize-space(/)",
    "concat(//d:n, '|', //@n)",
    "sum(//d:n[number(.) = number(.)])",
    "1 div 3",
    "0.1 + 0.2",
    "-0",
    "1 div 0",
    "0 div 0",
    "123456789012345678901234567890",
    "0.000001",
    "substring('12345', 1.5, 2.6)",
    "substring('12345', -42, 1 div 0)",
    "substring-before('1999/04/01', '/')",
    "translate('bar', 'abc', 'ABC')",
    "string-length(/)",
    "name(//d:i/following::*[2])",
    "name(//d:e/preceding::*[1])",
    "name(//d:e/preceding-sibling::*[last()])",
    "name(//d:i/text()/ancestor::*[2])",
    "count(//d:n[. > 0])",
    "//d:n = //d:n",
    "//d:n != 'abc'",
    "//d:n < //@n",
    "3.5 = //d:n",
    "count(//*[lang('en')])",
    "lang('en')",
    "round(-2.5)",
    "round(2.5)",
    "5 mod -2",
    "number('  12  ')",
    "number('1.')",
    "number('-.5')",
    "number('1e3')",
    "boolean(//d:none)",
    "count(//d:n | //p:i | //@*)",
    "name((//d:n | //p:i)[1])",
    "string((//d:n)[last()])",
    "count(/d:r/*[position() mod 2 = 0])",
)

private fun javaDocument(): Document {
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = true
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    factory.setFeature(DISALLOW_DOCTYPE, true)
    val builder = factory.newDocumentBuilder()
    return StringReader(SAMPLE).use { reader -> builder.parse(InputSource(reader)) }
}

private fun javaString(document: Document, expression: String): String {
    val xpath = XPathFactory.newInstance().newXPath()
    xpath.namespaceContext = object : NamespaceContext {
        override fun getNamespaceURI(prefix: String): String = sampleNamespaces[prefix] ?: XMLConstants.NULL_NS_URI

        override fun getPrefix(namespaceURI: String): String? = null

        override fun getPrefixes(namespaceURI: String): Iterator<String> = emptyList<String>().iterator()
    }
    return xpath.evaluate("string($expression)", document, XPathConstants.STRING) as String
}

class XPathDifferentialTest : FunSpec({
    context("an expression") {
        test("gives the string javax.xml.xpath gives") {
            val document = javaDocument()
            val differences = expressions.filter { expression ->
                textOf("string($expression)") != javaString(document, expression)
            }

            differences.shouldBeEmpty()
        }
    }
})
