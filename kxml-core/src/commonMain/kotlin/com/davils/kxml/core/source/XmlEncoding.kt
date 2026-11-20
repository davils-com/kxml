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

package com.davils.kxml.core.source

private val utf8Labels: Set<String> = setOf("UTF-8", "UTF8")
private val asciiLabels: Set<String> = setOf("US-ASCII", "ASCII")
private val latin1Labels: Set<String> = setOf(
    "ISO-8859-1",
    "ISO_8859-1",
    "LATIN1",
    "L1",
    "ISO-IR-100",
    "IBM819",
    "CP819",
    "CSISOLATIN1",
)
private val utf16Labels: Set<String> = setOf("UTF-16")
private val utf32Labels: Set<String> = setOf("UTF-32", "ISO-10646-UCS-4")

internal enum class XmlEncoding(val labels: Set<String>, val isAsciiCompatible: Boolean) {
    Utf8(labels = utf8Labels, isAsciiCompatible = true),
    UsAscii(labels = asciiLabels, isAsciiCompatible = true),
    Latin1(labels = latin1Labels, isAsciiCompatible = true),
    Utf16LittleEndian(labels = utf16Labels + "UTF-16LE", isAsciiCompatible = false),
    Utf16BigEndian(labels = utf16Labels + "UTF-16BE", isAsciiCompatible = false),
    Utf32LittleEndian(labels = utf32Labels + "UTF-32LE", isAsciiCompatible = false),
    Utf32BigEndian(labels = utf32Labels + "UTF-32BE", isAsciiCompatible = false),
    ;

    fun accepts(label: String): Boolean = label.uppercase() in labels

    companion object {
        fun forLabel(label: String, detected: XmlEncoding): XmlEncoding? {
            val accepting = entries.filter { encoding -> encoding.accepts(label) }
            return accepting.firstOrNull { encoding -> encoding == detected } ?: accepting.lastOrNull()
        }
    }
}
