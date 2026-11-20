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
package com.davils.kxml.document.xpath

/**
 * The seven kinds of nodes of the data model of XPath 1.0, section 5.
 *
 * @since 1.0.0
 */
public enum class XPathNodeKind {
    /**
     * The root node, the parent of the document element and of the comments and processing instructions around it.
     *
     * @since 1.0.0
     */
    Root,

    /**
     * An element.
     *
     * @since 1.0.0
     */
    Element,

    /**
     * An attribute of an element; namespace declarations are namespace nodes instead.
     *
     * @since 1.0.0
     */
    Attribute,

    /**
     * A namespace in scope on an element, the namespace of the prefix `xml` included.
     *
     * @since 1.0.0
     */
    Namespace,

    /**
     * The text between two other nodes; adjacent text and CDATA sections form one text node.
     *
     * @since 1.0.0
     */
    Text,

    /**
     * A comment.
     *
     * @since 1.0.0
     */
    Comment,

    /**
     * A processing instruction.
     *
     * @since 1.0.0
     */
    ProcessingInstruction,
}
