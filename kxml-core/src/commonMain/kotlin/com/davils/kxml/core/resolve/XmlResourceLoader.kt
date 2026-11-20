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

import com.davils.kxml.core.source.XmlSource

/**
 * Loads the resource behind an absolute URI, such as a catalog file or the target a catalog maps an identifier to.
 *
 * A loader decides what a URI may reach, so it should load only what it trusts, such as files below one directory,
 * and refuse every other URI by returning `null`. The URI comes from a catalog or a document and is input. An
 * exception the loader throws is not caught. A loader may be called from several threads at once when a resolver is
 * shared.
 *
 * @since 1.0.0
 */
public fun interface XmlResourceLoader {
    /**
     * Returns the resource at [uri], or `null` to refuse it.
     *
     * @param uri an absolute URI reference, escaped as section 4.2.2 of XML 1.0 describes it.
     * @return the resource, or `null`.
     * @since 1.0.0
     */
    public fun load(uri: String): XmlSource?
}
