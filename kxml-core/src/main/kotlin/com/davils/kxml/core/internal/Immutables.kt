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

package com.davils.kxml.core.internal

import com.davils.kore.core.annotation.DavilsInternal
import java.util.List.copyOf as listCopyOf

/**
 * Returns an unmodifiable copy of this collection that no cast can change, for the public values of kxml.
 *
 * @receiver the elements to copy.
 * @return the elements in the same order.
 * @since 1.0.0
 */
@DavilsInternal
public fun <T : Any> Collection<T>.immutableList(): List<T> = listCopyOf(this)
