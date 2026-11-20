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

package com.davils.kxml.core.dtd

internal enum class AttributeType(val keyword: String, val isTokenized: Boolean) {
    CData(keyword = "CDATA", isTokenized = false),
    IdRefs(keyword = "IDREFS", isTokenized = true),
    IdRef(keyword = "IDREF", isTokenized = true),
    Id(keyword = "ID", isTokenized = true),
    Entities(keyword = "ENTITIES", isTokenized = true),
    Entity(keyword = "ENTITY", isTokenized = true),
    NmTokens(keyword = "NMTOKENS", isTokenized = true),
    NmToken(keyword = "NMTOKEN", isTokenized = true),
    Notation(keyword = "NOTATION", isTokenized = true),
    Enumeration(keyword = "(", isTokenized = true),
}
