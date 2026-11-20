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

package com.davils.kxml.core.syntax.internal

import com.davils.kxml.core.syntax.XmlSyntaxKind

internal class GreenNode(
    override val kind: XmlSyntaxKind,
    override val children: List<GreenElement>,
) : GreenElement {
    init {
        require(!kind.isToken) { "A green node needs a node kind, but $kind is a token kind" }
    }

    override val length: Int = children.sumOf(GreenElement::length)

    internal val identity: Int get() = super.hashCode()

    override val text: String
        get() {
            val target = StringBuilder(length)
            appendTextTo(target)
            return target.toString()
        }

    override fun appendTextTo(target: StringBuilder) {
        GreenWalk.forEachToken(this) { token -> token.appendTextTo(target) }
    }

    override fun equals(other: Any?): Boolean = other is GreenNode && GreenWalk.isSame(first = this, second = other)

    override fun hashCode(): Int = GreenWalk.hashOf(this)

    override fun toString(): String = "$kind(${children.size} children, $length)"
}
