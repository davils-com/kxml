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

private const val SCHEME_END: Char = ':'
private const val AUTHORITY_START: String = "//"
private const val QUERY_START: Char = '?'

internal class UriParts(val scheme: String?, val authority: String?, val path: String, val query: String?) {
    fun text(): String {
        val written = StringBuilder()
        scheme?.let { value -> written.append(value).append(SCHEME_END) }
        authority?.let { value -> written.append(AUTHORITY_START).append(value) }
        written.append(path)
        query?.let { value -> written.append(QUERY_START).append(value) }
        return written.toString()
    }

    companion object {
        fun of(text: String): UriParts {
            val withoutFragment = text.substringBefore('#')
            val schemeEnd = withoutFragment.indexOfFirst { char -> char in ":/?" }
            val hasScheme = schemeEnd > 0 && withoutFragment[schemeEnd] == SCHEME_END
            val scheme = withoutFragment.substringBefore(SCHEME_END).takeIf { hasScheme }
            val rest = withoutFragment.substringAfter(SCHEME_END).takeIf { hasScheme } ?: withoutFragment
            val hasAuthority = rest.startsWith(AUTHORITY_START)
            val afterSlashes = rest.removePrefix(AUTHORITY_START)
            val delimiter = afterSlashes.indexOfFirst { char -> char in "/?" }
            val authorityEnd = delimiter.takeIf { found -> found >= 0 } ?: afterSlashes.length
            val authority = afterSlashes.take(authorityEnd).takeIf { hasAuthority }
            val pathAndQuery = afterSlashes.drop(authorityEnd).takeIf { hasAuthority } ?: rest
            val hasQuery = QUERY_START in pathAndQuery
            val query = pathAndQuery.substringAfter(QUERY_START).takeIf { hasQuery }
            val path = pathAndQuery.substringBefore(QUERY_START)
            return UriParts(scheme = scheme, authority = authority, path = path, query = query)
        }
    }
}
