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

/**
 * How the decoder compares the names of the document with the names the type expects.
 *
 * @since 1.0.0
 */
public enum class XmlNamespaceMatching {
    /**
     * The local names and the namespaces must be equal.
     *
     * @since 1.0.0
     */
    Exact,

    /**
     * Only the local names must be equal; for documents whose producers are careless with namespaces.
     *
     * @since 1.0.0
     */
    LocalName,
}
