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

import com.davils.kore.core.text.TextEdit
import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.syntax.XmlSyntaxKind
import com.davils.kxml.core.syntax.XmlSyntaxTree
import com.davils.kxml.core.syntax.ast.XmlAttributeSyntax
import com.davils.kxml.core.syntax.ast.XmlElementSyntax

private const val NAMESPACE_PREFIX: String = "xmlns"
private const val PREFIX_SEPARATOR: Char = ':'
private const val DOUBLE_QUOTE: Char = '"'

/**
 * Edits the elements and attributes of an [XmlSyntaxTree] by small text edits, keeping every character of the
 * document it does not touch: comments, whitespace, the quotes of other attributes and every entity reference.
 *
 * Each operation takes an element of the [typed view][XmlSyntaxTree.document] of the same tree and returns either the
 * edited tree, together with the text edits that produced it, or the reason it refused. The original tree never
 * changes, so an editor is immutable and safe to share between threads; to make several edits, create an editor for
 * each result. An editor refuses every edit of a tree with errors.
 *
 * Nothing it writes can inject markup. A name must be a qualified XML name whose prefix is declared in the scope of
 * the element, and namespace declarations are never added, changed or removed, so no edit moves another name into
 * another namespace. Text and attribute values are escaped for the version of the document: `&`, `<` and `>` as
 * entity references, the quote of an attribute value, a carriage return, a tab or a line feed in a value and every
 * character that the version only allows as a reference as a character reference, so the edited document reads back
 * to exactly the text that was set. A character that the version cannot represent at all, such as NUL or a lone
 * surrogate, is refused.
 *
 * The edited text is parsed again through [XmlSyntaxTree.edit], with the configuration of the tree; an edit that makes
 * the text longer than `XmlLimits.maximumInputLength` yields a tree with an `InputTooLong` diagnostic.
 *
 * @param tree the tree to edit.
 * @since 1.0.0
 */
public class XmlEditor(private val tree: XmlSyntaxTree) {
    private val version: String get() = tree.document.version

    /**
     * Sets the attribute [name] of [element] to [value]: an existing attribute keeps its place and its quotes, a new
     * one is added after the other attributes in double quotes.
     *
     * @param element the element, from the typed view of the tree of this editor.
     * @param name the qualified name of the attribute; it must not declare a namespace.
     * @param value the value, which may contain any character the version of the document can represent.
     * @return the edited tree, or why the edit was refused.
     * @since 1.0.0
     */
    public fun setAttribute(element: XmlElementSyntax, name: String, value: String): XmlEditResult {
        val refusal = elementRefusal(element) ?: attributeNameRefusal(element, name)
        val existing = element.attribute(name)
        val quote = existing?.let(::quoteOf) ?: DOUBLE_QUOTE
        val escaped = escapedValue(value, version, quote)
        val edit = escaped?.let { text ->
            attributeEdit(element = element, existing = existing, name = name, value = text)
        }
        return result(refusal ?: XmlEditRefusal.InvalidCharacter.takeIf { escaped == null }, listOfNotNull(edit))
    }

    /**
     * Removes the attribute [name] of [element], together with the whitespace before it.
     *
     * @param element the element, from the typed view of the tree of this editor.
     * @param name the qualified name of the attribute; it must not declare a namespace.
     * @return the edited tree, or why the edit was refused.
     * @since 1.0.0
     */
    public fun removeAttribute(element: XmlElementSyntax, name: String): XmlEditResult {
        val existing = element.attribute(name)
        val refusal = elementRefusal(element)
            ?: XmlEditRefusal.NamespaceDeclaration.takeIf { isNamespaceName(name) }
            ?: XmlEditRefusal.MissingAttribute.takeIf { existing == null }
        return result(refusal, listOfNotNull(existing?.let { attribute -> removal(element, attribute) }))
    }

    /**
     * Replaces the content of [element] with [text]: its character data, its child elements, comments and
     * processing instructions. An empty-element tag is written as a start tag and an end tag.
     *
     * @param element the element, from the typed view of the tree of this editor.
     * @param text the new character data, which may contain any character the version of the document can represent.
     * @return the edited tree, or why the edit was refused.
     * @since 1.0.0
     */
    public fun setText(element: XmlElementSyntax, text: String): XmlEditResult {
        val escaped = escapedText(text, version)
        val refusal = elementRefusal(element) ?: XmlEditRefusal.InvalidCharacter.takeIf { escaped == null }
        return result(refusal, listOfNotNull(escaped?.let { content -> contentEdit(element, content) }))
    }

    /**
     * Appends an empty child element [name], `<name/>`, after the content of [parent].
     *
     * @param parent the element to append to, from the typed view of the tree of this editor.
     * @param name the qualified name of the new element.
     * @return the edited tree, or why the edit was refused.
     * @since 1.0.0
     */
    public fun appendElement(parent: XmlElementSyntax, name: String): XmlEditResult {
        val refusal = elementRefusal(parent) ?: elementNameRefusal(parent, name)
        return result(refusal, listOf(appending(parent, "<$name/>")))
    }

