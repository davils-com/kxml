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

package com.davils.kxml.serialization.fixture

import com.davils.kxml.serialization.annotation.XmlAsAttribute
import com.davils.kxml.serialization.annotation.XmlAsText
import com.davils.kxml.serialization.annotation.XmlNames
import com.davils.kxml.serialization.annotation.XmlNamespace
import kotlinx.serialization.Serializable

@Serializable
@XmlNamespace("urn:annotated", prefix = "a")
internal data class Annotated(
    @XmlAsAttribute
    val id: Int,
    @XmlAsAttribute
    @XmlNamespace("urn:other", prefix = "o")
    val origin: String? = null,
    @XmlAsText
    val text: String,
    @XmlNames("Version", "Revision")
    val version: VersionId? = null,
)
