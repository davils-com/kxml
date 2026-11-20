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

@file:OptIn(DavilsInternal::class)

package com.davils.kxml.core.resolve

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.uri.UriReference
import com.davils.kxml.core.limit.XmlLimitsBuilder
import com.davils.kxml.core.resolve.internal.referenceOf
import com.davils.kxml.core.source.XmlSource
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private const val FILE_SCHEME: String = "file"

/**
 * Loads `file:` URIs of the files below one directory and refuses every other URI.
 *
 * A URI is loaded only when it has the `file` scheme, no host, no query and no fragment, and when the file it names,
 * with every symbolic link followed, is a regular file below [root]. A path that leaves the directory through `..`,
 * a percent-encoded `..` or a symbolic link, a missing file, a directory and a file longer than [maximumLength] bytes
 * are refused with `null`. The loader reads a file only when it is asked for it, keeps no state and is safe to use
 * from several threads at once.
 *
 * @param root the directory to load from; it must exist.
 * @property maximumLength the longest file to load, in bytes; [XmlLimitsBuilder.DEFAULT_EXTERNAL_LENGTH] by default.
 * @throws IOException if [root] does not exist or cannot be read.
 * @throws IllegalArgumentException if [root] is no directory or [maximumLength] is not positive.
 * @since 1.0.0
 */
public class XmlDirectoryLoader(
    root: Path,
    public val maximumLength: Int = XmlLimitsBuilder.DEFAULT_EXTERNAL_LENGTH,
) : XmlResourceLoader {
    /**
     * The directory, with every symbolic link followed.
     *
     * @since 1.0.0
     */
    public val root: Path = root.toRealPath()

    /**
     * The `file:` URI of [root], ending in `/`, against which relative names are resolved.
     *
     * @since 1.0.0
     */
    public val rootUri: String = this.root.toUri().toString()

    init {
        require(Files.isDirectory(this.root)) { "The root of a directory loader must be a directory" }
        require(maximumLength > 0) { "The longest file of a directory loader must be positive, but was $maximumLength" }
    }

    /**
     * Returns the absolute URI of [name] resolved against [rootUri], such as `catalog.xml` or `dtd/book.dtd`, or `null`
     * if [name] is no URI reference. The result may still lie outside [root]; [load] refuses it then.
     *
     * @param name a relative or absolute URI reference.
     * @return the absolute URI.
     * @since 1.0.0
     */
    public fun uriOf(name: String): String? {
        val base = UriReference.parseOrNull(rootUri) ?: return null
        val reference = referenceOf(name)?.let(base::resolve)
        return reference?.toString()
    }

    /**
     * Returns the file [uri] names, or `null` when the class refuses it.
     *
     * @param uri the URI to load; it may come from a document or a catalog.
     * @return the file, with [uri] as its [XmlSource.uri], or `null`.
     * @since 1.0.0
     */
    override fun load(uri: String): XmlSource? {
        val path = pathOf(uri) ?: return null
        return bytesOf(path)?.let { bytes -> XmlSource.of(bytes = bytes, name = path.fileName?.toString(), uri = uri) }
    }

    /**
     * Returns the root, such as `XmlDirectoryLoader(file:///etc/xml/)`.
     *
     * @return a text that names the root.
     * @since 1.0.0
     */
    override fun toString(): String = "XmlDirectoryLoader($rootUri)"

    private fun pathOf(uri: String): Path? {
        val reference = referenceOf(uri)?.normalize()
        val plain = reference?.takeIf(::isPlainFile) ?: return null
        val path = filePathOf(plain.toString())?.normalize()
        val real = path?.takeIf { candidate -> candidate.startsWith(root) }?.let(::realPathOf)
        return real?.takeIf { candidate -> candidate.startsWith(root) && Files.isRegularFile(candidate) }
    }

    private fun isPlainFile(reference: UriReference): Boolean {
        val isLocal = reference.scheme == FILE_SCHEME && reference.authority.isNullOrEmpty()
        return isLocal && reference.query == null && reference.fragment == null
    }

    private fun filePathOf(uri: String): Path? = try {
        Paths.get(URI(uri))
    } catch (_: URISyntaxException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: FileSystemNotFoundException) {
        null
    }

    private fun realPathOf(path: Path): Path? = try {
        path.toRealPath()
    } catch (_: IOException) {
        null
    }

    private fun bytesOf(path: Path): ByteArray? = try {
        val bytes = Files.newInputStream(path).use { stream -> stream.readNBytes(maximumLength + 1) }
        bytes.takeIf { read -> read.size <= maximumLength }
    } catch (_: IOException) {
        null
    }
}
