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

import com.davils.kreate.coverage.Grouping

plugins {
    id("kxml.base")
}

val annotationPackage = "com.davils.kore.core.annotation"

kreate {
    project {
        coverage {
            enabled = true

            filters {
                excludes {
                    annotatedBy.add("$annotationPackage.DavilsPlatformBinding")
                    annotatedBy.add("$annotationPackage.DavilsGenerated")
                }
            }

            reports {
                xml {
                    onCheck = true
                }

                html {
                    onCheck = true
                }
            }

            verify {
                runOnCheck = true
                warningInsteadOfFailure = false
                groupBy = Grouping.APPLICATION
                minLineCoverage = 97
                minBranchCoverage = 97
                minInstructionCoverage = 97
            }
        }
    }
}
