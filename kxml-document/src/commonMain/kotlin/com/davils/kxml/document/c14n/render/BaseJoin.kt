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

package com.davils.kxml.document.c14n.render

private const val PARENT: String = ".."
private const val CURRENT: String = "."
private const val SLASH: String = "/"

internal object BaseJoin {
    fun joined(base: String, reference: String): String {
        val parts = UriParts.of(reference)
        val resolved = absoluteOf(parts) ?: relativeTo(UriParts.of(base.withTrailingParent()), parts)
        return resolved.text()
    }

    fun withoutDots(path: String): String {
        val isAbsolute = path.startsWith(SLASH)
        val segments = path.split(SLASH).filterIndexed { index, segment -> index == 0 || segment.isNotEmpty() }
        val kept = ArrayDeque<String>()
        var isDirectory = false
        segments.drop(1.takeIf { isAbsolute } ?: 0).forEach { segment ->
            isDirectory = segment == CURRENT || segment == PARENT
            removeOrKeep(segment, kept, isAbsolute)
        }
        val hasTrailingSlash = isDirectory || path.endsWith(SLASH)
        val joined = kept.joinToString(separator = SLASH)
        val prefix = SLASH.takeIf { isAbsolute }.orEmpty()
        val suffix = SLASH.takeIf { hasTrailingSlash && joined.isNotEmpty() }.orEmpty()
        return prefix + joined + suffix
    }

    private fun absoluteOf(parts: UriParts): UriParts? {
        if (parts.scheme == null) return null
        return UriParts(
            scheme = parts.scheme,
            authority = parts.authority,
            path = withoutDots(parts.path),
            query = parts.query,
        )
    }

    private fun relativeTo(base: UriParts, parts: UriParts): UriParts {
        val authority = parts.authority ?: return pathOf(base, parts)
        return UriParts(
            scheme = base.scheme,
            authority = authority,
            path = withoutDots(parts.path),
            query = parts.query,
        )
    }

    private fun pathOf(base: UriParts, parts: UriParts): UriParts {
        if (parts.path.isEmpty()) {
            return UriParts(
                scheme = base.scheme,
                authority = base.authority,
                path = base.path,
                query = parts.query ?: base.query,
            )
        }
        val path = parts.path.takeIf { written -> written.startsWith(SLASH) } ?: merged(base, parts.path)
        return UriParts(scheme = base.scheme, authority = base.authority, path = withoutDots(path), query = parts.query)
    }

    private fun merged(base: UriParts, path: String): String {
        if (base.authority != null && base.path.isEmpty()) return SLASH + path
        val directory = base.path.substringBeforeLast(SLASH, missingDelimiterValue = "")
        val separator = SLASH.takeIf { base.path.contains(SLASH) }.orEmpty()
        return directory + separator + path
    }

    private fun String.withTrailingParent(): String {
        val parts = UriParts.of(this)
        val isParent = parts.path == PARENT || parts.path.endsWith("/$PARENT")
        if (!isParent) return this
        val directory = UriParts(
            scheme = parts.scheme,
            authority = parts.authority,
            path = parts.path + SLASH,
            query = parts.query,
        )
        return directory.text()
    }

    private fun removeOrKeep(segment: String, kept: ArrayDeque<String>, isAbsolute: Boolean) {
        if (segment == CURRENT || segment.isEmpty()) return
        if (segment != PARENT) {
            kept.addLast(segment)
            return
        }
        val isRemovable = kept.isNotEmpty() && kept.last() != PARENT
        if (isRemovable) kept.removeLast()
        if (!isRemovable && !isAbsolute) kept.addLast(PARENT)
    }
}
