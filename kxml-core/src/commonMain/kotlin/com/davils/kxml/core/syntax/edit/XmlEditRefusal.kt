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

package com.davils.kxml.core.syntax.edit

/**
 * Why an [XmlEditor] refused an edit.
 *
 * @since 1.0.0
 */
public enum class XmlEditRefusal {
    /**
     * The tree has errors, so its elements and ranges cannot be trusted to edit.
     *
     * @since 1.0.0
     */
    InvalidDocument,

    /**
     * The element does not belong to the tree of the editor.
     *
     * @since 1.0.0
     */
    ForeignElement,

    /**
     * The name is no qualified XML name, or an element name has the reserved prefix `xmlns`.
     *
     * @since 1.0.0
     */
    InvalidName,

    /**
     * The prefix of the name is not declared in the scope of the element.
     *
     * @since 1.0.0
     */
    UndeclaredPrefix,

    /**
     * The attribute is a namespace declaration, `xmlns` or `xmlns:prefix`, whose change would move other names into
     * another namespace.
     *
     * @since 1.0.0
     */
    NamespaceDeclaration,

    /**
     * Another attribute of the element has the same namespace and local name.
     *
     * @since 1.0.0
     */
    DuplicateAttribute,

    /**
     * The element has no attribute of that name.
     *
     * @since 1.0.0
     */
    MissingAttribute,

    /**
     * The text holds a character that the version of the document cannot represent, neither as it is nor as a
     * character reference, such as NUL or a lone surrogate.
     *
     * @since 1.0.0
     */
    InvalidCharacter,

    /**
     * The root element cannot be removed.
     *
     * @since 1.0.0
     */
    RootElement,
}
