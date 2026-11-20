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
package com.davils.kxml.document.xpath.evaluate

private const val ORDINAL_SHIFT: Int = 32
private const val INDEX_MASK: Long = 0xFFFF_FFFFL

internal object NodeKeys {
    fun keyOf(ordinal: Int): Long = ordinal.toLong() shl ORDINAL_SHIFT

    fun namespaceKeyOf(element: Int, index: Int): Long = keyOf(element) + index + 1

    fun ordinalOf(key: Long): Int = (key shr ORDINAL_SHIFT).toInt()

    fun namespaceIndexOf(key: Long): Int = (key and INDEX_MASK).toInt() - 1

    fun isNamespace(key: Long): Boolean = key and INDEX_MASK != 0L
}
