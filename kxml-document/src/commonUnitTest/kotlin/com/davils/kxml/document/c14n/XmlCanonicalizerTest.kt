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

import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.limit.xmlLimits
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.xml
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.fixture.GROWTH_FACTOR
import com.davils.kxml.document.fixture.documentOf
import com.davils.kxml.document.fixture.shouldGrowLinearly
import com.davils.kxml.document.xpath.XPath
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

private val outside: String = """
    |<?xml version="1.0"?>
    |<?xml-stylesheet   href="doc.xsl"
    |   type="text/xsl"   ?>
    |<doc>Hello, world!<!-- Comment 1 --></doc>
    |<?pi-without-data     ?>
    |<!-- Comment 2 -->
    |<!-- Comment 3 -->
""".trimMargin()
private val outsideCanonical: String = """
    |<?xml-stylesheet href="doc.xsl"
    |   type="text/xsl"   ?>
    |<doc>Hello, world!</doc>
    |<?pi-without-data?>
""".trimMargin()
private val outsideWithComments: String = """
    |<?xml-stylesheet href="doc.xsl"
    |   type="text/xsl"   ?>
    |<doc>Hello, world!<!-- Comment 1 --></doc>
    |<?pi-without-data?>
    |<!-- Comment 2 -->
    |<!-- Comment 3 -->
""".trimMargin()
private const val TAGS: String = """<doc xmlns:b="http://www.ietf.org" xmlns:a="http://www.w3.org" """ +
    """xmlns="http://example.org"><e1/><e2 xmlns:b="http://www.ietf.org" b:attr="sorted" attr2="all" """ +
    """a:attr="out" attr="I'm"></e2></doc>"""
private val subsetDocument: String = """
    |<!DOCTYPE doc [<!ATTLIST e2 xml:space (default|preserve) 'preserve'>]>
    |<doc xmlns="http://www.ietf.org" xmlns:w3c="http://www.w3.org" xml:base="something/else">
    |<e1>
    |<e2 xmlns="" xml:id="abc" xml:base="bar/">
    |<e3 id="E3" xml:base="foo"/>
    |</e2>
    |</e1>
    |</doc>
""".trimMargin()
private const val SUBSET_EXPRESSION: String = "(//. | //@* | //namespace::*)[self::ietf:e1 or " +
    "(parent::ietf:e1 and not(self::text() or self::e2)) or " +
    "count(//*[@id = 'E3'] | ancestor-or-self::node()) = count(ancestor-or-self::node())]"
private const val EXCLUSIVE_DOCUMENT: String = """<n0:local xmlns:n0="foo:bar" xmlns:n3="ftp://example.org">""" +
    """<n1:elem2 xmlns:n1="http://example.net" xml:lang="en"><n3:stuff xmlns:n3="ftp://example.org"/>""" +
    "</n1:elem2></n0:local>"
private const val ELEMENT_TWO: String = "(//. | //@* | //namespace::*)[ancestor-or-self::n1:elem2]"
private const val SMALL: Int = 9
private const val SUPPLEMENTARY: String = "\uD800\uDC00"
private const val REPLACEMENT: String = "\uFFFD"

private const val GROWTH_SIZE: Int = 400
private const val DECLARATIONS: Int = 200

private fun namespaced(children: Int): String {
    val declarations = (0..<DECLARATIONS).joinToString(separator = " ") { index -> "xmlns:p$index='urn:$index'" }
    return "<r $declarations>" + "<c/>".repeat(children) + "</r>"
}

private val ietf: Map<String, String> = mapOf("ietf" to "http://www.ietf.org")
private val example: Map<String, String> = mapOf("n1" to "http://example.net")

private fun canonical(text: String, method: XmlCanonicalization, prefixes: Set<String> = emptySet()): String {
    val bytes = XmlCanonicalizer(method, inclusivePrefixes = prefixes).canonicalize(documentOf(text))
    return bytes.decodeToString()
}

private fun subsetOf(
    document: XmlDocument,
    expression: String,
    method: XmlCanonicalization,
    namespaces: Map<String, String> = emptyMap(),
): String {
    val subset = XPath.parse(expression, namespaces = namespaces)
    val bytes = XmlCanonicalizer(method).canonicalize(document, subset)
    return bytes.decodeToString()
}

private fun withSubset(text: String): XmlDocument {
    val reader = xml { doctype = XmlDoctype.Internal }
    val parsed = reader.parse(text).shouldBeInstanceOf<XmlParseResult.Parsed>()
    return parsed.document
}

