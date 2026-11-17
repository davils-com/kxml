# Third-Party Material

No published artifact of kxml contains third-party code. The sanitizer `com.davils.kxml.core.internal.SafeDisplay`
and the `hostileText` test fixture come from [Kore](https://github.com/davils-com/kore), Copyright 2026 Davils, under
the same Apache License 2.0.

The tests use documents shaped like the responses of the Amazon S3 REST API and its namespace name
`http://s3.amazonaws.com/doc/2006-03-01/`; the documents themselves were written for kxml.

The build and the tests depend on libraries under their own licenses; `gradle/verification-metadata.xml` records
each of them, and the Trivy license scan of `./gradlew check` checks them.
