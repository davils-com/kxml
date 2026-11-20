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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Collects the limits of [XmlLimits]; used through [xmlLimits] and
 * [XmlBuilder.limits][com.davils.kxml.core.XmlBuilder.limits].
 *
 * Every limit must lie between 1 and its hard ceiling, a `HARD_*` constant of [XmlLimits]; the builder is not thread
 * safe.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class XmlLimitsBuilder internal constructor() : ValidatingBuilder<XmlLimits>() {
    override val subject: String get() = "xml limits"

    /**
     * The longest source in characters, or in bytes for a source of bytes; [DEFAULT_INPUT_LENGTH] by default, at most
     * [XmlLimits.HARD_INPUT_LENGTH].
     *
     * @since 1.0.0
     */
    public var maximumInputLength: Int = DEFAULT_INPUT_LENGTH

    /**
     * The deepest nesting of elements; [DEFAULT_DEPTH] by default, at most [XmlLimits.HARD_DEPTH].
     *
     * @since 1.0.0
     */
    public var maximumDepth: Int = DEFAULT_DEPTH

    /**
     * The most elements in one document; [DEFAULT_ELEMENTS] by default, at most [XmlLimits.HARD_ELEMENTS].
     *
     * @since 1.0.0
     */
    public var maximumElements: Long = DEFAULT_ELEMENTS

    /**
     * The most attributes on one element; [DEFAULT_ATTRIBUTES] by default, at most [XmlLimits.HARD_ATTRIBUTES].
     *
     * @since 1.0.0
     */
    public var maximumAttributes: Int = DEFAULT_ATTRIBUTES

    /**
     * The longest run of text; [DEFAULT_TEXT_LENGTH] by default, at most [XmlLimits.HARD_INPUT_LENGTH].
     *
     * @since 1.0.0
     */
    public var maximumTextLength: Int = DEFAULT_TEXT_LENGTH

    /**
     * The longest name; [DEFAULT_NAME_LENGTH] by default, at most [XmlLimits.HARD_NAME_LENGTH].
     *
     * @since 1.0.0
     */
    public var maximumNameLength: Int = DEFAULT_NAME_LENGTH

    /**
     * The most entity declarations; [DEFAULT_ENTITY_DECLARATIONS] by default, at most
     * [XmlLimits.HARD_ENTITY_DECLARATIONS].
     *
     * @since 1.0.0
     */
    public var maximumEntityDeclarations: Int = DEFAULT_ENTITY_DECLARATIONS

    /**
     * The most attribute definitions; [DEFAULT_ATTRIBUTE_DECLARATIONS] by default, at most
     * [XmlLimits.HARD_ATTRIBUTE_DECLARATIONS].
     *
     * @since 1.0.0
     */
    public var maximumAttributeDeclarations: Int = DEFAULT_ATTRIBUTE_DECLARATIONS

    /**
     * The most characters of entity expansion; [DEFAULT_ENTITY_EXPANSION] by default, at most
     * [XmlLimits.HARD_ENTITY_EXPANSION].
     *
     * @since 1.0.0
     */
    public var maximumEntityExpansion: Long = DEFAULT_ENTITY_EXPANSION

    /**
     * The deepest nesting of entities; [DEFAULT_ENTITY_DEPTH] by default, at most [XmlLimits.HARD_ENTITY_DEPTH].
     *
     * @since 1.0.0
     */
    public var maximumEntityDepth: Int = DEFAULT_ENTITY_DEPTH

    /**
     * The most external resources of one document; [DEFAULT_EXTERNAL_RESOURCES] by default, at most
     * [XmlLimits.HARD_EXTERNAL_RESOURCES].
     *
     * @since 1.0.0
     */
    public var maximumExternalResources: Int = DEFAULT_EXTERNAL_RESOURCES

    /**
     * The longest external resource; [DEFAULT_EXTERNAL_LENGTH] by default, at most [XmlLimits.HARD_INPUT_LENGTH].
     *
     * @since 1.0.0
     */
    public var maximumExternalLength: Int = DEFAULT_EXTERNAL_LENGTH

    /**
     * The most catalog files one catalog loads; [DEFAULT_CATALOG_FILES] by default, at most
     * [XmlLimits.HARD_CATALOG_FILES].
     *
     * @since 1.0.0
     */
    public var maximumCatalogFiles: Int = DEFAULT_CATALOG_FILES

    /**
     * The longest chain of catalog files and the most delegations of one lookup; [DEFAULT_CATALOG_DEPTH] by default,
     * at most [XmlLimits.HARD_CATALOG_DEPTH].
     *
     * @since 1.0.0
     */
    public var maximumCatalogDepth: Int = DEFAULT_CATALOG_DEPTH

    /**
     * The longest text the writer writes; [DEFAULT_INPUT_LENGTH] by default, at most [XmlLimits.HARD_INPUT_LENGTH].
     *
     * @since 1.0.0
     */
    public var maximumOutputLength: Int = DEFAULT_INPUT_LENGTH

    /**
     * The longest XPath expression, in code units; [DEFAULT_QUERY_LENGTH] by default, at most
     * [XmlLimits.HARD_QUERY_LENGTH].
     *
     * @since 1.0.0
     */
    public var maximumQueryLength: Int = DEFAULT_QUERY_LENGTH

    /**
     * The deepest nesting of an XPath expression; [DEFAULT_QUERY_NESTING] by default, at most
     * [XmlLimits.HARD_QUERY_NESTING].
     *
     * @since 1.0.0
     */
    public var maximumQueryNesting: Int = DEFAULT_QUERY_NESTING

    /**
     * The most steps one evaluation of an XPath expression takes; [DEFAULT_QUERY_STEPS] by default, at most
     * [XmlLimits.HARD_QUERY_STEPS].
     *
     * @since 1.0.0
     */
    public var maximumQuerySteps: Long = DEFAULT_QUERY_STEPS

    /**
     * The most nodes in one node-set of an evaluation; [DEFAULT_QUERY_RESULTS] by default, at most
     * [XmlLimits.HARD_QUERY_RESULTS].
     *
     * @since 1.0.0
     */
    public var maximumQueryResults: Int = DEFAULT_QUERY_RESULTS

    /**
     * The longest string an evaluation builds, in code units; [DEFAULT_QUERY_STRING_LENGTH] by default, at most
     * [XmlLimits.HARD_INPUT_LENGTH].
     *
     * @since 1.0.0
     */
    public var maximumQueryStringLength: Int = DEFAULT_QUERY_STRING_LENGTH

    /**
     * The most operations of one XML patch; [DEFAULT_PATCH_OPERATIONS] by default, at most
     * [XmlLimits.HARD_PATCH_OPERATIONS].
     *
     * @since 1.0.0
     */
    public var maximumPatchOperations: Int = DEFAULT_PATCH_OPERATIONS

    /**
     * The most steps one diff of two documents takes; [DEFAULT_DIFF_WORK] by default, at most
     * [XmlLimits.HARD_DIFF_WORK].
     *
     * @since 1.0.0
     */
    public var maximumDiffWork: Long = DEFAULT_DIFF_WORK

    /**
     * The most layers one merge takes; [DEFAULT_MERGE_LAYERS] by default, at most [XmlLimits.HARD_MERGE_LAYERS].
     *
     * @since 1.0.0
     */
    public var maximumMergeLayers: Int = DEFAULT_MERGE_LAYERS

    /**
     * The most rules one redaction takes; [DEFAULT_REDACTION_RULES] by default, at most
     * [XmlLimits.HARD_REDACTION_RULES].
     *
     * @since 1.0.0
     */
    public var maximumRedactionRules: Int = DEFAULT_REDACTION_RULES

    internal fun from(limits: XmlLimits) {
        maximumInputLength = limits.maximumInputLength
        maximumDepth = limits.maximumDepth
        maximumElements = limits.maximumElements
        maximumAttributes = limits.maximumAttributes
        maximumTextLength = limits.maximumTextLength
        maximumNameLength = limits.maximumNameLength
        maximumEntityDeclarations = limits.maximumEntityDeclarations
        maximumAttributeDeclarations = limits.maximumAttributeDeclarations
        maximumEntityExpansion = limits.maximumEntityExpansion
        maximumEntityDepth = limits.maximumEntityDepth
        maximumExternalResources = limits.maximumExternalResources
        maximumExternalLength = limits.maximumExternalLength
        maximumCatalogFiles = limits.maximumCatalogFiles
        maximumCatalogDepth = limits.maximumCatalogDepth
        maximumOutputLength = limits.maximumOutputLength
        maximumQueryLength = limits.maximumQueryLength
        maximumQueryNesting = limits.maximumQueryNesting
        maximumQuerySteps = limits.maximumQuerySteps
        maximumQueryResults = limits.maximumQueryResults
        maximumQueryStringLength = limits.maximumQueryStringLength
        maximumPatchOperations = limits.maximumPatchOperations
        maximumDiffWork = limits.maximumDiffWork
        maximumMergeLayers = limits.maximumMergeLayers
        maximumRedactionRules = limits.maximumRedactionRules
    }

    override fun assemble(): XmlLimits = XmlLimits(this)

    /**
     * Holds the defaults, the limits of [XmlLimits.Untrusted].
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * 64 Mi characters or bytes.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_INPUT_LENGTH: Int = 64 * 1024 * 1024

        /**
         * 256 levels.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_DEPTH: Int = 256

        /**
         * Five million elements.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_ELEMENTS: Long = 5_000_000L

        /**
         * 1024 attributes.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_ATTRIBUTES: Int = 1_024

        /**
         * 16 Mi characters.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_TEXT_LENGTH: Int = 16 * 1024 * 1024

        /**
         * 1024 characters.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_NAME_LENGTH: Int = 1_024

        /**
         * The default of [maximumEntityDeclarations]: 10,000.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_ENTITY_DECLARATIONS: Int = 10_000

        /**
         * The default of [maximumAttributeDeclarations]: 10,000.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_ATTRIBUTE_DECLARATIONS: Int = 10_000

        /**
         * The default of [maximumEntityExpansion]: 1,000,000 characters.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_ENTITY_EXPANSION: Long = 1_000_000L

        /**
         * The default of [maximumEntityDepth]: 16 levels.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_ENTITY_DEPTH: Int = 16

        /**
         * The default of [maximumExternalResources]: 16 resources.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_EXTERNAL_RESOURCES: Int = 16

        /**
         * The default of [maximumExternalLength]: 8 Mi.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_EXTERNAL_LENGTH: Int = 8_388_608

        /**
         * The default of [maximumCatalogFiles]: 32 catalog files.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_CATALOG_FILES: Int = 32

        /**
         * The default of [maximumCatalogDepth]: 8 catalog files in a chain.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_CATALOG_DEPTH: Int = 8

        /**
         * The default of [maximumQueryLength]: 64 Ki code units.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_QUERY_LENGTH: Int = 64 * 1024

        /**
         * The default of [maximumQueryNesting]: 64 levels.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_QUERY_NESTING: Int = 64

        /**
         * The default of [maximumQuerySteps]: 10,000,000 steps.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_QUERY_STEPS: Long = 10_000_000L

        /**
         * The default of [maximumQueryResults]: 1,000,000 nodes.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_QUERY_RESULTS: Int = 1_000_000

        /**
         * The default of [maximumQueryStringLength]: 16 Mi code units.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_QUERY_STRING_LENGTH: Int = 16 * 1024 * 1024

        /**
         * The default of [maximumPatchOperations]: 10,000 operations.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_PATCH_OPERATIONS: Int = 10_000

        /**
         * The default of [maximumDiffWork]: 10,000,000 steps.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_DIFF_WORK: Long = 10_000_000L

        /**
         * The default of [maximumMergeLayers]: 64 layers.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_MERGE_LAYERS: Int = 64

        /**
         * The default of [maximumRedactionRules]: 1,024 rules.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_REDACTION_RULES: Int = 1_024
    }
}
