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

pluginManagement {
    repositories {
        gradlePluginPortal {
            content {
                excludeGroupAndSubgroups("com.davils")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        val davilsReleases = maven("https://repo.davils.com/releases") {
            name = "DavilsReleases"
            mavenContent {
                releasesOnly()
            }
        }

        val davilsSnapshots = maven("https://repo.davils.com/snapshots") {
            name = "DavilsSnapshots"
            mavenContent {
                snapshotsOnly()
            }
        }

        exclusiveContent {
            forRepositories(davilsReleases, davilsSnapshots)

            filter {
                includeGroupAndSubgroups("com.davils")
            }
        }

        exclusiveContent {
            forRepository {
                gradlePluginPortal()
            }

            filter {
                includeModule("org.jetbrains.kotlinx", "kotlinx-benchmark-plugin")
                includeModule("io.kotest", "kotest-framework-plugin-gradle")
            }
        }

        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
