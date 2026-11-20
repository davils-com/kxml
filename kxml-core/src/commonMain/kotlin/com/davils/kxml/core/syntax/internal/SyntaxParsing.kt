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

package com.davils.kxml.core.syntax.internal

import com.davils.kore.core.text.TextRange
import com.davils.kxml.core.XmlConfiguration
import com.davils.kxml.core.diagnostic.XmlDiagnostic
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.parser.GreenBuilder
import com.davils.kxml.core.parser.ParseFailure
import com.davils.kxml.core.parser.XmlScanner
import com.davils.kxml.core.syntax.XmlSyntaxKind

internal object SyntaxParsing {
    fun parse(text: String, configuration: XmlConfiguration): SyntaxParse {
        val builder = GreenBuilder(text)
        val limit = configuration.limits.maximumInputLength
        if (text.length > limit) {
            val refusal = XmlDiagnostic(
                XmlDiagnosticCode.InputTooLong,
                TextRange.at(0),
                "The source is longer than $limit characters",
            )
            builder.startNode(XmlSyntaxKind.DOCUMENT)
            return SyntaxParse(builder.finish(), listOf(refusal))
        }
        val scanner = XmlScanner(
            text = text,
            baseUri = null,
            configuration = configuration,
            encoding = null,
            sink = builder,
        )
        val failure = failureOf(scanner)
        return SyntaxParse(builder.finish(), scanner.warnings + listOfNotNull(failure))
    }

    private fun failureOf(scanner: XmlScanner): XmlDiagnostic? = try {
        generateSequence(scanner::next).forEach { _ -> }
        null
    } catch (failure: ParseFailure) {
        failure.diagnostic
    }
}
