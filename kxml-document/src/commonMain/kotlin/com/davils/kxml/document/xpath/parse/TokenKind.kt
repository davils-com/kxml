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

internal enum class TokenKind(val isOperator: Boolean = false) {
    LeftParen,
    RightParen,
    LeftBracket,
    RightBracket,
    Dot,
    DotDot,
    At,
    Comma,
    ColonColon,
    NameTest,
    NodeType,
    FunctionName,
    AxisName,
    Literal,
    Number,
    Variable,
    Slash(isOperator = true),
    DoubleSlash(isOperator = true),
    Pipe(isOperator = true),
    Plus(isOperator = true),
    Minus(isOperator = true),
    Equal(isOperator = true),
    NotEqual(isOperator = true),
    Less(isOperator = true),
    LessOrEqual(isOperator = true),
    Greater(isOperator = true),
    GreaterOrEqual(isOperator = true),
    And(isOperator = true),
    Or(isOperator = true),
    Mod(isOperator = true),
    Div(isOperator = true),
    Multiply(isOperator = true),
    End,
}
