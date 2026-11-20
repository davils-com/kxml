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

/**
 * An element of an [XmlSyntaxTree]: either an [XmlSyntaxNode] that groups other elements or an [XmlSyntaxToken] that
 * holds a piece of the source text.
 *
 * Elements are positioned views of the immutable tree: they compare equal when they stand for the same element at
 * the same offset of the same parse, and they are safe to share between threads.
 *
 * @since 1.0.0
 */
public sealed interface XmlSyntaxElement {
    /**
     * The kind of this element; [XmlSyntaxKind.isToken] tells whether it is a token or a node.
     *
     * @since 1.0.0
     */
    public val kind: XmlSyntaxKind

    /**
     * The range of the source text this element covers, as UTF-16 offsets into [XmlSyntaxTree.text].
     *
     * @since 1.0.0
     */
    public val range: TextRange

    /**
     * The node this element belongs to, or `null` for the root of the tree.
     *
     * @since 1.0.0
     */
    public val parent: XmlSyntaxNode?

    /**
     * The source text this element covers, exactly as it was parsed: whitespace, line breaks and references included.
     *
     * The text comes from the input unchanged; escape it before it is shown, for example with `SafeText` of Kore.
     *
     * @since 1.0.0
     */
    public val text: String

    /**
     * The nodes that contain this element, from its [parent] up to the root of the tree.
     *
     * @since 1.0.0
     */
    public val ancestors: Sequence<XmlSyntaxNode> get() = generateSequence(parent) { it.parent }
}
