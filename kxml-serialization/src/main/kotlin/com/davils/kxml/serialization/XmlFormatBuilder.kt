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

package com.davils.kxml.serialization

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import com.davils.kxml.core.Xml
import com.davils.kxml.core.XmlBuilder
import com.davils.kxml.core.name.XmlName
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import com.davils.kxml.core.xml as buildXml

/**
 * Collects the settings of an [XmlFormatConfiguration]; used through [xmlFormat].
 *
 * The builder is not thread safe and is meant for one configuration only.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class XmlFormatBuilder internal constructor() : ValidatingBuilder<XmlFormatConfiguration>() {
    override val subject: String get() = "xml format"

    /**
     * The reader and writer of the documents; [Xml.Default] by default.
     *
     * @since 1.0.0
     */
    public var xml: Xml = Xml.Default

    /**
     * The namespace of every element without an [XmlNamespace][com.davils.kxml.serialization.annotation.XmlNamespace]
     * of its own; `null`, no namespace, by default.
     *
     * @since 1.0.0
     */
    public var namespace: String? = null

    /**
     * How the decoder compares names; [XmlNamespaceMatching.Exact] by default.
     *
     * @since 1.0.0
     */
    public var namespaceMatching: XmlNamespaceMatching = XmlNamespaceMatching.Exact

    /**
     * Whether the decoder skips unknown elements and attributes; `false` by default.
     *
     * @since 1.0.0
     */
    public var isUnknownElementIgnored: Boolean = false

    /**
     * Whether the encoder writes properties whose value equals their default; `true` by default.
     *
     * @since 1.0.0
     */
    public var isDefaultEncoded: Boolean = true

    /**
     * Whether an XML declaration is written; `true` by default.
     *
     * @since 1.0.0
     */
    public var isDeclarationWritten: Boolean = true

    /**
     * How the names of properties are derived; [XmlNamingStrategy.Identity] by default.
     *
     * @since 1.0.0
     */
    public var namingStrategy: XmlNamingStrategy = XmlNamingStrategy.Identity

    /**
     * How a polymorphic value names its type; [XmlPolymorphism.Attribute] by default.
     *
     * @since 1.0.0
     */
    public var polymorphism: XmlPolymorphism = XmlPolymorphism.Attribute

    /**
     * The name of the type attribute; [DEFAULT_DISCRIMINATOR] in no namespace by default.
     *
     * @since 1.0.0
     */
    public var classDiscriminator: XmlName = XmlName(DEFAULT_DISCRIMINATOR)

    /**
     * The module of contextual and polymorphic serializers; empty by default.
     *
     * @since 1.0.0
     */
    public var serializersModule: SerializersModule = EmptySerializersModule()

    /**
     * Replaces [xml] with a reader and writer configured by [configure].
     *
     * @param configure the settings, starting from the defaults of [XmlBuilder].
     * @throws com.davils.kore.core.validation.report.ValidationException if a setting is invalid.
     * @since 1.0.0
     */
    public fun xml(configure: XmlBuilder.() -> Unit) {
        xml = buildXml(configure)
    }

    override fun assemble(): XmlFormatConfiguration = XmlFormatConfiguration(this)

    /**
     * Holds the defaults.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The local name of the default type attribute, `type`.
         *
         * @since 1.0.0
         */
        public const val DEFAULT_DISCRIMINATOR: String = "type"
    }
}
