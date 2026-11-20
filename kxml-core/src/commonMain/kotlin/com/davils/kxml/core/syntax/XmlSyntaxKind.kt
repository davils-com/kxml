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

/**
 * The kind of an [XmlSyntaxElement]: a token kind for the pieces of text the reader reads, or a node kind for the
 * constructs of the XML grammar they make up.
 *
 * @property isToken whether elements of this kind are [XmlSyntaxToken]s rather than [XmlSyntaxNode]s.
 * @property isTrivia whether tokens of this kind carry no content: whitespace between markup and byte order marks.
 * @since 1.0.0
 */
public enum class XmlSyntaxKind(public val isToken: Boolean, public val isTrivia: Boolean = false) {
    /**
     * A byte order mark at the start of the text.
     *
     * @since 1.0.0
     */
    BYTE_ORDER_MARK(isToken = true, isTrivia = true),

    /**
     * Whitespace between markup outside the root element and inside tags and declarations.
     *
     * @since 1.0.0
     */
    WHITESPACE(isToken = true, isTrivia = true),

    /**
     * A run of character data inside an element, line breaks and whitespace included.
     *
     * @since 1.0.0
     */
    TEXT(isToken = true),

    /**
     * A character reference such as `&#60;` or `&#x3C;`.
     *
     * @since 1.0.0
     */
    CHARACTER_REFERENCE(isToken = true),

    /**
     * An entity reference such as `&lt;`.
     *
     * @since 1.0.0
     */
    ENTITY_REFERENCE(isToken = true),

    /**
     * The `<` that opens a start tag.
     *
     * @since 1.0.0
     */
    LESS_THAN(isToken = true),

    /**
     * The `</` that opens an end tag.
     *
     * @since 1.0.0
     */
    END_TAG_OPEN(isToken = true),

    /**
     * The `>` that closes a tag or a document type declaration.
     *
     * @since 1.0.0
     */
    GREATER_THAN(isToken = true),

    /**
     * The `/>` that closes an empty element.
     *
     * @since 1.0.0
     */
    EMPTY_TAG_CLOSE(isToken = true),

    /**
     * A name: of an element, an attribute, a target of a processing instruction or `xml` of the XML declaration.
     *
     * @since 1.0.0
     */
    NAME(isToken = true),

    /**
     * The `=` between the name and the value of an attribute.
     *
     * @since 1.0.0
     */
    EQUALS(isToken = true),

    /**
     * A quote that opens or closes the value of an attribute.
     *
     * @since 1.0.0
     */
    QUOTE(isToken = true),

    /**
     * A run of literal text inside the value of an attribute.
     *
     * @since 1.0.0
     */
    ATTRIBUTE_TEXT(isToken = true),

    /**
     * The `<!--` that opens a comment.
     *
     * @since 1.0.0
     */
    COMMENT_OPEN(isToken = true),

    /**
     * The text of a comment.
     *
     * @since 1.0.0
     */
    COMMENT_TEXT(isToken = true),

    /**
     * The `-->` that closes a comment.
     *
     * @since 1.0.0
     */
    COMMENT_CLOSE(isToken = true),

    /**
     * The `<![CDATA[` that opens a CDATA section.
     *
     * @since 1.0.0
     */
    CDATA_OPEN(isToken = true),

    /**
     * The text of a CDATA section.
     *
     * @since 1.0.0
     */
    CDATA_TEXT(isToken = true),

    /**
     * The `]]>` that closes a CDATA section.
     *
     * @since 1.0.0
     */
    CDATA_CLOSE(isToken = true),

    /**
     * The `<?` that opens a processing instruction or the XML declaration.
     *
     * @since 1.0.0
     */
    INSTRUCTION_OPEN(isToken = true),

    /**
     * The data of a processing instruction.
     *
     * @since 1.0.0
     */
    INSTRUCTION_TEXT(isToken = true),

    /**
     * The `?>` that closes a processing instruction or the XML declaration.
     *
     * @since 1.0.0
     */
    INSTRUCTION_CLOSE(isToken = true),

    /**
     * The `<!DOCTYPE` that opens a document type declaration.
     *
     * @since 1.0.0
     */
    DOCTYPE_OPEN(isToken = true),

    /**
     * The text of a document type declaration between its keyword and its closing `>`.
     *
     * @since 1.0.0
     */
    DOCTYPE_TEXT(isToken = true),

    /**
     * Text the reader could not read: everything from the first error to the end of the text.
     *
     * @since 1.0.0
     */
    ERROR(isToken = true),

    /**
     * The whole text: the XML declaration, the prolog, the root element and the epilog.
     *
     * @since 1.0.0
     */
    DOCUMENT(isToken = false),

    /**
     * The XML declaration, such as `<?xml version="1.0"?>`.
     *
     * @since 1.0.0
     */
    XML_DECLARATION(isToken = false),

    /**
     * A document type declaration.
     *
     * @since 1.0.0
     */
    DOCTYPE(isToken = false),

    /**
     * An element: its start tag, its content and its end tag, or an empty element tag.
     *
     * @since 1.0.0
     */
    ELEMENT(isToken = false),

    /**
     * A start tag or an empty element tag with its attributes.
     *
     * @since 1.0.0
     */
    START_TAG(isToken = false),

    /**
     * An end tag.
     *
     * @since 1.0.0
     */
    END_TAG(isToken = false),

    /**
     * An attribute, or a pseudo attribute of the XML declaration: its name, `=` and its value.
     *
     * @since 1.0.0
     */
    ATTRIBUTE(isToken = false),

    /**
     * The quoted value of an attribute with its quotes, text and references.
     *
     * @since 1.0.0
     */
    ATTRIBUTE_VALUE(isToken = false),

    /**
     * A comment.
     *
     * @since 1.0.0
     */
    COMMENT(isToken = false),

    /**
     * A CDATA section.
     *
     * @since 1.0.0
     */
    CDATA_SECTION(isToken = false),

    /**
     * A processing instruction.
     *
     * @since 1.0.0
     */
    PROCESSING_INSTRUCTION(isToken = false),
}
