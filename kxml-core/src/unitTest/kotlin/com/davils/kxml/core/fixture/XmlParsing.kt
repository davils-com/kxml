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

package com.davils.kxml.core.fixture

import com.davils.kxml.core.Xml
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.core.node.XmlNode
import io.kotest.matchers.types.shouldBeInstanceOf

internal const val CR: String = "\r"
internal const val LF: String = "\n"
internal const val CRLF: String = "\r\n"
internal const val TAB: String = "\t"
internal const val QUOTE: String = "\""

internal fun Xml.document(text: String): XmlDocument {
    val parsed = parse(text).shouldBeInstanceOf<XmlParseResult.Parsed>()
    return parsed.document
}

internal fun Xml.root(text: String): XmlNode.Element {
    val document = document(text)
    return document.root
}

internal fun Xml.refusal(text: String): XmlDiagnosticCode {
    val failed = parse(text).shouldBeInstanceOf<XmlParseResult.Failed>()
    return failed.diagnostics.last().code
}
