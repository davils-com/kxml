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
@file:OptIn(DavilsInternal::class)

package com.davils.kxml.document.patch

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.patch.apply.DiffReader
import com.davils.kxml.document.patch.apply.PatchApplier
import com.davils.kxml.document.patch.apply.PatchFailure
import com.davils.kxml.document.patch.apply.PatchWriter

private const val OPERATIONS_LIMIT: String = "maximumPatchOperations"

/**
 * An XML patch of RFC 5261: `<add>`, `<replace>` and `<remove>` operations applied in order, each to the document the
 * operation before it produced.
 *
 * A patch is atomic: [apply] returns the patched document only when every operation applied, and otherwise the error
 * of the first operation that failed, with the target document unchanged, since documents are immutable. Every
 * selector must locate exactly one node. Removing the document element, giving it a sibling element or putting text
 * outside it fails, and so does removing a namespace declaration that a name still uses. Text and CDATA nodes may end
 * up adjacent, which is one text node of the XPath data model, as section 4.3.5 of RFC 5261 requires.
 *
 * Every application is bounded by [limits]: the number of operations by [XmlLimits.maximumPatchOperations], and the
 * work, the documents it builds and the selectors it evaluates, by [XmlLimits.maximumQuerySteps] and the other query
 * limits. A patch is immutable and safe to share between threads.
 *
 * @param operations the operations in the order they apply.
 * @property limits the limits the patch applies under.
 * @throws XmlDocumentLimitException if there are more operations than [XmlLimits.maximumPatchOperations].
 * @since 1.0.0
 */
public class XmlPatch(operations: List<XmlPatchOperation>, public val limits: XmlLimits = XmlLimits.Untrusted) {
    /**
     * The operations in the order they apply, which cannot be changed.
     *
     * @since 1.0.0
     */
    public val operations: List<XmlPatchOperation> = operations.immutableList()

    init {
        if (operations.size > limits.maximumPatchOperations) throw XmlDocumentLimitException(OPERATIONS_LIMIT)
    }

    /**
     * Applies the operations to [document] in order.
     *
     * @param document the target document.
     * @return the patched document, or the error of the first operation that failed.
     * @throws XmlDocumentLimitException if the application needs more work than [XmlLimits.maximumQuerySteps] or
     * exceeds another query limit.
     * @since 1.0.0
     */
    public fun apply(document: XmlDocument): XmlPatchResult = PatchApplier(limits).applied(document, operations)

    /**
     * Writes the patch as a diff document of RFC 5261: a `diff` element of no namespace with an `add`, `replace` or
     * `remove` element for every operation, which declares the namespaces of the operation and holds its content.
     * [read] reads the document back as an equal patch.
     *
     * @return the diff document.
     * @since 1.0.0
     */
    public fun toDocument(): XmlDocument = PatchWriter.documentOf(operations)

    /**
     * Returns whether [other] is a patch with equal operations and limits.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an equal patch.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is XmlPatch && operations == other.operations && limits == other.limits

    /**
     * Returns a hash code of the operations.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = operations.hashCode()

    /**
     * Returns the number of operations, such as `XmlPatch(operations=3)`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlPatch(operations=${operations.size})"

    /**
     * Reads patch documents.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads [diff] as a patch document of RFC 5261: the children of its document element are `<add>`, `<replace>`
         * and `<remove>` elements of any namespace, with white space, comments and processing instructions between
         * them. The namespace declarations in scope on each operation resolve its prefixes.
         *
         * @param diff the patch document.
         * @param limits the limits the patch is read and applied under; [XmlLimits.Untrusted] by default.
         * @return the patch, or why [diff] is none, with the index of the operation that is wrong.
         * @throws XmlDocumentLimitException if [diff] holds more operations than [XmlLimits.maximumPatchOperations].
         * @since 1.0.0
         */
        public fun read(diff: XmlDocument, limits: XmlLimits = XmlLimits.Untrusted): XmlPatchReading {
            val reader = DiffReader(limits)
            return try {
                XmlPatchReading.Read(XmlPatch(reader.operations(diff), limits))
            } catch (failure: PatchFailure) {
                XmlPatchReading.Refused(XmlPatchError(failure.code, reader.failedAt(), failure.reason))
            }
        }
    }
}
