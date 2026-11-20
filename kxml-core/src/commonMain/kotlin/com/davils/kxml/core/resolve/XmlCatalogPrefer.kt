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

package com.davils.kxml.core.resolve

/**
 * Which entries of a catalog an external identifier with both a public and a system identifier may match, the
 * `prefer` setting of section 4.1.1 of XML Catalogs 1.1.
 *
 * A `prefer` attribute on a `catalog` or `group` element overrides the setting for the entries it encloses; this
 * value is the setting where no attribute says otherwise.
 *
 * @since 1.0.0
 */
public enum class XmlCatalogPrefer {
    /**
     * `public` and `delegatePublic` entries match an external identifier whether or not it has a system identifier.
     *
     * @since 1.0.0
     */
    Public,

    /**
     * `public` and `delegatePublic` entries match only an external identifier without a system identifier.
     *
     * @since 1.0.0
     */
    System,
}
