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

package com.davils.kxml.core.syntax

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.syntax.internal.GreenToken

/**
 * A token of an [XmlSyntaxTree]: a piece of the source text, such as a name, a quote, a run of text or whitespace,
 * that the parser does not divide any further.
 *
 * Tokens are immutable views of the tree and safe to share between threads. Two tokens are equal when they stand for
 * the same token at the same offset of the same parse; a token of another parse of the same text is not equal.
 *
 * @since 1.0.0
 */
public class XmlSyntaxToken internal constructor(
    private val green: GreenToken,
    private val offset: Int,
    override val parent: XmlSyntaxNode?,
) : XmlSyntaxElement {
    override val kind: XmlSyntaxKind get() = green.kind

    override val range: TextRange get() = TextRange.of(start = offset, length = green.length)

    override val text: String get() = green.text

    override fun equals(other: Any?): Boolean =
        other is XmlSyntaxToken && offset == other.offset && green === other.green

    override fun hashCode(): Int = green.identity * HASH_FACTOR + offset

    override fun toString(): String = "$kind@$range"

    private companion object {
        const val HASH_FACTOR: Int = 31
    }
}
