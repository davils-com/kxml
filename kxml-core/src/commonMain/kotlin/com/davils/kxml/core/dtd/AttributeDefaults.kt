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

package com.davils.kxml.core.dtd

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet
import com.davils.kxml.core.parser.RawAttribute
import com.davils.kxml.core.parser.RawTag

private const val SPACE: String = " "

internal object AttributeDefaults {
    fun applied(tag: RawTag, dtd: Dtd): RawTag {
        val definitions = dtd.attributes[tag.qualified] ?: return tag
        val names = OrderedSet<String>()
        val specified = tag.attributes.map { raw ->
            names.add(raw.qualified)
            val type = definitions[raw.qualified]?.type
            raw.copy(value = collapsed(raw.value)).takeIf { type?.isTokenized == true } ?: raw
        }
        val defaults = definitions.entries.mapNotNull { (name, definition) ->
            val value = definition.defaultValue?.takeIf { name !in names }
            value?.let { RawAttribute(qualified = name, value = it, range = tag.range) }
        }
        return tag.copy(attributes = specified + defaults)
    }

    fun collapsed(value: String): String {
        val tokens = value.split(SPACE).filter(String::isNotEmpty)
        return tokens.joinToString(separator = SPACE)
    }
}
