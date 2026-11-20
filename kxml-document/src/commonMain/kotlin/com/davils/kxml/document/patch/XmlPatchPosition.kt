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
package com.davils.kxml.document.patch

/**
 * Where an `<add>` operation of RFC 5261 puts its nodes, by its `pos` attribute.
 *
 * @since 1.0.0
 */
public enum class XmlPatchPosition {
    /**
     * After the last child of the located element; the default, when `pos` is absent.
     *
     * @since 1.0.0
     */
    Append,

    /**
     * Before the first child of the located element, `pos="prepend"`.
     *
     * @since 1.0.0
     */
    Prepend,

    /**
     * Immediately before the located node, `pos="before"`.
     *
     * @since 1.0.0
     */
    Before,

    /**
     * Immediately after the located node, `pos="after"`.
     *
     * @since 1.0.0
     */
    After,
}
