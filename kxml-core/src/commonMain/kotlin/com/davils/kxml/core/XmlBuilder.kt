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

package com.davils.kxml.core

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import com.davils.kxml.core.emit.XmlEmitOptions
import com.davils.kxml.core.emit.XmlEmitOptionsBuilder
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.limit.XmlLimitsBuilder
import com.davils.kxml.core.resolve.XmlResolver

/**
 * Collects the settings of an [XmlConfiguration]; used through [xml].
 *
 * Every setting starts at the safe default. The builder is not thread safe and is meant for one configuration only.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class XmlBuilder internal constructor() : ValidatingBuilder<XmlConfiguration>() {
    override val subject: String get() = "xml"

    /**
     * How the reader treats a document type declaration; [XmlDoctype.Refuse] by default.
     *
     * @since 1.0.0
     */
    public var doctype: XmlDoctype = XmlDoctype.Refuse

    /**
     * The resolver of external resources, which [XmlDoctype.External] needs and every other [doctype] refuses; `null`
     * by default.
     *
     * @since 1.0.0
     */
    public var resolver: XmlResolver? = null

    /**
     * Which whitespace the tree keeps; [XmlWhitespace.Keep] by default.
     *
     * @since 1.0.0
     */
    public var whitespace: XmlWhitespace = XmlWhitespace.Keep

    /**
     * The limits the reader enforces; [XmlLimits.Untrusted] by default.
     *
     * @since 1.0.0
     */
    public var limits: XmlLimits = XmlLimits.Untrusted

    /**
     * How the writer lays out its output; [XmlEmitOptions.Default] by default.
     *
     * @since 1.0.0
     */
    public var emitter: XmlEmitOptions = XmlEmitOptions.Default

    /**
     * Changes [limits], starting from the current value.
     *
     * Invalid limits are reported by [build] under the path `limits`, together with every other problem.
     *
     * @param configure the changes to the limits.
     * @since 1.0.0
     */
    public fun limits(configure: XmlLimitsBuilder.() -> Unit) {
        val builder = XmlLimitsBuilder()
        builder.from(limits)
        nested(builder, ::limits, configure)?.let { changed -> limits = changed }
    }

    /**
     * Changes [emitter], starting from the current value.
     *
     * Invalid options are reported by [build] under the path `emitter`, together with every other problem.
     *
     * @param configure the changes to the options.
     * @since 1.0.0
     */
    public fun emitter(configure: XmlEmitOptionsBuilder.() -> Unit) {
        val builder = XmlEmitOptionsBuilder()
        builder.from(emitter)
        nested(builder, ::emitter, configure)?.let { changed -> emitter = changed }
    }

    override fun assemble(): XmlConfiguration = XmlConfiguration(this)
}
