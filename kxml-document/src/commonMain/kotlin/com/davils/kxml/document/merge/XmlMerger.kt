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

package com.davils.kxml.document.merge

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.name.XmlNamespaces
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.merge.trace.LayerMerging
import com.davils.kxml.document.merge.trace.OriginWriter
import com.davils.kxml.document.merge.trace.TracedNode

private const val LAYERS_LIMIT: String = "maximumMergeLayers"

/**
 * Merges layers of documents into one, such as defaults, an environment and local overrides, and records which layer
 * every node comes from.
 *
 * The first layer is the base and every further layer is merged onto the result of the ones before it. The document
 * elements of all layers must have the same expanded name. Merging an element of a layer onto the element it matches
 * sets its attributes, overriding those of the same expanded name, and adds the namespace declarations of prefixes the
 * element does not declare. When the element of the layer holds text other than white space, its content replaces the
 * content below; otherwise its child elements are matched as [options] describes, merged onto their matches, appended
 * where nothing matches and removed where they carry the deletion marker. Comments and processing instructions of a
 * layer are kept only in content it replaces, and the declaration, the prolog and the epilog are those of the base.
 * The names of nodes a layer contributes are mapped onto the namespace declarations in scope, as an XML patch maps
 * added content.
 *
 * The work grows with the nodes of all layers; the number of layers is bounded by [XmlLimits.maximumMergeLayers] of
 * [limits]. A merger is immutable and safe to share between threads.
 *
 * @property options how elements of a layer match those below it.
 * @property limits the limits of a merge.
 * @since 1.0.0
 */
public class XmlMerger(
    public val options: XmlMergeOptions = XmlMergeOptions(),
    public val limits: XmlLimits = XmlLimits.Untrusted,
) {
    /**
     * Merges [layers] in order, the first being the base.
     *
     * @param layers the layers, at least one.
     * @return the merged document and the origin of every node.
     * @throws IllegalArgumentException if [layers] is empty or a document element differs from the one of the base.
     * @throws XmlDocumentLimitException if there are more layers than [XmlLimits.maximumMergeLayers].
     * @since 1.0.0
     */
    public fun merge(layers: List<XmlLayer>): XmlMerged {
        require(layers.isNotEmpty()) { "A merge needs at least one layer" }
        if (layers.size > limits.maximumMergeLayers) throw XmlDocumentLimitException(LAYERS_LIMIT)
        val base = layers.first().document
        val root = TracedNode.of(base.root, layers.first().name) as TracedNode.Element
        layers.drop(1).forEach { layer ->
            require(layer.document.root.name == base.root.name) {
                "The layer ${SafeText.quoted(layer.name)} has another document element than the base"
            }
            LayerMerging(options, layer.name).merged(root, layer.document.root, xmlScope)
        }
        val document = XmlDocument(
            root = root.toNode(),
            declaration = base.declaration,
            prolog = base.prolog,
            epilog = base.epilog,
            doctype = base.doctype,
        )
        return XmlMerged(document, OriginWriter.originsOf(root))
    }

    /**
     * Returns whether [other] is a merger with equal options and limits.
     *
     * @param other the object to compare with.
     * @return `true` if [other] merges the same way.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is XmlMerger && options == other.options && limits == other.limits

    /**
     * Returns a hash code of the options.
     *
     * @return the hash code of [options].
     * @since 1.0.0
     */
    override fun hashCode(): Int = options.hashCode()

    /**
     * Returns the options, such as `XmlMerger(XmlMergeOptions(keys=0, deletion=none))`.
     *
     * @return a text that is safe to log.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlMerger($options)"

    private companion object {
        val xmlScope: Map<String, String> = mapOf(XmlNamespaces.XML_PREFIX to XmlNamespaces.XML)
    }
}
