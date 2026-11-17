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
 * Every limit must be at least 1; the builder is not thread safe.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class XmlLimitsBuilder internal constructor() : ValidatingBuilder<XmlLimits>() {
    override val subject: String get() = "xml limits"

    /**
     * The longest source in characters, or in bytes for a source of bytes; [DEFAULT_INPUT_LENGTH] by default.
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
     * The most elements in one document; [DEFAULT_ELEMENTS] by default.
     *
     * @since 1.0.0
     */
    public var maximumElements: Long = DEFAULT_ELEMENTS

    /**
     * The most attributes on one element; [DEFAULT_ATTRIBUTES] by default.
     *
     * @since 1.0.0
     */
    public var maximumAttributes: Int = DEFAULT_ATTRIBUTES

    /**
     * The longest run of text; [DEFAULT_TEXT_LENGTH] by default.
     *
     * @since 1.0.0
     */
    public var maximumTextLength: Int = DEFAULT_TEXT_LENGTH

    /**
     * The longest name; [DEFAULT_NAME_LENGTH] by default.
     *
     * @since 1.0.0
     */
    public var maximumNameLength: Int = DEFAULT_NAME_LENGTH

    internal fun from(limits: XmlLimits) {
        maximumInputLength = limits.maximumInputLength
        maximumDepth = limits.maximumDepth
        maximumElements = limits.maximumElements
        maximumAttributes = limits.maximumAttributes
        maximumTextLength = limits.maximumTextLength
        maximumNameLength = limits.maximumNameLength
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
    }
}
