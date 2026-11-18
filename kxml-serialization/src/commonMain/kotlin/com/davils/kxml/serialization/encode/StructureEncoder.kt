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

package com.davils.kxml.serialization.encode

import kotlinx.serialization.descriptors.SerialDescriptor

internal class StructureEncoder(
    context: EncodeContext,
    private val collector: ContentCollector,
    private val parent: (Content) -> Unit,
) : ValueEncoder(context, parent) {
    private var index: Int = 0

    override fun emit(content: Content) {
        collector.add(index, content)
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        this.index = index
        return true
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        parent(collector.build())
    }
}
