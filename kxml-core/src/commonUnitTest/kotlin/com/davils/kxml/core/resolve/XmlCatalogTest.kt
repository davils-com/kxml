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

import com.davils.kxml.core.fixture.GROWTH_FACTOR
import com.davils.kxml.core.fixture.hostileText
import com.davils.kxml.core.fixture.shouldBeShownSafely
import com.davils.kxml.core.fixture.shouldGrowLinearly
import com.davils.kxml.core.limit.xmlLimits
import com.davils.kxml.core.resolve.fixture.MapLoader
import com.davils.kxml.core.resolve.fixture.OTHER
import com.davils.kxml.core.resolve.fixture.ROOT
import com.davils.kxml.core.resolve.fixture.THIRD
import com.davils.kxml.core.resolve.fixture.catalog
import com.davils.kxml.core.resolve.fixture.loaded
import com.davils.kxml.core.resolve.fixture.loadedFrom
import com.davils.kxml.core.resolve.fixture.nextCatalog
import com.davils.kxml.core.resolve.fixture.public
import com.davils.kxml.core.resolve.fixture.single
import com.davils.kxml.core.resolve.fixture.system
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.checkAll

private const val BASE: String = "https://c.test/"
private const val FOURTH: String = "https://c.test/fourth.xml"
private const val PUBLIC_ID: String = "-//P//DTD A//EN"
private const val DOCBOOK: String = "-//OASIS//DTD DocBook XML V4.1.2//EN"
private const val DOCBOOK_URN: String = "urn:publicid:-:OASIS:DTD+DocBook+XML+V4.1.2:EN"
private const val DELEGATED: String = "http://d.test/long/a.dtd"
private const val SCHEMA: String = "http://s.test/"
private const val A_DTD: String = "a.dtd"
private const val P_DTD: String = "p.dtd"
private const val S_DTD: String = "s.dtd"
private const val DOCBOOK_DTD: String = "docbook.dtd"
private const val B_DTD: String = "b.dtd"
private const val BROKEN: String = "broken.xml"
private const val FOREIGN: String = "foreign.xml"
private const val UNKNOWN: String = "<unknown/>"
private const val U_DTD: String = "u.dtd"
private const val CASES: Int = 200
private const val SMALL_ENTRIES: Int = 200

private fun target(path: String): String = BASE + path

private fun entries(count: Int): String {
    val systems = Array(count) { index -> system(systemId = "s$index", uri = "t$index") }
    return catalog(entries = systems)
}

private fun group(attributes: String, entry: String): String = "<group $attributes>$entry</group>"

