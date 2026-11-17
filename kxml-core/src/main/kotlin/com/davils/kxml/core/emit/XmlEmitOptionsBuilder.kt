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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Collects the options of [XmlEmitOptions]; used through [XmlBuilder.emitter][com.davils.kxml.core.XmlBuilder.emitter].
 *
 * The builder starts from the current options and is not thread safe.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class XmlEmitOptionsBuilder internal constructor() : ValidatingBuilder<XmlEmitOptions>() {
    override val subject: String get() = "xml emitter"

    /**
     * The number of spaces per level; [DEFAULT_INDENT] by default, between 0 and [XmlEmitOptions.MAXIMUM_INDENT].
     *
     * @since 1.0.0
     */
    public var indent: Int = DEFAULT_INDENT

    /**
     * The line break between indented lines; [XmlLineBreak.Lf] by default.
     *
     * @since 1.0.0
     */
    public var lineBreak: XmlLineBreak = XmlLineBreak.Lf

    /**
     * Whether an element without children is written as `<a/>`; `true` by default.
     *
     * @since 1.0.0
     */
    public var isEmptyElementCollapsed: Boolean = true

    internal fun from(options: XmlEmitOptions) {
        indent = options.indent
        lineBreak = options.lineBreak
        isEmptyElementCollapsed = options.isEmptyElementCollapsed
    }

    override fun assemble(): XmlEmitOptions = XmlEmitOptions(this)

    /**
     * Holds the defaults.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The indentation of [XmlEmitOptions.Default].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_INDENT: Int = 2
    }
}
