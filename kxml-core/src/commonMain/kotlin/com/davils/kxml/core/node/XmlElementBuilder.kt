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

package com.davils.kxml.core.node

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.name.XmlNamespaceBinding

/**
 * Collects the attributes, namespace declarations and children of an element; used through [xmlElement].
 *
 * Every function checks its arguments at once and throws on an invalid one. The builder is not thread safe.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class XmlElementBuilder internal constructor(private val name: XmlName) {
    private val attributes = mutableListOf<XmlAttribute>()

    private val children = mutableListOf<XmlNode>()

    private val namespaces = mutableListOf<XmlNamespaceBinding>()

    /**
     * Adds an attribute.
     *
     * @param name the name of the attribute.
     * @param value the value; the writer escapes it.
     * @throws IllegalArgumentException if the element has an attribute of that name already or [value] holds a
     *   character XML cannot carry.
     * @since 1.0.0
     */
    public fun attribute(name: XmlName, value: String) {
        require(attributes.none { attribute -> attribute.name == name }) { "The attribute $name is set twice" }
        require(XmlCharacters.isText(value)) { "The attribute $name holds characters XML cannot carry" }
        attributes += XmlAttribute(name, value, range = null)
    }

    /**
     * Declares a namespace on the element, so that the writer uses [prefix] for [uri].
     *
     * @param prefix the prefix, or `null` for the default namespace.
     * @param uri the namespace name.
     * @throws IllegalArgumentException if [prefix] is not a name without a colon or is bound to an empty [uri].
     * @since 1.0.0
     */
    public fun namespace(prefix: String?, uri: String) {
        namespaces += XmlNamespaceBinding(prefix, uri)
    }

    /**
     * Adds a child element.
     *
     * @param name the name of the child.
     * @param configure adds the attributes and children of the child.
     * @since 1.0.0
     */
    public fun element(name: XmlName, configure: XmlElementBuilder.() -> Unit = {}) {
        children += xmlElement(name, configure)
    }

    /**
     * Adds a text node, see [xmlText].
     *
     * @param value the text.
     * @throws IllegalArgumentException if [value] holds a character XML cannot carry.
     * @since 1.0.0
     */
    public fun text(value: String) {
        children += xmlText(value)
    }

    /**
     * Adds a CDATA section, see [xmlCData].
     *
     * @param value the content.
     * @throws IllegalArgumentException if [value] is no valid content of a CDATA section.
     * @since 1.0.0
     */
    public fun cdata(value: String) {
        children += xmlCData(value)
    }

    /**
     * Adds a comment, see [xmlComment].
     *
     * @param value the content.
     * @throws IllegalArgumentException if [value] is no valid content of a comment.
     * @since 1.0.0
     */
    public fun comment(value: String) {
        children += xmlComment(value)
    }

    /**
     * Adds a processing instruction, see [xmlInstruction].
     *
     * @param target the target.
     * @param data the data; empty by default.
     * @throws IllegalArgumentException if [target] or [data] is invalid.
     * @since 1.0.0
     */
    public fun instruction(target: String, data: String = "") {
        children += xmlInstruction(target, data)
    }

    /**
     * Adds a node that was built before, such as an element of a parsed document.
     *
     * @param node the node.
     * @since 1.0.0
     */
    public fun node(node: XmlNode) {
        children += node
    }

    internal fun build(): XmlNode.Element = XmlNode.Element(
        name = name,
        attributes = attributes,
        children = children,
        namespaces = namespaces,
        range = null,
    )
}