class XmlCatalogTest : FunSpec({
    context("loading") {
        test("reads the entries of the catalog namespace and skips other namespaces with what they enclose") {
            val catalog = single(
                system(systemId = A_DTD, uri = "x/a.dtd"),
                "<o:ext xmlns:o='urn:other'>${system(systemId = B_DTD, uri = B_DTD)}</o:ext>",
            )

            catalog.resolveExternalId(publicId = null, systemId = A_DTD) shouldBe target("x/a.dtd")
            catalog.resolveExternalId(publicId = null, systemId = B_DTD).shouldBeNull()
            catalog.problems.shouldBeEmpty()
        }

        test("treats a refused, malformed or foreign catalog file as empty and reports it") {
            val catalog = loaded(
                ROOT to catalog(
                    nextCatalog("missing.xml"),
                    nextCatalog(BROKEN),
                    nextCatalog(FOREIGN),
                    nextCatalog("good.xml"),
                ),
                target(BROKEN) to "<catalog",
                target(FOREIGN) to "<other/>",
                target("good.xml") to catalog(system(systemId = A_DTD, uri = A_DTD)),
            )

            catalog.resolveExternalId(publicId = null, systemId = A_DTD) shouldBe target(A_DTD)
            catalog.problems.map(XmlCatalogProblem::kind) shouldContainExactly listOf(
                XmlCatalogProblemKind.Unloaded,
                XmlCatalogProblemKind.Malformed,
                XmlCatalogProblemKind.NotCatalog,
            )
            catalog.problems.map(XmlCatalogProblem::uri) shouldContainExactly
                listOf(target("missing.xml"), target(BROKEN), target(FOREIGN))
        }

        test("ignores and reports an entry that misses an attribute, names no URI or is unknown") {
            val catalog = single(
                "<system uri='x'/>",
                "<system systemId='a' uri='%zz'/>",
                UNKNOWN,
                group(attributes = "xml:base='%'", entry = system(systemId = "b", uri = "b")),
                group(attributes = "prefer='sometimes'", entry = public(publicId = PUBLIC_ID, uri = P_DTD)),
            )

            catalog.problems.map(XmlCatalogProblem::detail) shouldContainExactly listOf(
                "misses the attribute systemId on system",
                "has a uri on system that is no URI reference",
                "has the unknown element unknown",
                "has an xml:base on group that is no URI reference",
                "has a prefer attribute that is neither public nor system",
            )
            catalog.problems.map(XmlCatalogProblem::kind).toSet() shouldBe setOf(XmlCatalogProblemKind.InvalidEntry)
            catalog.resolveExternalId(publicId = null, systemId = "b").shouldBeNull()
            catalog.resolveExternalId(publicId = PUBLIC_ID, systemId = null) shouldBe target(P_DTD)
        }

        test("refuses a catalog file that is named by a relative URI") {
            val loader = MapLoader()
            val catalog = XmlCatalog.load(catalogs = listOf("catalog.xml"), loader = loader)

            catalog.problems.single().detail shouldBe "is no absolute URI"
            loader.loads.shouldBeEmpty()
            catalog.toString() shouldBe "XmlCatalog(files=0, problems=1)"
        }

        test("loads every catalog file once, also when catalog files refer to each other in a circle") {
            val loader = MapLoader(
                ROOT to catalog(nextCatalog(OTHER)),
                OTHER to catalog(nextCatalog(ROOT), "<delegateSystem systemIdStartString='x' catalog='catalog.xml'/>"),
            )
            val catalog = XmlCatalog.load(catalogs = listOf(ROOT, ROOT), loader = loader)

            catalog.resolveExternalId(publicId = null, systemId = "y").shouldBeNull()
            catalog.resolveExternalId(publicId = null, systemId = "xa").shouldBeNull()
            loader.loads shouldContainExactly listOf(ROOT, OTHER)
            catalog.toString() shouldBe "XmlCatalog(files=2, problems=0)"
        }

        test("reads a catalog with the document type declaration of the specification without loading the DTD") {
            val doctype = "<!DOCTYPE catalog PUBLIC '-//OASIS//DTD XML Catalogs V1.1//EN' " +
                "'http://www.oasis-open.org/committees/entity/release/1.1/catalog.dtd'>"
            val loader = MapLoader(ROOT to doctype + catalog(system(systemId = "a", uri = A_DTD)))

            XmlCatalog.load(listOf(ROOT), loader).resolveExternalId(publicId = null, systemId = "a") shouldBe
                target(A_DTD)
            loader.loads shouldContainExactly listOf(ROOT)
        }

        test("stops a lookup at a catalog file beyond the most files instead of skipping it") {
            val files = mapOf(
                ROOT to catalog(nextCatalog(OTHER), nextCatalog(THIRD)),
                OTHER to catalog(),
                THIRD to catalog(system(systemId = "a", uri = A_DTD)),
            )

            val two = loadedFrom(files, limits = xmlLimits { maximumCatalogFiles = 2 })
            val three = loadedFrom(files, limits = xmlLimits { maximumCatalogFiles = 3 })

            two.resolveExternalId(publicId = null, systemId = "a").shouldBeNull()
            two.problems.single().kind shouldBe XmlCatalogProblemKind.LimitExceeded
            two.problems.single().uri shouldBe THIRD
            three.resolveExternalId(publicId = null, systemId = "a") shouldBe target(A_DTD)
        }

        test("stops a lookup at a catalog file beyond the deepest chain of catalog files") {
            val files = mapOf(
                ROOT to catalog(nextCatalog(OTHER)),
                OTHER to catalog(nextCatalog(THIRD)),
                THIRD to catalog(system(systemId = "a", uri = A_DTD)),
            )

            val shallow = loadedFrom(files, limits = xmlLimits { maximumCatalogDepth = 1 })
            val deep = loadedFrom(files, limits = xmlLimits { maximumCatalogDepth = 2 })

            shallow.resolveExternalId(publicId = null, systemId = "a").shouldBeNull()
            deep.resolveExternalId(publicId = null, systemId = "a") shouldBe target(A_DTD)
        }
    }

    context("resolveExternalId") {
        test("returns the first system entry that equals the system identifier, made absolute against its base") {
            val catalog = single(system(systemId = A_DTD, uri = "one.dtd"), system(systemId = A_DTD, uri = "two.dtd"))

            catalog.resolveExternalId(publicId = null, systemId = A_DTD) shouldBe target("one.dtd")
        }

        test("compares a relative system identifier as it is written") {
            val catalog = single(system(systemId = "https://doc.test/a.dtd", uri = A_DTD))

            catalog.resolveExternalId(publicId = null, systemId = A_DTD).shouldBeNull()
        }

        test("rewrites a system identifier with the longest matching prefix") {
            val catalog = single(
                "<rewriteSystem systemIdStartString='http://x.test/' rewritePrefix='short/'/>",
                "<rewriteSystem systemIdStartString='http://x.test/dtd/' rewritePrefix='long/'/>",
            )

            catalog.resolveExternalId(publicId = null, systemId = "http://x.test/dtd/a/b.dtd") shouldBe
                target("long/a/b.dtd")
            catalog.resolveExternalId(publicId = null, systemId = "http://x.test/c.dtd") shouldBe target("short/c.dtd")
        }

        test("maps a system identifier by the longest matching suffix") {
            val catalog = single(
                "<systemSuffix systemIdSuffix='b.dtd' uri='short.dtd'/>",
                "<systemSuffix systemIdSuffix='/a/b.dtd' uri='long.dtd'/>",
            )

            catalog.resolveExternalId(publicId = null, systemId = "http://y.test/a/b.dtd") shouldBe target("long.dtd")
            catalog.resolveExternalId(publicId = null, systemId = "http://y.test/b.dtd") shouldBe target("short.dtd")
        }

        test("tries the system entries of a file before its public entries, whatever their order") {
            val catalog = single(public(publicId = PUBLIC_ID, uri = P_DTD), system(systemId = S_DTD, uri = S_DTD))

            catalog.resolveExternalId(publicId = PUBLIC_ID, systemId = S_DTD) shouldBe target(S_DTD)
            catalog.resolveExternalId(publicId = PUBLIC_ID, systemId = null) shouldBe target(P_DTD)
            catalog.resolveExternalId(publicId = PUBLIC_ID, systemId = "other.dtd") shouldBe target(P_DTD)
        }

        test("matches a public entry with a system identifier only where prefer is public") {
            val preferSystem = single(public(publicId = PUBLIC_ID, uri = P_DTD), attributes = "prefer='system'")
            val groupPrefersPublic = single(
                group(attributes = "prefer='public'", entry = public(publicId = PUBLIC_ID, uri = "g.dtd")),
                attributes = "prefer='system'",
            )
            val byDefault = loaded(
                ROOT to catalog(public(publicId = PUBLIC_ID, uri = P_DTD)),
                prefer = XmlCatalogPrefer.System,
            )

            preferSystem.resolveExternalId(publicId = PUBLIC_ID, systemId = U_DTD).shouldBeNull()
            preferSystem.resolveExternalId(publicId = PUBLIC_ID, systemId = null) shouldBe target(P_DTD)
            groupPrefersPublic.resolveExternalId(publicId = PUBLIC_ID, systemId = U_DTD) shouldBe target("g.dtd")
            byDefault.resolveExternalId(publicId = PUBLIC_ID, systemId = U_DTD).shouldBeNull()
        }

        test("delegates by system identifier, longest prefix first, without returning to the first list") {
            val catalog = loaded(
                ROOT to catalog(
                    "<delegateSystem systemIdStartString='http://d.test/' catalog='other.xml'/>",
                    "<delegateSystem systemIdStartString='http://d.test/long/' catalog='third.xml'/>",
                    nextCatalog("fourth.xml"),
                ),
                OTHER to catalog(
                    system(systemId = DELEGATED, uri = "from-other.dtd"),
                    public(publicId = PUBLIC_ID, uri = "public.dtd"),
                ),
                THIRD to catalog(system(systemId = DELEGATED, uri = "from-third.dtd")),
                FOURTH to catalog(system(systemId = "http://d.test/x.dtd", uri = "from-fourth.dtd")),
            )

            catalog.resolveExternalId(publicId = null, systemId = DELEGATED) shouldBe target("from-third.dtd")
            catalog.resolveExternalId(publicId = null, systemId = "http://d.test/x.dtd").shouldBeNull()
            catalog.resolveExternalId(publicId = PUBLIC_ID, systemId = "http://d.test/y.dtd").shouldBeNull()
        }

        test("delegates by public identifier") {
            val catalog = loaded(
                ROOT to catalog("<delegatePublic publicIdStartString='-//P//' catalog='other.xml'/>"),
                OTHER to catalog(public(publicId = PUBLIC_ID, uri = A_DTD)),
            )

            catalog.resolveExternalId(publicId = PUBLIC_ID, systemId = null) shouldBe target(A_DTD)
            catalog.resolveExternalId(publicId = "-//Q//EN", systemId = null).shouldBeNull()
        }

        test("searches the next catalogs after the other entries of a file, depth first and in order") {
            val catalog = loaded(
                ROOT to catalog(nextCatalog(OTHER), nextCatalog(THIRD), system(systemId = "a", uri = "root-a.dtd")),
                OTHER to catalog(system(systemId = "b", uri = "other-b.dtd"), nextCatalog(FOURTH)),
                THIRD to catalog(
                    system(systemId = "b", uri = "third-b.dtd"),
                    system(systemId = "c", uri = "third-c.dtd"),
                    system(systemId = "d", uri = "third-d.dtd"),
                ),
                FOURTH to catalog(system(systemId = "c", uri = "fourth-c.dtd")),
            )

            catalog.resolveExternalId(publicId = null, systemId = "a") shouldBe target("root-a.dtd")
            catalog.resolveExternalId(publicId = null, systemId = "b") shouldBe target("other-b.dtd")
            catalog.resolveExternalId(publicId = null, systemId = "c") shouldBe target("fourth-c.dtd")
            catalog.resolveExternalId(publicId = null, systemId = "d") shouldBe target("third-d.dtd")
        }

        test("unwraps a public identifier written as a urn:publicid URN") {
            val catalog = single(
                public(publicId = DOCBOOK, uri = DOCBOOK_DTD),
                public(publicId = PUBLIC_ID, uri = P_DTD),
            )

            catalog.resolveExternalId(publicId = null, systemId = DOCBOOK_URN) shouldBe target(DOCBOOK_DTD)
            catalog.resolveExternalId(publicId = DOCBOOK_URN, systemId = null) shouldBe target(DOCBOOK_DTD)
            catalog.resolveExternalId(publicId = DOCBOOK, systemId = DOCBOOK_URN) shouldBe target(DOCBOOK_DTD)
            catalog.resolveExternalId(publicId = PUBLIC_ID, systemId = DOCBOOK_URN) shouldBe target(P_DTD)
        }

        test("compares identifiers after the normalization of section 6") {
            val catalog = single(
                public(publicId = "-//P//DTD  Spaced//EN", uri = "spaced.dtd"),
                system(systemId = "a b.dtd", uri = "space.dtd"),
                system(systemId = "ä.dtd", uri = "umlaut.dtd"),
            )

            catalog.resolveExternalId(publicId = " -//P//DTD\tSpaced//EN ", systemId = null) shouldBe
                target("spaced.dtd")
            catalog.resolveExternalId(publicId = null, systemId = "a%20b.dtd") shouldBe target("space.dtd")
            catalog.resolveExternalId(publicId = null, systemId = "%C3%A4.dtd") shouldBe target("umlaut.dtd")
            catalog.resolveExternalId(publicId = null, systemId = "%c3%a4.dtd").shouldBeNull()
            catalog.resolveExternalId(publicId = null, systemId = "\uD800.dtd").shouldBeNull()
        }

        test("makes a target absolute against the base that xml:base sets") {
            val catalog = single(
                group(
                    attributes = "xml:base='https://elsewhere.test/dtd/'",
                    entry = system(systemId = "a", uri = A_DTD),
                ),
                system(systemId = "b", uri = B_DTD),
                "<system xml:base='sub/' systemId='c' uri='c.dtd'/>",
            )

            catalog.resolveExternalId(publicId = null, systemId = "a") shouldBe "https://elsewhere.test/dtd/a.dtd"
            catalog.resolveExternalId(publicId = null, systemId = "b") shouldBe target(B_DTD)
            catalog.resolveExternalId(publicId = null, systemId = "c") shouldBe target("sub/c.dtd")
        }

        test("never throws and maps hostile identifiers to nothing or to a target of the catalog") {
            val catalog = single(
                "<systemSuffix systemIdSuffix='.dtd' uri='any.dtd'/>",
                public(publicId = PUBLIC_ID, uri = P_DTD),
            )

            checkAll(CASES, hostileText, hostileText) { publicId, systemId ->
                val target = catalog.resolveExternalId(publicId = publicId, systemId = systemId)

                (target == null || target.startsWith(BASE)) shouldBe true
                (catalog.resolveUri(systemId) == null) shouldBe true
            }
        }

        test("grows linearly with the entries it searches") {
            shouldGrowLinearly(small = entries(SMALL_ENTRIES), large = entries(SMALL_ENTRIES * GROWTH_FACTOR)) { text ->
                loaded(ROOT to text).resolveExternalId(publicId = null, systemId = "missing")
            }
        }
    }

    context("resolveUri") {
        test("follows uri, rewriteURI, uriSuffix and delegateURI entries") {
            val catalog = loaded(
                ROOT to catalog(
                    "<uri name='${SCHEMA}a.xsd' uri='a.xsd'/>",
                    "<rewriteURI uriStartString='${SCHEMA}r/' rewritePrefix='r/'/>",
                    "<uriSuffix uriSuffix='/s.xsd' uri='s.xsd'/>",
                    "<delegateURI uriStartString='${SCHEMA}d/' catalog='other.xml'/>",
                    public(publicId = PUBLIC_ID, uri = "p.xsd"),
                ),
                OTHER to catalog("<uri name='${SCHEMA}d/x.xsd' uri='d-x.xsd'/>"),
            )

            catalog.resolveUri("${SCHEMA}a.xsd") shouldBe target("a.xsd")
            catalog.resolveUri("${SCHEMA}r/b/c.xsd") shouldBe target("r/b/c.xsd")
            catalog.resolveUri("${SCHEMA}deep/s.xsd") shouldBe target("s.xsd")
            catalog.resolveUri("${SCHEMA}d/x.xsd") shouldBe target("d-x.xsd")
            catalog.resolveUri("${SCHEMA}d/y.xsd").shouldBeNull()
            catalog.resolveUri("urn:publicid:-:P:DTD+A:EN") shouldBe target("p.xsd")
            catalog.resolveUri("\uDC00").shouldBeNull()
        }
    }

    context("a problem") {
        test("compares every property and shows its URI and detail safely") {
            val first = single(UNKNOWN).problems.single()
            val second = single(UNKNOWN).problems.single()

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
            first shouldNotBe single("<other/>").problems.single()
            first.toString() shouldBe
                """XmlCatalogProblem(InvalidEntry, uri="$ROOT", detail="has the unknown element unknown")"""
            checkAll(CASES, hostileText) { uri ->
                XmlCatalog.load(listOf(uri), MapLoader()).problems.forEach { problem -> problem.shouldBeShownSafely() }
            }
        }
    }
})
