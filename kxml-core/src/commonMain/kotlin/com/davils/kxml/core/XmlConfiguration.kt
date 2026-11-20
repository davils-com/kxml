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

import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kxml.core.emit.XmlEmitOptions
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.resolve.XmlResolver
import com.davils.kore.core.validation.validate as report

/**
 * The settings an [Xml] reads and writes with; created by [xml].
 *
 * An instance is immutable.
 *
 * @since 1.0.0
 */
public class XmlConfiguration internal constructor(builder: XmlBuilder) : Validatable {
    /**
     * How the reader treats a document type declaration.
     *
     * @since 1.0.0
     */
    public val doctype: XmlDoctype = builder.doctype

    /**
     * The resolver of external resources under [XmlDoctype.External], or `null`.
     *
     * @since 1.0.0
     */
    public val resolver: XmlResolver? = builder.resolver

    /**
     * Which whitespace the tree keeps.
     *
     * @since 1.0.0
     */
    public val whitespace: XmlWhitespace = builder.whitespace

    /**
     * The limits the reader enforces.
     *
     * @since 1.0.0
     */
    public val limits: XmlLimits = builder.limits

    /**
     * How the writer lays out its output.
     *
     * @since 1.0.0
     */
    public val emitter: XmlEmitOptions = builder.emitter

    /**
     * Reports the violations of [limits] and [emitter] under their names, and
     * [RESOLVER_MISMATCH][XmlViolationCodes.RESOLVER_MISMATCH] when [doctype] is [XmlDoctype.External] without a
     * [resolver] or another value with one.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        val isExternal = doctype == XmlDoctype.External
        if (isExternal != (resolver != null)) {
            fail(::resolver, "is needed by and only by XmlDoctype.External", XmlViolationCodes.RESOLVER_MISMATCH)
        }
        within(::limits) { include(limits.report("limits").violations) }
        within(::emitter) { include(emitter.report("emitter").violations) }
    }

    /**
     * Returns every setting, such as `XmlConfiguration(doctype=Refuse, resolver=none, ...)`; a resolver is shown only
     * as `set`.
     *
     * @return a text for diagnostics.
     * @since 1.0.0
     */
    override fun toString(): String {
        val shownResolver = RESOLVER_SET.takeIf { resolver != null } ?: RESOLVER_NONE
        return "XmlConfiguration(doctype=$doctype, resolver=$shownResolver, whitespace=$whitespace, " +
            "limits=$limits, emitter=$emitter)"
    }

    private companion object {
        const val RESOLVER_SET: String = "set"
        const val RESOLVER_NONE: String = "none"
    }
}
