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

package com.davils.kxml.core.limit

import com.davils.kore.core.Limits
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.validateOrThrow
import com.davils.kxml.core.XmlViolationCodes
import kotlin.reflect.KProperty0

/**
 * The limits that bound the work and the memory kxml spends on one source or one written text.
 *
 * Every input of kxml may come from an attacker, so the reader checks its input against these limits before it does
 * work that grows with the input. A document that breaks a limit is not read further; the result carries the
 * diagnostic of the limit, such as [TooDeep][com.davils.kxml.core.diagnostic.XmlDiagnosticCode.TooDeep]. Nothing is
 * truncated silently.
 *
 * Every limit is at least 1 and at most a hard ceiling, a `HARD_*` constant of the companion that no preset and no
 * configuration exceeds. [Untrusted] is the default and fits documents from unknown sources; [Relaxed] raises every
 * limit for trusted, large documents and is still finite. Other limits are built with [xmlLimits] or the `limits`
 * block of [com.davils.kxml.core.xml], which validate them and throw a
 * [com.davils.kore.core.validation.report.ValidationException] for a value below 1 or above its ceiling. The limits
 * validate themselves when they are created, however they are created, so no limits beyond a ceiling exist.
 *
 * The limits are an immutable value and safe to share between threads; two limits are equal when every limit is
 * equal.
 *
 * @since 1.0.0
 */
public class XmlLimits internal constructor(builder: XmlLimitsBuilder) : Validatable {
    /**
     * The longest source in UTF-16 code units, or in bytes for a source of bytes: 64 Mi (67,108,864) by default, at
     * most [HARD_INPUT_LENGTH].
     *
     * A longer source is refused with an [InputTooLong][com.davils.kxml.core.diagnostic.XmlDiagnosticCode.InputTooLong]
     * diagnostic before it is read.
     *
     * @since 1.0.0
     */
    public val maximumInputLength: Int = builder.maximumInputLength

    /**
     * The deepest nesting of elements: 256 by default, at most [HARD_DEPTH].
     *
     * Deeper nesting stops the reader with a [TooDeep][com.davils.kxml.core.diagnostic.XmlDiagnosticCode.TooDeep]
     * diagnostic. Whatever the limit, the reader and the writer never nest deeper than [HARD_DEPTH].
     *
     * @since 1.0.0
     */
    public val maximumDepth: Int = builder.maximumDepth

    /**
     * The most elements in one document, counted as they are read: 5,000,000 by default, at most [HARD_ELEMENTS].
     *
     * @since 1.0.0
     */
    public val maximumElements: Long = builder.maximumElements

    /**
     * The most attributes on one element, namespace declarations included: 1,024 by default, at most
     * [HARD_ATTRIBUTES].
     *
     * @since 1.0.0
     */
    public val maximumAttributes: Int = builder.maximumAttributes

    /**
     * The longest run of text, attribute value, comment, CDATA section or data of a processing instruction, in UTF-16
     * code units: 16 Mi (16,777,216) by default, at most [HARD_INPUT_LENGTH].
     *
     * @since 1.0.0
     */
    public val maximumTextLength: Int = builder.maximumTextLength

    /**
     * The longest name of an element, an attribute, a prefix or a processing instruction, in UTF-16 code units: 1,024
     * by default, at most [HARD_NAME_LENGTH].
     *
     * @since 1.0.0
     */
    public val maximumNameLength: Int = builder.maximumNameLength

    /**
     * The most entities a document type declaration may declare, general and parameter entities together: 10,000 by
     * default, at most [HARD_ENTITY_DECLARATIONS].
     *
     * @since 1.0.0
     */
    public val maximumEntityDeclarations: Int = builder.maximumEntityDeclarations

    /**
     * The most attribute definitions a document type declaration may hold over all its attribute-list declarations:
     * 10,000 by default, at most [HARD_ATTRIBUTE_DECLARATIONS].
     *
     * @since 1.0.0
     */
    public val maximumAttributeDeclarations: Int = builder.maximumAttributeDeclarations

    /**
     * The most characters of replacement text the expansion of entities may produce in one document, counted while
     * the entities are expanded; every reference charges at least one character, so a billion laughs document and
     * one with a quadratic blowup fail at this budget: 1,000,000 by default, at most [HARD_ENTITY_EXPANSION].
     *
     * @since 1.0.0
     */
    public val maximumEntityExpansion: Long = builder.maximumEntityExpansion

    /**
     * The deepest nesting of entity references within replacement texts: 16 by default, at most [HARD_ENTITY_DEPTH].
     *
     * @since 1.0.0
     */
    public val maximumEntityDepth: Int = builder.maximumEntityDepth

    /**
     * The most external resources, the external subset and external entities, one document may load through its
     * resolver; every resource counts once, however often it is referenced: 16 by default, at most
     * [HARD_EXTERNAL_RESOURCES].
     *
     * @since 1.0.0
     */
    public val maximumExternalResources: Int = builder.maximumExternalResources

