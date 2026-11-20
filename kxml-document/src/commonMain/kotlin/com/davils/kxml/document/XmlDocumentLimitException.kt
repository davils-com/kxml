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
package com.davils.kxml.document

/**
 * Thrown when work on a document needs more than a limit of
 * [XmlLimits][com.davils.kxml.core.limit.XmlLimits] allows, such as an XPath evaluation that takes more than
 * [maximumQuerySteps][com.davils.kxml.core.limit.XmlLimits.maximumQuerySteps] steps.
 *
 * The work stops before it exceeds the limit and returns no part of its result. The message names the limit and never
 * anything of the document or the expression.
 *
 * @property limit the name of the limit, such as `maximumQuerySteps`.
 * @since 1.0.0
 */
public class XmlDocumentLimitException internal constructor(public val limit: String) :
    IllegalStateException("The work needs more than the limit $limit of XmlLimits allows")
