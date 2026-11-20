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
import com.davils.kxml.core.source.XmlSource
import com.davils.kxml.core.xml
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.IOException
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path

private const val CATALOG_NAMESPACE: String = "urn:oasis:names:tc:entity:xmlns:xml:catalog"
private const val PUBLIC_ID: String = "-//P//DTD A//EN"
private const val SUBSET: String = "dtd/a.dtd"
private const val OUTSIDE: String = "outside.xml"
private const val INSIDE: String = "inside.xml"
private const val SMALL: Int = 4

private fun Path.write(name: String, text: String): Path {
    val file = resolve(name)
    Files.createDirectories(file.parent)
    Files.writeString(file, text)
    return file
}

private fun linked(link: Path, target: Path): Boolean = try {
    Files.createSymbolicLink(link, target)
    true
} catch (_: FileSystemException) {
    false
} catch (_: UnsupportedOperationException) {
    false
}

private fun publicCatalog(uri: String): String =
    "<catalog xmlns='$CATALOG_NAMESPACE'><public publicId='$PUBLIC_ID' uri='$uri'/></catalog>"

private fun parsed(resolver: XmlResolver, root: Path): XmlParseResult {
    val reader = xml {
        doctype = XmlDoctype.External
        this.resolver = resolver
    }
    val document = "<!DOCTYPE a PUBLIC '$PUBLIC_ID' 'http://remote.test/a.dtd'><a>&e;</a>"
    val uri = root.resolve("doc.xml").toUri()
    return reader.parse(XmlSource.of(document, uri = uri.toString()))
}

class XmlDirectoryLoaderTest : FunSpec({
    context("a directory loader") {
        test("loads a file below its root under the URI it was asked for") {
            val root = tempdir().toPath()
            root.write(name = SUBSET, text = "<!ENTITY e 'x'>")
            val loader = XmlDirectoryLoader(root)
            val uri = loader.uriOf(SUBSET).shouldNotBeNull()

            val source = loader.load(uri).shouldNotBeNull()

            source.uri shouldBe uri
            source.name shouldBe "a.dtd"
            loader.rootUri shouldEndWith "/"
            loader.toString() shouldBe "XmlDirectoryLoader(${loader.rootUri})"
        }

        test("refuses a path that leaves the root, directly or percent-encoded") {
            val parent = tempdir().toPath()
            val root = Files.createDirectory(parent.resolve("root"))
            parent.write(name = OUTSIDE, text = "secret")
            root.write(name = INSIDE, text = "fine")
            val loader = XmlDirectoryLoader(root)

            loader.load(loader.rootUri + "../outside.xml").shouldBeNull()
            loader.load(loader.rootUri + "%2E%2E/outside.xml").shouldBeNull()
            loader.load(loader.rootUri + "sub%2F..%2F..%2Foutside.xml").shouldBeNull()
            val outside = parent.resolve(OUTSIDE).toUri()
            loader.load(outside.toString()).shouldBeNull()
            loader.load(loader.rootUri + INSIDE).shouldNotBeNull()
        }

        test("refuses a symbolic link that leaves the root") {
            val parent = tempdir().toPath()
            val root = Files.createDirectory(parent.resolve("root"))
            val outside = parent.write(name = OUTSIDE, text = "secret")
            val isLinked = linked(link = root.resolve("link.xml"), target = outside)
            val loader = XmlDirectoryLoader(root)

            if (isLinked) loader.load(loader.rootUri + "link.xml").shouldBeNull()
        }

        test("refuses other schemes, hosts, queries, fragments, directories, missing and too long files") {
            val root = tempdir().toPath()
            root.write(name = INSIDE, text = "fine")
            root.write(name = "long.xml", text = "x".repeat(SMALL + 1))
            Files.createDirectory(root.resolve("dir"))
            val loader = XmlDirectoryLoader(root, maximumLength = SMALL)
            val inside = loader.rootUri + INSIDE

            loader.load(inside).shouldNotBeNull()
            loader.load("https://example.test/$INSIDE").shouldBeNull()
            loader.load(inside.replace(oldValue = "file:///", newValue = "file://host/")).shouldBeNull()
            loader.load("$inside?q").shouldBeNull()
            loader.load("$inside#f").shouldBeNull()
            loader.load(loader.rootUri + "dir").shouldBeNull()
            loader.load(loader.rootUri + "missing.xml").shouldBeNull()
            loader.load(loader.rootUri + "long.xml").shouldBeNull()
            loader.load("%zz").shouldBeNull()
            loader.load("file:").shouldBeNull()
            loader.uriOf("%zz").shouldBeNull()
        }

        test("needs an existing directory and a positive length") {
            val root = tempdir().toPath()
            val file = root.write(name = INSIDE, text = "fine")

            shouldThrow<IOException> { XmlDirectoryLoader(root.resolve("missing")) }
            shouldThrow<IllegalArgumentException> { XmlDirectoryLoader(file) }
            shouldThrow<IllegalArgumentException> { XmlDirectoryLoader(root, maximumLength = 0) }
        }
    }

    context("fromDirectory") {
        test("resolves through the catalog of a directory and loads only from it") {
            val root = tempdir().toPath()
            root.write(name = "catalog.xml", text = publicCatalog("dtd/a.dtd"))
            root.write(name = SUBSET, text = "<!ENTITY e 'from the catalog'>")

            val result = parsed(XmlCatalogResolver.fromDirectory(root), root)

            val parsedResult = result.shouldBeInstanceOf<XmlParseResult.Parsed>()
            parsedResult.document.root.text shouldBe "from the catalog"
            XmlCatalog.fromDirectory(root).toString() shouldBe "XmlCatalog(files=1, problems=0)"
        }

        test("refuses what the catalog maps to outside the directory") {
            val parent = tempdir().toPath()
            val root = Files.createDirectory(parent.resolve("root"))
            parent.write(name = "a.dtd", text = "<!ENTITY e 'outside'>")
            root.write(name = "catalog.xml", text = publicCatalog("../a.dtd"))

            val failed = parsed(XmlCatalogResolver.fromDirectory(root), root)
                .shouldBeInstanceOf<XmlParseResult.Failed>()

            failed.diagnostics.last().code shouldBe XmlDiagnosticCode.UnresolvedEntity
            val escaped = XmlCatalog.fromDirectory(root, catalogFile = "../outside.xml")
            escaped.problems.single().kind shouldBe XmlCatalogProblemKind.Unloaded
        }
    }
})
