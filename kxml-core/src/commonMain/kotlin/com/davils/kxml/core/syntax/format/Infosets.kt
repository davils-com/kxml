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

package com.davils.kxml.core.syntax.format

import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.syntax.XmlSyntaxTree

private const val PRESERVE: String = "preserve"
private const val DEFAULT: String = "default"

private val spaceName: XmlName = XmlName(local = "space", namespace = XmlNamespaces.XML)

internal fun isSameInfoset(original: XmlSyntaxTree, formatted: XmlSyntaxTree): Boolean {
    val first = documentOf(original)
    val second = documentOf(formatted)
    return first != null && second != null && isSameDocument(first, second)
}

private fun documentOf(tree: XmlSyntaxTree): XmlDocument? = (tree.compose() as? XmlParseResult.Parsed)?.document

private fun isSameDocument(first: XmlDocument, second: XmlDocument): Boolean {
    val isSameProlog = first.declaration == second.declaration && first.doctype == second.doctype
    val isSameMisc = first.prolog == second.prolog && first.epilog == second.epilog
    return isSameProlog && isSameMisc && isSameElement(first = first.root, second = second.root)
}

private fun isSameElement(first: XmlNode.Element, second: XmlNode.Element): Boolean {
    val pending = ArrayDeque<Pair<Pair<XmlNode.Element, XmlNode.Element>, Boolean>>()
    pending.addLast(first to second to false)
    var isSame = true
    while (isSame && pending.isNotEmpty()) {
        val (pair, isInherited) = pending.removeLast()
        val (left, right) = pair
        val isPreserved = isPreserved(left, isInherited)
        val leftChildren = significant(left, isPreserved)
        val rightChildren = significant(right, isPreserved)
        isSame = isSameShell(left, right) && leftChildren.size == rightChildren.size
        leftChildren.zip(rightChildren).forEach { (leftChild, rightChild) ->
            val areElements = leftChild is XmlNode.Element && rightChild is XmlNode.Element
            if (areElements) pending.addLast(leftChild to rightChild to isPreserved)
            isSame = isSame && (areElements || leftChild == rightChild)
        }
    }
    return isSame
}

private fun isSameShell(left: XmlNode.Element, right: XmlNode.Element): Boolean {
    val isSameName = left.name == right.name && left.name.prefix == right.name.prefix
    return isSameName && left.attributes == right.attributes && left.namespaces == right.namespaces
}

private fun isPreserved(element: XmlNode.Element, isInherited: Boolean): Boolean {
    val space = element.attribute(spaceName)?.value
    if (space == PRESERVE) return true
    return isInherited && space != DEFAULT
}

private fun significant(element: XmlNode.Element, isPreserved: Boolean): List<XmlNode> {
    val hasElements = element.children.any { child -> child is XmlNode.Element }
    val isIgnorable = hasElements && !isPreserved
    return element.children.filterNot { child -> isIgnorable && child is XmlNode.Text && child.isWhitespace }
}
