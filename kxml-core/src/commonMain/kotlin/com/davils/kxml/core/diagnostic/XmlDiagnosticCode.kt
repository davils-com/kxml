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

package com.davils.kxml.core.diagnostic

import com.davils.kore.core.validation.violation.Severity

/**
 * The stable codes of the problems kxml reports, each with the severity it has by default.
 *
 * The [value] of a code never changes once released; the message that comes with a diagnostic may.
 *
 * @property value the code as a dotted name, such as `kxml.syntax.mismatched-tag`.
 * @property severity the severity of a diagnostic with this code.
 * @since 1.0.0
 */
public enum class XmlDiagnosticCode(public val value: String, public val severity: Severity) {
    /**
     * A character XML does not allow, such as a control character, or a character that is out of place.
     *
     * @since 1.0.0
     */
    InvalidCharacter("kxml.syntax.invalid-character", Severity.Error),

    /**
     * Content where it may not stand, such as text outside the root element.
     *
     * @since 1.0.0
     */
    UnexpectedContent("kxml.syntax.unexpected-content", Severity.Error),

    /**
     * The document ends inside a construct.
     *
     * @since 1.0.0
     */
    UnexpectedEnd("kxml.syntax.unexpected-end", Severity.Error),

    /**
     * A name that is not an XML name.
     *
     * @since 1.0.0
     */
    InvalidName("kxml.syntax.invalid-name", Severity.Error),

    /**
     * An end tag that does not close the open element.
     *
     * @since 1.0.0
     */
    MismatchedTag("kxml.syntax.mismatched-tag", Severity.Error),

    /**
     * An element that is still open at the end of the document.
     *
     * @since 1.0.0
     */
    UnclosedElement("kxml.syntax.unclosed-element", Severity.Error),

    /**
     * A document without a root element.
     *
     * @since 1.0.0
     */
    MissingRoot("kxml.syntax.missing-root", Severity.Error),

    /**
     * A second root element after the first one.
     *
     * @since 1.0.0
     */
    ContentAfterRoot("kxml.syntax.content-after-root", Severity.Error),

    /**
     * An attribute that appears twice on one element, also through two prefixes of one namespace.
     *
     * @since 1.0.0
     */
    DuplicateAttribute("kxml.syntax.duplicate-attribute", Severity.Error),

    /**
     * An attribute without `=`, with a value outside quotes or with a `<` in its value.
     *
     * @since 1.0.0
     */
    InvalidAttribute("kxml.syntax.invalid-attribute", Severity.Error),

    /**
     * A comment that contains `--` or ends with `-`.
     *
     * @since 1.0.0
     */
    InvalidComment("kxml.syntax.invalid-comment", Severity.Error),

    /**
     * A processing instruction without a valid target.
     *
     * @since 1.0.0
     */
    InvalidProcessingInstruction("kxml.syntax.invalid-processing-instruction", Severity.Error),

    /**
     * A processing instruction whose target is `xml` in any case.
     *
     * @since 1.0.0
     */
    ReservedProcessingInstruction("kxml.syntax.reserved-processing-instruction", Severity.Error),

    /**
     * The sequence `]]>` in text.
     *
     * @since 1.0.0
     */
    CDataEndInText("kxml.syntax.cdata-end-in-text", Severity.Error),

    /**
     * An XML declaration that does not follow the grammar.
     *
     * @since 1.0.0
     */
    InvalidDeclaration("kxml.declaration.invalid", Severity.Error),

    /**
     * An XML declaration that is not at the very start of the document.
     *
     * @since 1.0.0
     */
    MisplacedDeclaration("kxml.declaration.misplaced", Severity.Error),

    /**
     * An XML declaration with a version other than 1.x.
     *
     * @since 1.0.0
     */
    UnsupportedVersion("kxml.declaration.unsupported-version", Severity.Error),

    /**
     * A character or entity reference that does not follow the grammar or names a character XML does not allow.
     *
     * @since 1.0.0
     */
    InvalidReference("kxml.reference.invalid", Severity.Error),

    /**
     * A reference to an entity other than the five predefined ones.
     *
     * @since 1.0.0
     */
    UndefinedEntity("kxml.reference.undefined-entity", Severity.Error),

    /**
     * A document type declaration under [XmlDoctype.Refuse][com.davils.kxml.core.XmlDoctype.Refuse].
     *
     * @since 1.0.0
     */
    RefusedDoctype("kxml.doctype.refused", Severity.Error),

    /**
     * A document type declaration that was skipped under [XmlDoctype.Ignore][com.davils.kxml.core.XmlDoctype.Ignore]; a
     * warning.
     *
     * @since 1.0.0
     */
    IgnoredDoctype("kxml.doctype.ignored", Severity.Warning),

    /**
     * A document type declaration after the root element or after another one.
     *
     * @since 1.0.0
     */
    MisplacedDoctype("kxml.doctype.misplaced", Severity.Error),

