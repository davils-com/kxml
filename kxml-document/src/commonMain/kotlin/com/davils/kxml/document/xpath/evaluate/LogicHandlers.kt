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
import kotlin.math.ceil
import kotlin.math.floor

internal fun logicHandlersOf(evaluator: Evaluator): Map<CoreFunction, FunctionHandler> = mapOf(
    CoreFunction.Boolean to FunctionHandler { arguments, focus ->
        Value.Truth(evaluator.truthOf(arguments.single(), focus))
    },
    CoreFunction.Not to FunctionHandler { arguments, focus ->
        Value.Truth(!evaluator.truthOf(arguments.single(), focus))
    },
    CoreFunction.True to FunctionHandler { _, _ -> Value.Truth(true) },
    CoreFunction.False to FunctionHandler { _, _ -> Value.Truth(false) },
    CoreFunction.Number to FunctionHandler { arguments, focus ->
        val value = evaluator.argumentOrContext(arguments, focus)
        Value.Number(evaluator.conversions.numberOf(value))
    },
    CoreFunction.Floor to FunctionHandler { arguments, focus ->
        Value.Number(floor(evaluator.numberOf(arguments.single(), focus)))
    },
    CoreFunction.Ceiling to FunctionHandler { arguments, focus ->
        Value.Number(ceil(evaluator.numberOf(arguments.single(), focus)))
    },
    CoreFunction.Round to FunctionHandler { arguments, focus ->
        Value.Number(roundOf(evaluator.numberOf(arguments.single(), focus)))
    },
)
