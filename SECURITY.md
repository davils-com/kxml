# Security Policy

## Supported Versions

Security fixes are released for the latest minor version of the current major release.

| Version | Supported |
|---------|-----------|
| 1.0.x   | Yes       |
| < 1.0   | No        |

## Reporting a Vulnerability

**Do not open a public issue for a security problem.**

Report it privately through GitHub's
[private vulnerability reporting](https://github.com/davils-com/kxml/security/advisories/new),
or by email to development@davils.com.

Please include:

- the affected kxml version, module and JDK,
- a description of the impact — what an attacker gains,
- the steps to reproduce it, ideally as a minimal Kotlin snippet or test,
- any mitigation you are already aware of.

We acknowledge a report within three working days and agree on a disclosure date with you.

## Scope

kxml is a library: it runs with the privileges of the application that uses it. It treats every document it reads as
untrusted, and so the names, namespace names and values a caller hands to it.

The following are **in scope**:

- a document that makes kxml use memory or CPU beyond what `XmlLimits` allows, overflow the stack, or throw out of
  the reader,
- any access to a file, a URL or another external resource, any way past `XmlDoctype.Refuse`, and any entity
  expansion beyond `maximumEntityExpansion` or `maximumEntityDepth`,
- a document that the reader accepts although it is not well-formed, or a node tree the writer turns into XML that is
  not well-formed or means something else,
- untrusted text that reaches a message, a `toString` or the message of `XmlDecodingException` unescaped, so it can
  forge log lines or control a terminal,
- a decoded value that appears in a message or a `toString`.

The following are **not** vulnerabilities in kxml:

- a vulnerability in the JDK, the Kotlin standard library, kotlinx.serialization or Kore — report those to the
  respective project,
- an application that reads untrusted documents with `XmlLimits.Relaxed` or limits of its own that are too large,
- an application that logs `XmlDiagnostic.message` without escaping it; the message is input, and
  `XmlDiagnostic.toString` escapes it,
- the meaning an application gives to a document that is well-formed; kxml does not validate against a schema.

## Known Limitations

- kxml reads XML 1.0 and 1.1 with namespaces in UTF-8, UTF-16, UTF-32, ISO-8859-1 and US-ASCII only; it refuses every other
  encoding, such as EBCDIC and Windows-1252, and reads the encoding declaration from the first 1,024 bytes. It does not validate against a DTD or a schema.
- Under `XmlDoctype.Internal`, kxml reads the internal subset only. It never loads the external subset or an external
  entity, so a document that relies on declarations there is refused or, after an unread parameter entity, read
  without the declarations that follow it, as section 5.1 of XML 1.0 requires. A document type declaration changes
  what a document means through defaults, normalization and entities; an application that signs or compares
  documents keeps `XmlDoctype.Refuse`.
- On Wasm, `SafeText` of Kore takes the Unicode categories from the regular expressions of the JavaScript engine, on the
  JVM from the JDK. An engine with an older Unicode version may treat a newly assigned character as unassigned and
  escape it, never the other way round for the categories that matter.
- The limits bound the work of one call. An application that reads many documents at once bounds their number
  itself.
- `@DavilsInternal` is a compile-time opt-in, not an access boundary. Kotlin `internal` is public in JVM bytecode:
  Java code and reflection reach internal declarations through their mangled names. On the module path the
  `module-info` of each module exports only packages with public API and never an `internal` package; on the class
  path every class is reachable. kxml therefore validates in every
  public constructor.
- Escapes are written as `\u{XXXX}`, `\n`, `\r` and `\t`. A literal backslash in free text is shown as it is, so
  input can display text that looks like an escape, but never a real control character.

## Verifying a Release

Releases are published to `https://repo.davils.com/releases` and signed with the release key of Davils; its
fingerprint is published in this section with the first release, so a consumer can pin it as a `trusted-key` in its
own `gradle/verification-metadata.xml`. A release also carries a signed build provenance attestation made by the
GitHub Actions run that built it:

```bash
gh attestation verify kxml-core-1.0.0.jar --repo davils-com/kxml --signer-repo davils-com/kotlin-workflow
```

Snapshots in `https://repo.davils.com/snapshots` are neither signed nor attested and are not meant for production.
