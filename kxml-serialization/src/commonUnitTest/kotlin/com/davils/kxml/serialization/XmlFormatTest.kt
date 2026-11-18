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

package com.davils.kxml.serialization

import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.emit.XmlEmitOptions
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.serialization.fixture.Annotated
import com.davils.kxml.serialization.fixture.Bucket
import com.davils.kxml.serialization.fixture.BucketList
import com.davils.kxml.serialization.fixture.Checksum
import com.davils.kxml.serialization.fixture.CommonPrefix
import com.davils.kxml.serialization.fixture.Contact
import com.davils.kxml.serialization.fixture.Defaults
import com.davils.kxml.serialization.fixture.Labels
import com.davils.kxml.serialization.fixture.ListBucketResult
import com.davils.kxml.serialization.fixture.ObjectSummary
import com.davils.kxml.serialization.fixture.Owner
import com.davils.kxml.serialization.fixture.Payloads
import com.davils.kxml.serialization.fixture.Plain
import com.davils.kxml.serialization.fixture.Primitives
import com.davils.kxml.serialization.fixture.StorageClass
import com.davils.kxml.serialization.fixture.Tagging
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private const val SIZE: Long = 434_234
private const val SMALL: Byte = -7
private const val SHORT: Short = 300
private const val INT: Int = 70_000
private const val LONG: Long = 5_000_000_000
private const val FLOAT: Float = 1.5f
private const val DOUBLE: Double = 2.25
private const val FIRST_BYTE: Byte = 1
private const val SECOND_BYTE: Byte = 2
private const val SEVEN: Int = 7
private const val TYPED_PREFIX: String = "<!DOCTYPE c><CommonPrefix><prefix>p</prefix></CommonPrefix>"

private val s3: XmlFormat = xmlFormat {
    namespace = Payloads.S3
    namingStrategy = XmlNamingStrategy.PascalCase
    xml { emitter = XmlEmitOptions.Compact }
}

private val listing = ListBucketResult(
    name = "photos",
    keyCount = 2,
    isTruncated = false,
    contents = listOf(
        ObjectSummary(key = "2026/a.jpg", eTag = "\"9b2cf535f27731c974343645a3985328\"", size = SIZE),
        ObjectSummary(key = "2026/b.jpg", eTag = "\"a1\"", size = 1, storageClass = StorageClass.Glacier),
    ),
    commonPrefixes = listOf(CommonPrefix("2026/raw/")),
)

