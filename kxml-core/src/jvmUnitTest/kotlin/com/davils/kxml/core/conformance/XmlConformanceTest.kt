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

package com.davils.kxml.core.conformance

import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.conformance.fixture.ConformanceSuite
import com.davils.kxml.core.conformance.fixture.SuiteCase
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.source.XmlSource
import com.davils.kxml.core.xml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import java.nio.file.Files
import java.nio.file.Path

private val reader = xml {
    doctype = XmlDoctype.Internal
    limits = XmlLimits.Relaxed
}

private fun knownFailures(): Map<String, String> {
    val resource = XmlConformanceTest::class.java.getResource("/conformance/known-failures.txt")
    val listing = resource?.readText().orEmpty()
    val entries = listing.lines().filter { line -> line.isNotBlank() && !line.startsWith("#") }
    return entries.associate { line ->
        line.substringBefore(" ") to line.substringAfter(delimiter = " ", missingDelimiterValue = "")
    }
}

private fun report(cases: List<SuiteCase>, known: Map<String, String>) {
    val lines = cases.map { case ->
        val outcome = "pass".takeIf { isConforming(case) } ?: "fail"
        val listed = "listed".takeIf { case.id in known }.orEmpty()
        "${case.id} ${case.type} ${case.recommendation} $outcome $listed".trim()
    }
    val target = Path.of(System.getProperty("user.dir"), "build", "reports", "xmlconf.txt")
    Files.createDirectories(target.parent)
    Files.write(target, lines)
}

private fun isConforming(case: SuiteCase): Boolean {
    val bytes = Files.readAllBytes(case.path)
    val isRefused = reader.parse(XmlSource.of(bytes, name = case.id)) is XmlParseResult.Failed
    return isRefused == case.isRefusalExpected
}

class XmlConformanceTest : FunSpec({
    context("the W3C XML Conformance Test Suite") {
        val cases = SuiteCase.all(ConformanceSuite.root).filter(SuiteCase::isApplicable)
        val known = knownFailures()
        report(cases, known)

        test("passes every case that is not listed as a known failure") {
            val failing = cases.filter { case -> case.id !in known && !isConforming(case) }

            failing.map { case -> "${case.id} ${case.type} ${case.recommendation}" }.shouldBeEmpty()
        }

        test("fails every case that is listed as a known failure") {
            val fixed = cases.filter { case -> case.id in known && isConforming(case) }

            fixed.map(SuiteCase::id).shouldBeEmpty()
        }

        test("lists only cases that exist") {
            val ids = cases.map(SuiteCase::id).toSet()

            known.keys.filter { id -> id !in ids }.shouldBeEmpty()
        }
    }
})
