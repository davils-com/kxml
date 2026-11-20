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
 * The white space text nodes a `<remove>` operation of RFC 5261 removes around the located node, by its `ws`
 * attribute.
 *
 * @since 1.0.0
 */
public enum class XmlPatchWhitespace {
    /**
     * None; the default, when `ws` is absent.
     *
     * @since 1.0.0
     */
    None,

    /**
     * The white space text node immediately before the located node, `ws="before"`.
     *
     * @since 1.0.0
     */
    Before,

    /**
     * The white space text node immediately after the located node, `ws="after"`.
     *
     * @since 1.0.0
     */
    After,

    /**
     * The white space text nodes immediately before and after the located node, `ws="both"`.
     *
     * @since 1.0.0
     */
    Both,
}
