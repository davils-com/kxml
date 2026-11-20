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
@file:OptIn(DavilsInternal::class)

package com.davils.kxml.document.redact

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.validateOrThrow
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.name.XmlCharacters
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.syntax.XmlSyntaxTree
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.XmlDocumentViolationCodes
import com.davils.kxml.document.internal.codePointsOf
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.redact.rule.ModelWriter
import com.davils.kxml.document.redact.rule.RedactionRules
import com.davils.kxml.document.redact.rule.SyntaxRedaction
import com.davils.kxml.document.xpath.XPath

private const val SUBJECT: String = "xml redaction"
private val forbiddenInReplacement: List<String> = listOf("--", "?>", "]]>")

/**
 * Removes secrets from XML documents, from a document or in place from a lossless syntax tree, and fails closed.
 *
 * A node is secret when a configured XPath expression selects it, when it is an element whose name a configured
 * predicate accepts, or when it is an attribute whose name a configured predicate accepts. A secret element loses its
 * content to the [replacement] and the values of its attributes to it as well; a secret attribute, text, comment or
 * processing instruction has its value replaced. Value patterns replace the parts of texts and attribute values they
 * match, comment patterns those of comments; every replacement is literal. Namespace nodes an expression selects are
 * ignored.
 *
 * Redacting a syntax tree keeps the layout and everything that is not secret: it edits only the text of secret values
 * and contents, escaping the replacement for where it stands. A secret that comes from a general entity of the
 * internal subset redacts the literal of the entity declaration as well, which redacts every other use of the entity
 * too. A tree that does not compose without errors is refused as a whole, never redacted in part, and so is one whose
 * secret the redaction cannot reach in the text, such as an attribute a document type declaration supplies by
 * default or an entity declared outside the document, and one whose redacted text would not compose into a document
 * that a further redaction leaves unchanged. No secret ever appears in a message, an exception or a `toString` of
 * kxml-document.
 *
 * A redaction is immutable and safe to share between threads, as long as its predicates are.
 *
 * @since 1.0.0
 */
public class XmlRedaction internal constructor(builder: XmlRedactionBuilder) : Validatable {
    /**
     * The text that replaces a redacted value or the matched part of a text.
     *
     * @since 1.0.0
     */
    public val replacement: String = builder.replacement

    /**
     * The limits the redaction runs under.
     *
     * @since 1.0.0
     */
    public val limits: XmlLimits = builder.limits

    /**
     * The XPath expressions that select secret nodes, which cannot be changed.
     *
     * @since 1.0.0
     */
    public val paths: List<String> = builder.paths.immutableList()

    private val namespaces: Map<String, String> = builder.namespaces.toMap()
    private val ruleCount: Int = builder.paths.size +
        builder.elements.size +
        builder.attributes.size +
        builder.values.size +
        builder.comments.size
    private val parsed: List<XPath?> = paths.map { path ->
        runCatching { XPath.parseOrNull(path, namespaces = namespaces, limits = limits) }.getOrNull()
    }

    init {
        validateOrThrow(SUBJECT)
    }

    private val rules: RedactionRules = RedactionRules(
        replacement = replacement,
        limits = limits,
        paths = parsed.filterNotNull().map(XPath::parsed),
        elements = builder.elements.toList(),
        attributes = builder.attributes.toList(),
        values = builder.values.toList(),
        comments = builder.comments.toList(),
    )

    /**
     * Returns a redacted copy of [document], with every secret replaced.
     *
     * @param document the document, which stays unchanged.
     * @return the redacted document; adjacent text and CDATA sections become one text.
     * @throws XmlDocumentLimitException if an expression exceeds a query limit, or a value a pattern would have to
     * read is longer than [XmlLimits.maximumQueryStringLength]; nothing is returned in part.
     * @throws IllegalStateException if a predicate throws; nothing is returned in part.
     * @since 1.0.0
     */
    public fun redact(document: XmlDocument): XmlDocument {
        val model = DocumentModel.of(document)
        return ModelWriter(model, rules.planOf(model), replacement).documentOf(document)
    }

    /**
     * Redacts the text of [tree] in place, keeping its layout and everything that is not secret.
     *
     * @param tree the syntax tree, which stays unchanged.
     * @return the redacted tree with its text edits, or why it was refused.
     * @throws IllegalStateException if a predicate throws.
     * @since 1.0.0
     */
    public fun redact(tree: XmlSyntaxTree): XmlRedactionResult = SyntaxRedaction(rules).redacted(tree)

    override fun ValidationScope.validate() {
        paths.forEachIndexed { index, _ ->
            if (parsed[index]?.isNodeSet != true) {
                fail(
                    ::paths,
                    "is not an XPath expression of a node-set within the limits",
                    XmlDocumentViolationCodes.INVALID_PATH,
                )
            }
        }
        val isText = replacement.codePointsOf().all(XmlCharacters::isCharacter)
        val isSafe = replacement.isNotEmpty() &&
            forbiddenInReplacement.none(replacement::contains) &&
            !replacement.startsWith('-') &&
            !replacement.endsWith('-')
        if (!isText || !isSafe) {
            fail(
                ::replacement,
                "must be text a comment and an instruction can hold, and not empty",
                XmlDocumentViolationCodes.INVALID_REPLACEMENT,
            )
        }
        if (ruleCount > limits.maximumRedactionRules) {
            fail("holds more rules than maximumRedactionRules allows", XmlDocumentViolationCodes.TOO_MANY_RULES)
        }
    }

    /**
     * Returns the replacement and the paths, escaped; never a secret, since the redaction holds none.
     *
     * @return a description such as `XmlRedaction(replacement="***", paths=["//password"])`.
     * @since 1.0.0
     */
    override fun toString(): String {
        val shownPaths = paths.joinToString(
            separator = ", ",
            prefix = "[",
            postfix = "]",
        ) { path -> SafeText.quoted(path) }
        return "XmlRedaction(replacement=${SafeText.quoted(replacement)}, paths=$shownPaths)"
    }
}
