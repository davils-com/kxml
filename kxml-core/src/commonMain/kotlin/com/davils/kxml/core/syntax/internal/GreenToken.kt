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

internal class GreenToken(
    override val kind: XmlSyntaxKind,
    override val text: String,
) : GreenElement {
    init {
        require(kind.isToken) { "A green token needs a token kind, but $kind is a node kind" }
    }

    override val length: Int get() = text.length

    override val children: List<GreenElement> get() = emptyList()

    internal val identity: Int get() = super.hashCode()

    override fun appendTextTo(target: StringBuilder) {
        target.append(text)
    }

    override fun equals(other: Any?): Boolean = other is GreenToken && kind == other.kind && text == other.text

    override fun hashCode(): Int = HASH_FACTOR * kind.hashCode() + text.hashCode()

    override fun toString(): String = "$kind(${text.length})"

    private companion object {
        const val HASH_FACTOR: Int = 31
    }
}
