# Third-Party Material

No published artifact of kxml contains third-party code.

The tests use documents shaped like the responses of the Amazon S3 REST API and its namespace name
`http://s3.amazonaws.com/doc/2006-03-01/`; the documents themselves were written for kxml.

The tests of `kxml-document` use example documents of the W3C Recommendations
[Canonical XML 1.0](https://www.w3.org/TR/xml-c14n/) and
[Exclusive XML Canonicalization 1.0](https://www.w3.org/TR/xml-exc-c14n/),
Copyright © World Wide Web Consortium, under the
[W3C Document License](https://www.w3.org/copyright/document-license-2023/), and the examples of
[RFC 5261](https://www.rfc-editor.org/rfc/rfc5261), Copyright © IETF Trust and the persons identified as the
document authors, as the [IETF Trust Legal Provisions](https://trustee.ietf.org/documents/trust-legal-provisions/)
allow. None of them is part of a published artifact.

The W3C XML Conformance Test Suite is not part of the repository: `XmlConformanceTest` downloads it from w3.org when
the tests run, as its licenses allow.

The build and the tests depend on libraries under their own licenses; `gradle/verification-metadata.xml` records
each of them, and the Trivy license scan of `./gradlew check` checks them.
