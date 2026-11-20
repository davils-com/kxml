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

package com.davils.kxml.document.diff

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.budget.WorkBudget
import com.davils.kore.core.budget.WorkLimitException
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.diff.align.DiffWalk
import com.davils.kxml.document.diff.align.NodeSource
import com.davils.kxml.document.navigation.DocumentModel
import com.davils.kxml.document.patch.XmlPatch

private const val WORK_LIMIT: String = "maximumDiffWork"

/**
 * Computes an XML patch of RFC 5261 that turns one document into another.
 *
 * The diff works on the data model of XPath 1.0: adjacent text and CDATA sections are one text node, and the XML
 * declaration and the document type declaration are not compared. Applying the patch to the old document gives a
 * document whose canonical form with comments equals the one of the new document. The children of matching elements
 * are aligned by their kind, their expanded name and, for texts, comments and processing instructions, their value;
 * attributes are compared by their expanded names. Every selector of the patch is positional, counting the children
 * of each element with `node()`, and the operations are ordered so that each selector still locates its node when the
 * operations before it have been applied.
 *
 * The work is bounded by [XmlLimits.maximumDiffWork] of [limits]: every node visited and every cell of an alignment
 * counts. An alignment that would exceed what remains of it, an element whose namespace declarations changed and a
 * change that would join two texts replace the element as a whole instead, so the patch stays correct and only grows
 * coarser. The patch holds at most [XmlLimits.maximumPatchOperations] operations. A diff is immutable and safe to
 * share between threads.
 *
 * @property limits the limits of the diff and of the patch it makes.
 * @since 1.0.0
 */
public class XmlDiff(public val limits: XmlLimits = XmlLimits.Untrusted) {
    /**
     * Computes the patch that turns [old] into [new].
     *
     * @param old the document the patch applies to.
     * @param new the document the patch produces.
     * @return the patch, empty if the documents are equal.
     * @throws XmlDocumentLimitException if the diff visits more nodes than [XmlLimits.maximumDiffWork] allows or the
     * patch would hold more operations than [XmlLimits.maximumPatchOperations].
     * @since 1.0.0
     */
    public fun diff(old: XmlDocument, new: XmlDocument): XmlPatch {
        val newModel = DocumentModel.of(new)
        val budget = WorkBudget(maximum = limits.maximumDiffWork, limit = WORK_LIMIT)
        val walk = DiffWalk(
            old = DocumentModel.of(old),
            new = newModel,
            source = NodeSource(new, newModel),
            budget = budget,
        )
        val operations = try {
            walk.operations()
        } catch (_: WorkLimitException) {
            throw XmlDocumentLimitException(WORK_LIMIT)
        }
        return XmlPatch(operations, limits)
    }

    /**
     * Returns whether [other] is a diff with equal limits.
     *
     * @param other the object to compare with.
     * @return `true` if [other] is an [XmlDiff] with equal [limits].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is XmlDiff && limits == other.limits

    /**
     * Returns a hash code of the limits.
     *
     * @return the hash code of [limits].
     * @since 1.0.0
     */
    override fun hashCode(): Int = limits.hashCode()

    /**
     * Returns `XmlDiff` and the work it may take, such as `XmlDiff(work=10000000)`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlDiff(work=${limits.maximumDiffWork})"
}