class XmlFormatTest : FunSpec({
    context("an S3 listing") {
        test("is decoded from the document S3 sends") {
            s3.decodeFromString<ListBucketResult>(Payloads.listing) shouldBe listing
        }

        test("is encoded as S3 expects it") {
            val text = s3.encodeToString(listing)

            text shouldStartWith """<?xml version="1.0" encoding="UTF-8"?><ListBucketResult xmlns="${Payloads.S3}">"""
            text shouldContain "<Contents><Key>2026/a.jpg</Key>"
            text shouldContain "<CommonPrefixes><Prefix>2026/raw/</Prefix></CommonPrefixes>"
            s3.decodeFromString<ListBucketResult>(text) shouldBe listing
        }

        test("is decoded without a namespace when only local names are compared") {
            val lenient = xmlFormat {
                namespace = Payloads.S3
                namespaceMatching = XmlNamespaceMatching.LocalName
                namingStrategy = XmlNamingStrategy.PascalCase
            }
            val text = Payloads.listing.replace(oldValue = """ xmlns="${Payloads.S3}"""", newValue = "")

            lenient.decodeFromString<ListBucketResult>(text) shouldBe listing
        }
    }

    context("collections") {
        test("are wrapped when asked") {
            val written = listOf(
                Bucket(name = "a", creationDate = "2026-01-01"),
                Bucket(name = "b", creationDate = "2026-01-02"),
            )
            val list = BucketList(Owner("o1"), written)
            val element = s3.encodeToElement(list)
            val buckets = element.firstElement(XmlName(local = "Buckets", namespace = Payloads.S3))

            buckets?.run { elements.size } shouldBe 2
            s3.decodeFromElement<BucketList>(element) shouldBe list
        }

        test("keep their default when none of their elements stands in the document") {
            XmlFormat.Default.decodeFromString<Defaults>("<Defaults/>") shouldBe Defaults()
            XmlFormat.Default.decodeFromString<Defaults>("<Defaults><tags>a</tags></Defaults>") shouldBe
                Defaults(listOf("a"))
        }

        test("map entries to keys and values") {
            val tagging = Tagging(mapOf("team" to "storage", "tier" to "gold"))
            val text = s3.encodeToString(tagging)

            text shouldContain "<TagSet><Tag><Key>team</Key><Value>storage</Value></Tag>"
            s3.decodeFromString<Tagging>(text) shouldBe tagging
        }

        test("map entries with their own names and absent values") {
            val labels = Labels(mapOf("a" to 1, "b" to null))
            val text = s3.encodeToString(labels)

            text shouldContain "<Label><Name>a</Name><Text>1</Text></Label><Label><Name>b</Name></Label>"
            s3.decodeFromString<Labels>(text) shouldBe labels
        }

        test("of values repeat their element") {
            val primitives = Primitives(
                byte = SMALL,
                short = SHORT,
                int = INT,
                long = LONG,
                float = FLOAT,
                double = DOUBLE,
                char = 'x',
                isOn = true,
                bytes = listOf(FIRST_BYTE, SECOND_BYTE),
                keys = listOf("a", "b"),
            )
            val text = s3.encodeToString(primitives)

            text shouldContain "<Keys>a</Keys><Keys>b</Keys>"
            s3.decodeFromString<Primitives>(text) shouldBe primitives
        }
    }

    context("annotations") {
        test("place values in attributes, text, other namespaces and aliases") {
            val annotated = Annotated(id = SEVEN, origin = "eu", text = "body", version = "v1")
            val text = XmlFormat.Default.encodeToString(annotated)

            text shouldContain """<a:Annotated xmlns:a="urn:annotated" xmlns:o="urn:other" id="7" o:origin="eu">body"""
            XmlFormat.Default.decodeFromString<Annotated>(text) shouldBe annotated
        }

        test("leave out an attribute without a value") {
            val annotated = Annotated(id = 1, text = "body")
            val text = XmlFormat.Default.encodeToString(annotated)

            text shouldContain """<a:Annotated xmlns:a="urn:annotated" id="1">body</a:Annotated>"""
        }

        test("read a property under an alias") {
            val text = """<a:Annotated xmlns:a="urn:annotated" id="1">x<a:Revision>r2</a:Revision></a:Annotated>"""
            val annotated = XmlFormat.Default.decodeFromString<Annotated>(text)

            annotated.version shouldBe "r2"
        }

        test("take a property out of the namespace of the document") {
            val text = s3.encodeToString(Plain("p"))

            text shouldContain """<Plain xmlns="${Payloads.S3}"><Name xmlns="">p</Name></Plain>"""
            s3.decodeFromString<Plain>(text) shouldBe Plain("p")
        }
    }

    context("values") {
        test("write binary data as base64") {
            val checksum = Checksum(byteArrayOf(FIRST_BYTE, SECOND_BYTE))
            val text = XmlFormat.Default.encodeToString(checksum)
            val decoded = XmlFormat.Default.decodeFromString<Checksum>(text)

            text shouldContain "<value>AQI=</value>"
            decoded.isSameAs(checksum) shouldBe true
        }

        test("read a missing nullable property as null") {
            XmlFormat.Default.decodeFromString<Contact>("<Contact/>") shouldBe Contact(null)
        }
    }

    context("documents") {
        test("leave out the declaration when asked") {
            val bare = xmlFormat { isDeclarationWritten = false }

            bare.encodeToString(CommonPrefix("p")) shouldStartWith "<CommonPrefix>"
        }

        test("leave out defaults when asked") {
            val sparse = xmlFormat {
                isDefaultEncoded = false
                xml { emitter = XmlEmitOptions.Compact }
            }

            val summary = ObjectSummary(key = "k", eTag = "e", size = 1)
            sparse.encodeToString(summary) shouldContain "<size>1</size></ObjectSummary>"
        }

        test("write an absent root as an empty element") {
            XmlFormat.Default.encodeToString<Owner?>(null) shouldContain "<Owner/>"
        }

        test("keep the warnings of the reader") {
            val tolerant = xmlFormat { xml { doctype = XmlDoctype.Ignore } }
            val decoding = tolerant.decodeOrFailure<CommonPrefix>(TYPED_PREFIX)
            val decoded = decoding.shouldBeInstanceOf<XmlDecoding.Decoded<CommonPrefix>>()

            decoded.value shouldBe CommonPrefix("p")
            decoded.diagnostics.size shouldBe 1
        }

        test("decode an element") {
            val element = XmlFormat.Default.encodeToElement(CommonPrefix("p"))
            val decoding = XmlFormat.Default.decodeOrFailure<CommonPrefix>(element)

            decoding shouldBe XmlDecoding.Decoded(CommonPrefix("p"), emptyList())
        }
    }
})
