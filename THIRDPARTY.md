# Third-Party Material

No published artifact of kxml contains third-party code.

The tests use documents shaped like the responses of the Amazon S3 REST API and its namespace name
`http://s3.amazonaws.com/doc/2006-03-01/`; the documents themselves were written for kxml.

The build and the tests depend on libraries under their own licenses; `gradle/verification-metadata.xml` records
each of them, and the Trivy license scan of `./gradlew check` checks them.
