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

package com.davils.kxml.core.resolve

import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.diagnostic.XmlDiagnosticCode
import com.davils.kxml.core.limit.xmlLimits
import com.davils.kxml.core.resolve.fixture.MapLoader
import com.davils.kxml.core.resolve.fixture.OTHER
import com.davils.kxml.core.resolve.fixture.ROOT
import com.davils.kxml.core.resolve.fixture.catalog
import com.davils.kxml.core.resolve.fixture.nextCatalog
import com.davils.kxml.core.resolve.fixture.public
import com.davils.kxml.core.resolve.fixture.system
import com.davils.kxml.core.source.XmlSource
import com.davils.kxml.core.xml
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val DOCUMENT: String = "https://doc.test/book.xml"
private const val SUBSET: String = "https://c.test/dtd/a.dtd"
private const val ENTITY: String = "https://c.test/dtd/ent.xml"
private const val LOCAL: String = "x.dtd"
private const val PUBLIC_ID: String = "-//P//DTD A//EN"
private const val WITH_PUBLIC_SUBSET: String = "<!DOCTYPE a PUBLIC '$PUBLIC_ID' 'http://remote.test/a.dtd'><a>&ent;</a>"
private const val WITH_LOCAL_SUBSET: String = "<!DOCTYPE a SYSTEM 'x.dtd'><a/>"

private fun parsed(resolver: XmlResolver, text: String): XmlParseResult {
    val reader = xml {
        doctype = XmlDoctype.External
        this.resolver = resolver
    }
    return reader.parse(XmlSource.of(text, uri = DOCUMENT))
}

private fun refusal(resolver: XmlResolver, text: String): XmlDiagnosticCode {
    val failed = parsed(resolver, text).shouldBeInstanceOf<XmlParseResult.Failed>()
    return failed.diagnostics.last().code
}

private fun resolverOf(loader: MapLoader, fallback: XmlResolver? = null): XmlCatalogResolver {
    val catalog = XmlCatalog.load(catalogs = listOf(ROOT), loader = loader)
    return XmlCatalogResolver(catalog = catalog, loader = loader, fallback = fallback)
}

private fun request(systemId: String, baseUri: String?): XmlResourceRequest = XmlResourceRequest(
    kind = XmlResourceKind.ExternalSubset,
    name = null,
    publicId = null,
    systemId = systemId,
    baseUri = baseUri,
)

class XmlCatalogResolverTest : FunSpec({
    context("a catalog resolver") {
        test("loads what the catalog maps a request to, with the mapped URI as the base of its references") {
            val loader = MapLoader(
                ROOT to catalog(
                    public(publicId = PUBLIC_ID, uri = "dtd/a.dtd"),
                    system(systemId = ENTITY, uri = "dtd/ent.xml"),
                ),
                SUBSET to "<!ENTITY ent SYSTEM 'ent.xml'>",
                ENTITY to "chapter",
            )

            val result = parsed(resolverOf(loader), WITH_PUBLIC_SUBSET).shouldBeInstanceOf<XmlParseResult.Parsed>()

            result.document.root.text shouldBe "chapter"
            loader.loads shouldContainExactly listOf(ROOT, SUBSET, ENTITY)
        }

        test("asks the fallback for what the catalog does not map and refuses it without one") {
            val loader = MapLoader(ROOT to catalog())
            val fallback = RecordingResolver(LOCAL to "")

            parsed(resolverOf(loader, fallback), WITH_LOCAL_SUBSET).shouldBeInstanceOf<XmlParseResult.Parsed>()
            fallback.requests.map(XmlResourceRequest::systemId) shouldContainExactly listOf(LOCAL)
            refusal(resolverOf(loader), WITH_LOCAL_SUBSET) shouldBe XmlDiagnosticCode.UnresolvedEntity
        }

        test("refuses what the catalog maps but the loader refuses, without asking the fallback") {
            val loader = MapLoader(ROOT to catalog(system(systemId = LOCAL, uri = "missing.dtd")))
            val fallback = RecordingResolver(LOCAL to "")

            refusal(resolverOf(loader, fallback), WITH_LOCAL_SUBSET) shouldBe XmlDiagnosticCode.UnresolvedEntity
            fallback.requests.shouldBeEmpty()
        }

        test("refuses a request whose lookup stops at the limits, without asking the fallback") {
            val loader = MapLoader(
                ROOT to catalog(nextCatalog(OTHER)),
                OTHER to catalog(system(systemId = LOCAL, uri = LOCAL)),
            )
            val catalog = XmlCatalog.load(listOf(ROOT), loader, limits = xmlLimits { maximumCatalogFiles = 1 })
            val fallback = RecordingResolver(LOCAL to "")

            val resolver = XmlCatalogResolver(catalog = catalog, loader = loader, fallback = fallback)

            refusal(resolver, WITH_LOCAL_SUBSET) shouldBe XmlDiagnosticCode.UnresolvedEntity
            fallback.requests.shouldBeEmpty()
        }

        test("shows its catalog and whether a fallback is set") {
            val loader = MapLoader(ROOT to catalog())

            resolverOf(loader).toString() shouldBe "XmlCatalogResolver(XmlCatalog(files=1, problems=0), fallback=none)"
            resolverOf(loader, RecordingResolver()).toString() shouldBe
                "XmlCatalogResolver(XmlCatalog(files=1, problems=0), fallback=set)"
        }
    }

    context("absoluteSystemId") {
        test("escapes the system identifier and resolves it against the base URI") {
            request(systemId = "a b/ä.dtd", baseUri = "https://c.test/dir/doc.xml").absoluteSystemId shouldBe
                "https://c.test/dir/a%20b/%C3%A4.dtd"
            request(
                systemId = "../x.dtd",
                baseUri = "https://c.test/dir/doc.xml",
            ).absoluteSystemId shouldBe "https://c.test/x.dtd"
            request(systemId = "urn:x:y", baseUri = null).absoluteSystemId shouldBe "urn:x:y"
        }

        test("is null when the system identifier cannot be made absolute") {
            request(systemId = LOCAL, baseUri = null).absoluteSystemId.shouldBeNull()
            request(systemId = LOCAL, baseUri = "relative/doc.xml").absoluteSystemId.shouldBeNull()
            request(systemId = "%zz", baseUri = DOCUMENT).absoluteSystemId.shouldBeNull()
            request(systemId = LOCAL, baseUri = "%zz").absoluteSystemId.shouldBeNull()
        }
    }
})
