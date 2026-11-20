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
package com.davils.kxml.document.merge.trace

import com.davils.kxml.core.name.XmlName

internal class TracedAttribute(val name: XmlName, value: String, origin: String) {
    var value: String = value
        private set

    var origin: String = origin
        private set

    fun set(value: String, origin: String) {
        this.value = value
        this.origin = origin
    }
}
