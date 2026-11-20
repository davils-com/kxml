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
package com.davils.kxml.document.merge

import com.davils.kxml.core.limit.xmlLimits
import com.davils.kxml.core.name.XmlName
import com.davils.kxml.core.node.XmlDocument
import com.davils.kxml.document.XmlDocumentLimitException
import com.davils.kxml.document.c14n.XmlCanonicalization
import com.davils.kxml.document.c14n.XmlCanonicalizer
import com.davils.kxml.document.fixture.documentOf
import com.davils.kxml.document.fixture.hostileText
import com.davils.kxml.document.fixture.shouldBeSafeToShow
import com.davils.kxml.document.fixture.shouldBeShownSafely
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.checkAll

private const val CASES: Int = 100
private const val BASE: String = "base"
private const val LOG_TEXT: String = "/config[1]/log[1]/text()[1]"
private const val UNKEYED: Int = 3
private const val PRODUCTION: String = "production"
private const val DEFAULTS: String =
    "<config a='1' xmlns:p='urn:p'><server id='s1' port='80'/><server id='s2' port='81'/>" +
        "<log>info</log><p:extra/></config>"

private val server: XmlName = XmlName("server")
private val id: XmlName = XmlName("id")
private val delete: XmlName = XmlName("delete")
private val keyed: XmlMerger = XmlMerger(XmlMergeOptions(keys = mapOf(server to listOf(id)), deletionMarker = delete))
private val canonicalizer: XmlCanonicalizer = XmlCanonicalizer(XmlCanonicalization.Canonical10WithComments)

private fun canonicalOf(document: XmlDocument): String = canonicalizer.canonicalize(document).decodeToString()

private fun merged(vararg layers: Pair<String, String>, merger: XmlMerger = keyed): XmlMerged =
    merger.merge(layers.map { (name, text) -> XmlLayer(name, documentOf(text)) })

private fun shouldMergeTo(merged: XmlMerged, expected: String) {
    canonicalOf(merged.document) shouldBe canonicalOf(documentOf(expected))
}