    /**
     * The longest external resource, in bytes or code units as the resolver hands it over, checked before it is
     * decoded: 8 Mi by default, at most [HARD_INPUT_LENGTH].
     *
     * @since 1.0.0
     */
    public val maximumExternalLength: Int = builder.maximumExternalLength

    init {
        validateOrThrow(SUBJECT)
    }

    private val bounds: List<Bound>
        get() = listOf(
            Bound(
                property = ::maximumInputLength,
                value = maximumInputLength.toLong(),
                ceiling = HARD_INPUT_LENGTH.toLong(),
            ),
            Bound(property = ::maximumDepth, value = maximumDepth.toLong(), ceiling = HARD_DEPTH.toLong()),
            Bound(property = ::maximumElements, value = maximumElements, ceiling = HARD_ELEMENTS),
            Bound(
                property = ::maximumAttributes,
                value = maximumAttributes.toLong(),
                ceiling = HARD_ATTRIBUTES.toLong(),
            ),
            Bound(
                property = ::maximumTextLength,
                value = maximumTextLength.toLong(),
                ceiling = HARD_INPUT_LENGTH.toLong(),
            ),
            Bound(
                property = ::maximumNameLength,
                value = maximumNameLength.toLong(),
                ceiling = HARD_NAME_LENGTH.toLong(),
            ),
            Bound(
                property = ::maximumEntityDeclarations,
                value = maximumEntityDeclarations.toLong(),
                ceiling = HARD_ENTITY_DECLARATIONS.toLong(),
            ),
            Bound(
                property = ::maximumAttributeDeclarations,
                value = maximumAttributeDeclarations.toLong(),
                ceiling = HARD_ATTRIBUTE_DECLARATIONS.toLong(),
            ),
            Bound(
                property = ::maximumEntityExpansion,
                value = maximumEntityExpansion,
                ceiling = HARD_ENTITY_EXPANSION,
            ),
            Bound(
                property = ::maximumEntityDepth,
                value = maximumEntityDepth.toLong(),
                ceiling = HARD_ENTITY_DEPTH.toLong(),
            ),
            Bound(
                property = ::maximumExternalResources,
                value = maximumExternalResources.toLong(),
                ceiling = HARD_EXTERNAL_RESOURCES.toLong(),
            ),
            Bound(
                property = ::maximumExternalLength,
                value = maximumExternalLength.toLong(),
                ceiling = HARD_INPUT_LENGTH.toLong(),
            ),
        )

    private val values: List<Long> get() = bounds.map(Bound::value)

    /**
     * Checks that every limit lies between 1 and its hard ceiling: a value below 1 is reported with
     * [XmlViolationCodes.NOT_POSITIVE], a value above its ceiling with [XmlViolationCodes.ABOVE_HARD_LIMIT].
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        bounds.forEach { bound -> bounded(bound) }
    }

    /**
     * Returns whether [other] holds the same value for every limit.
     *
     * @param other the object to compare with, which may be `null`.
     * @return `true` if [other] is equal limits, `false` otherwise.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is XmlLimits && values == other.values

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code of every limit.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns every limit by a short name, such as `XmlLimits(input=67108864, depth=256, …)`.
     *
     * @return the limits as text.
     * @since 1.0.0
     */
    override fun toString(): String =
        "XmlLimits(input=$maximumInputLength, depth=$maximumDepth, elements=$maximumElements, " +
            "attributes=$maximumAttributes, text=$maximumTextLength, name=$maximumNameLength, " +
            "entities=$maximumEntityDeclarations, attributeDeclarations=$maximumAttributeDeclarations, " +
            "entityExpansion=$maximumEntityExpansion, entityDepth=$maximumEntityDepth, " +
            "externalResources=$maximumExternalResources, externalLength=$maximumExternalLength)"

    private fun ValidationScope.bounded(bound: Bound) {
        val value = bound.value
        if (value < 1) fail(bound.property, "must be at least 1, but was $value", XmlViolationCodes.NOT_POSITIVE)
        if (value > bound.ceiling) {
            val message = "must not exceed ${bound.ceiling}, but was $value"
            fail(bound.property, message, XmlViolationCodes.ABOVE_HARD_LIMIT)
        }
    }

    private data class Bound(val property: KProperty0<*>, val value: Long, val ceiling: Long)

    /**
     * The hard ceilings of every limit and the presets [Untrusted] and [Relaxed].
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The ceiling of [maximumInputLength] and [maximumTextLength]: 64 Mi (67,108,864) UTF-16 code units, the
         * longest text the line map of Kore, [Limits.MAX_LINE_MAP_LENGTH], indexes, so every source a limit admits can
         * be given lines and columns.
         *
         * @since 1.0.0
         */
        public const val HARD_INPUT_LENGTH: Int = Limits.MAX_LINE_MAP_LENGTH

