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

import java.time.Duration

plugins {
    id("kxml.base")
}

kreate {
    assurance {
        standards {
            enabled = true
            reviewEvery = Duration.ofDays(365)

            inventory {
                includes.set(setOf("*.md", "docs/**", "kxml-*/src/**"))
            }

            w3c("xml") {
                note = "XML 1.0 Fifth Edition: the reader, the composer and the writer of kxml-core."
            }
            w3c("xml-names") {
                note = "Namespaces in XML 1.0: the namespace resolution of the reader and the prefixes the writer declares."
            }
            rfc(3629) {
                note = "The strict UTF-8 decoder of XML sources in kxml-core."
            }
            rfc(2781) {
                note = "The strict UTF-16 decoder of XML sources in kxml-core."
            }
            unicode {
                note = "The safe display of text follows the Unicode tables of the platform."
            }
        }
    }
}
