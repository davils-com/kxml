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

import com.davils.kreate.testing.KotestModule
import com.davils.kreate.testing.LegacyTestPolicy

plugins {
    id("kxml.base")
}

val parallelForks: Int = maxOf(1, Runtime.getRuntime().availableProcessors() / 2)
val catalog: VersionCatalog = the<VersionCatalogsExtension>().named("libs")
val koreTesting: String = notationOf("kore-testing")
val serializationJson: String = notationOf("kotlinx-serialization-json")

fun notationOf(alias: String): String {
    val library = catalog.findLibrary(alias).get().get()
    return "${library.module}:${library.versionConstraint.requiredVersion}"
}

kreate {
    project {
        tests {
            enabled = true
            ignoreFailures = false
            failOnNoDiscoveredTests = true
            legacyTestSourceSet = LegacyTestPolicy.FAIL
            maxParallelForks = parallelForks

            report {
                enabled = true
                xml = true
                html = true
            }

            kotest {
                enabled = true
                modules = listOf(KotestModule.ASSERTIONS, KotestModule.PROPERTY, KotestModule.JUNIT_XML)
                addJUnitPlatformLauncher = true
            }

            suites {
                named("unitTest") {
                    nonJvmTargets = true
                    dependencies {
                        implementation(koreTesting)
                        implementation(serializationJson)
                    }
                }
                named("integrationTest") {
                    enabled = false
                }
            }
        }
    }
}