    /**
     * Removes [element] with all its content.
     *
     * @param element the element, from the typed view of the tree of this editor; not the root element.
     * @return the edited tree, or why the edit was refused.
     * @since 1.0.0
     */
    public fun removeElement(element: XmlElementSyntax): XmlEditResult {
        val refusal = elementRefusal(element) ?: XmlEditRefusal.RootElement.takeIf { element.parent == null }
        return result(refusal, listOf(TextEdit(element.node.range, "")))
    }

    /**
     * Renames [element] in its start tag and its end tag.
     *
     * @param element the element, from the typed view of the tree of this editor.
     * @param name the new qualified name; its prefix must be declared in the scope of the element.
     * @return the edited tree, or why the edit was refused.
     * @since 1.0.0
     */
    public fun rename(element: XmlElementSyntax, name: String): XmlEditResult {
        val refusal = elementRefusal(element) ?: elementNameRefusal(element, name)
        val names = listOfNotNull(element.startTag.token(XmlSyntaxKind.NAME), element.endTag?.token(XmlSyntaxKind.NAME))
        return result(refusal, names.map { token -> TextEdit(token.range, name) })
    }

    /**
     * Returns whether a tree is set, such as `XmlEditor(XmlSyntaxTree(120 characters, 0 diagnostics))`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlEditor($tree)"

    private fun result(refusal: XmlEditRefusal?, edits: List<TextEdit>): XmlEditResult {
        if (refusal != null) return XmlEditResult.Refused(refusal)
        val ordered = edits.sortedBy { edit -> edit.range.start }
        return XmlEditResult.Edited(ordered, tree.edit(ordered))
    }

    private fun elementRefusal(element: XmlElementSyntax): XmlEditRefusal? {
        val document = element.node.ancestors.lastOrNull()
        if (!tree.isValid) return XmlEditRefusal.InvalidDocument
        return XmlEditRefusal.ForeignElement.takeIf { document != tree.root }
    }

    private fun elementNameRefusal(scope: XmlElementSyntax, name: String): XmlEditRefusal? {
        val prefix = prefixOf(name)
        val isValid = XmlCharacters.isQualifiedName(name) && prefix != NAMESPACE_PREFIX
        if (!isValid) return XmlEditRefusal.InvalidName
        return XmlEditRefusal.UndeclaredPrefix.takeIf { prefix != null && scope.namespaceFor(prefix) == null }
    }

    private fun attributeNameRefusal(element: XmlElementSyntax, name: String): XmlEditRefusal? {
        val prefix = prefixOf(name)
        val namespace = prefix?.let(element::namespaceFor)
        val isDuplicate = element.attributes.any { attribute ->
            isSameExpandedName(element = element, attribute = attribute, name = name, namespace = namespace)
        }
        return XmlEditRefusal.InvalidName.takeUnless { XmlCharacters.isQualifiedName(name) }
            ?: XmlEditRefusal.NamespaceDeclaration.takeIf { isNamespaceName(name) }
            ?: XmlEditRefusal.UndeclaredPrefix.takeIf { prefix != null && namespace == null }
            ?: XmlEditRefusal.DuplicateAttribute.takeIf { isDuplicate }
    }

    private fun isSameExpandedName(
        element: XmlElementSyntax,
        attribute: XmlAttributeSyntax,
        name: String,
        namespace: String?,
    ): Boolean {
        val attributeNamespace = attribute.prefix?.let(element::namespaceFor)
        val isSameLocal = attribute.localName == name.substringAfter(PREFIX_SEPARATOR)
        return attribute.name != name && isSameLocal && attributeNamespace == namespace
    }

    private fun attributeEdit(
        element: XmlElementSyntax,
        existing: XmlAttributeSyntax?,
        name: String,
        value: String,
    ): TextEdit {
        if (existing != null) return TextEdit(existing.valueRange, value)
        return TextEdit(TextRange.at(closeOf(element).start), " $name=\"$value\"")
    }

    private fun removal(element: XmlElementSyntax, attribute: XmlAttributeSyntax): TextEdit {
        val children = element.startTag.children
        val index = children.indexOfFirst { child -> child.range == attribute.node.range }
        val start = children[index - 1].range.start
        return TextEdit(TextRange(start = start, end = attribute.node.range.end), "")
    }

    private fun contentEdit(element: XmlElementSyntax, content: String): TextEdit {
        val endTag = element.endTag ?: return TextEdit(closeOf(element), ">$content</${element.name}>")
        return TextEdit(TextRange(start = element.startTag.range.end, end = endTag.range.start), content)
    }

    private fun appending(parent: XmlElementSyntax, markup: String): TextEdit {
        val endTag = parent.endTag ?: return TextEdit(closeOf(parent), ">$markup</${parent.name}>")
        return TextEdit(TextRange.at(endTag.range.start), markup)
    }

    private fun closeOf(element: XmlElementSyntax): TextRange {
        val close = element.startTag.children.last()
        return close.range
    }

    private fun quoteOf(attribute: XmlAttributeSyntax): Char {
        val value = attribute.node.children.last()
        return value.text.first()
    }

    private fun prefixOf(name: String): String? =
        name.substringBefore(PREFIX_SEPARATOR).takeIf { PREFIX_SEPARATOR in name }

    private fun isNamespaceName(name: String): Boolean = name == NAMESPACE_PREFIX || prefixOf(name) == NAMESPACE_PREFIX
}
