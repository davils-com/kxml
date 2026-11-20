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

import com.davils.kxml.document.xpath.evaluate.numberIn
import com.davils.kxml.document.xpath.syntax.Axis
import com.davils.kxml.document.xpath.syntax.CoreFunction
import com.davils.kxml.document.xpath.syntax.Expression
import com.davils.kxml.document.xpath.syntax.Link
import com.davils.kxml.document.xpath.syntax.NodeTest
import com.davils.kxml.document.xpath.syntax.Operation
import com.davils.kxml.document.xpath.syntax.Relation
import com.davils.kxml.document.xpath.syntax.Step
import com.davils.kxml.document.xpath.syntax.ValueType

private const val INSTRUCTION: String = "processing-instruction"
private const val NEEDS_NODE_SET: String = "the operand must be a node-set"

private val descendantOrSelf: Step =
    Step(axis = Axis.DescendantOrSelf, test = NodeTest.AnyNode, predicates = emptyList())

private val abbreviations: Map<TokenKind, Step> = mapOf(
    TokenKind.Dot to Step(axis = Axis.Self, test = NodeTest.AnyNode, predicates = emptyList()),
    TokenKind.DotDot to Step(axis = Axis.Parent, test = NodeTest.AnyNode, predicates = emptyList()),
)

private val filterStarts: Set<TokenKind> = setOf(
    TokenKind.Variable,
    TokenKind.LeftParen,
    TokenKind.Literal,
    TokenKind.Number,
    TokenKind.FunctionName,
)

private val stepStarts: Set<TokenKind> = setOf(
    TokenKind.Dot,
    TokenKind.DotDot,
    TokenKind.At,
    TokenKind.AxisName,
    TokenKind.NameTest,
    TokenKind.NodeType,
)

private val equalities: Map<TokenKind, Relation> = mapOf(
    TokenKind.Equal to Relation.Equal,
    TokenKind.NotEqual to Relation.NotEqual,
)

private val relations: Map<TokenKind, Relation> = mapOf(
    TokenKind.Less to Relation.Less,
    TokenKind.LessOrEqual to Relation.LessOrEqual,
    TokenKind.Greater to Relation.Greater,
    TokenKind.GreaterOrEqual to Relation.GreaterOrEqual,
)

private val additions: Map<TokenKind, Operation> = mapOf(
    TokenKind.Plus to Operation.Plus,
    TokenKind.Minus to Operation.Minus,
)

private val multiplications: Map<TokenKind, Operation> = mapOf(
    TokenKind.Multiply to Operation.Times,
    TokenKind.Div to Operation.Divide,
    TokenKind.Mod to Operation.Modulo,
)

