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
package com.davils.kxml.document.patch

import com.davils.kxml.core.node.XmlDocument

/**
 * The outcome of applying an [XmlPatch]: the patched document or the error of the first operation that failed.
 *
 * @since 1.0.0
 */
public sealed interface XmlPatchResult {
    /**
     * Every operation applied.
     *
     * @param document the patched document.
     * @since 1.0.0
     */
    public class Applied internal constructor(document: XmlDocument) : XmlPatchResult {
        /**
         * The patched document.
         *
         * @since 1.0.0
         */
        public val document: XmlDocument = document

        /**
         * Returns `Applied`, without the document.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        override fun toString(): String = "Applied"
    }

    /**
     * An operation failed; the target document is unchanged.
     *
     * @param error why the operation failed.
     * @since 1.0.0
     */
    public class Failed internal constructor(error: XmlPatchError) : XmlPatchResult {
        /**
         * Why the operation failed.
         *
         * @since 1.0.0
         */
        public val error: XmlPatchError = error

        /**
         * Returns the error, such as `Failed(XmlPatchError(unlocated-node at 0: ...))`.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        override fun toString(): String = "Failed($error)"
    }
}
