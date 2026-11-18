# AGENTS.md

Binding rules for every agent and every person working in this repository. kxml is the XML library of Davils: a
hardened, namespace-aware XML 1.0 reader and writer and a kotlinx.serialization format on top of it, for Kotlin on the
JVM and on WebAssembly (`wasmJs`). It is built on [Kore](https://github.com/davils-com/kore), follows the rules of Kore and is published under the
Apache License 2.0, with security first.

## Repository

| Module | Artifact | Purpose |
|---|---|---|
| `kxml-core` | `com.davils:kxml-core` | The reader with its event stream, limits and diagnostics, the immutable node tree, the writer and the escaping of untrusted text shared by both modules |
| `kxml-serialization` | `com.davils:kxml-serialization` | The kotlinx.serialization format: elements, attributes, text, namespaces, wrapped and repeated collections, maps, polymorphism and decoding with diagnostics |
| `build-logic` | - | One convention plugin per Kreate concern, composed by `kxml.kotlin-library` for the modules and `kxml.root` for the root project; they hold nothing but `plugins { }` and `kreate { }` |

The root package of a module is `com.davils.kxml.<module without kxml->`, such as `com.davils.kxml.core`, and its
Java module has the same name. Both modules declare it in `src/jvmMain/java/module-info.java` and export only
packages with public API. The one exception is `com.davils.kxml.core.internal`, which `kxml-core` exports to
`com.davils.kxml.serialization` alone.

kxml uses Kore wherever Kore has the building block: validation and violation codes, `ValidatingBuilder` and
`@DavilsDsl`, `TextRange` and `LineMap`, `Limits.MAX_DISPLAY_LENGTH` and the `Davils*` annotations. Never write a
second version of something Kore has; what kxml needs from Kore and Kore does not offer yet is added to Kore.

## Targets

Every module is a Kotlin Multiplatform library with two targets, `jvm` and `wasmJs`, like Kore 2.0. The Wasm tests run
under Node.js; the published klib runs in the browser as well.

- Code lives in `src/commonMain/kotlin` unless it needs the JVM. What only the JVM can offer lives in
  `src/jvmMain/kotlin`; a member of a common class that needs a JDK type becomes an extension function there.
- A platform difference hides behind an `internal expect fun` in `<Role>.kt` with its `actual` in `<Role>.jvm.kt` and
  `<Role>.wasmJs.kt`, such as `UnicodeCategories` of `SafeDisplay`. kxml declares no `expect` or `actual` class:
  they are a Beta language feature.
- JavaScript interop is written with `js("...")` or `@JsFun` in `src/wasmJsMain/kotlin` only, behind a file-level
  `@file:OptIn(ExperimentalWasmJsInterop::class)`.
- Text is walked by code point with the internal helpers of `CodePoints.kt`, never with `Character`, `String.format`
  or `java.nio`; sources are decoded by `SourceDecoding` and `Utf16` in common code.

## Verify

```bash
./gradlew check
```

Nothing is done until it is green. `check` compiles with warnings as errors (Kotlin with progressive mode and
extra warnings, javac with `-Xlint:all -Werror`), runs Detekt with type resolution, the Kreate rule set and
`allRules`, both test modules, Kover verification at 97 % of lines, branches and instructions, the API and semver
check, the license header check, the changelog check, every Trivy scan and the Kreate assurance gate, which blocks
on every finding. The gate reads OSV.dev and deps.dev and Trivy downloads its database, so `check` needs the
network. The build runs on JDK 25; `gradle/gradle-daemon-jvm.properties` pins the daemon to it and toolchains are
never downloaded.

To build against a local checkout of Kore, such as a branch that is not published yet, substitute it for the published
artifact without changing a file:

```bash
./gradlew --include-build ../kore check
```

## Code rules

Enforced by Detekt with the Kreate rule set, the `libraries` rule set, `allRules` and warnings as errors, without
a baseline. Build scripts and `build-logic` follow the same rules, although Detekt does not analyse them.

- **License header first.** Every `.kt`, `.kts`, `.java`, `.properties` and YAML file starts with the Apache 2.0
  header, byte for byte as in the existing files; only the files Gradle generates in `gradle/` are exempt.
  `kreateLicenseHeaderCheck` and Detekt enforce it.
- **No comments.** No `//`, no `/* */` other than the license header. Names, types and tests carry the intent.
- **KDoc on public API only**, with `@param`, `@return`, `@throws` and `@since`, and never on internal or private
  declarations; protected members of public open or abstract classes are API and get KDoc. A new declaration gets
  the version it first ships in; existing `@since` values never change. KDoc states what a caller must know:
  behaviour on invalid input, the limits that apply, thread safety and every security consideration.
- **No `else`.** Return early; a `when` over a sealed type or an enum is exhaustive without `else ->`.
- **At most two chained calls.** Name intermediate results and computed conditions.
- **Named arguments** wherever a call passes two or more values of which the meaning is not obvious from the call.
- **One top-level type per file**, named after it; a file of functions is named after its role, such as `Xmls.kt`.
- **No package for a single file.** A subpackage holds at least two files. A file that would stand alone in a
  package lives in the parent package instead, and when a subpackage shrinks to one file, that file moves up and the
  subpackage goes away. Exceptions are the root package of a module and an `internal` package.
- **No `!!`, no `lateinit`.** Model absence as a type or return early.
- **Explicit API.** Everything is `internal` unless a consumer needs it. A type only the reader creates, such as an
  event or a result, has an `internal` constructor.
- **Names.** No type, file, function or property carries the name of the library as a prefix: `XmlName`, never
  `KxmlName`. `Xml` names the domain, as `Semantic` does in `SemanticVersion` of Kore. The only other prefix is
  `Davils` on the annotations of Kore. The ids of the Gradle plugins in `build-logic` keep their `kxml.` prefix.
- **`@DavilsInternal`** marks API that `kxml-serialization` shares with `kxml-core` and consumers must not use; it
  lives in `com.davils.kxml.core.internal` and is opted into locally with `@OptIn(DavilsInternal::class)`.
- **Immutable values.** Public types are immutable; public collections are copied with the internal
  `immutableList()`, never stored from `toList()` or `+`, so neither a cast nor the caller's list can change them.
- **No public `data class`.** It freezes `copy` and `componentN` into the binary API. Write a regular class whose
  `equals` and `hashCode` compare a private `values` list and whose `toString` escapes; `value class` stays allowed.
- **Explicit return types** on public functions with an expression body or a result; a block body that returns
  nothing omits `: Unit`, which the compiler rejects as redundant.
- **Kotlin first**, Java only where Kotlin offers no equivalent, and only in `jvmMain`. Experimental Kotlin and kotlinx.serialization API is
  allowed only through a local `@OptIn` and never appears in a public signature.
- **Nothing global.** No mutable singletons, no static configuration, no threads or timers of the library's own.
  Detekt does not enforce this rule; reviews do.

## Security rules

Every document kxml reads may come from an attacker, and so may every name, namespace and value a caller hands to
it. kxml is built so that a mistake of a caller fails closed.

- **No entity expansion, no external resources.** The reader never reads a document type declaration's internal
  subset, never loads anything and never expands an entity but the five predefined ones. `XmlDoctype.Refuse` is the
  default; `XmlDoctype.Ignore` skips the declaration with a warning. Nothing may change that.
- **Limits before work.** The reader checks every input against `XmlLimits` before it does work that grows with the
  input: the length of the source, the depth, the number of elements and attributes, the length of names and texts.
  What is too large ends reading with a diagnostic; nothing is truncated silently. A new limit goes into
  `XmlLimits` and `XmlLimitsBuilder`, never into a private constant. `XmlLimits.Untrusted` is the default.
- **Malformed input never throws.** The reader turns every problem of a document into an `XmlDiagnostic` with a
  stable `XmlDiagnosticCode` and a `TextRange`; an exception out of the reader is a bug.
- **Escape untrusted text exactly once, at the sink.** Whatever shows text that may come from input, such as an
  exception message, a `toString` or the message of `XmlDecodingException`, writes it through the internal
  `SafeDisplay`, which escapes control, format and separator characters, bidirectional overrides and lone
  surrogates and bounds the length. Names that were checked to be XML names are shown as they are. The message of an
  `XmlDiagnostic` is input and is only escaped where it is shown.
- **No raw input in exceptions.** Messages are constant text plus escaped, bounded fields.
- **Values may be sensitive.** A decoded value never appears in a `toString` or a message.
- **The writer only writes well-formed XML.** Nodes check their content when they are created, and the writer
  escapes every text and attribute value.
- **Hide what Java must not reach.** Kotlin's `internal` is `public` in the bytecode, so a Java caller on the module
  path reaches every internal class of an exported package. Implementation lives in packages that are not exported,
  such as `parser`, `compose`, `decode` and `encode`, and `module-info.java` exports an `internal` package to
  `com.davils.kxml.serialization` only.
- **Secure randomness only.** No `java.util.Random`, `kotlin.random.Random` or `ThreadLocalRandom`.
- **Every sink has an injection test.** A renderer is tested with the `hostileText` fixture and
  `shouldBeSafeToShow` or `shouldBeShownSafely`.

## Tests

- Kotest `FunSpec` only, Kotest assertions only, no mocking library; fakes come from a `fixture`.
- One `context` per behaviour or function; test names read as sentences.
- Tests live in `src/commonUnitTest/kotlin`, which runs on the JVM and on Wasm, in the package of the code they test,
  even when that package holds a single test. A test that needs the JVM, through a JDK type or `@JvmInline`, which
  only compiles in the JVM test sources, lives in `src/jvmUnitTest/kotlin`; a test of common code stays common
  wherever it can. A fixture lives in the `fixture` folder of the only subpackage that uses it, or in the shared `fixture` folder
  of the area; a `fixture` folder holds at least two files, so a lone fixture lives next to the tests that use it.
  `src/test` is rejected by the build.
- Property tests with adversarial generators for every parser and every security invariant.
- Every change ships with its tests; a fixed weakness ships with a test that would have caught it.

## Build

- Every module applies exactly one plugin, `id("kxml.kotlin-library")`, and declares nothing but its description
  and its dependencies, through the configurations of the source sets such as `commonMainApi`. Detekt, Kover, Kotest, Trivy, the compiler and publishing are configured only in
  `build-logic`, through Kreate.
- `build-logic` holds no Kotlin classes, no tasks of its own and no Gradle API calls beyond reading the version
  catalog. What Kreate cannot express yet is added to Kreate, never programmed here.
- Every plugin in `build-logic` configures one Kreate concern and applies `kxml.base`:

  | Plugin | Configures |
  |---|---|
  | `kxml.base` | The Kreate plugin, the group and the version |
  | `kxml.dependency-locking` | Strict locking of every configuration |
  | `kxml.git-hooks` | The Git hooks |
  | `kxml.trivy` | Every Trivy scan |
  | `kxml.assurance` | The assurance gate and its `ignore` rules |
  | `kxml.platform` | The JDK, the Kotlin compiler, the targets and the Java module name |
  | `kxml.web-tools` | Node.js and Yarn for the Wasm target, from the repositories of the settings |
  | `kxml.serialization` | The kotlinx.serialization compiler plugin |
  | `kxml.testing` | Kotest and the test suites |
  | `kxml.detekt` | Detekt and its reports |
  | `kxml.coverage` | Kover and its verification |
  | `kxml.api-validation` | The API dump and the semver check |
  | `kxml.docs` | Dokka |
  | `kxml.publish` | The POM, the release guard and the Davils repository |
  | `kxml.archives` | The legal files and the manifest of every archive |
  | `kxml.tool-classpaths` | The version floors on the class paths of the build tools |
  | `kxml.changelog` | The changelog check |
  | `kxml.license-header` | The license header check |

  `kxml.kotlin-library` composes them for a module and adds only the class paths the assurance gate reads;
  `kxml.root` composes them for the root project. A new concern gets a plugin of its own, never a block in a
  composing plugin.
- Dependencies are declared in `gradle/libs.versions.toml`. kxml depends on `kore-core` and, in
  `kxml-serialization`, on `kotlinx-serialization-core`. Libraries of JetBrains and of Davils may be added when they
  are needed; every other dependency needs the approval of a maintainer.
- Only `repo.davils.com` serves `com.davils` and its subgroups, through `exclusiveContent`. Everything else comes
  from Maven Central, except the Gradle plugins `kotlin-dsl`, `kotlinx-benchmark-plugin` and
  `kotest-framework-plugin-gradle`, which only the Gradle Plugin Portal has, and Node.js and Yarn, which come from
  `nodejs.org` and the GitHub releases of Yarn. `gradle/verification-metadata.xml` names the Node.js archive of every
  platform the pipeline runs on. Nothing is ever resolved from `mavenLocal()`.
- After a dependency changes, refresh the locks and the verification metadata and review what changed:

  ```bash
  ./gradlew kreateResolveAndLockAll :build-logic:dependencies --write-locks --no-configuration-cache
  ```

  ```bash
  ./gradlew --write-verification-metadata pgp,sha256 --export-keys --no-configuration-cache check
  ```

- kxml builds only with released versions of Kreate and Kore. `gradle/verification-metadata.xml` trusts
  `com.davils` artifacts only by the signature of the Davils release key `36F578E8AD2D4391E0FA6C18154C625FAD370063`;
  a snapshot is never put into the catalog, and no trust rule for unsigned snapshots is ever added.
- A finding of the assurance gate that is a known, harmless property of a dependency gets an `ignore` rule in
  `build-logic/src/main/kotlin/kxml.assurance.gradle.kts`, with a reason, the exact module and an expiry date no more
  than six months ahead. CRITICAL and HIGH findings can never be ignored.

## Public API

The `api/*.api` dumps are the contract. `kreateApiCheck` fails on every unrecorded change and
`kreateApiSemverCheck` on a version that does not match it. A deliberate change is recorded with
`./gradlew kreateApiDump` and gets a `CHANGELOG.md` entry. A breaking change needs a major version and a commit
marked as breaking. Deprecate before removing: `@Deprecated` with a `ReplaceWith`, for at least one minor release.
The values of `XmlDiagnosticCode` and of the violation codes never change once released.

## Releases

The version lives in `gradle.properties` and nowhere else; `kreateReleaseGuard`, which runs before every
publication, refuses a build whose resolved version differs from it, so no environment variable can change what is
published. Every build without a release tag is a `-SNAPSHOT`. A release is a tag without a `v`, such as `1.0.0`,
that equals that version, sits on a commit of `main` and is built without any snapshot tooling; the guard refuses
every other combination before anything is published. Snapshots go to `https://repo.davils.com/snapshots`, releases
to `https://repo.davils.com/releases`, and never to Maven Central. The pipeline publishes with
`publishAllPublicationsToDavilsRepository` and `--no-configuration-cache`, with the credentials in
`DAVILS_MAVEN_NAME` and `DAVILS_MAVEN_SECRET`, as kotlin-workflow hands them to Gradle, and the signing key in
`ORG_GRADLE_PROJECT_signingInMemoryKey`, `…KeyId` and `…KeyPassword`; a release without a signing key fails. A
release of kxml needs a released Kore.

## Git

Work on `develop`, release from `main`. Conventional Commits in English with the module as scope, such as
`feat(core): ...` or `fix(serialization): ...`. Small commits, one concern each. Commit only when asked. No
`Co-Authored-By` trailer and no other attribution to tools or AI assistants.