internal class QueryParser(
    private val tokens: List<Token>,
    namespaces: (String) -> String?,
    private val maximumNesting: Int,
) {
    private val names: QueryNames = QueryNames(namespaces)
    private var position: Int = 0
    private var depth: Int = 0

    private val current: Token get() = tokens[position]

    private val primaries: Map<TokenKind, (Token) -> Expression> = mapOf(
        TokenKind.Variable to names::variable,
        TokenKind.Literal to { token -> Expression.Literal(token.text) },
        TokenKind.Number to { token -> Expression.Number(numberIn(token.text)) },
        TokenKind.FunctionName to ::call,
        TokenKind.LeftParen to { _ ->
            val inner = expression()
            expect(kind = TokenKind.RightParen, reason = "a parenthesis is not closed")
            inner
        },
    )

    fun parse(): Expression {
        val parsed = expression()
        expect(kind = TokenKind.End, reason = "the expression goes on after its end")
        return parsed
    }

    private fun expression(): Expression {
        depth += 1
        if (depth > maximumNesting) throw QuerySyntaxError("the expression nests too deep", current.offset)
        val parsed = or()
        depth -= 1
        return parsed
    }

    private fun or(): Expression {
        val operands = mutableListOf(and())
        while (accept(TokenKind.Or) != null) operands += and()
        return operands.singleOrNull() ?: Expression.Or(operands)
    }

    private fun and(): Expression {
        val operands = mutableListOf(equality())
        while (accept(TokenKind.And) != null) operands += equality()
        return operands.singleOrNull() ?: Expression.And(operands)
    }

    private fun equality(): Expression = chain(::relational, equalities, Expression::Comparison)

    private fun relational(): Expression = chain(::additive, relations, Expression::Comparison)

    private fun additive(): Expression = chain(::multiplicative, additions, Expression::Arithmetic)

    private fun multiplicative(): Expression = chain(::unary, multiplications, Expression::Arithmetic)

    private fun <O : Enum<O>> chain(
        operand: () -> Expression,
        operators: Map<TokenKind, O>,
        joined: (Expression, List<Link<O>>) -> Expression,
    ): Expression {
        val first = operand()
        val links = mutableListOf<Link<O>>()
        var operator = operators[current.kind]
        while (operator != null) {
            position += 1
            links += Link(operator, operand())
            operator = operators[current.kind]
        }
        if (links.isEmpty()) return first
        return joined(first, links)
    }

    private fun unary(): Expression {
        var signs = 0
        while (accept(TokenKind.Minus) != null) signs += 1
        val operand = union()
        if (signs == 0) return operand
        return Expression.Negation(operand, isNegated = signs % 2 == 1)
    }

    private fun union(): Expression {
        val start = current.offset
        val operands = mutableListOf(path())
        while (accept(TokenKind.Pipe) != null) operands += path()
        val isNodeSets = operands.size == 1 || operands.all { operand -> operand.type == ValueType.NodeSet }
        if (!isNodeSets) throw QuerySyntaxError(NEEDS_NODE_SET, start)
        return operands.singleOrNull() ?: Expression.Union(operands)
    }

    private fun path(): Expression {
        if (current.kind in filterStarts) return filterPath()
        return locationPath()
    }

    private fun locationPath(): Expression {
        val isRoot = accept(TokenKind.Slash) != null
        val isDescending = !isRoot && accept(TokenKind.DoubleSlash) != null
        val steps = mutableListOf<Step>()
        if (isDescending) steps += descendantOrSelf
        if (!isRoot || current.kind in stepStarts) steps += relativeSteps()
        return Expression.Path(base = null, isAbsolute = isRoot || isDescending, steps = steps)
    }

    private fun filterPath(): Expression {
        val start = current.offset
        val primary = primary()
        val predicates = predicates()
        val isRelative = accept(TokenKind.Slash) != null
        val isDescending = !isRelative && accept(TokenKind.DoubleSlash) != null
        val isNodeSetNeeded = predicates.isNotEmpty() || isRelative || isDescending
        if (isNodeSetNeeded && primary.type != ValueType.NodeSet) throw QuerySyntaxError(NEEDS_NODE_SET, start)
        val filter = Expression.Filter(primary, predicates).takeIf { predicates.isNotEmpty() } ?: primary
        if (!isRelative && !isDescending) return filter
        val steps = mutableListOf<Step>()
        if (isDescending) steps += descendantOrSelf
        steps += relativeSteps()
        return Expression.Path(base = filter, isAbsolute = false, steps = steps)
    }

    private fun relativeSteps(): List<Step> {
        val steps = mutableListOf(step())
        var isFollowed = true
        while (isFollowed) {
            val isDescending = accept(TokenKind.DoubleSlash) != null
            if (isDescending) steps += descendantOrSelf
            isFollowed = isDescending || accept(TokenKind.Slash) != null
            if (isFollowed) steps += step()
        }
        return steps
    }

    private fun step(): Step {
        val abbreviated = abbreviations[current.kind]
        if (abbreviated != null) position += 1
        return abbreviated ?: Step(axis(), nodeTest(), predicates())
    }

    private fun axis(): Axis {
        if (accept(TokenKind.At) != null) return Axis.Attribute
        return namedAxis(accept(TokenKind.AxisName))
    }

    private fun namedAxis(name: Token?): Axis {
        if (name == null) return Axis.Child
        val axis = Axis.of(name.text) ?: throw QuerySyntaxError("the axis is not one of XPath", name.offset)
        expect(kind = TokenKind.ColonColon, reason = "an axis needs two colons")
        return axis
    }

    private fun nodeTest(): NodeTest {
        val test = current
        position += 1
        if (test.kind == TokenKind.NameTest) return names.nameTest(test)
        if (test.kind != TokenKind.NodeType) throw QuerySyntaxError("a step needs a node test", test.offset)
        expect(kind = TokenKind.LeftParen, reason = "a node type needs parentheses")
        val target = accept(TokenKind.Literal).takeIf { test.text == INSTRUCTION }
        expect(kind = TokenKind.RightParen, reason = "a node type takes no argument")
        return names.typeTest(type = test.text, target = target?.text)
    }

    private fun predicates(): List<Expression> {
        val predicates = mutableListOf<Expression>()
        while (accept(TokenKind.LeftBracket) != null) {
            predicates += expression()
            expect(kind = TokenKind.RightBracket, reason = "a predicate needs a closing bracket")
        }
        return predicates
    }

    private fun primary(): Expression {
        val token = current
        position += 1
        val read = primaries.getValue(token.kind)
        return read(token)
    }

    private fun call(token: Token): Expression {
        val function = CoreFunction.of(token.text)
            ?: throw QuerySyntaxError("the function is not one of the core library", token.offset)
        expect(kind = TokenKind.LeftParen, reason = "a function call needs parentheses")
        val arguments = mutableListOf<Expression>()
        if (accept(TokenKind.RightParen) == null) {
            arguments += expression()
            while (accept(TokenKind.Comma) != null) arguments += expression()
            expect(kind = TokenKind.RightParen, reason = "a function call needs a closing parenthesis")
        }
        return checked(Expression.Call(function, arguments), token.offset)
    }

    private fun checked(call: Expression.Call, offset: Int): Expression {
        val function = call.function
        if (call.arguments.size !in function.arity) throw QuerySyntaxError("the function takes other arguments", offset)
        val arguments = call.arguments
        val isTyped = !function.hasNodeSetArguments || arguments.all { argument -> argument.type == ValueType.NodeSet }
        if (!isTyped) throw QuerySyntaxError("the function needs a node-set", offset)
        return call
    }

    private fun accept(kind: TokenKind): Token? {
        val token = current.takeIf { candidate -> candidate.kind == kind } ?: return null
        position += 1
        return token
    }

    private fun expect(kind: TokenKind, reason: String): Token =
        accept(kind) ?: throw QuerySyntaxError(reason, current.offset)
}
