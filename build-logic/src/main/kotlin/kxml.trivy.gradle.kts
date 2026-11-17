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

import com.davils.kreate.assurance.Severity

plugins {
    id("kxml.base")
}

val everySeverity: List<Severity> = Severity.entries.toList()
val isRoot: Boolean = path == ":"
val scannedRoot: String = projectDir.absolutePath
val generatedDirectories: List<String> = listOf("**/build", "**/.gradle", "**/.kotlin", "**/.git")
    .map { directory -> "$scannedRoot/$directory" } + listOf("$scannedRoot/.idea", "$scannedRoot/.port")

kreate {
    trivy {
        enabled = true
        useInstalled = false

        secrets {
            severity = everySeverity
            failOnFindings = true
            runOnCheck = true
            secretConfig = isolated.rootProject.projectDirectory.file("trivy-secret.yaml")

            if (isRoot) {
                sourceFiles.setFrom(
                    fileTree(projectDir) {
                        exclude("kxml-*/**", "**/build/**", "**/.gradle/**", "**/.kotlin/**", ".git/**", ".idea/**")
                        exclude(".port/**", "gradle/wrapper/gradle-wrapper.jar", "gradle/verification-keyring.gpg")
                    }
                )
            }
        }

        misconfig {
            enabled = isRoot
            severity = everySeverity
            failOnFindings = true
            skipDirectories = generatedDirectories
        }

        filesystem {
            enabled = isRoot
            severity = everySeverity
            failOnFindings = true
            skipDirectories = generatedDirectories
        }

        vulnerability {
            severity = everySeverity
            failOnFindings = true
            failOnMissingLockFiles = !isRoot
            ignoreUnfixed = false

            if (isRoot) {
                lockFiles.from(layout.projectDirectory.file("build-logic/gradle.lockfile"))
            }
        }

        license {
            severity = everySeverity - Severity.LOW
            failOnFindings = true
            failOnMissingLockFiles = !isRoot
        }
    }
}
