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
@file:OptIn(DavilsInternal::class)

package com.davils.kxml.document.diff.align

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget
import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.node.XmlNode
import com.davils.kxml.core.node.xmlText
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.navigation.NodeKind
import com.davils.kxml.document.patch.XmlPatchOperation
import com.davils.kxml.document.patch.XmlPatchPosition
import com.davils.kxml.document.patch.XmlPatchWhitespace
import com.davils.kxml.document.patch.apply.prefixOf

private const val ROOT_SELECTOR: String = "/*"
private const val ATTRIBUTE_PREFIX: String = "p0"
private const val SEPARATOR: Char = '\u0000'

private val replaceable: Set<NodeKind> =
    setOf(NodeKind.Element, NodeKind.Text, NodeKind.Comment, NodeKind.ProcessingInstruction)

internal class DiffWalk(
    private val old: DocumentModel,
    private val new: DocumentModel,
    private val source: NodeSource,
    private val budget: WorkBudget,
) {
    private val operations: MutableList<XmlPatchOperation> = mutableListOf()

    fun operations(): List<XmlPatchOperation> {
        val oldTop = old.children(DocumentModel.ROOT).toList()
        val newTop = new.children(DocumentModel.ROOT).toList()
        val top = Siblings(parent = "", old = oldTop, new = newTop)
        val oldRoot = oldTop.indexOfFirst(old::isElement)
        val newRoot = newTop.indexOfFirst(new::isElement)
        val epilog = oldRoot + 1..<oldTop.size
        aligned(siblings = top, oldRange = epilog, newRange = newRoot + 1..<newTop.size, isStrict = true)
        val isSameRoot = keyOf(old, oldTop[oldRoot]) == keyOf(new, newTop[newRoot])
        if (isSameRoot) element(selector = ROOT_SELECTOR, oldElement = oldTop[oldRoot], newElement = newTop[newRoot])
        if (!isSameRoot) operations += XmlPatchOperation.Replace(ROOT_SELECTOR, listOf(source.nodeAt(newTop[newRoot])))
        aligned(siblings = top, oldRange = 0..<oldRoot, newRange = 0..<newRoot, isStrict = true)
        return operations
    }

    private fun aligned(siblings: Siblings, oldRange: IntRange, newRange: IntRange, isStrict: Boolean): Boolean {
        val oldKeys = oldRange.map { index -> keyOf(old, siblings.old[index]) }
        val newKeys = newRange.map { index -> keyOf(new, siblings.new[index]) }
        val cost = Alignment.middleCells(old = oldKeys, new = newKeys) + oldKeys.size + newKeys.size
        if (!isStrict && cost > budget.remaining) return false
        budget.charge(cost)
        val matches = Alignment.matches(old = oldKeys, new = newKeys).map { (left, right) ->
            left + oldRange.first to right + newRange.first
        }
        val first = listOf(oldRange.first - 1 to newRange.first - 1)
        val last = listOf(oldRange.last + 1 to newRange.last + 1)
        val bounds = first + matches + last
        val gaps = bounds.indices.reversed().drop(1)
        return gaps.all { gap ->
            val (leftOld, leftNew) = bounds[gap]
            val (rightOld, rightNew) = bounds[gap + 1]
            val oldGap = leftOld + 1..<rightOld
            val isGapAligned = gapAligned(siblings, oldRange = oldGap, newRange = leftNew + 1..<rightNew)
            val isElement = isGapAligned && leftOld >= oldRange.first && old.isElement(siblings.old[leftOld])
            if (isElement) {
                element(
                    selector = siblings.selectorOf(leftOld),
                    oldElement = siblings.old[leftOld],
                    newElement = siblings.new[leftNew],
                )
            }
            isGapAligned
        }
    }

    private fun element(selector: String, oldElement: Int, newElement: Int) {
        budget.charge()
        val start = operations.size
        val isSameScope = declarationsOf(old, oldElement) == declarationsOf(new, newElement)
        if (isSameScope) attributes(selector, oldElement, newElement)
        val oldChildren = old.children(oldElement).toList()
        val newChildren = new.children(newElement).toList()
        val siblings = Siblings(parent = selector, old = oldChildren, new = newChildren)
        val isAligned = isSameScope && childrenAligned(siblings)
        if (isAligned) return
        while (operations.size > start) operations.removeLast()
        operations += XmlPatchOperation.Replace(selector, listOf(source.nodeAt(newElement)))
    }

    private fun childrenAligned(siblings: Siblings): Boolean =
        aligned(siblings = siblings, oldRange = siblings.old.indices, newRange = siblings.new.indices, isStrict = false)

    private fun attributes(selector: String, oldElement: Int, newElement: Int) {
        val before = old.attributes(oldElement).associateBy { attribute -> expandedOf(old.attributeName(attribute)) }
        val after = new.attributes(newElement).associateBy { attribute -> expandedOf(new.attributeName(attribute)) }
        budget.charge((before.size + after.size).toLong())
        before.forEach { (key, attribute) ->
            val (written, namespaces) = writtenOf(old.attributeName(attribute))
            val target = "$selector/@$written"
            val replacement = after[key]?.let(new::value)
            if (replacement == null) operations += XmlPatchOperation.Remove(target, namespaces = namespaces)
            if (replacement != null && replacement != old.value(attribute)) {
                val values = listOf(replacement).filter(String::isNotEmpty)
                operations += XmlPatchOperation.Replace(
                    selector = target,
                    content = values.map(::xmlText),
                    namespaces = namespaces,
                )
            }
        }
        after.filterKeys { key -> key !in before }.values.forEach { attribute ->
            val (written, namespaces) = writtenOf(new.attributeName(attribute))
            operations += XmlPatchOperation.AddAttribute(
                selector = selector,
                name = written,
                value = new.value(attribute).orEmpty(),
                namespaces = namespaces,
            )
        }
    }

    private fun gapAligned(siblings: Siblings, oldRange: IntRange, newRange: IntRange): Boolean {
        val isOneSided = oldRange.isEmpty() || newRange.isEmpty()
        if (isOneSided) return removed(siblings, oldRange) && inserted(siblings, oldRange, newRange)
        return replacedSingle(siblings, oldRange, newRange) || replacedPlain(siblings, oldRange, newRange)
    }

    private fun removed(siblings: Siblings, range: IntRange): Boolean {
        var index = range.last
        var isRemovable = true
        while (isRemovable && index >= range.first) {
            val isMerging = !isText(siblings, index) && isText(siblings, index - 1) && isText(siblings, range.last + 1)
            val isPrevious = isMerging && index - 1 >= range.first && isBlank(siblings, index - 1)
            isRemovable = !isMerging || isPrevious
            val whitespace = XmlPatchWhitespace.Before.takeIf { isPrevious } ?: XmlPatchWhitespace.None
            if (isRemovable) operations += XmlPatchOperation.Remove(siblings.selectorOf(index), whitespace)
            budget.charge()
            index -= 2.takeIf { isPrevious } ?: 1
        }
        return isRemovable
    }

    private fun inserted(siblings: Siblings, oldRange: IntRange, newRange: IntRange): Boolean {
        if (newRange.isEmpty()) return true
        val content = newRange.map { index -> source.nodeAt(siblings.new[index]) }
        budget.charge(content.size.toLong())
        operations += anchored(siblings, content, after = oldRange.first - 1)
        return true
    }

    private fun anchored(siblings: Siblings, content: List<XmlNode>, after: Int): XmlPatchOperation {
        val isAfter = after >= 0
        val isPrepended = !isAfter && siblings.parent.isNotEmpty()
        val position = XmlPatchPosition.After.takeIf { isAfter }
            ?: XmlPatchPosition.Prepend.takeIf { isPrepended }
            ?: XmlPatchPosition.Before
        val anchor = after.takeIf { isAfter } ?: after.inc()
        val selector = siblings.parent.takeIf { isPrepended } ?: siblings.selectorOf(anchor)
        return XmlPatchOperation.Add(selector, content, position)
    }

    private fun replacedSingle(siblings: Siblings, oldRange: IntRange, newRange: IntRange): Boolean {
        val isSingle = oldRange.first == oldRange.last && newRange.first == newRange.last
        val oldNode = siblings.old[oldRange.first]
        val newNode = siblings.new[newRange.first]
        val kind = old.kind(oldNode)
        val isReplaceable = isSingle && kind == new.kind(newNode) && kind in replaceable
        if (isReplaceable) {
            operations += XmlPatchOperation.Replace(siblings.selectorOf(oldRange.first), listOf(source.nodeAt(newNode)))
        }
        return isReplaceable
    }

    private fun replacedPlain(siblings: Siblings, oldRange: IntRange, newRange: IntRange): Boolean {
        val touched = oldRange.first - 1..oldRange.last + 1
        val hasOldText = touched.any { index -> isText(siblings, index) }
        val hasNewText = newRange.any { index -> new.kind(siblings.new[index]) == NodeKind.Text }
        if (hasOldText || hasNewText) return false
        return removed(siblings, oldRange) && inserted(siblings, oldRange, newRange)
    }

    private fun isText(siblings: Siblings, index: Int): Boolean =
        index in siblings.old.indices && old.kind(siblings.old[index]) == NodeKind.Text

    private fun isBlank(siblings: Siblings, index: Int): Boolean {
        val value = old.value(siblings.old[index]).orEmpty()
        return value.all(XmlCharacters::isWhitespace)
    }

    private fun keyOf(model: DocumentModel, node: Int): String {
        val kind = model.kind(node)
        val written = model.name(node)?.let(::expandedOf)
        val name = written.orEmpty()
        val value = model.value(node).takeIf { kind != NodeKind.Element }
        return "${kind.ordinal}$name$SEPARATOR${value.orEmpty()}"
    }

    private fun expandedOf(name: XmlName): String = "{${name.namespace.orEmpty()}}${name.local}"

    private fun declarationsOf(model: DocumentModel, element: Int): Set<Pair<String, String>> =
        model.declaredNamespaces(element).mapTo(mutableSetOf()) { binding -> prefixOf(binding) to binding.uri }

    private fun writtenOf(name: XmlName): Pair<String, Map<String, String>> {
        val namespace = name.namespace ?: return name.local to emptyMap()
        val isXml = namespace == XmlNamespaces.XML
        val prefix = XmlNamespaces.XML_PREFIX.takeIf { isXml } ?: ATTRIBUTE_PREFIX
        val declared = mapOf(ATTRIBUTE_PREFIX to namespace)
        val namespaces = declared.takeUnless { isXml }.orEmpty()
        return "$prefix:${name.local}" to namespaces
    }
}
