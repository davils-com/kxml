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

package com.davils.kxml.serialization.fixture

private const val LINE_BREAK: String = "\n"

internal object Payloads {
    const val S3: String = "http://s3.amazonaws.com/doc/2006-03-01/"

    const val XSI: String = "http://www.w3.org/2001/XMLSchema-instance"

    val listing: String = listOf(
        """<?xml version="1.0" encoding="UTF-8"?>""",
        """<ListBucketResult xmlns="$S3">""",
        "  <Name>photos</Name>",
        "  <KeyCount>2</KeyCount>",
        "  <IsTruncated>false</IsTruncated>",
        "  <Contents>",
        "    <Key>2026/a.jpg</Key>",
        "    <ETag>&quot;9b2cf535f27731c974343645a3985328&quot;</ETag>",
        "    <Size>434234</Size>",
        "    <StorageClass>STANDARD</StorageClass>",
        "  </Contents>",
        "  <Contents>",
        "    <Key>2026/b.jpg</Key>",
        "    <ETag>&quot;a1&quot;</ETag>",
        "    <Size>1</Size>",
        "    <StorageClass>GLACIER</StorageClass>",
        "  </Contents>",
        "  <CommonPrefixes><Prefix>2026/raw/</Prefix></CommonPrefixes>",
        "</ListBucketResult>",
    ).joinToString(separator = LINE_BREAK)
}
