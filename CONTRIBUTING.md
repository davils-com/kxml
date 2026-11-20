# Contributing to kxml

Thank you for helping to build kxml. This document explains how a change gets in. The binding rules for code,
tests and the build are in [AGENTS.md](AGENTS.md); read it before your first change.

## Code of Conduct

Everyone who takes part follows the [Code of Conduct](CODE_OF_CONDUCT.md).

## How to Contribute

1. Open an issue for anything larger than a small fix, so the approach can be agreed on first. Security problems
   are never reported as an issue; see [SECURITY.md](SECURITY.md).
2. Branch from `develop` and keep the change to one concern.
3. Run `./gradlew check` until it is green. It needs JDK 25 and network access.
4. Open a pull request against `develop` and describe what changed and why.

## Development Standards

### One command before you push

```bash
./gradlew check
```

It compiles with warnings as errors, runs Detekt, the tests, coverage, the API check, the license header check,
the changelog check, Trivy and the Kreate assurance gate. A pull request is only merged when it is green.

### Security first

Every document kxml reads may come from an attacker. A change to the reader checks the input against `XmlLimits`
before the work grows with it, never resolves an entity or an external resource, escapes every untrusted value it
shows through `SafeText` of Kore, never puts a decoded value into a message and fails closed. It ships with tests that feed
hostile input through every new sink.

### The public API is a gate

`kxml-core/api/kxml-core.api` and `kxml-serialization/api/kxml-serialization.api` record the binary API. A
deliberate change is recorded with `./gradlew kreateApiDump`, gets an entry in [CHANGELOG.md](CHANGELOG.md) and,
when it breaks compatibility, waits for the next major version.

### Documentation

Every public declaration has KDoc with `@since`. Internal and private declarations have none, and there are no
other comments.

## Changing a Dependency

Every dependency, the classpath of `build-logic` included, is locked and verified. After changing
`gradle/libs.versions.toml`, refresh both and review what changed:

```bash
./gradlew kreateResolveAndLockAll :build-logic:dependencies --write-locks --no-configuration-cache
```

```bash
./gradlew --write-verification-metadata pgp,sha256 --export-keys --no-configuration-cache check
```

A new dependency other than a library of JetBrains or Davils needs the approval of a maintainer.

## Snapshots and Releases

`gradle.properties` holds the version in development. Every push to `develop` publishes it as a `-SNAPSHOT` to
`https://repo.davils.com/snapshots`. A release is the tag of that version without a `v`, such as `1.0.0`, pushed
on a commit of `main`; the pipeline signs it, publishes it to `https://repo.davils.com/releases` and attests it.
A release needs a released version of Kore in the catalog. `kreateReleaseGuard` refuses a tag that does not equal
the version, a tag on a commit outside `main` and a build with snapshot tooling, such as a Kreate snapshot. Protect the tags `[0-9]*` with a GitHub tag ruleset so that only
maintainers can push them. Before the tag, the version gets its section in [CHANGELOG.md](CHANGELOG.md).

## Commit Messages

Conventional Commits in English with the module as scope, such as
`feat(core): add a limit on processing instructions` or `fix(serialization): match aliases of attributes`. A breaking change is marked with `!` and a `BREAKING CHANGE:`
footer.
