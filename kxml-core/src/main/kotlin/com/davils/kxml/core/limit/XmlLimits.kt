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

import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kxml.core.XmlViolationCodes
import kotlin.reflect.KProperty0

/**
 * The bounds the reader checks before its work grows with the input.
 *
 * A document that breaks a limit is not read further; the result carries the diagnostic of the limit, such as
 * [TooDeep][com.davils.kxml.core.diagnostic.XmlDiagnosticCode.TooDeep]. Use [Untrusted] for input from outside,
 * [Trusted] only for documents the application wrote itself, and [xmlLimits] for anything in between. An instance is
 * immutable.
 *
 * @since 1.0.0
 */
public class XmlLimits internal constructor(builder: XmlLimitsBuilder) : Validatable {
    /**
     * The longest source in characters, or in bytes for a source of bytes.
     *
     * @since 1.0.0
     */
    public val maximumInputLength: Int = builder.maximumInputLength

    /**
     * The deepest nesting of elements; never above [HARD_DEPTH].
     *
     * @since 1.0.0
     */
    public val maximumDepth: Int = builder.maximumDepth

    /**
     * The most elements in one document.
     *
     * @since 1.0.0
     */
    public val maximumElements: Long = builder.maximumElements

    /**
     * The most attributes on one element.
     *
     * @since 1.0.0
     */
    public val maximumAttributes: Int = builder.maximumAttributes

    /**
     * The longest run of text.
     *
     * @since 1.0.0
     */
    public val maximumTextLength: Int = builder.maximumTextLength

    /**
     * The longest name.
     *
     * @since 1.0.0
     */
    public val maximumNameLength: Int = builder.maximumNameLength

    private val values: List<Long>
        get() = listOf(
            maximumInputLength.toLong(),
            maximumDepth.toLong(),
            maximumElements,
            maximumAttributes.toLong(),
            maximumTextLength.toLong(),
            maximumNameLength.toLong(),
        )

    /**
     * Reports every limit below 1 with [XmlViolationCodes.NOT_POSITIVE] and a depth above [HARD_DEPTH] with
     * [XmlViolationCodes.ABOVE_HARD_LIMIT].
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        positive(::maximumInputLength, maximumInputLength.toLong())
        positive(::maximumDepth, maximumDepth.toLong())
        positive(::maximumElements, maximumElements)
        positive(::maximumAttributes, maximumAttributes.toLong())
        positive(::maximumTextLength, maximumTextLength.toLong())
        positive(::maximumNameLength, maximumNameLength.toLong())
        if (maximumDepth > HARD_DEPTH) {
            fail(
                ::maximumDepth,
                "must not exceed $HARD_DEPTH, the depth the reader and the writer can nest safely",
                XmlViolationCodes.ABOVE_HARD_LIMIT,
            )
        }
    }

    /**
     * Compares every limit.
     *
     * @param other the value to compare with.
     * @return whether [other] holds the same limits.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        val isSame = other is XmlLimits && values == other.values
        return this === other || isSame
    }

    /**
     * Returns a hash code derived from every limit.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns every limit, such as `XmlLimits(input=67108864, depth=256, ...)`.
     *
     * @return a text for diagnostics.
     * @since 1.0.0
     */
    override fun toString(): String =
        "XmlLimits(input=$maximumInputLength, depth=$maximumDepth, elements=$maximumElements, " +
            "attributes=$maximumAttributes, text=$maximumTextLength, name=$maximumNameLength)"

    private fun ValidationScope.positive(property: KProperty0<*>, value: Long) {
        if (value < 1) fail(property, "must be at least 1, but was $value", XmlViolationCodes.NOT_POSITIVE)
    }

    /**
     * Holds the hard bound and the predefined limits.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The deepest nesting the reader and the writer handle without running out of stack.
         *
         * @since 1.0.0
         */
        public const val HARD_DEPTH: Int = 1000

        /**
         * The defaults of [XmlLimitsBuilder], meant for input from outside.
         *
         * @since 1.0.0
         */
        public val Untrusted: XmlLimits = XmlLimitsBuilder().build()

        /**
         * No bound but [HARD_DEPTH]; only for documents the application wrote itself.
         *
         * @since 1.0.0
         */
        public val Trusted: XmlLimits = trusted()

        private fun trusted(): XmlLimits {
            val builder = XmlLimitsBuilder()
            builder.maximumInputLength = Int.MAX_VALUE
            builder.maximumDepth = HARD_DEPTH
            builder.maximumElements = Long.MAX_VALUE
            builder.maximumAttributes = Int.MAX_VALUE
            builder.maximumTextLength = Int.MAX_VALUE
            builder.maximumNameLength = Int.MAX_VALUE
            return builder.build()
        }
    }
}
