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

import com.davils.kxml.core.fixture.KxmlOutline
import com.davils.kxml.core.fixture.REFUSED
import com.davils.kxml.core.fixture.SaxOutline
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val CASES: Int = 3_000
private const val MOST_TOKENS: Int = 24
private const val MOST_LEAVES: Int = 4
private const val MOST_CHILDREN: Int = 6

private val tokens: List<String> = listOf(
    "<a>", "</a>", "<b x='1'>", "</b>", "<c/>", "<p:d xmlns:p='urn:p'>", "</p:d>", "<e xmlns='urn:e' y=\"&lt;\">",
    "</e>", "<f a='1' a='2'/>", "<q:g/>", "text", " ", "\n", "\r\n", "&amp;", "&lt;", "&gt;", "&#x41;", "&#65;",
    "&#0;", "&#xD800;", "&unknown;", "&", "<", ">", "]]>", "'", "\"", "<!--c-->", "<!--a--b-->", "<?pi data?>",
    "<?xml version='1.0'?>", "<?xml-stylesheet x?>", "<![CDATA[<x>&]]>", "<!DOCTYPE a>", "é", "😀",
    "\u0001", "￾", "<h i='a\tb'/>", "<j k='\n'/>",
)

private val names: List<String> = listOf("a", "p:b", "c", "q:d")
private val attributes: List<String> =
    listOf(
        "",
        " x='1'",
        " p:y=\"&quot;\"",
        " z='a&#9;b&#10;c'",
        " xml:lang='en'",
        " xmlns='urn:d'",
        " xmlns:q='urn:other'",
    )
private val leaves: List<String> = listOf(
    "t", " ", "\r\n", "&amp;&lt;&gt;", "&#x1F600;", "<!--c-->", "<?pi data?>", "<![CDATA[ <&> ]]>", "\u00E9", "]",
)

private val elements: Arb<String> = Arb.list(Arb.element(leaves), 0..MOST_LEAVES).flatMap { inner ->
    Arb.element(names).flatMap { name ->
        Arb.element(attributes).map { attribute ->
            val content = inner.joinToString(separator = "")
            "<$name$attribute>$content</$name>"
        }
    }
}

private val rootAttributes: List<String> = attributes.filterNot { attribute -> "xmlns:q" in attribute }

private val wellFormed: Arb<String> = Arb.list(elements, 0..MOST_CHILDREN).flatMap { children ->
    Arb.element(rootAttributes).map { attribute ->
        val content = children.joinToString(separator = "")
        "<?xml version='1.0'?><!--p--><r xmlns:p='urn:p' xmlns:q='urn:q'$attribute>$content</r><?e?>"
    }
}

private val documents: Arb<String> =
    Arb.list(Arb.element(tokens), 1..MOST_TOKENS).map { parts -> parts.joinToString(separator = "") }

class XmlReaderDifferentialTest : FunSpec({
    context("a document built from fragments") {
        test("is accepted and read as the SAX parser of the JDK reads it") {
            checkAll(CASES, documents) { text ->
                KxmlOutline.of(text) shouldBe SaxOutline.of(text)
            }
        }

        test("that is well-formed is read with the names, attributes and texts SAX reports") {
            checkAll(CASES, wellFormed) { text ->
                val outline = KxmlOutline.of(text)
                outline shouldBe SaxOutline.of(text)
                (outline == REFUSED) shouldBe false
            }
        }
    }
})
