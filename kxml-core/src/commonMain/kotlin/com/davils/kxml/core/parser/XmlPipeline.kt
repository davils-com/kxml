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

package com.davils.kxml.core.parser

import com.davils.kore.core.text.TextRange
import com.davils.kore.core.validation.violation.Severity
import com.davils.kxml.core.XmlConfiguration
import com.davils.kxml.core.XmlEventResult
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.compose.TreeComposer
import com.davils.kxml.core.diagnostic.XmlDiagnostic
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.source.DecodedSource
import com.davils.kxml.core.source.SourceDecoding
import com.davils.kxml.core.source.XmlSource

internal class XmlPipeline(private val configuration: XmlConfiguration) {
    fun parse(source: XmlSource): XmlParseResult {
        val result = events(source)
        if (!result.isValid) return XmlParseResult.Failed(result.diagnostics)
        val document = TreeComposer(configuration.whitespace).compose(result.events)
        return XmlParseResult.Parsed(document, result.diagnostics)
    }

    fun events(source: XmlSource): XmlEventResult {
        val refusal = refusalOf(source) ?: return decoded(source)
        return XmlEventResult(emptyList(), listOf(refusal))
    }

    private fun refusalOf(source: XmlSource): XmlDiagnostic? {
        val limit = configuration.limits.maximumInputLength
        if (source.length <= limit) return null
        return XmlDiagnostic(
            XmlDiagnosticCode.InputTooLong,
            TextRange.at(0),
            "The source is longer than $limit characters",
        )
    }

    private fun decoded(source: XmlSource): XmlEventResult {
        val decoded = SourceDecoding.decode(source)
        val isRefused = decoded.diagnostics.any { diagnostic -> diagnostic.severity == Severity.Error }
        if (isRefused) return XmlEventResult(emptyList(), decoded.diagnostics)
        return scanned(decoded)
    }

    private fun scanned(decoded: DecodedSource): XmlEventResult {
        val scanner = XmlScanner(decoded.text, configuration, decoded.encoding)
        val failure = failureOf(scanner)
        return XmlEventResult(scanner.events, decoded.diagnostics + scanner.warnings + listOfNotNull(failure))
    }

    private fun failureOf(scanner: XmlScanner): XmlDiagnostic? = try {
        scanner.scan()
        null
    } catch (failure: ParseFailure) {
        failure.diagnostic
    }
}
