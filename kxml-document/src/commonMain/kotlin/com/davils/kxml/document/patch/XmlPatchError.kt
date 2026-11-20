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
 * Why a patch of RFC 5261 could not be read or applied.
 *
 * @property code the error condition.
 * @property operation the index of the operation that failed, from 0 in document order.
 * @property message a description of constant text that holds nothing of the documents.
 * @since 1.0.0
 */
public class XmlPatchError internal constructor(
    public val code: XmlPatchErrorCode,
    public val operation: Int,
    public val message: String,
) {
    private val values: List<Any> get() = listOf(code, operation, message)

    /**
     * Returns whether [other] is an error with the same code, operation and message.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an equal error.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is XmlPatchError && values == other.values

    /**
     * Returns a hash code of the code, the operation and the message.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Returns the code, the operation and the message, such as `XmlPatchError(unlocated-node at 2: ...)`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlPatchError(${code.element} at $operation: $message)"
}
