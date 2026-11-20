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

internal enum class Axis(val label: String, val isReverse: Boolean) {
    Ancestor(label = "ancestor", isReverse = true),
    AncestorOrSelf(label = "ancestor-or-self", isReverse = true),
    Attribute(label = "attribute", isReverse = false),
    Child(label = "child", isReverse = false),
    Descendant(label = "descendant", isReverse = false),
    DescendantOrSelf(label = "descendant-or-self", isReverse = false),
    Following(label = "following", isReverse = false),
    FollowingSibling(label = "following-sibling", isReverse = false),
    Namespace(label = "namespace", isReverse = false),
    Parent(label = "parent", isReverse = true),
    Preceding(label = "preceding", isReverse = true),
    PrecedingSibling(label = "preceding-sibling", isReverse = true),
    Self(label = "self", isReverse = false),
    ;

    companion object {
        fun of(label: String): Axis? = entries.firstOrNull { axis -> axis.label == label }
    }
}
