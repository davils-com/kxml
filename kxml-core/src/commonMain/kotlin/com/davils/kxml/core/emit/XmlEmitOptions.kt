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

package com.davils.kxml.core.emit

import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kxml.core.XmlViolationCodes

/**
 * How the writer lays out its output; created with [XmlBuilder.emitter][com.davils.kxml.core.XmlBuilder.emitter].
 *
 * An instance is immutable. Options built with an invalid [indent] are refused when the configuration is built.
 *
 * @since 1.0.0
 */
public class XmlEmitOptions internal constructor(builder: XmlEmitOptionsBuilder) : Validatable {
    /**
     * The number of spaces per level; 0 writes everything on one line.
     *
     * @since 1.0.0
     */
    public val indent: Int = builder.indent

    /**
     * The line break between indented lines.
     *
     * @since 1.0.0
     */
    public val lineBreak: XmlLineBreak = builder.lineBreak

    /**
     * Whether an element without children is written as `<a/>` instead of `<a></a>`.
     *
     * @since 1.0.0
     */
    public val isEmptyElementCollapsed: Boolean = builder.isEmptyElementCollapsed

    private val values: List<Any> get() = listOf(indent, lineBreak, isEmptyElementCollapsed)

    /**
     * Reports an [indent] outside `0..`[MAXIMUM_INDENT] with [XmlViolationCodes.INVALID_INDENT].
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (indent !in 0..MAXIMUM_INDENT) {
            fail(
                ::indent,
                "must be between 0 and $MAXIMUM_INDENT, but was $indent",
                XmlViolationCodes.INVALID_INDENT,
            )
        }
    }

    /**
     * Compares every option.
     *
     * @param other the value to compare with.
     * @return whether [other] holds the same options.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean {
        val isSame = other is XmlEmitOptions && values == other.values
        return this === other || isSame
    }

    /**
     * Returns a hash code derived from every option.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns every option, such as `XmlEmitOptions(indent=2, lineBreak=Lf, isEmptyElementCollapsed=true)`.
     *
     * @return a text for diagnostics.
     * @since 1.0.0
     */
    override fun toString(): String =
        "XmlEmitOptions(indent=$indent, lineBreak=$lineBreak, isEmptyElementCollapsed=$isEmptyElementCollapsed)"

    /**
     * Holds the bounds and the predefined options.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The largest indentation the writer accepts.
         *
         * @since 1.0.0
         */
        public const val MAXIMUM_INDENT: Int = 16

        /**
         * Indents by two spaces, breaks lines with a line feed and collapses empty elements.
         *
         * @since 1.0.0
         */
        public val Default: XmlEmitOptions = XmlEmitOptionsBuilder().build()

        /**
         * Writes everything on one line and collapses empty elements.
         *
         * @since 1.0.0
         */
        public val Compact: XmlEmitOptions = compact()

        private fun compact(): XmlEmitOptions {
            val builder = XmlEmitOptionsBuilder()
            builder.indent = 0
            return builder.build()
        }
    }
}
