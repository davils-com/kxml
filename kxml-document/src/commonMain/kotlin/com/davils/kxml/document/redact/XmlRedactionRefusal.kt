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
package com.davils.kxml.document.redact

/**
 * Why a redaction of a syntax tree refused to redact it.
 *
 * @since 1.0.0
 */
public enum class XmlRedactionRefusal {
    /**
     * The text of the tree does not compose into a document without errors.
     *
     * @since 1.0.0
     */
    InvalidDocument,

    /**
     * A path, a walk or the text a pattern would read exceeds a limit.
     *
     * @since 1.0.0
     */
    LimitExceeded,

    /**
     * The redaction cannot decide how to redact the text in place, such as a secret attribute that a document type
     * declaration supplies by default, or a secret that comes from an entity declared outside the document; or the
     * redacted text would not compose into a document that is redacted.
     *
     * @since 1.0.0
     */
    Undecidable,
}
