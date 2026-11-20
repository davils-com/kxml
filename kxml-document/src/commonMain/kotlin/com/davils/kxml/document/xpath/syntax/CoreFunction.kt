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
package com.davils.kxml.document.xpath.syntax

internal enum class CoreFunction(
    val label: String,
    val arity: IntRange,
    val result: ValueType,
    val hasNodeSetArguments: Boolean = false,
) {
    Last(label = "last", arity = 0..0, result = ValueType.Number),
    Position(label = "position", arity = 0..0, result = ValueType.Number),
    Count(label = "count", arity = 1..1, result = ValueType.Number, hasNodeSetArguments = true),
    Id(label = "id", arity = 1..1, result = ValueType.NodeSet),
    LocalName(label = "local-name", arity = 0..1, result = ValueType.Text, hasNodeSetArguments = true),
    NamespaceUri(label = "namespace-uri", arity = 0..1, result = ValueType.Text, hasNodeSetArguments = true),
    Name(label = "name", arity = 0..1, result = ValueType.Text, hasNodeSetArguments = true),
    String(label = "string", arity = 0..1, result = ValueType.Text),
    Concat(label = "concat", arity = 2..Int.MAX_VALUE, result = ValueType.Text),
    StartsWith(label = "starts-with", arity = 2..2, result = ValueType.Truth),
    Contains(label = "contains", arity = 2..2, result = ValueType.Truth),
    SubstringBefore(label = "substring-before", arity = 2..2, result = ValueType.Text),
    SubstringAfter(label = "substring-after", arity = 2..2, result = ValueType.Text),
    Substring(label = "substring", arity = 2..3, result = ValueType.Text),
    StringLength(label = "string-length", arity = 0..1, result = ValueType.Number),
    NormalizeSpace(label = "normalize-space", arity = 0..1, result = ValueType.Text),
    Translate(label = "translate", arity = 3..3, result = ValueType.Text),
    Boolean(label = "boolean", arity = 1..1, result = ValueType.Truth),
    Not(label = "not", arity = 1..1, result = ValueType.Truth),
    True(label = "true", arity = 0..0, result = ValueType.Truth),
    False(label = "false", arity = 0..0, result = ValueType.Truth),
    Lang(label = "lang", arity = 1..1, result = ValueType.Truth),
    Number(label = "number", arity = 0..1, result = ValueType.Number),
    Sum(label = "sum", arity = 1..1, result = ValueType.Number, hasNodeSetArguments = true),
    Floor(label = "floor", arity = 1..1, result = ValueType.Number),
    Ceiling(label = "ceiling", arity = 1..1, result = ValueType.Number),
    Round(label = "round", arity = 1..1, result = ValueType.Number),
    ;

    companion object {
        fun of(label: kotlin.String): CoreFunction? = entries.firstOrNull { function -> function.label == label }
    }
}
