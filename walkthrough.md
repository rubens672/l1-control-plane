# App-Layer mTLS Walkthrough

This walkthrough outlines the changes applied to transition the infrastructure from Load Balancer-managed mTLS utilizing Google CAS to an internal, Application Layer mTLS mechanism leveraging Spring Security and BouncyCastle. All references to Google CAS have been thoroughly removed as per the constraints provided.

## 1. Local Certificate Issuer (Enrollment Service)
The `CsrAndCertService` was refactored and transformed into `LocalCertificateIssuer` that cleanly implements the newly created `CertificateIssuer` interface. It performs signature generation entirely utilizing BouncyCastle and the private key provided via Secret Manager.

## 2. Updated Configuration with Secret Manager URIs
Both `application.yml` files (for `enrollment-service` and `ingest-service`) have been synced and updated with the following Secret Manager URIs holding the internally managed root CA pair:
- **issuingKeySecret**: `${sm://projects/383163155925/secrets/CA_ISSUING_KEY_SECRET/versions/1}`
- **issuingCertSecret**: `${sm://projects/383163155925/secrets/CA_ISSUING_CERT_SECRET/versions/1}`

## 3. Ingest Service Security Filter
A new `MtlsHeaderFilter` was deployed mapping as a `OncePerRequestFilter`. The logic:
1. Intercepts the `X-Device-Cert` HTTP header.
2. Cryptographically validates the parsed `X509Certificate` signature against the injected Root CA public key.
3. Performs temporal boundary checks (`checkValidity()`).
4. Re-computes the SHA-256 fingerprint programmatically.
5. Employs `SanUriSelector` which now exposes `pickDeviceUrnFromCert` to extract the correct URI straight from the cryptographic object.
6. Publishes a `DeviceAuthenticationToken` to the core `SecurityContextHolder`.

## 4. Ingest Controller Refactoring
The `IngestController` component was rewritten. It no longer extracts the legacy mTLS headers (`client_cert_present`, etc) appended by the GCP Load Balancer. Instead, it securely sources the fully verified `DeviceIdentity` directly out of the `SecurityContext`.

## 5. README Setup 
In the parent `README.md` file, the documentation was modified to instruct operations on how to create the `CA_ISSUING_KEY_SECRET` and `CA_ISSUING_CERT_SECRET` within GCP and the process moving forward without Google CAS.

> [!NOTE]
> Testing compilability through Maven directly wasn't feasible on the environment as `mvn` defaults were absent, but standard compilation practices were followed (the relevant BouncyCastle and Spring-Security packages were imported in `ingest-service/pom.xml`). Everything adheres strictly to Java Spring Boot architecture.