    /**
     * A prefix without a namespace declaration in scope.
     *
     * @since 1.0.0
     */
    UndeclaredPrefix("kxml.namespace.undeclared-prefix", Severity.Error),

    /**
     * A declaration that binds `xml` or `xmlns` wrongly, or binds another prefix to their namespaces.
     *
     * @since 1.0.0
     */
    ReservedPrefix("kxml.namespace.reserved-prefix", Severity.Error),

    /**
     * A declaration that binds a prefix to the empty namespace name.
     *
     * @since 1.0.0
     */
    EmptyPrefixBinding("kxml.namespace.empty-prefix-binding", Severity.Error),

    /**
     * Bytes that are not valid in the encoding of the document.
     *
     * @since 1.0.0
     */
    InvalidEncoding("kxml.source.invalid-encoding", Severity.Error),

    /**
     * An encoding kxml does not read, such as EBCDIC, UCS-4 in an unusual byte order or Windows-1252, whether the bytes
     * start like it, the document declares it or the charset of the source names it.
     *
     * @since 1.0.0
     */
    UnsupportedEncoding("kxml.source.unsupported-encoding", Severity.Error),

    /**
     * A declared encoding that contradicts the bytes, such as UTF-16 declared for bytes that start like ASCII or UTF-8
     * declared after a byte order mark of UTF-16.
     *
     * @since 1.0.0
     */
    EncodingMismatch("kxml.source.encoding-mismatch", Severity.Error),

    /**
     * The charset of the source overrides the encoding the document declares, as RFC 7303 orders it; a warning.
     *
     * @since 1.0.0
     */
    EncodingOverridden("kxml.source.encoding-overridden", Severity.Warning),

    /**
     * A source longer than [XmlLimits.maximumInputLength][com.davils.kxml.core.limit.XmlLimits.maximumInputLength].
     *
     * @since 1.0.0
     */
    InputTooLong("kxml.limit.input-length", Severity.Error),

    /**
     * Elements nested deeper than [XmlLimits.maximumDepth][com.davils.kxml.core.limit.XmlLimits.maximumDepth].
     *
     * @since 1.0.0
     */
    TooDeep("kxml.limit.depth", Severity.Error),

    /**
     * More elements than [XmlLimits.maximumElements][com.davils.kxml.core.limit.XmlLimits.maximumElements].
     *
     * @since 1.0.0
     */
    TooManyElements("kxml.limit.elements", Severity.Error),

    /**
     * An element with more attributes than
     * [XmlLimits.maximumAttributes][com.davils.kxml.core.limit.XmlLimits.maximumAttributes].
     *
     * @since 1.0.0
     */
    TooManyAttributes("kxml.limit.attributes", Severity.Error),

    /**
     * A text longer than [XmlLimits.maximumTextLength][com.davils.kxml.core.limit.XmlLimits.maximumTextLength].
     *
     * @since 1.0.0
     */
    TextTooLong("kxml.limit.text-length", Severity.Error),

    /**
     * A name longer than [XmlLimits.maximumNameLength][com.davils.kxml.core.limit.XmlLimits.maximumNameLength].
     *
     * @since 1.0.0
     */
    NameTooLong("kxml.limit.name-length", Severity.Error),

    /**
     * A decoded value that does not fit the type it is decoded into.
     *
     * @since 1.0.0
     */
    TypeMismatch("kxml.decode.type-mismatch", Severity.Error),

    /**
     * A required property without an element or attribute.
     *
     * @since 1.0.0
     */
    MissingProperty("kxml.decode.missing-property", Severity.Error),

    /**
     * An element or attribute no property is decoded from, where unknown content is refused.
     *
     * @since 1.0.0
     */
    UnknownProperty("kxml.decode.unknown-property", Severity.Error),

    /**
     * A root element with another name than the decoded type expects.
     *
     * @since 1.0.0
     */
    UnexpectedRoot("kxml.decode.unexpected-root", Severity.Error),

    /**
     * A number that does not fit the type it is decoded into.
     *
     * @since 1.0.0
     */
    NumberOutOfRange("kxml.decode.number-out-of-range", Severity.Error),

    /**
     * A value that names no entry of the decoded enum.
     *
     * @since 1.0.0
     */
    UnknownEnumValue("kxml.decode.unknown-enum-value", Severity.Error),

    /**
     * A polymorphic value without its type.
     *
     * @since 1.0.0
     */
    MissingPolymorphicType("kxml.decode.missing-polymorphic-type", Severity.Error),

    /**
     * A polymorphic value whose type is not registered.
     *
     * @since 1.0.0
     */
    UnknownPolymorphicType("kxml.decode.unknown-polymorphic-type", Severity.Error),
    ;

    /**
     * Returns [value].
     *
     * @return the code as a dotted name.
     * @since 1.0.0
     */
    override fun toString(): String = value
}
