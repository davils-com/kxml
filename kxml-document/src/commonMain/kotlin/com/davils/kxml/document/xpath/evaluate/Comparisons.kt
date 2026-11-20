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

package com.davils.kxml.document.xpath.evaluate

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet
import com.davils.kxml.document.xpath.syntax.Relation

internal class Comparisons(
    private val conversions: Conversions,
    private val nodes: NodeAccess,
    private val bounds: EvaluationBounds,
) {
    fun holds(left: Value, relation: Relation, right: Value): Boolean {
        val leftKeys = (left as? Value.Nodes)?.keys
        val rightKeys = (right as? Value.Nodes)?.keys
        if (leftKeys != null && rightKeys != null) return between(leftKeys, relation, rightKeys)
        return leftKeys?.let { keys -> withNodes(keys, relation, right) }
            ?: rightKeys?.let { keys -> withNodes(keys, relation.mirrored, left) }
            ?: scalars(left, relation, right)
    }

    private fun withNodes(keys: LongArray, relation: Relation, other: Value): Boolean {
        if (other is Value.Truth) return scalars(Value.Truth(keys.isNotEmpty()), relation, other)
        return keys.any { key -> scalars(Value.Text(valueOf(key)), relation, other) }
    }

    private fun scalars(left: Value, relation: Relation, right: Value): Boolean {
        val isNumeric = !relation.isEquality || isNumeric(left, right)
        if (isNumeric) return relation.holds(left = conversions.numberOf(left), right = conversions.numberOf(right))
        val isEqual = equalTruths(left, right) ?: equalTexts(left, right)
        return isEqual == (relation == Relation.Equal)
    }

    private fun isNumeric(left: Value, right: Value): Boolean {
        val hasTruth = left is Value.Truth || right is Value.Truth
        val hasNumber = left is Value.Number || right is Value.Number
        return !hasTruth && hasNumber
    }

    private fun equalTexts(left: Value, right: Value): Boolean =
        conversions.stringOf(left) == conversions.stringOf(right)

    private fun equalTruths(left: Value, right: Value): Boolean? {
        val hasTruth = left is Value.Truth || right is Value.Truth
        if (!hasTruth) return null
        return conversions.truthOf(left) == conversions.truthOf(right)
    }

    private fun between(left: LongArray, relation: Relation, right: LongArray): Boolean {
        if (left.isEmpty() || right.isEmpty()) return false
        return when (relation) {
            Relation.Equal -> {
                shareValue(left, right)
            }

            Relation.NotEqual -> {
                differ(left, right)
            }

            Relation.Less, Relation.LessOrEqual, Relation.Greater, Relation.GreaterOrEqual -> {
                ordered(left, relation, right)
            }
        }
    }

    private fun ordered(left: LongArray, relation: Relation, right: LongArray): Boolean {
        val leftNumbers = numbersOf(left)
        val rightNumbers = numbersOf(right)
        if (leftNumbers.isEmpty() || rightNumbers.isEmpty()) return false
        val leftBound = leftNumbers.min().takeIf { relation.isLess } ?: leftNumbers.max()
        val rightBound = rightNumbers.max().takeIf { relation.isLess } ?: rightNumbers.min()
        return relation.holds(left = leftBound, right = rightBound)
    }

    private fun shareValue(left: LongArray, right: LongArray): Boolean {
        val values = OrderedSet<String>()
        left.forEach { key -> values.add(valueOf(key)) }
        return right.any { key -> valueOf(key) in values }
    }

    private fun differ(left: LongArray, right: LongArray): Boolean {
        val first = valueOf(left.first())
        val hasOther = left.any { key -> valueOf(key) != first }
        if (hasOther) return true
        return right.any { key -> valueOf(key) != first }
    }

    private fun numbersOf(keys: LongArray): DoubleArray {
        val numbers = keys.map { key -> numberIn(valueOf(key)) }
        return numbers.filterNot(Double::isNaN).toDoubleArray()
    }

    private fun valueOf(key: Long): String {
        bounds.charge()
        return nodes.stringValueOf(key)
    }
}
