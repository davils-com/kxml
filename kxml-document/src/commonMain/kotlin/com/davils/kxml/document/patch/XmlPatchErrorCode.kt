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
package com.davils.kxml.document.patch

/**
 * The error conditions of section 5.1 of RFC 5261, each by the name of its error element.
 *
 * @property element the local name of the error element in the namespace `urn:ietf:params:xml:ns:patch-ops-error`.
 * @since 1.0.0
 */
public enum class XmlPatchErrorCode(public val element: String) {
    /**
     * A `sel`, `type`, `ws` or `pos` attribute or the value of an added attribute is not allowed.
     *
     * @since 1.0.0
     */
    InvalidAttributeValue("invalid-attribute-value"),

    /**
     * The diff is no patch document of RFC 5261: an operation is unknown or misses its `sel` attribute.
     *
     * @since 1.0.0
     */
    InvalidDiffFormat("invalid-diff-format"),

    /**
     * A prefix is not declared, a namespace declaration is not on the located element or is still in use, or an
     * added one exists already.
     *
     * @since 1.0.0
     */
    InvalidNamespacePrefix("invalid-namespace-prefix"),

    /**
     * A namespace URI is empty.
     *
     * @since 1.0.0
     */
    InvalidNamespaceUri("invalid-namespace-uri"),

    /**
     * The content does not fit the located node, such as text where a `<replace>` needs an element.
     *
     * @since 1.0.0
     */
    InvalidNodeTypes("invalid-node-types"),

    /**
     * The operation would remove the document element or give it a sibling element.
     *
     * @since 1.0.0
     */
    InvalidRootElementOperation("invalid-root-element-operation"),

    /**
     * The operation would put text outside the document element or change the root node.
     *
     * @since 1.0.0
     */
    InvalidXmlPrologOperation("invalid-xml-prolog-operation"),

    /**
     * A `ws` directive names a sibling that is no white space text node.
     *
     * @since 1.0.0
     */
    InvalidWhitespaceDirective("invalid-whitespace-directive"),

    /**
     * The `sel` attribute locates no node or more than one.
     *
     * @since 1.0.0
     */
    UnlocatedNode("unlocated-node"),
}
