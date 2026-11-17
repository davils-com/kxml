# kxml

kxml is the XML library of Davils: a hardened, namespace-aware XML 1.0 reader and writer and a kotlinx.serialization
format for Kotlin on the JVM, safe to feed with untrusted documents. It is built on
[Kore](https://github.com/davils-com/kore) and published under the [Apache License 2.0](LICENSE).

| Module | Artifact | What it holds |
|---|---|---|
| `kxml-core` | `com.davils:kxml-core` | The reader with events, limits and diagnostics, the immutable node tree and the writer |
| `kxml-serialization` | `com.davils:kxml-serialization` | A kotlinx.serialization format for XML |

kxml requires JDK 25 and is written for Kotlin 2.4 or newer. `kxml-core` depends on `kore-core`;
`kxml-serialization` adds `kotlinx-serialization-core`. Both are named Java modules, `com.davils.kxml.core` and
`com.davils.kxml.serialization`.

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
source and a message. Document type declarations are refused unless `doctype = XmlDoctype.Ignore`; entities other
than the five predefined ones and external resources are never resolved.

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

## Security

Every document may come from an attacker. The reader checks the document against `XmlLimits` before the work grows
with it, never expands entities, and every `toString` and exception message escapes what it shows. See
[SECURITY.md](SECURITY.md) for the scope, known limitations and how to report a vulnerability privately.

## Building

```bash
./gradlew check
```

It needs JDK 25 and network access: besides compilation with warnings as errors, Detekt, the tests and coverage it
runs Trivy and the Kreate assurance gate. See [CONTRIBUTING.md](CONTRIBUTING.md) and [AGENTS.md](AGENTS.md).

## License

Copyright 2026 Davils. Licensed under the [Apache License, Version 2.0](LICENSE).
