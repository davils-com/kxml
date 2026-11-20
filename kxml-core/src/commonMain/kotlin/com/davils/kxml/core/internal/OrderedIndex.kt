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

package com.davils.kxml.core.internal

internal class OrderedIndex<K : Comparable<K>> {
    private var root: Node<K>? = null

    var size: Int = 0
        private set

    fun ordinalOf(key: K): Int? {
        var current = root
        while (current != null) {
            val order = key.compareTo(current.key)
            if (order == 0) return current.ordinal
            current = childToward(current, order)
        }
        return null
    }

    fun ordinalFor(key: K): Int {
        val found = ordinalOf(key)
        if (found != null) return found
        root = inserted(root, key)
        size++
        return size - 1
    }

    private fun inserted(node: Node<K>?, key: K): Node<K> {
        if (node == null) return Node(key, size)
        val order = key.compareTo(node.key)
        if (order < 0) node.left = inserted(node.left, key)
        if (order > 0) node.right = inserted(node.right, key)
        return balanced(node)
    }

    private fun childToward(node: Node<K>, order: Int): Node<K>? {
        if (order < 0) return node.left
        return node.right
    }

    private fun balanced(node: Node<K>): Node<K> {
        node.measure()
        if (node.balance > 1) return leftHeavyFixed(node)
        return rightHeavyFixed(node)
    }

    private fun leftHeavyFixed(node: Node<K>): Node<K> {
        val left = checkNotNull(node.left)
        if (left.balance < 0) node.left = rotatedLeft(left)
        return rotatedRight(node)
    }

    private fun rightHeavyFixed(node: Node<K>): Node<K> {
        if (node.balance >= -1) return node
        val right = checkNotNull(node.right)
        if (right.balance > 0) node.right = rotatedRight(right)
        return rotatedLeft(node)
    }

    private fun rotatedRight(node: Node<K>): Node<K> {
        val pivot = checkNotNull(node.left)
        node.left = pivot.right
        pivot.right = node
        node.measure()
        pivot.measure()
        return pivot
    }

    private fun rotatedLeft(node: Node<K>): Node<K> {
        val pivot = checkNotNull(node.right)
        node.right = pivot.left
        pivot.left = node
        node.measure()
        pivot.measure()
        return pivot
    }

    private class Node<K>(val key: K, val ordinal: Int) {
        var left: Node<K>? = null

        var right: Node<K>? = null

        var height: Int = 1
            private set

        val balance: Int get() = heightOf(left) - heightOf(right)

        fun measure() {
            height = maxOf(a = heightOf(left), b = heightOf(right)) + 1
        }

        private fun heightOf(node: Node<K>?): Int = node?.height ?: 0
    }
}