class XmlCanonicalizerTest : FunSpec({
    context("a whole document") {
        test("loses its declaration and keeps what surrounds the document element on lines of its own") {
            canonical(outside, XmlCanonicalization.Canonical10) shouldBe outsideCanonical
            canonical(outside, XmlCanonicalization.Canonical11WithComments) shouldBe outsideWithComments
        }

        test("writes start and end tags with sorted declarations and attributes") {
            canonical(TAGS, XmlCanonicalization.Canonical10) shouldBe
                """<doc xmlns="http://example.org" xmlns:a="http://www.w3.org" xmlns:b="http://www.ietf.org">""" +
                """<e1></e1><e2 attr="I'm" attr2="all" b:attr="sorted" a:attr="out"></e2></doc>"""
        }

        test("escapes text and attribute values") {
            val text = """<doc a='&quot;&lt;&amp;&#9;&#10;&#13;&gt;'>&lt;&amp;&gt;&#13;"'</doc>"""

            canonical(text, XmlCanonicalization.Canonical10) shouldBe
                """<doc a="&quot;&lt;&amp;&#x9;&#xA;&#xD;>">&lt;&amp;&gt;&#xD;"'</doc>"""
        }

        test("undeclares the default namespace only where it was declared") {
            canonical("<a xmlns='urn:a'><b xmlns=''><c xmlns=''/></b></a>", XmlCanonicalization.Canonical10) shouldBe
                """<a xmlns="urn:a"><b xmlns=""><c></c></b></a>"""
            canonical("<a><b xmlns=''/></a>", XmlCanonicalization.Canonical10) shouldBe "<a><b></b></a>"
            canonical("<a xmlns:xml='http://www.w3.org/XML/1998/namespace'/>", XmlCanonicalization.Canonical10) shouldBe
                "<a></a>"
        }

        test("orders by code points") {
            canonical("<a $SUPPLEMENTARY='2' $REPLACEMENT='1'/>", XmlCanonicalization.Canonical10) shouldBe
                """<a $REPLACEMENT="1" $SUPPLEMENTARY="2"></a>"""
        }

        test("is refused when it declares a relative namespace URI") {
            shouldThrow<IllegalArgumentException> {
                canonical("<a xmlns:p='relative'/>", XmlCanonicalization.Canonical10)
            }
            canonical("<a xmlns:p='urn:x' xmlns:q='a1+.-:b'/>", XmlCanonicalization.Canonical10) shouldBe
                """<a xmlns:p="urn:x" xmlns:q="a1+.-:b"></a>"""
            canonical("<a xmlns:p='Urn:x'/>", XmlCanonicalization.Canonical10) shouldBe """<a xmlns:p="Urn:x"></a>"""
            shouldThrow<IllegalArgumentException> { canonical("<a xmlns:p='1a:x'/>", XmlCanonicalization.Exclusive10) }
            shouldThrow<IllegalArgumentException> { canonical("<a xmlns:p=':x'/>", XmlCanonicalization.Exclusive10) }
            shouldThrow<IllegalArgumentException> { canonical("<a xmlns:p='a_b:x'/>", XmlCanonicalization.Exclusive10) }
        }

        test("grows linearly with its elements, whatever namespaces are in scope on each") {
            val canonicalizer = XmlCanonicalizer(XmlCanonicalization.Canonical10)
            val small = namespaced(GROWTH_SIZE)
            val large = namespaced(GROWTH_SIZE * GROWTH_FACTOR)
            val documents = mapOf(small to documentOf(small), large to documentOf(large))

            shouldGrowLinearly(small, large) { text -> canonicalizer.canonicalize(documents.getValue(text)) }
        }

        test("stops at its output limit") {
            val canonicalizer = XmlCanonicalizer(
                XmlCanonicalization.Canonical10,
                limits = xmlLimits { maximumOutputLength = SMALL },
            )

            canonicalizer.canonicalize(documentOf("<a>xx</a>")).decodeToString() shouldBe "<a>xx</a>"
            val error = shouldThrow<XmlDocumentLimitException> { canonicalizer.canonicalize(documentOf("<a>xxx</a>")) }
            error.limit shouldBe "maximumOutputLength"
        }
    }

    context("a document subset") {
        test("imports every attribute of the XML namespace into orphans under Canonical XML 1.0") {
            val document = withSubset(subsetDocument)

            subsetOf(
                document = document,
                expression = SUBSET_EXPRESSION,
                method = XmlCanonicalization.Canonical10,
                namespaces = ietf,
            ) shouldBe
                """<e1 xmlns="http://www.ietf.org" xmlns:w3c="http://www.w3.org" xml:base="something/else">""" +
                """<e3 xmlns="" id="E3" xml:base="foo" xml:id="abc" xml:space="preserve"></e3></e1>"""
        }

        test("imports xml:lang and xml:space and joins xml:base under Canonical XML 1.1") {
            val document = withSubset(subsetDocument)

            subsetOf(
                document = document,
                expression = SUBSET_EXPRESSION,
                method = XmlCanonicalization.Canonical11,
                namespaces = ietf,
            ) shouldBe
                """<e1 xmlns="http://www.ietf.org" xmlns:w3c="http://www.w3.org" xml:base="something/else">""" +
                """<e3 xmlns="" id="E3" xml:base="bar/foo" xml:space="preserve"></e3></e1>"""
            val bases = "<a xml:base='foo/bar'><b xml:base='..'><c xml:base='..'><d xml:base='x'/></c></b></a>"
            val dropped = "//a | //a/@* | //d | //d/@*"
            subsetOf(documentOf(bases), dropped, XmlCanonicalization.Canonical11) shouldBe
                """<a xml:base="foo/bar"><d xml:base="../../x"></d></a>"""
            val cancelled = "<a><b xml:base='x/'><c xml:base='../'/></b></a>"
            subsetOf(documentOf(cancelled), "//c | //c/@* | /a", XmlCanonicalization.Canonical11) shouldBe
                "<a><c></c></a>"
            subsetOf(documentOf(cancelled), "//c | /a", XmlCanonicalization.Canonical11) shouldBe
                """<a><c xml:base="x/"></c></a>"""
        }

        test("renders only the comments and processing instructions in the subset") {
            val document = documentOf("<!--a--><r><!--b--><?p?></r><?q?>")

            subsetOf(document, "//r | //comment()", XmlCanonicalization.Canonical10WithComments) shouldBe
                "<!--a-->\n<r><!--b--></r>"
            subsetOf(document, "//r | //processing-instruction()", XmlCanonicalization.Canonical10WithComments) shouldBe
                "<r><?p?></r>\n<?q?>"
        }

        test("renders loose attributes and namespace nodes of omitted elements") {
            val attributes = documentOf("<a x='1' xmlns:p='urn:p'><b y='2'/></a>")
            val namespaces = documentOf("<a xmlns:p='urn:p'/>")

            subsetOf(attributes, "//@*", XmlCanonicalization.Canonical10) shouldBe """ x="1" y="2""""
            subsetOf(namespaces, "/a/namespace::p", XmlCanonicalization.Canonical10) shouldBe
                """ xmlns:p="urn:p""""
        }

        test("imports nothing into an element whose parent is rendered, and undeclares the default namespace") {
            val text = "<p:a xmlns:p='urn:p' xmlns='urn:d' q:x='1' xmlns:q='urn:q' xml:lang='de'><b xmlns=''>" +
                "<c xml:space='preserve'>t</c></b><p:d xmlns:p='urn:other'><e/></p:d></p:a>"
            val all = "(//. | //@* | //namespace::*)[not(self::text())]"
            val withoutB = "(//. | //@* | //namespace::*)[not(self::b)]"

            subsetOf(documentOf(text), all, XmlCanonicalization.Canonical10) shouldBe
                """<p:a xmlns="urn:d" xmlns:p="urn:p" xmlns:q="urn:q" xml:lang="de" q:x="1"><b xmlns="">""" +
                """<c xml:space="preserve"></c></b><p:d xmlns:p="urn:other"><e></e></p:d></p:a>"""
            subsetOf(documentOf(text), withoutB, XmlCanonicalization.Canonical10) shouldBe
                """<p:a xmlns="urn:d" xmlns:p="urn:p" xmlns:q="urn:q" xml:lang="de" q:x="1">""" +
                """<c xmlns="" xml:lang="de" xml:space="preserve">t</c>""" +
                """<p:d xmlns:p="urn:other"><e></e></p:d></p:a>"""
        }

        test("""renders xmlns="" below an output ancestor with a default namespace""") {
            val text = "<a xmlns='urn:a'><b xmlns=''><c/></b></a>"
            val subset = "(//. | //@* | //namespace::*)[not(self::b)]"

            subsetOf(documentOf(text), subset, XmlCanonicalization.Canonical10) shouldBe
                """<a xmlns="urn:a"><c xmlns=""></c></a>"""
            subsetOf(documentOf(text), subset, XmlCanonicalization.Exclusive10) shouldBe
                """<a xmlns="urn:a"><c xmlns=""></c></a>"""
        }
    }

    context("exclusive canonicalization") {
        test("declares only the namespaces an element visibly utilizes") {
            canonical(EXCLUSIVE_DOCUMENT, XmlCanonicalization.Exclusive10) shouldBe
                """<n0:local xmlns:n0="foo:bar"><n1:elem2 xmlns:n1="http://example.net" xml:lang="en">""" +
                """<n3:stuff xmlns:n3="ftp://example.org"></n3:stuff></n1:elem2></n0:local>"""
            val exclusive = documentOf(EXCLUSIVE_DOCUMENT)
            subsetOf(
                document = exclusive,
                expression = ELEMENT_TWO,
                method = XmlCanonicalization.Exclusive10,
                namespaces = example,
            ) shouldBe
                """<n1:elem2 xmlns:n1="http://example.net" xml:lang="en">""" +
                """<n3:stuff xmlns:n3="ftp://example.org"></n3:stuff></n1:elem2>"""
            subsetOf(
                document = exclusive,
                expression = ELEMENT_TWO,
                method = XmlCanonicalization.Canonical10,
                namespaces = example,
            ) shouldBe
                """<n1:elem2 xmlns:n0="foo:bar" xmlns:n1="http://example.net" xmlns:n3="ftp://example.org" """ +
                """xml:lang="en"><n3:stuff></n3:stuff></n1:elem2>"""
        }

        test("handles the inclusive prefixes as Canonical XML does") {
            canonical(EXCLUSIVE_DOCUMENT, XmlCanonicalization.Exclusive10, prefixes = setOf("n3")) shouldBe
                """<n0:local xmlns:n0="foo:bar" xmlns:n3="ftp://example.org">""" +
                """<n1:elem2 xmlns:n1="http://example.net" xml:lang="en"><n3:stuff></n3:stuff></n1:elem2></n0:local>"""
            val text = "<p:a xmlns:p='urn:p' xmlns='urn:d' q:x='1' xmlns:q='urn:q'><b xmlns=''/></p:a>"
            canonical(text, XmlCanonicalization.Exclusive10WithComments) shouldBe
                """<p:a xmlns:p="urn:p" xmlns:q="urn:q" q:x="1"><b></b></p:a>"""
            val withDefault = setOf(XmlCanonicalizer.DEFAULT_NAMESPACE)
            canonical(text, XmlCanonicalization.Exclusive10, prefixes = withDefault) shouldBe
                """<p:a xmlns="urn:d" xmlns:p="urn:p" xmlns:q="urn:q" q:x="1"><b xmlns=""></b></p:a>"""
        }
    }

    context("a canonicalizer") {
        test("is described by its method and its prefixes") {
            val canonicalizer = XmlCanonicalizer(XmlCanonicalization.Exclusive10, inclusivePrefixes = setOf("a", "b"))

            val reordered = XmlCanonicalizer(XmlCanonicalization.Exclusive10, inclusivePrefixes = setOf("b", "a"))
            canonicalizer shouldBe reordered
            canonicalizer.hashCode() shouldBe
                XmlCanonicalizer(XmlCanonicalization.Exclusive10, inclusivePrefixes = setOf("a", "b")).hashCode()
            canonicalizer shouldNotBe XmlCanonicalizer(XmlCanonicalization.Exclusive10)
            canonicalizer shouldNotBe XmlCanonicalizer(XmlCanonicalization.Exclusive10WithComments, setOf("a", "b"))
            canonicalizer.equals("x") shouldBe false
            canonicalizer.toString() shouldBe "XmlCanonicalizer(Exclusive10, prefixes=2)"
            XmlCanonicalization.of("http://www.w3.org/2006/12/xml-c14n11") shouldBe XmlCanonicalization.Canonical11
            XmlCanonicalization.of("urn:none").shouldBeNull()
        }

        test("refuses prefixes it cannot use and subsets that are no node-sets") {
            shouldThrow<IllegalArgumentException> { XmlCanonicalizer(XmlCanonicalization.Canonical10, setOf("a")) }
            shouldThrow<IllegalArgumentException> { XmlCanonicalizer(XmlCanonicalization.Exclusive10, setOf("1a")) }
            shouldThrow<IllegalArgumentException> {
                XmlCanonicalizer(XmlCanonicalization.Canonical10).canonicalize(documentOf("<a/>"), XPath.parse("1"))
            }
        }
    }
})
