# l1-control-plane
Caricamento Telemetria e Gestione Sottoscrizioni

## Creazione alberatura maven
#!/usr/bin/env bash
set -euo pipefail

BASE="src/main/java/ai/berticloud/shared"

mkdir -p src/{main,test}/{java,resources}
mkdir -p "$BASE"/{security,identity}

touch src/main/resources/application.yml

touch \
  pom.xml \
  "$BASE/security/MtlsHeaders.java" \
  "$BASE/identity/DeviceIdentity.java" \
  "$BASE/identity/SanUriParser.java" \
  "$BASE/identity/SanUriSelector.java"

echo "Project structure created."

## elimina gli a capo \r
sed -i 's/\r$//' init_maven.sh

## Nel tuo repo Git aggiungi .gitattributes
*.sh text eol=lf  

## Variabili env per Cloud Run (minime)

Tutti i servizi:  
DB_NAME  
DB_USER  
DB_PASS  
CLOUDSQL_INSTANCE (project:region:instance)  

ingest-service:  
PUBSUB_TOPIC (nome topic)  

admin-service + enrollment-service:  
BOOTSTRAP_HMAC_KEY_BASE64 (base64 di 32 bytes random)  

enrollment-service:  
CA_ISSUING_KEY_SECRET (Secret Manager resource)  
CA_ISSUING_CERT_SECRET  
CA_CHAIN_SECRET (opzionale)  

## alberatura
l1-control-plane/  
&nbsp;pom.xml  
&nbsp;shared/  
&nbsp;&nbsp;pom.xml  
&nbsp;&nbsp;src/main/java/ai/berticloud/shared/security/MtlsHeaders.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/shared/identity/DeviceIdentity.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/shared/identity/SanUriParser.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/shared/identity/SanUriSelector.java  
&nbsp;ingest-service/  
&nbsp;&nbsp;pom.xml  
&nbsp;&nbsp;Dockerfile  
&nbsp;&nbsp;src/main/resources/application.yml  
&nbsp;&nbsp;src/main/java/ai/berticloud/ingest/IngestApplication.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/ingest/config/CacheConfig.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/ingest/auth/DeviceAuthContext.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/ingest/auth/AuthzService.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/ingest/db/AuthzRepository.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/ingest/api/IngestController.java  
&nbsp;enrollment-service/  
&nbsp;&nbsp;pom.xml  
&nbsp;&nbsp;Dockerfile  
&nbsp;&nbsp;src/main/resources/application.yml  
&nbsp;&nbsp;src/main/java/ai/berticloud/enroll/EnrollmentApplication.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/enroll/api/EnrollmentController.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/enroll/api/dto/SignCsrRequest.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/enroll/api/dto/SignCsrResponse.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/enroll/db/EnrollmentRepository.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/enroll/security/BootstrapTokenService.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/enroll/ca/CaMaterial.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/enroll/ca/CaMaterialLoader.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/enroll/ca/CsrAndCertService.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/enroll/util/CryptoUtil.java  
&nbsp;admin-service/  
&nbsp;&nbsp;pom.xml  
&nbsp;&nbsp;Dockerfile  
&nbsp;&nbsp;src/main/resources/application.yml  
&nbsp;&nbsp;src/main/java/ai/berticloud/admin/AdminApplication.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/admin/api/AdminController.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/admin/api/dto/*.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/admin/db/AdminRepository.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/admin/security/BootstrapTokenIssuer.java  
&nbsp;&nbsp;src/main/java/ai/berticloud/admin/util/CryptoUtil.java  