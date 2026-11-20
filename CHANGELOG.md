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
- Both modules are Kotlin Multiplatform libraries with a `jvm` and a `wasmJs` target. Sources are decoded in common
  code, and untrusted text is escaped by `SafeText` of Kore.
- Sources of bytes are detected as Appendix F of XML 1.0 describes it and decoded strictly from UTF-8, UTF-16 and
  UTF-32 in either byte order, and from ISO-8859-1 and US-ASCII when the declaration names them. `XmlSource.of`
  takes the charset the bytes were labelled with, which, as RFC 7303 orders it, gives way to a byte order mark and
  overrides the declared encoding with an `EncodingOverridden` warning. A declared encoding that contradicts the
  bytes is refused with `EncodingMismatch`, and EBCDIC, UCS-4 in an unusual byte order and every other encoding with
  `UnsupportedEncoding`.
- `XmlLimits` gives every limit a hard ceiling, `HARD_INPUT_LENGTH`, `HARD_DEPTH`, `HARD_ELEMENTS`, `HARD_ATTRIBUTES`
  and `HARD_NAME_LENGTH`, which no preset and no configuration exceeds; the limits validate themselves when they are
  created.
- The standards kxml implements are declared and reviewed in `standards.lock`, and the assurance gate reports a new
  version, a replacement or an undeclared standard.

### Changed

- Compared with the internal original, types lost their project prefix (`XmlName` instead of `KideXmlName`), public
  data classes became regular immutable classes, events and results have internal constructors, and every
  `toString` and exception message escapes the text it shows.
- `XmlLimits.Trusted`, which left every limit but the depth unbounded, is replaced by `XmlLimits.Relaxed`, whose
  limits are raised but finite.
- The escaping of untrusted text and the immutable lists are those of Kore; `com.davils.kxml.core.internal` is no
  longer exported to `kxml-serialization`.

### Security

- Attribute names, namespace prefixes and the attributes compared by `XmlNode.Element.equals` are indexed in a total
  order instead of by hash codes, so names with colliding hash codes cannot make reading quadratic, and the
  duplicate check of attribute names no longer compares every pair.
- A document type declaration after the root element is refused also under `XmlDoctype.Ignore`, `<!DOCTYPE` needs
  whitespace before the name, and a processing instruction inside a skipped internal subset is skipped as a whole.
- An entity reference must name an XML name, and a character reference accepts only ASCII digits, so
  `&#\u0666\u0665;` no longer reads as `A`.
- UTF-16 without a byte order mark is only read when the document starts with `<?`, as Appendix F detects it, and
  encoding names are matched without regard to case.
