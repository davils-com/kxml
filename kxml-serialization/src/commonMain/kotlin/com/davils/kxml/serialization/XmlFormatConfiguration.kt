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

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kxml.core.Xml
import com.davils.kxml.core.name.XmlName
import kotlinx.serialization.modules.SerializersModule

/**
 * The settings of an [XmlFormat]; created by [xmlFormat].
 *
 * An instance is immutable.
 *
 * @since 1.0.0
 */
public class XmlFormatConfiguration internal constructor(builder: XmlFormatBuilder) : Validatable {
    /**
     * The reader and writer of the documents.
     *
     * @since 1.0.0
     */
    public val xml: Xml = builder.xml

    /**
     * The namespace of every element without an [XmlNamespace][com.davils.kxml.serialization.annotation.XmlNamespace]
     * of its own, or `null` for no namespace.
     *
     * @since 1.0.0
     */
    public val namespace: String? = builder.namespace

    /**
     * How the decoder compares names.
     *
     * @since 1.0.0
     */
    public val namespaceMatching: XmlNamespaceMatching = builder.namespaceMatching

    /**
     * Whether the decoder skips elements and attributes no property is decoded from instead of failing with
     * [UnknownProperty][com.davils.kxml.core.diagnostic.XmlDiagnosticCode.UnknownProperty].
     *
     * @since 1.0.0
     */
    public val isUnknownElementIgnored: Boolean = builder.isUnknownElementIgnored

    /**
     * Whether the encoder writes properties whose value equals their default.
     *
     * @since 1.0.0
     */
    public val isDefaultEncoded: Boolean = builder.isDefaultEncoded

    /**
     * Whether [XmlFormat.encodeToString] writes an XML declaration.
     *
     * @since 1.0.0
     */
    public val isDeclarationWritten: Boolean = builder.isDeclarationWritten

    /**
     * Whether the encoder writes a `null` value as an empty element with `xsi:nil="true"`.
     *
     * @since 1.0.0
     */
    public val isNilWritten: Boolean = builder.isNilWritten

    /**
     * How the names of properties are derived.
     *
     * @since 1.0.0
     */
    public val namingStrategy: XmlNamingStrategy = builder.namingStrategy

    /**
     * How a polymorphic value names its type.
     *
     * @since 1.0.0
     */
    public val polymorphism: XmlPolymorphism = builder.polymorphism

    /**
     * The name of the type attribute under [XmlPolymorphism.Attribute].
     *
     * @since 1.0.0
     */
    public val classDiscriminator: XmlName = builder.classDiscriminator

    /**
     * The module of contextual and polymorphic serializers.
     *
     * @since 1.0.0
     */
    public val serializersModule: SerializersModule = builder.serializersModule

    /**
     * Reports an empty [namespace] with [XmlFormatViolationCodes.EMPTY_NAMESPACE].
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (namespace?.isEmpty() == true) {
            fail(::namespace, "must not be empty, leave it out instead", XmlFormatViolationCodes.EMPTY_NAMESPACE)
        }
    }

    /**
     * Returns the settings except the reader and the module, with the namespace escaped.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String =
        "XmlFormatConfiguration(namespace=${namespace?.let(SafeText::quoted) ?: "none"}, " +
            "namespaceMatching=$namespaceMatching, " +
            "isUnknownElementIgnored=$isUnknownElementIgnored, isDefaultEncoded=$isDefaultEncoded, " +
            "isDeclarationWritten=$isDeclarationWritten, isNilWritten=$isNilWritten, polymorphism=$polymorphism, " +
            "classDiscriminator=$classDiscriminator)"
}
