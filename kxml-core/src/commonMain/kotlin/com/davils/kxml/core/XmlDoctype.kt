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

/**
 * How the reader treats a document type declaration, `<!DOCTYPE ...>`.
 *
 * kxml never loads anything on its own: an external subset or an external entity is read only through a resolver the
 * caller hands to the reader, and without one a reference that needs it ends reading. Every expansion of an entity
 * is charged to [XmlLimits.maximumEntityExpansion][com.davils.kxml.core.limit.XmlLimits.maximumEntityExpansion] while
 * it is done, so a billion laughs document fails at its budget.
 *
 * @since 1.0.0
 */
public enum class XmlDoctype {
    /**
     * Stops the reader with [RefusedDoctype][com.davils.kxml.core.diagnostic.XmlDiagnosticCode.RefusedDoctype]. The
     * default: a document type declaration changes what a document means through its entities and attribute defaults,
     * which a document from outside rarely needs and a signed document must not get.
     *
     * @since 1.0.0
     */
    Refuse,

    /**
     * Reads the internal subset: its entities are expanded within the limits, its attribute defaults are added and
     * its attribute types normalize the values, as a non-validating processor of XML 1.0 does. Element declarations
     * are checked to be well formed but not validated.
     *
     * @since 1.0.0
     */
    Internal,
}
