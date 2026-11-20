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
package com.davils.kxml.document.xpath.parse

import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.document.xpath.syntax.Expression
import com.davils.kxml.document.xpath.syntax.NodeTest

private const val COLON: Char = ':'
private const val WILDCARD: String = "*"

private val typeTests: Map<String, NodeTest> = mapOf(
    "comment" to NodeTest.Comment,
    "text" to NodeTest.Text,
    "node" to NodeTest.AnyNode,
)

internal class QueryNames(private val namespaces: (String) -> String?, private val elementNamespace: String?) {
    fun nameTest(token: Token, isElement: Boolean): NodeTest {
        if (token.text == WILDCARD) return NodeTest.AnyName
        val local = token.text.substringAfter(COLON)
        val isDefaulted = isElement && COLON !in token.text
        val namespace = elementNamespace.takeIf { isDefaulted } ?: namespaceOf(token)
        val anyLocal = namespace?.takeIf { local == WILDCARD }?.let(NodeTest::AnyLocal)
        return anyLocal ?: NodeTest.Name(local, namespace)
    }

    fun typeTest(type: String, target: String?): NodeTest = typeTests[type] ?: NodeTest.Instruction(target)

    fun variable(token: Token): Expression {
        val local = token.text.substringAfter(COLON)
        return Expression.Variable(XmlName(local = local, namespace = namespaceOf(token)))
    }

    private fun namespaceOf(token: Token): String? {
        val prefix = token.text.substringBefore(COLON, missingDelimiterValue = "")
        if (prefix.isEmpty()) return null
        return resolve(prefix, token.offset)
    }

    private fun resolve(prefix: String, offset: Int): String {
        if (prefix == XmlNamespaces.XML_PREFIX) return XmlNamespaces.XML
        return namespaces(prefix) ?: throw QuerySyntaxError("the prefix is not declared", offset)
    }
}
