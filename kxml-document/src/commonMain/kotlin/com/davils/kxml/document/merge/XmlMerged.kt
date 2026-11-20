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

package com.davils.kxml.document.merge

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.collection.immutableList
import com.davils.kxml.core.node.XmlDocument

private const val STEP: Char = '/'

/**
 * The outcome of a merge: the merged document and the layer every node of it comes from.
 *
 * The origins are found by comparison, never by hash code, so paths an attacker chose to collide cannot make a lookup
 * slow.
 *
 * @param document the merged document.
 * @param origins the layer of every node, by the path of the node.
 * @since 1.0.0
 */
public class XmlMerged internal constructor(document: XmlDocument, private val origins: OrderedMap<String, String>) {
    /**
     * The merged document.
     *
     * @since 1.0.0
     */
    public val document: XmlDocument = document

    /**
     * The path of every element, attribute, text, comment and processing instruction of [document], in document
     * order. A path names every step by its qualified name or its node test and its position among the siblings of
     * that step, such as `/config[1]/server[2]/@port` or `/config[1]/text()[1]`.
     *
     * @since 1.0.0
     */
    public val paths: List<String> = origins.keys.immutableList()

    /**
     * Returns the name of the layer [path] comes from, or the one of its nearest ancestor among [paths]. The layer of
     * an element is the one that created it or last replaced its content, and the one of an attribute the one that last
     * set it.
     *
     * @param path a path in the form of [paths].
     * @return the name of the layer, or `null` if neither the path nor any ancestor of it is a node of [document].
     * @since 1.0.0
     */
    public fun originOf(path: String): String? {
        var current = path
        var origin = origins[current]
        while (origin == null && current.lastIndexOf(STEP) > 0) {
            current = current.substringBeforeLast(STEP)
            origin = origins[current]
        }
        return origin
    }

    /**
     * Returns the number of paths, such as `XmlMerged(paths=12)`, without the document.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlMerged(paths=${paths.size})"
}
