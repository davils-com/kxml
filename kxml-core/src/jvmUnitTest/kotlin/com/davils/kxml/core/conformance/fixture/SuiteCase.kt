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

package com.davils.kxml.core.conformance.fixture

import java.nio.file.Files
import java.nio.file.Path

private val manifests: List<String> = listOf(
    "xmltest/xmltest.xml",
    "japanese/japanese.xml",
    "sun/sun-valid.xml",
    "sun/sun-invalid.xml",
    "sun/sun-not-wf.xml",
    "sun/sun-error.xml",
    "oasis/oasis.xml",
    "ibm/ibm_oasis_invalid.xml",
    "ibm/ibm_oasis_not-wf.xml",
    "ibm/ibm_oasis_valid.xml",
    "ibm/xml-1.1/ibm_invalid.xml",
    "ibm/xml-1.1/ibm_not-wf.xml",
    "ibm/xml-1.1/ibm_valid.xml",
    "eduni/errata-2e/errata2e.xml",
    "eduni/xml-1.1/xml11.xml",
    "eduni/namespaces/1.0/rmt-ns10.xml",
    "eduni/namespaces/1.1/rmt-ns11.xml",
    "eduni/errata-3e/errata3e.xml",
    "eduni/errata-4e/errata4e.xml",
    "eduni/namespaces/errata-1e/errata1e.xml",
    "eduni/misc/ht-bh.xml",
)

private val testTag: Regex = Regex("""<TEST\s([^>]*)>""")
private val attribute: Regex = Regex("""([A-Z]+)=(?:"([^"]*)"|'([^']*)')""")

internal class SuiteCase(
    val id: String,
    val type: String,
    val path: Path,
    private val attributes: Map<String, String>,
) {
    val recommendation: String get() = attributes["RECOMMENDATION"] ?: "XML1.0"

    val isApplicable: Boolean
        get() {
            val edition = attributes["EDITION"]
            val isFifthEdition = edition == null || "5" in edition.split(" ")
            val isNamespaceAware = attributes["NAMESPACE"] != "no"
            return type != "error" && isFifthEdition && isNamespaceAware
        }

    val isRefusalExpected: Boolean get() = type == "not-wf"

    override fun toString(): String = id

    companion object {
        fun all(root: Path): List<SuiteCase> = manifests.flatMap { manifest ->
            val path = root.resolve(manifest)
            val text = Files.readString(path)
            val tags = testTag.findAll(text).toList()
            tags.map { match -> caseOf(path.parent, match.groupValues[1]) }
        }

        private fun caseOf(base: Path, tag: String): SuiteCase {
            val attributes = attribute.findAll(tag).associate { match ->
                val (name, doubleQuoted, singleQuoted) = match.destructured
                name to doubleQuoted.ifEmpty { singleQuoted }
            }
            val id = attributes.getValue("ID")
            return SuiteCase(
                id = id,
                type = attributes.getValue("TYPE"),
                path = base.resolve(attributes.getValue("URI")),
                attributes = attributes,
            )
        }
    }
}
