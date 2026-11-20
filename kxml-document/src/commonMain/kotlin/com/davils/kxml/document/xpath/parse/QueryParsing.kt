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
package com.davils.kxml.document.xpath.parse

import com.davils.kxml.core.limit.XmlLimits

internal object QueryParsing {
    fun read(expression: String, namespaces: (String) -> String?, limits: XmlLimits): QueryReading {
        if (expression.length > limits.maximumQueryLength) {
            return QueryReading.Refused(reason = "the expression is longer than maximumQueryLength", offset = 0)
        }
        return try {
            val tokens = QueryLexer(expression).tokens()
            val parser = QueryParser(tokens, namespaces = namespaces, maximumNesting = limits.maximumQueryNesting)
            QueryReading.Read(parser.parse())
        } catch (error: QuerySyntaxError) {
            QueryReading.Refused(reason = error.reason, offset = error.offset)
        }
    }
}
