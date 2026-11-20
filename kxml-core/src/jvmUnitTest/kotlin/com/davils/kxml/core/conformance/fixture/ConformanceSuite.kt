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

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Duration
import java.util.HexFormat
import java.util.zip.GZIPInputStream

private const val ARCHIVE: String = "https://www.w3.org/XML/Test/xmlts20130923.tar.gz"
private const val ARCHIVE_SHA256: String = "9b61db9f5dbffa545f4b8d78422167083a8568c59bd1129f94138f936cf6fc1f"
private const val MOST_ARCHIVE_BYTES: Int = 4 * 1024 * 1024
private const val TIMEOUT_SECONDS: Long = 120
private const val SUITE: String = "xmlconf"
private const val OK: Int = 200

internal object ConformanceSuite {
    val root: Path by lazy { extracted() }

    private fun extracted(): Path {
        val home = Path.of(System.getProperty("user.dir"), "build", SUITE, ARCHIVE_SHA256)
        val root = home.resolve(SUITE)
        if (Files.isDirectory(root)) return root
        val staging = Files.createTempDirectory(Files.createDirectories(home.parent), SUITE)
        GZIPInputStream(downloaded().inputStream()).use { input -> TarExtraction(staging).extract(input) }
        Files.move(staging, home, StandardCopyOption.ATOMIC_MOVE)
        return root
    }

    private fun downloaded(): ByteArray {
        val clientBuilder = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
        val client = clientBuilder.build()
        val requestBuilder = HttpRequest.newBuilder(URI.create(ARCHIVE)).timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
        val request = requestBuilder.build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        check(response.statusCode() == OK) { "The suite could not be downloaded: ${response.statusCode()}" }
        val bytes = response.body().use { body -> body.readNBytes(MOST_ARCHIVE_BYTES + 1) }
        check(bytes.size <= MOST_ARCHIVE_BYTES) { "The archive of the suite is larger than expected" }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
        val digest = HexFormat.of().formatHex(hash)
        check(digest == ARCHIVE_SHA256) { "The archive of the suite does not have the pinned SHA-256" }
        return bytes
    }
}
