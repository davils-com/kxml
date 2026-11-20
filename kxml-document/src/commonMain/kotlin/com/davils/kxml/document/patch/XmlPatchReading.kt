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

/**
 * The outcome of reading a patch document of RFC 5261 into an [XmlPatch].
 *
 * @since 1.0.0
 */
public sealed interface XmlPatchReading {
    /**
     * The document is a patch.
     *
     * @param patch the patch, ready to apply.
     * @since 1.0.0
     */
    public class Read internal constructor(patch: XmlPatch) : XmlPatchReading {
        /**
         * The patch, ready to apply.
         *
         * @since 1.0.0
         */
        public val patch: XmlPatch = patch

        /**
         * Returns the number of operations, such as `Read(operations=3)`.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        override fun toString(): String = "Read(operations=${patch.operations.size})"
    }

    /**
     * The document is no patch.
     *
     * @param error why it was refused.
     * @since 1.0.0
     */
    public class Refused internal constructor(error: XmlPatchError) : XmlPatchReading {
        /**
         * Why the document was refused.
         *
         * @since 1.0.0
         */
        public val error: XmlPatchError = error

        /**
         * Returns the error, such as `Refused(XmlPatchError(invalid-diff-format at 1: ...))`.
         *
         * @return a text that is safe to log.
         * @since 1.0.0
         */
        override fun toString(): String = "Refused($error)"
    }
}
