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

package com.davils.kxml.document.c14n

import com.davils.kxml.document.fixture.documentOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import java.io.ByteArrayInputStream
import javax.xml.crypto.OctetStreamData
import javax.xml.crypto.dom.DOMCryptoContext
import javax.xml.crypto.dsig.TransformService

private const val DOM: String = "DOM"

private val outside: String = """
    |<?xml version="1.0"?>
    |<?xml-stylesheet   href="doc.xsl"
    |   type="text/xsl"   ?>
    |<doc>Hello, world!<!-- Comment 1 --></doc>
    |<?pi-without-data     ?>
    |<!-- Comment 2 -->
    |<!-- Comment 3 -->
""".trimMargin()

private val whitespace: String = """
    |<doc>
    |   <clean>   </clean>
    |   <dirty>   A   B   </dirty>
    |   <mixed>
    |      A
    |      <clean>   </clean>
    |      B
    |      <dirty>   A   B   </dirty>
    |      C
    |   </mixed>
    |</doc>
""".trimMargin()

private val documents: List<String> = listOf(
    outside,
    whitespace,
    """<doc xmlns:b="http://www.ietf.org" xmlns:a="http://www.w3.org" xmlns="http://example.org"><e1/>""" +
        """<e2 xmlns:b="http://www.ietf.org" b:attr="sorted" attr2="all" a:attr="out" attr="I'm"></e2>""" +
        """<e3 xmlns=""><e4 xmlns=""/><e5 xmlns="http://example.org" xmlns:c="http://www.ietf.org"/></e3></doc>""",
    "<doc a='&quot;&lt;&amp;&#9;&#10;&#13;&gt;'>&lt;&amp;&gt;&#13;'<![CDATA[<x> & y]]>\u00E9\uD800\uDC00</doc>",
    """<n0:local xmlns:n0="foo:bar" xmlns:n3="ftp://example.org"><n1:elem2 xmlns:n1="http://example.net" """ +
        """xml:lang="en"><n3:stuff xmlns:n3="ftp://example.org"/></n1:elem2></n0:local>""",
    "<p:a xmlns:p='urn:p' xmlns='urn:d' q:x='1' xmlns:q='urn:q' xml:lang='de'><b xmlns=''><c xml:space='preserve'>" +
        "t</c></b><p:d xmlns:p='urn:other'><e/></p:d></p:a>",
)

private val context: DOMCryptoContext = object : DOMCryptoContext() {}

private fun jdkCanonical(text: String, method: XmlCanonicalization): String {
    val service = TransformService.getInstance(method.uri, DOM)
    service.init(null)
    val data = OctetStreamData(ByteArrayInputStream(text.encodeToByteArray()))
    val canonical = service.transform(data, context) as OctetStreamData
    val bytes = canonical.octetStream.readAllBytes()
    return bytes.decodeToString()
}

private fun kxmlCanonical(text: String, method: XmlCanonicalization): String {
    val bytes = XmlCanonicalizer(method).canonicalize(documentOf(text))
    return bytes.decodeToString()
}

class XmlCanonicalizerDifferentialTest : FunSpec({
    context("the canonical form of a whole document") {
        test("is the one the canonicalizers of the JDK write") {
            val differences = XmlCanonicalization.entries.flatMap { method ->
                val differing = documents.filter { text -> kxmlCanonical(text, method) != jdkCanonical(text, method) }
                differing.map { text -> "$method $text" }
            }

            differences.shouldBeEmpty()
        }
    }
})
