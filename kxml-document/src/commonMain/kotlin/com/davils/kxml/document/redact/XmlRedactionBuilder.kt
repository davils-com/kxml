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
package com.davils.kxml.document.redact

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.name.XmlName

/**
 * Configures an [XmlRedaction]: what is secret in a document and what stands in its place.
 *
 * A builder is not safe to share between threads; the redaction it builds is.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class XmlRedactionBuilder internal constructor() : ValidatingBuilder<XmlRedaction>() {
    override val subject: String get() = "xml redaction"

    /**
     * The text that replaces a redacted value or the matched part of a text; `***` by default. It must be text XML
     * allows, without `--`, `?>` or a leading or trailing `-`, since it may stand in a comment or a processing
     * instruction, and must not be empty, so that a redaction stays visible and cannot join two dashes of a
     * comment.
     *
     * @since 1.0.0
     */
    public var replacement: String = DEFAULT_REPLACEMENT

    /**
     * The limits the redaction runs under: those of its XPath expressions, [XmlLimits.maximumQueryStringLength] for
     * the text a value or comment pattern may read, and [XmlLimits.maximumRedactionRules] for the number of rules;
     * [XmlLimits.Untrusted] by default.
     *
     * @since 1.0.0
     */
    public var limits: XmlLimits = XmlLimits.Untrusted

    internal val namespaces: MutableMap<String, String> = mutableMapOf()

    internal val paths: MutableList<String> = mutableListOf()

    internal val elements: MutableList<(XmlName) -> Boolean> = mutableListOf()

    internal val attributes: MutableList<(XmlName) -> Boolean> = mutableListOf()

    internal val values: MutableList<Regex> = mutableListOf()

    internal val comments: MutableList<Regex> = mutableListOf()

    /**
     * Binds [prefix] to [uri] for the XPath expressions of [path].
     *
     * @param prefix the prefix.
     * @param uri the namespace URI.
     * @since 1.0.0
     */
    public fun namespace(prefix: String, uri: String) {
        checkOpen()
        namespaces[prefix] = uri
    }

    /**
     * Marks every node the XPath 1.0 expression [expression] selects as secret: an element with its content and
     * attributes, an attribute, a text, a comment or a processing instruction; the root node stands for the document
     * element.
     *
     * @param expression the expression, parsed with [limits] and the namespaces of [namespace] when the redaction is
     * built; one that is not valid or gives no node-set fails the build.
     * @since 1.0.0
     */
    public fun path(expression: String) {
        checkOpen()
        paths += expression
    }

    /**
     * Marks every element whose expanded name [predicate] accepts as secret, with its content and attributes.
     *
     * @param predicate receives the name of an element, which may come from an attacker. When it throws an
     * [Exception], the redaction fails with an [IllegalStateException] that carries neither the name nor the
     * exception.
     * @since 1.0.0
     */
    public fun element(predicate: (XmlName) -> Boolean) {
        checkOpen()
        elements += predicate
    }

    /**
     * Marks the value of every attribute whose expanded name [predicate] accepts as secret.
     *
     * @param predicate receives the name of an attribute, as for [element].
     * @since 1.0.0
     */
    public fun attribute(predicate: (XmlName) -> Boolean) {
        checkOpen()
        attributes += predicate
    }

    /**
     * Replaces every match of [pattern] in a text and an attribute value with the [replacement].
     *
     * The pattern runs on the regular expression engine of the platform, which may take exponential time, so it only
     * ever sees a value of at most [XmlLimits.maximumQueryStringLength] characters; a longer value fails the
     * redaction.
     *
     * @param pattern the pattern, supplied by the caller and never by a document.
     * @since 1.0.0
     */
    public fun value(pattern: Regex) {
        checkOpen()
        values += pattern
    }

    /**
     * Replaces every match of [pattern] in the text of a comment with the [replacement], as [value] does for values.
     *
     * @param pattern the pattern, supplied by the caller and never by a document.
     * @since 1.0.0
     */
    public fun comment(pattern: Regex) {
        checkOpen()
        comments += pattern
    }

    override fun assemble(): XmlRedaction = XmlRedaction(this)

    private companion object {
        const val DEFAULT_REPLACEMENT: String = "***"
    }
}
