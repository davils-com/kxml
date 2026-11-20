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

@file:Suppress("UnstableApiUsage")

import com.davils.kreate.assurance.ReportFormat
import com.davils.kreate.assurance.Severity
import java.time.Duration

plugins {
    id("kxml.base")
}

val isRoot: Boolean = path == ":"

kreate {
    assurance {
        enabled = true
        runOnCheck = true
        reportOnly = false
        failOn = Severity.UNKNOWN
        minSeverity = Severity.UNKNOWN
        forbidIgnoreOf = setOf(Severity.CRITICAL, Severity.HIGH)
        internalGroups = setOf("com.davils")
        maxAdvisoryAge = Duration.ofDays(1)
        reuseVerdictFor = Duration.ofHours(1)

        ignore("LOW_SCORECARD") {
            reason = "JetBrains' nullability annotations, which kotlinx-coroutines brings along, carry no runtime code."
            module = "org.jetbrains:annotations"
            expires = "2027-05-16"
        }

        ignore("STANDARD_UNDECLARED") {
            reason = "The inventory reads the encoding label ISO-10646-UCS-4 as iso:10646; the standard is declared as " +
                "iso:iec-10646, the id Kreate gives ISO/IEC 10646."
            module = "iso:10646"
            expires = "2027-05-20"
        }

        baseline {
            maxSeverity = Severity.LOW
            failOnStale = true
        }

        advisories {
            enabled = true
        }

        inspection {
            enabled = true
            failOnRisk = true
        }

        trust {
            enabled = true
            includeBuildscript = true
        }

        confusion {
            enabled = true
            includeBuildscript = true
        }

        dependencyPolicy {
            enabled = true
            forbidDynamicVersions = true
            forbidChangingVersions = true
            requireLocking = true
            requireVerification = true
            allowedRepositoryHosts = setOf("repo.maven.apache.org", "plugins.gradle.org", "repo.davils.com")
            checkOutdated = true
            maxMajorBehind = 0
        }

        workspace {
            enabled = true
            alignVersions = true
        }

        aggregate {
            enabled = isRoot
            members = setOf(":", ":kxml-core", ":kxml-serialization")
        }

        reports {
            formats = ReportFormat.entries.toSet()
            ingestQuality = true
        }
    }
}
