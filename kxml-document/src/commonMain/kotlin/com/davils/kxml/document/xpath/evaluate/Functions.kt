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
package com.davils.kxml.document.xpath.evaluate

import com.davils.kxml.document.xpath.syntax.CoreFunction
import com.davils.kxml.document.xpath.syntax.Expression

internal class Functions(evaluator: Evaluator) {
    private val handlers: Map<CoreFunction, FunctionHandler> =
        NodeFunctions(evaluator).handlers + TextFunctions(evaluator).handlers + logicHandlersOf(evaluator)

    fun call(call: Expression.Call, focus: Focus): Value {
        val handler = handlers.getValue(call.function)
        return handler.call(call.arguments, focus)
    }
}