class XmlMergerTest : FunSpec({
    context("a merge") {
        test("overrides attributes and matches elements by their keys") {
            val result = merged(
                BASE to DEFAULTS,
                PRODUCTION to "<config b='2'><server id='s2' port='443'/><server id='s3' port='82'/></config>",
            )

            shouldMergeTo(
                result,
                "<config a='1' b='2' xmlns:p='urn:p'><server id='s1' port='80'/><server id='s2' port='443'/>" +
                    "<log>info</log><p:extra/><server id='s3' port='82'/></config>",
            )
            result.originOf("/config[1]/server[2]/@port") shouldBe PRODUCTION
            result.originOf("/config[1]/server[1]/@port") shouldBe BASE
            result.originOf("/config[1]/server[3]") shouldBe PRODUCTION
            result.originOf("/config[1]/@b") shouldBe PRODUCTION
            result.originOf(LOG_TEXT) shouldBe BASE
            result.originOf("/config[1]/server[3]/@id/missing") shouldBe PRODUCTION
            result.originOf("/other[1]").shouldBeNull()
        }

        test("replaces text content and merges elements that occur once") {
            val overlay = "<config><log>debug</log><p:extra x='1' xmlns:p='urn:p'/></config>"
            val result = merged(BASE to DEFAULTS, PRODUCTION to overlay)

            result.originOf("/config[1]/log[1]") shouldBe PRODUCTION
            result.originOf(LOG_TEXT) shouldBe PRODUCTION
            result.originOf("/config[1]/p:extra[1]/@x") shouldBe PRODUCTION
            val root = result.document.root
            root.elements[2].text shouldBe "debug"
        }

        test("deletes what the deletion marker names and ignores what it names in vain") {
            val overlay = "<config><server id='s1' delete='true'/><server id='s9' delete='true'/></config>"
            val result = merged(BASE to DEFAULTS, PRODUCTION to overlay)

            shouldMergeTo(
                result,
                "<config a='1' xmlns:p='urn:p'><server id='s2' port='81'/><log>info</log><p:extra/></config>",
            )
        }

        test("appends elements that match nothing, after the white space of the last element") {
            val result = merged(
                BASE to "<config>\n  <a/>\n</config>",
                PRODUCTION to "<config><b/><a/><a/><q:c xmlns:q='urn:q'/></config>",
                merger = XmlMerger(),
            )
            val root = result.document.root
            val names = root.elements.map { element -> element.name.local }

            names shouldBe listOf("a", "b", "a", "a", "c")
            val empty = merged(BASE to "<config/>", PRODUCTION to "<config><b/></config>", merger = XmlMerger())
            val emptyRoot = empty.document.root
            val only = emptyRoot.elements.single()
            only.name.local shouldBe "b"
        }

        test("keeps elements whose keys are missing apart") {
            val result = merged(BASE to DEFAULTS, PRODUCTION to "<config><server port='1'/></config>")

            val root = result.document.root
            val servers = root.elements.count { element -> element.name == server }
            servers shouldBe UNKEYED
        }
    }

    context("the content a layer replaces") {
        test("keeps its comments, instructions and CDATA sections with their origins") {
            val result = merged(
                BASE to "<config><log><!--b--></log></config>",
                PRODUCTION to "<config><log><![CDATA[x]]><!--c--><?p d?></log><other/><other/></config>",
                merger = XmlMerger(),
            )

            result.originOf("/config[1]/log[1]/comment()[1]") shouldBe PRODUCTION
            result.originOf("/config[1]/log[1]/processing-instruction()[1]") shouldBe PRODUCTION
            result.originOf(LOG_TEXT) shouldBe PRODUCTION
            result.originOf("/config[1]/other[2]") shouldBe PRODUCTION
            XmlMergeOptions().hashCode() shouldBe XmlMergeOptions().hashCode()
        }
    }

    context("the layers of a merge") {
        test("must share the document element and stay within the limit") {
            shouldThrow<IllegalArgumentException> { keyed.merge(emptyList()) }
            shouldThrow<IllegalArgumentException> { merged(BASE to "<a/>", PRODUCTION to "<b/>") }
            val limited = XmlMerger(limits = xmlLimits { maximumMergeLayers = 1 })
            shouldThrow<XmlDocumentLimitException> { merged(BASE to "<a/>", PRODUCTION to "<a/>", merger = limited) }
        }

        test("show their names escaped") {
            checkAll(CASES, hostileText) { name ->
                XmlLayer(name, documentOf("<a/>")).shouldBeShownSafely()
                val error = runCatching { merged(BASE to "<a/>", name to "<b/>") }.exceptionOrNull()
                error?.message?.shouldBeSafeToShow()
            }
        }

        test("compare by name and document") {
            val layer = XmlLayer(BASE, documentOf("<a/>"))

            layer shouldBe XmlLayer(BASE, documentOf("<a/>"))
            layer.hashCode() shouldBe XmlLayer(BASE, documentOf("<a/>")).hashCode()
            layer shouldNotBe XmlLayer(PRODUCTION, documentOf("<a/>"))
            layer.equals(BASE) shouldBe false
            layer.toString() shouldBe "XmlLayer(\"base\")"
        }
    }

    context("options and mergers") {
        test("compare by their content") {
            val options = XmlMergeOptions(keys = mapOf(server to listOf(id)), deletionMarker = delete)

            options shouldBe XmlMergeOptions(keys = mapOf(server to listOf(id)), deletionMarker = delete)
            val same = XmlMergeOptions(keys = mapOf(server to listOf(id)), deletionMarker = delete)
            options.hashCode() shouldBe same.hashCode()
            options shouldNotBe XmlMergeOptions()
            options.equals(id) shouldBe false
            options.toString() shouldBe "XmlMergeOptions(keys=1, deletion=delete)"
            XmlMergeOptions().toString() shouldBe "XmlMergeOptions(keys=0, deletion=none)"
            shouldThrow<IllegalArgumentException> { XmlMergeOptions(keys = mapOf(server to emptyList())) }
            keyed shouldBe XmlMerger(options)
            keyed.hashCode() shouldBe XmlMerger(options).hashCode()
            keyed shouldNotBe XmlMerger()
            keyed.equals(options) shouldBe false
            keyed.toString() shouldBe "XmlMerger(XmlMergeOptions(keys=1, deletion=delete))"
            merged(BASE to "<a/>").toString() shouldBe "XmlMerged(origins=1)"
        }
    }
})
