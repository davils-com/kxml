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
package com.davils.kxml.document.fixture

import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.document.xpath.XPath
import com.davils.kxml.document.xpath.XPathNodeKind
import com.davils.kxml.document.xpath.XPathValue
import io.kotest.matchers.types.shouldBeInstanceOf

internal const val SAMPLE: String = "<?xml version='1.0'?><!--c0-->" +
    "<r xmlns='urn:d' xmlns:p='urn:p' a='1' xml:lang='en-US'>" +
    "<p:i id='i1' xml:id='x1'>one</p:i>" +
    "<i n='2'>two<![CDATA[ cd]]><!--c1--><?pi data?></i>" +
    "<e/><n>3.5</n><n>-1</n><n>abc</n></r><?tail t?>"

internal val sampleNamespaces: Map<String, String> = mapOf("d" to "urn:d", "p" to "urn:p")

internal fun valueOf(expression: String, text: String = SAMPLE, limits: XmlLimits = XmlLimits.Untrusted): XPathValue {
    val query = XPath.parse(expression, namespaces = sampleNamespaces, limits = limits)
    return query.evaluate(documentOf(text))
}

internal fun textOf(expression: String, text: String = SAMPLE): String {
    val value = valueOf(expression, text).shouldBeInstanceOf<XPathValue.Text>()
    return value.value
}

internal fun numberOf(expression: String, text: String = SAMPLE): Double {
    val value = valueOf(expression, text).shouldBeInstanceOf<XPathValue.Number>()
    return value.value
}

internal fun truthOf(expression: String, text: String = SAMPLE): Boolean {
    val value = valueOf(expression, text).shouldBeInstanceOf<XPathValue.Truth>()
    return value.isTrue
}

internal fun namesOf(expression: String, text: String = SAMPLE): List<String> {
    val query = XPath.parse(expression, namespaces = sampleNamespaces)
    val nodes = query.select(documentOf(text))
    return nodes.map { node -> node.name?.qualified ?: node.kind.label }
}

private val XPathNodeKind.label: String get() = "#" + name.lowercase()
