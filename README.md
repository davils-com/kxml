# kxml

kxml is the XML library of Davils: a hardened, namespace-aware XML 1.0 and 1.1 reader and writer and a
kotlinx.serialization format for Kotlin on the JVM and on WebAssembly (`wasmJs`), safe to feed with untrusted documents.
It is built on [Kore](https://github.com/davils-com/kore) and published under the [Apache License 2.0](LICENSE).

| Module | Artifact | What it holds |
|---|---|---|
| `kxml-core` | `com.davils:kxml-core` | The reader with events, limits and diagnostics, the immutable node tree and the writer |
| `kxml-serialization` | `com.davils:kxml-serialization` | A kotlinx.serialization format for XML |
| `kxml-document` | `com.davils:kxml-document` | Tools for whole documents on top of `kxml-core` |

All modules are Kotlin Multiplatform libraries with a `jvm` and a `wasmJs` target, like Kore 2.0. The JVM target
requires JDK 25; the Wasm target runs in the browser and under Node.js. kxml is written for Kotlin 2.4 or newer.
`kxml-core` depends on `kore-core`; `kxml-serialization` adds `kotlinx-serialization-core`, and `kxml-document` depends
on `kxml-core` alone. On the JVM they are named Java modules, `com.davils.kxml.core`, `com.davils.kxml.serialization`
and `com.davils.kxml.document`.

## Installation

kxml is published to the Davils repository, not to Maven Central. Declare it for the `com.davils` group only:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { maven("https://repo.davils.com/releases") }
            filter { includeGroupAndSubgroups("com.davils") }
        }
        mavenCentral()
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.davils:kxml-serialization:1.0.0")
}
```

In a multiplatform project, declare it in `commonMain`; Gradle picks the artifact of each target. A Maven build depends
on `kxml-core-jvm`, `kxml-serialization-jvm` and `kxml-document-jvm`.

## A short tour

### Reading and writing

```kotlin
val xml = xml {
    whitespace = XmlWhitespace.DropIgnorable
    limits { maximumDepth = 64 }
}

when (val result = xml.parse("""<order id="7"><item>book</item></order>""")) {
    is XmlParseResult.Parsed -> {
        val order = result.document.root
        order.attribute(XmlName("id"))?.value                // "7"
        order.firstElement(XmlName("item"))?.text            // "book"
    }
    is XmlParseResult.Failed -> result.diagnostics.forEach(::println)
}

val written = xml.emit(xmlElement(XmlName("order")) { element(XmlName("item")) { text("pen") } })
```

A malformed or oversized document never throws: the result lists diagnostics with a stable code, the range in the
source and a message. Document type declarations are refused unless `doctype = XmlDoctype.Internal`, which reads
the internal subset and expands its entities within the budget of `XmlLimits`. External resources are loaded only
under `XmlDoctype.External`, through an `XmlResolver` the caller hands over, such as an `XmlCatalogResolver` of
OASIS XML Catalogs 1.1:

```kotlin
val xml = xml {
    doctype = XmlDoctype.External
    resolver = XmlCatalogResolver.fromDirectory(Path.of("/etc/xml/dtd"))   // JVM: catalog.xml and resources below it
}
```

### Editing and formatting

```kotlin
val tree = XmlSyntaxTree.parse(text)
val item = tree.document.root?.elements("item")?.first() ?: return

when (val result = XmlEditor(tree).setAttribute(item, name = "id", value = "8 & up")) {
    is XmlEditResult.Edited -> XmlFormatter().format(result.tree)   // only whitespace changes
    is XmlEditResult.Refused -> println(result.reason)
}
```

The syntax tree keeps every character of a document. `XmlSyntaxTree.edit` parses only the element an edit touches
again, `XmlEditor` escapes what it writes and keeps everything else, entity references included, and
`XmlFormatter` refuses rather than change what a document means.

### Serialization

```kotlin
@Serializable
@XmlNamespace("urn:shop")
data class Order(
    @XmlAsAttribute val id: Int,
    @XmlWrapped("item") val items: List<String>,
)

val format = xmlFormat { isDeclarationWritten = false }
val text = format.encodeToString(Order(7, listOf("book")))
val order = format.decodeFromString<Order>(text)

when (val decoding = format.decodeOrFailure<Order>("<Order/>")) {
    is XmlDecoding.Decoded -> decoding.value
    is XmlDecoding.Failed -> decoding.diagnostics
}
```

### XPath

```kotlin
val parsed = xml { }.parse("<shop xmlns='urn:shop'><book id='b1'>Kotlin</book></shop>") as XmlParseResult.Parsed
val books = XPath.parse("/s:shop/s:book[@id = 'b1']", namespaces = mapOf("s" to "urn:shop"))
books.select(parsed.document).map(XPathNode::value)                    // ["Kotlin"]
XPath.parse("count(//*)").evaluate(parsed.document)                    // XPathValue.Number(2.0)
```

`XPath` of `kxml-document` evaluates XPath 1.0 with its core function library. Every evaluation is bounded by the
query limits of `XmlLimits` and stops with an `XmlDocumentLimitException` instead of running on.

## Security

Every document may come from an attacker. The reader checks the document against `XmlLimits` before the work grows
with it, expands entities only within a budget, loads an external resource only through the caller's resolver, and
every `toString` and exception message escapes what it shows. See
[SECURITY.md](SECURITY.md) for the scope, known limitations and how to report a vulnerability privately.

## Building

```bash
./gradlew check
```

It needs JDK 25 and network access: besides compilation with warnings as errors, Detekt, the tests and coverage it
runs Trivy and the Kreate assurance gate. See [CONTRIBUTING.md](CONTRIBUTING.md) and [AGENTS.md](AGENTS.md).

## License

Copyright 2026 Davils. Licensed under the [Apache License, Version 2.0](LICENSE).
