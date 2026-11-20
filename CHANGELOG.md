# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

The first version of kxml: the XML reader, writer and kotlinx.serialization format of an internal Davils project as
an open source library under the Apache License 2.0, for the JVM and WebAssembly, built on Kore 2.1.1 with Kreate
5.3.0.

### Added

- `kxml-core`: a namespace-aware XML 1.0 reader that never expands entities or loads external resources, refuses
  document type declarations by default and checks every document against `XmlLimits`; an event stream, an immutable
  node tree, diagnostics with stable codes and positions, and a writer with configurable layout.
- `kxml-serialization`: a kotlinx.serialization `StringFormat` for XML with attributes, text content, namespaces,
  wrapped and repeated lists, maps, polymorphism by attribute or element, naming strategies and decoding with
  diagnostics instead of exceptions.
- Both modules are Kotlin Multiplatform libraries with a `jvm` and a `wasmJs` target. Sources are decoded from UTF-8
  and UTF-16 in common code, and untrusted text is escaped by `SafeText` of Kore.

### Changed

- Compared with the internal original, types lost their project prefix (`XmlName` instead of `KideXmlName`), public
  data classes became regular immutable classes, events and results have internal constructors, and every
  `toString` and exception message escapes the text it shows.
- The escaping of untrusted text and the immutable lists are those of Kore; `com.davils.kxml.core.internal` is no
  longer exported to `kxml-serialization`.
