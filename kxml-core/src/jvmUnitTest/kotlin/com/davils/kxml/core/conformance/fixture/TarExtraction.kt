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

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

private const val BLOCK: Int = 512
private const val NAME_LENGTH: Int = 100
private const val SIZE_OFFSET: Int = 124
private const val SIZE_LENGTH: Int = 12
private const val TYPE_OFFSET: Int = 156
private const val PREFIX_OFFSET: Int = 345
private const val PREFIX_LENGTH: Int = 155
private const val OCTAL: Int = 8
private const val MOST_ENTRIES: Int = 10_000
private const val MOST_BYTES: Long = 64L * 1024 * 1024
private const val REGULAR: Char = '0'
private const val OLD_REGULAR: Char = '\u0000'
private const val DIRECTORY: Char = '5'
private val metadata: Set<Char> = setOf('x', 'g')

internal class TarExtraction(private val target: Path) {
    private var entries: Int = 0
    private var bytes: Long = 0L

    fun extract(input: InputStream) {
        val header = ByteArray(BLOCK)
        while (input.readNBytes(header, 0, BLOCK) == BLOCK && header.any { byte -> byte != 0.toByte() }) {
            entries++
            check(entries <= MOST_ENTRIES) { "The archive holds too many entries" }
            entry(header, input)
        }
    }

    private fun entry(header: ByteArray, input: InputStream) {
        val type = header[TYPE_OFFSET].toInt().toChar()
        val size = sizeOf(header)
        bytes += size
        check(bytes <= MOST_BYTES) { "The archive is too large" }
        val content = input.readNBytes(size.toInt())
        check(content.size.toLong() == size) { "The archive ends inside an entry" }
        input.skipNBytes(padding(size))
        val isFile = type == REGULAR || type == OLD_REGULAR
        check(isFile || type == DIRECTORY || type in metadata) { "The archive holds an entry of type $type" }
        if (isFile) write(nameOf(header), content)
    }

    private fun write(name: String, content: ByteArray) {
        val path = target.resolve(name).normalize()
        val isInside = path.startsWith(target) && !Path.of(name).isAbsolute
        check(isInside && ".." !in name.split('/')) { "The archive names a path outside its target" }
        Files.createDirectories(path.parent)
        Files.write(path, content)
    }

    private fun nameOf(header: ByteArray): String {
        val name = textAt(header, offset = 0, length = NAME_LENGTH)
        val prefix = textAt(header, offset = PREFIX_OFFSET, length = PREFIX_LENGTH)
        return name.takeIf { prefix.isEmpty() } ?: "$prefix/$name"
    }

    private fun sizeOf(header: ByteArray): Long {
        val digits = textAt(header, offset = SIZE_OFFSET, length = SIZE_LENGTH).trim()
        return digits.toLongOrNull(OCTAL) ?: 0L
    }

    private fun textAt(header: ByteArray, offset: Int, length: Int): String {
        val field = header.copyOfRange(fromIndex = offset, toIndex = offset + length)
        val end = field.indexOf(0.toByte()).takeIf { index -> index >= 0 } ?: length
        return field.copyOf(end).decodeToString()
    }

    private fun padding(size: Long): Long = (BLOCK - size % BLOCK) % BLOCK
}
