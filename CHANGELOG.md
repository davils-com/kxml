# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

The first version of kxml: the XML reader, writer and kotlinx.serialization format of an internal Davils project as
an open source library under the Apache License 2.0, for the JVM and WebAssembly, built on Kore 2.3.1 with Kreate
5.3.0.

### Added

- `kxml-core`: a namespace-aware XML 1.0 and 1.1 reader that loads external resources only through a resolver of the
  caller, refuses document type declarations by default and checks every document against `XmlLimits`; an event stream, an immutable node tree,
  diagnostics with stable codes and positions, and a writer with configurable layout.
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
- A document that declares version 1.1 is read by the rules of XML 1.1 and Namespaces in XML 1.1: NEL, U+2028 and
  CR NEL end lines, the C1 controls are refused unless they stand as character references, references to C0
  controls are allowed and a prefix may be undeclared with `xmlns:p=""`, which `XmlNamespaceBinding` now allows and
  the writer writes only into a document of XML 1.1. Other minor versions are read by the rules of XML 1.0.
- `XmlReader`, from `Xml.reader`, reads a document event by event without building its tree and stops at the first
  error; `Xml.parse` composes the tree from it without holding every event.
- `XmlSyntaxTree` is the lossless syntax tree of a text: every character belongs to exactly one token, so the text of
  its root is the text again, and the rest of a text that stops being well formed becomes one `ERROR` token. It is
  read by the same rules and limits as `Xml.parse`, and every walk over it is iterative.
- `XmlDoctype.Internal` reads the internal subset of a document type declaration: general and parameter entities,
  attribute lists with their defaults and the normalization of their types, element types, notations and unparsed
  entities. Entities are expanded in content and in attribute values, defaults are added before namespaces are
  resolved, and parameter entities between declarations are expanded as section 2.8 of XML 1.0 describes it. After an
  undeclared or external parameter entity, later declarations are not processed, as section 5.1 requires. The
  declaration comes as `XmlEvent.DocumentType` and as `XmlDocument.doctype`, an `XmlDoctypeDeclaration` with its
  identifiers, `XmlNotation`s and `XmlUnparsedEntity`s; the syntax tree holds every declaration as a node of its own.
- `XmlLimits` bounds the internal subset with `maximumEntityDeclarations`, `maximumAttributeDeclarations`,
  `maximumEntityExpansion`, the characters all expansions of a document may produce, where every reference costs at
  least one, and `maximumEntityDepth`, each with a hard ceiling.
- The diagnostic codes `kxml.doctype.invalid-declaration`, `kxml.reference.recursive-entity`,
  `kxml.reference.misused-entity`, `kxml.reference.unresolved-entity`, `kxml.reference.unbalanced-entity`,
  `kxml.limit.entity-declarations`, `kxml.limit.attribute-declarations`, `kxml.limit.entity-expansion` and
  `kxml.limit.entity-depth`.
- `XmlDoctype.External` also reads the external subset, external parameter entities and external parsed entities,
  each through the `XmlResolver` of the configuration, which gets an `XmlResourceRequest` with the kind, the name,
  the identifiers and the base URI of the declaring resource and returns an `XmlSource` or `null` to refuse it.
  Every resource is decoded on its own and may start with a text declaration; in external resources, parameter
  entities may stand inside declarations and entity values, and conditional sections are read. `XmlSource` takes
  the `uri` it was retrieved from as the base of what it declares. `External` needs a resolver and every other
  `XmlDoctype` refuses one, reported as `kxml.config.resolver-mismatch`.
- `XmlLimits` bounds external resources with `maximumExternalResources` and `maximumExternalLength`, checked before
  a resource is decoded, reported as `kxml.limit.external-resources` and `kxml.limit.external-length`.
