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

import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.name.XmlName

private const val COMMENT_DASHES: String = "--"
private const val COMMENT_DASH: String = "-"
private const val INSTRUCTION_END: String = "?>"
private const val RESERVED_TARGET: String = "xml"

/**
 * Creates an element named [name] with the attributes and children that [configure] adds.
 *
 * @param name the name of the element.
 * @param configure adds attributes, namespace declarations and children.
 * @return the element.
 * @throws IllegalArgumentException if [configure] adds an invalid attribute or child, see [XmlElementBuilder].
 * @since 1.0.0
 */
public fun xmlElement(name: XmlName, configure: XmlElementBuilder.() -> Unit = {}): XmlNode.Element {
    val builder = XmlElementBuilder(name)
    builder.configure()
    return builder.build()
}

/**
 * Creates a text node.
 *
 * @param value the text; the writer escapes it.
 * @return the text node.
 * @throws IllegalArgumentException if [value] holds a character XML cannot carry, such as most control characters.
 * @since 1.0.0
 */
public fun xmlText(value: String): XmlNode.Text {
    require(XmlCharacters.isText(value)) { "The text holds characters XML cannot carry" }
    return XmlNode.Text(value, range = null)
}

/**
 * Creates a CDATA section.
 *
 * @param value the content; the writer splits it into two sections where it contains `]]>`.
 * @return the CDATA section.
 * @throws IllegalArgumentException if [value] holds a character XML cannot carry.
 * @since 1.0.0
 */
public fun xmlCData(value: String): XmlNode.CData {
    require(XmlCharacters.isText(value)) { "The character data holds characters XML cannot carry" }
    return XmlNode.CData(value, range = null)
}

/**
 * Creates a comment.
 *
 * @param value the content.
 * @return the comment.
 * @throws IllegalArgumentException if [value] holds a character XML cannot carry, contains `--` or ends with `-`.
 * @since 1.0.0
 */
public fun xmlComment(value: String): XmlNode.Comment {
    require(XmlCharacters.isText(value)) { "The comment holds characters XML cannot carry" }
    require(COMMENT_DASHES !in value && !value.endsWith(COMMENT_DASH)) {
        "A comment cannot contain $COMMENT_DASHES or end with $COMMENT_DASH"
    }
    return XmlNode.Comment(value, range = null)
}

/**
 * Creates a processing instruction.
 *
 * @param target the name the instruction is addressed to; a name without a colon other than `xml` in any case.
 * @param data the data; empty by default.
 * @return the processing instruction.
 * @throws IllegalArgumentException if [target] is not a valid target, or [data] holds a character XML cannot carry or
 *   contains `?>`.
 * @since 1.0.0
 */
public fun xmlInstruction(target: String, data: String = ""): XmlNode.ProcessingInstruction {
    require(XmlCharacters.isNcName(target)) { "The target of a processing instruction must be a name" }
    require(!target.equals(RESERVED_TARGET, ignoreCase = true)) { "The target $target is reserved by XML" }
    require(XmlCharacters.isText(data) && INSTRUCTION_END !in data) {
        "The data of a processing instruction cannot contain $INSTRUCTION_END"
    }
    return XmlNode.ProcessingInstruction(target, data, range = null)
}