        /**
         * The ceiling of [maximumDepth] and the deepest nesting the reader and the writer handle without running out
         * of stack: 1,000 levels.
         *
         * @since 1.0.0
         */
        public const val HARD_DEPTH: Int = 1_000

        /**
         * The ceiling of [maximumElements]: 100,000,000 elements.
         *
         * @since 1.0.0
         */
        public const val HARD_ELEMENTS: Long = 100_000_000L

        /**
         * The ceiling of [maximumAttributes]: 1,000,000 attributes on one element.
         *
         * @since 1.0.0
         */
        public const val HARD_ATTRIBUTES: Int = 1_000_000

        /**
         * The ceiling of [maximumNameLength]: 1 Mi (1,048,576) UTF-16 code units.
         *
         * @since 1.0.0
         */
        public const val HARD_NAME_LENGTH: Int = 1_024 * 1_024

        /**
         * The ceiling of [maximumEntityDeclarations]: 1,000,000 entities.
         *
         * @since 1.0.0
         */
        public const val HARD_ENTITY_DECLARATIONS: Int = 1_000_000

        /**
         * The ceiling of [maximumAttributeDeclarations]: 10,000,000 attribute definitions.
         *
         * @since 1.0.0
         */
        public const val HARD_ATTRIBUTE_DECLARATIONS: Int = 10_000_000

        /**
         * The ceiling of [maximumEntityExpansion]: 100,000,000 characters of replacement text.
         *
         * @since 1.0.0
         */
        public const val HARD_ENTITY_EXPANSION: Long = 100_000_000L

        /**
         * The ceiling of [maximumEntityDepth]: 256 levels of entities.
         *
         * @since 1.0.0
         */
        public const val HARD_ENTITY_DEPTH: Int = 256

        /**
         * The ceiling of [maximumExternalResources]: 10,000 resources.
         *
         * @since 1.0.0
         */
        public const val HARD_EXTERNAL_RESOURCES: Int = 10_000

        private const val SUBJECT: String = "xml limits"
        private const val RELAXED_ELEMENTS: Long = 50_000_000L
        private const val RELAXED_ATTRIBUTES: Int = 100_000
        private const val RELAXED_NAME_LENGTH: Int = 65_536
        private const val RELAXED_ENTITY_DECLARATIONS: Int = 100_000
        private const val RELAXED_ATTRIBUTE_DECLARATIONS: Int = 1_000_000
        private const val RELAXED_ENTITY_EXPANSION: Long = 10_000_000L
        private const val RELAXED_ENTITY_DEPTH: Int = 64
        private const val RELAXED_EXTERNAL_RESOURCES: Int = 1_000

        /**
         * The default limits, for documents from sources that are not trusted: the `DEFAULT_*` values of
         * [XmlLimitsBuilder], such as 64 Mi code units of input, 256 levels, 5,000,000 elements and 1,024 attributes
         * on an element.
         *
         * @since 1.0.0
         */
        public val Untrusted: XmlLimits = XmlLimitsBuilder().build()

        /**
         * Raised limits for large documents from trusted sources: [HARD_INPUT_LENGTH] code units of input and of
         * text, [HARD_DEPTH] levels, 50,000,000 elements, 100,000 attributes on an element, 65,536 code units of a
         * name, 100,000 entities, 1,000,000 attribute definitions, 10,000,000 characters of entity expansion, 64
         * levels of entities, 1,000 external resources and [HARD_INPUT_LENGTH] code units of each.
         *
         * Every limit stays finite and at most its hard ceiling, so even a trusted source cannot exhaust the memory
         * without bound.
         *
         * @since 1.0.0
         */
        public val Relaxed: XmlLimits = relaxed()

        private fun relaxed(): XmlLimits {
            val builder = XmlLimitsBuilder()
            builder.maximumInputLength = HARD_INPUT_LENGTH
            builder.maximumDepth = HARD_DEPTH
            builder.maximumElements = RELAXED_ELEMENTS
            builder.maximumAttributes = RELAXED_ATTRIBUTES
            builder.maximumTextLength = HARD_INPUT_LENGTH
            builder.maximumNameLength = RELAXED_NAME_LENGTH
            builder.maximumEntityDeclarations = RELAXED_ENTITY_DECLARATIONS
            builder.maximumAttributeDeclarations = RELAXED_ATTRIBUTE_DECLARATIONS
            builder.maximumEntityExpansion = RELAXED_ENTITY_EXPANSION
            builder.maximumEntityDepth = RELAXED_ENTITY_DEPTH
            builder.maximumExternalResources = RELAXED_EXTERNAL_RESOURCES
            builder.maximumExternalLength = HARD_INPUT_LENGTH
            return builder.build()
        }
    }
}
