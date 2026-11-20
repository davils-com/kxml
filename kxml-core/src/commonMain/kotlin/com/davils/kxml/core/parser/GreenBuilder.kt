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

package com.davils.kxml.core.parser

import com.davils.kxml.core.syntax.XmlSyntaxKind
import com.davils.kxml.core.syntax.internal.GreenElement
import com.davils.kxml.core.syntax.internal.GreenNode
import com.davils.kxml.core.syntax.internal.GreenToken

private const val INTERNED_LENGTH: Int = 16

internal class GreenBuilder(private val text: String) : SyntaxSink {
    private val children = ArrayList<GreenElement>()
    private val parents = ArrayList<Pair<XmlSyntaxKind, Int>>()
    private val interned = TokenTable()
    private var emitted: Int = 0

    override fun token(kind: XmlSyntaxKind, start: Int, end: Int) {
        if (end <= start) return
        check(start == emitted) { "A token must start where the last one ended" }
        children.add(intern(GreenToken(kind, text.substring(startIndex = start, endIndex = end))))
        emitted = end
    }

    override fun startNode(kind: XmlSyntaxKind) {
        parents.add(kind to children.size)
    }

    override fun finishNode() {
        val (kind, first) = parents.removeAt(parents.lastIndex)
        val node = GreenNode(kind, children.subList(first, children.size).toList())
        while (children.size > first) children.removeAt(children.lastIndex)
        children.add(node)
    }

    fun finish(): GreenNode {
        token(kind = XmlSyntaxKind.ERROR, start = emitted, end = text.length)
        while (parents.isNotEmpty()) finishNode()
        return children.single() as GreenNode
    }

    private fun intern(token: GreenToken): GreenToken {
        if (token.length > INTERNED_LENGTH) return token
        return interned.intern(token)
    }
}