- `XmlCatalog` reads OASIS XML Catalogs 1.1 through an `XmlResourceLoader` and resolves external identifiers and URIs
  as sections 7.1 and 7.2 describe: `system`, `rewriteSystem`, `systemSuffix` and `delegateSystem` before `public` and
  `delegatePublic`, `uri`, `rewriteURI`, `uriSuffix` and `delegateURI`, `nextCatalog`, `group`, `prefer`, `xml:base`,
  `urn:publicid:` URNs and the normalization of identifiers of section 6. A catalog file that cannot be loaded or
  read, and an entry that is invalid, are skipped as section 8 asks and listed in `XmlCatalog.problems`.
  `XmlCatalogResolver` resolves the requests of a document through a catalog, first as written and then in absolute
  form, loads what it maps to and hands what it does not map to an optional fallback; a lookup that reaches a catalog
  file beyond the limits is refused. On the JVM, `XmlDirectoryLoader` loads `file:` URIs below one directory only,
  and `XmlCatalog.fromDirectory` and `XmlCatalogResolver.fromDirectory` keep a catalog and its resources inside it.
- `XmlResourceRequest.absoluteSystemId` gives the system identifier escaped as section 4.2.2 of XML 1.0 describes it
  and resolved against the base URI by RFC 3986, with `UriReference` of Kore.
- `XmlLimits` bounds catalogs with `maximumCatalogFiles`, the catalog files one catalog loads, and
  `maximumCatalogDepth`, the longest chain of catalog files and the most delegations of one lookup, each with a hard
  ceiling. Catalog files that refer to each other in a circle are loaded once and searched once per lookup.
- The W3C XML Conformance Test Suite runs in the build: `XmlConformanceTest` downloads the pinned archive, reads it
  with a resolver confined to the suite, and lists every case kxml does not pass with its reason. Of 2,227 cases,
  2,225 pass. Of the other two, one expects an undeclared entity to be skipped, which kxml refuses, and one of XML
  1.1 contradicts another case of the suite.
- The standards kxml implements are declared and reviewed in `standards.lock`, and the assurance gate reports a new
  version, a replacement or an undeclared standard.

### Changed

- Compared with the internal original, types lost their project prefix (`XmlName` instead of `KideXmlName`), public
  data classes became regular immutable classes, events and results have internal constructors, and every
  `toString` and exception message escapes the text it shows.
- `XmlDoctype.Ignore`, which skipped the declaration and with it the declarations a document relies on, is replaced
  by `XmlDoctype.Internal`; `XmlDoctype.Refuse` stays the default, and `kxml.doctype.ignored` is gone.
- `XmlLimits.Trusted`, which left every limit but the depth unbounded, is replaced by `XmlLimits.Relaxed`, whose
  limits are raised but finite.
- The escaping of untrusted text and the immutable lists are those of Kore; `com.davils.kxml.core.internal` is no
  longer exported to `kxml-serialization`.

### Security

- Attribute names, namespace prefixes and the attributes compared by `XmlNode.Element.equals` are indexed in a total
  order instead of by hash codes, so names with colliding hash codes cannot make reading quadratic, and the
  duplicate check of attribute names no longer compares every pair.
- A document type declaration after the root element is refused, and `<!DOCTYPE` needs whitespace before the name.
- Every expansion of an entity charges a budget while it is done, so a billion laughs document, a quadratic blowup
  and entities nested past `maximumEntityDepth` end with a diagnostic, and the work of expanding stays linear in what
  the budget allows. Expansion runs on an explicit stack, an entity that refers to itself is refused, an entity
  must close every element it opens, and `<` never reaches an attribute value through an entity.
- An external entity, an external subset and an external parameter entity are loaded only through the resolver
  under `XmlDoctype.External`; one that is not, or that the resolver refuses, is refused with
  `kxml.reference.unresolved-entity` instead of being dropped. Every external entity counts against the expansion
  budget each time it is referenced, a resource is fetched at most once per document, an entity of XML 1.1 is
  refused in a document of XML 1.0, and a standalone document cannot use an entity the external subset declares.
- An entity reference must name an XML name, and a character reference accepts only ASCII digits, so
  `&#\u0666\u0665;` no longer reads as `A`.
- UTF-16 without a byte order mark is only read when the document starts with `<?`, as Appendix F detects it, and
  encoding names are matched without regard to case.
