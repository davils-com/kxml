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

import com.davils.kxml.core.name.XmlName

internal sealed interface Expression {
    val type: ValueType

    class Or(val operands: List<Expression>) : Expression {
        override val type: ValueType get() = ValueType.Truth
    }

    class And(val operands: List<Expression>) : Expression {
        override val type: ValueType get() = ValueType.Truth
    }

    class Comparison(val first: Expression, val links: List<Link<Relation>>) : Expression {
        override val type: ValueType get() = ValueType.Truth
    }

    class Arithmetic(val first: Expression, val links: List<Link<Operation>>) : Expression {
        override val type: ValueType get() = ValueType.Number
    }

    class Negation(val operand: Expression, val isNegated: Boolean) : Expression {
        override val type: ValueType get() = ValueType.Number
    }

    class Union(val operands: List<Expression>) : Expression {
        override val type: ValueType get() = ValueType.NodeSet
    }

    class Path(val base: Expression?, val isAbsolute: Boolean, val steps: List<Step>) : Expression {
        override val type: ValueType get() = ValueType.NodeSet
    }

    class Filter(val primary: Expression, val predicates: List<Expression>) : Expression {
        override val type: ValueType get() = ValueType.NodeSet
    }

    class Literal(val value: String) : Expression {
        override val type: ValueType get() = ValueType.Text
    }

    class Number(val value: Double) : Expression {
        override val type: ValueType get() = ValueType.Number
    }

    class Variable(val name: XmlName) : Expression {
        override val type: ValueType get() = ValueType.Scalar
    }

    class Call(val function: CoreFunction, val arguments: List<Expression>) : Expression {
        override val type: ValueType get() = function.result
    }
}
