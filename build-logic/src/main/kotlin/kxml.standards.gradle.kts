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
            document("W3C XML Conformance Test Suite") {
                note = "The cases XmlConformanceTest downloads as xmlts20130923.tar.gz, pinned by its SHA-256, and runs on the JVM."
                url = "https://www.w3.org/XML/Test/"
            }
            w3c("xml11") {
                note = "XML 1.1 Second Edition: its line ends, its restricted characters and references to C0 controls."
            }
            w3c("xml-names11") {
                note = "Namespaces in XML 1.1: undeclaring a prefix in a document of XML 1.1."
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
            rfc(20) {
                note = "The strict US-ASCII decoder of XML sources in kxml-core, which refuses every byte above 0x7F."
            }
            iso("ISO/IEC 10646") {
                note = "The universal coded character set behind UTF-8, UTF-16 and UTF-32 and the label ISO-10646-UCS-4."
            }
            iso("ISO/IEC 8859-1") {
                note = "The ISO-8859-1 decoder of XML sources in kxml-core."
            }
            rfc(7303) {
                note = "The precedence of a byte order mark over the charset of a source over the declared encoding."
            }
            unicode {
                note = "The safe display of text follows the Unicode tables of the platform."
            }
        }
    }
}
