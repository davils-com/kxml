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

package com.davils.kxml.serialization

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kxml.core.diagnostic.XmlDiagnostic
import kotlinx.serialization.SerializationException

/**
 * Thrown by [XmlFormat.decodeFromString] and [XmlFormat.decodeFromElement] when a document cannot be decoded.
 *
 * The message lists every diagnostic with its line and column; the parts of it that come from the document are
 * escaped and shortened, so it is safe to log. Use [XmlFormat.decodeOrFailure] to get the diagnostics without an
 * exception.
 *
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class XmlDecodingException internal constructor(
    diagnostics: List<XmlDiagnostic>,
    message: String,
) : SerializationException(message) {
    /**
     * The diagnostics of the reader and the decoder, with at least one error.
     *
     * @since 1.0.0
     */
    public val diagnostics: List<XmlDiagnostic> = diagnostics.immutableList()
}
