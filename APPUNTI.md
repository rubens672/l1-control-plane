# l1-control-plane
Caricamento Telemetria e Gestione Sottoscrizioni

### compilazione maven
mvn clean install -DskipTests

### controllo
gcloud auth list

### login gcp
gcloud auth login admin@berticloud.ai

### Re-autentica gcloud CLI
gcloud auth login --update-adc

### settare il project
gcloud config set project mosqhealthagent

### Scarica il binario cloud-sql-proxy
curl -o cloud-sql-proxy https://storage.googleapis.com/cloud-sql-connectors/cloud-sql-proxy/v2.21.1/cloud-sql-proxy.linux.amd64

### Rendilo eseguibile
chmod +x cloud-sql-proxy

### avvio della connessione proxy x Cloud Sql
./cloud-sql-proxy mosqhealthagent:europe-west12:control-plan-gateway-db --private-ip &

## Creazione alberatura maven
#!/usr/bin/env bash
set -euo pipefail

BASE="src/main/java/ai/berticloud/shared"

mkdir -p src/{main,test}/{java,resources}
mkdir -p "$BASE"/{security,identity}

touch \
  src/main/resources/application.yml \
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

<br>

<h3>Mini "mappa mentale ingest" (solo ingest, super chiara)</h3>
<p><strong>POST /v1/telemetry</strong></p>

<ol>
    <li><strong>IngestController</strong>
        <ul>
            <li>verifica mTLS (headers)</li>
            <li>SAN &rarr; <code>DeviceIdentity</code> (shared)</li>
            <li>body mismatch check</li>
            <li>chiama <code>AuthzService</code></li>
        </ul>
    </li>
    <li><strong>AuthzService</strong>
        <ul>
            <li>deny cache &rarr; stop</li>
            <li>allow cache &rarr; ok</li>
            <li>miss &rarr; <code>AuthzRepository.loadAuthContext()</code> (Cloud SQL join)</li>
            <li>check status/subscription/fingerprint</li>
        </ul>
    </li>
    <li><strong>IngestController</strong>
        <ul>
            <li>publish Pub/Sub (attrs)</li>
            <li><code>AuthzRepository.touchLastSeen()</code></li>
        </ul>
    </li>
</ol>
<br>
<h3>Mini mappa mentale admin-service (super rapida)</h3>

<ul>
    <li><strong>AdminController</strong>
        <ul>
            <li>Riceve REST admin</li>
            <li>Chiama <code>AdminRepository</code> per scrivere su DB</li>
            <li>Per bootstrap token:
                <ul>
                    <li>Chiama <code>BootstrapTokenIssuer</code> &rarr; (tokenPlain, tokenHash, exp)</li>
                    <li>Salva hash+exp su DB &rarr; ritorna tokenPlain all'admin</li>
                </ul>
            </li>
        </ul>
    </li>
    <li><strong>BootstrapTokenIssuer</strong>
        <ul>
            <li>Delega a <code>CryptoUtil</code> random + HMAC</li>
        </ul>
    </li>
    <li><strong>AdminRepository</strong>
        <ul>
            <li>Fa insert/upsert su Postgres <code>control_plane.*</code></li>
        </ul>
    </li>
</ul>

<br>

<h3>Mini mappa mentale enrollment-service (chi chiama chi)</h3>
<p><strong>POST /v1/enrollments/{deviceId}:signCsr</strong></p>

<ol>
    <li><strong>EnrollmentController</strong>
        <ul>
            <li>legge Bearer token</li>
            <li><code>EnrollmentRepository.findDeviceForEnrollForUpdate()</code></li>
            <li><code>BootstrapTokenService.verify()</code></li>
            <li><code>CsrAndCertService.parseAndValidateCsr()</code></li>
            <li><code>CaMaterialLoader.load()</code> (Secret Manager)</li>
            <li><code>CsrAndCertService.sign()</code></li>
            <li><code>EnrollmentRepository.activateAndStoreCert()</code></li>
            <li>response PEM</li>
        </ul>
    </li>
</ol>

<br>

<h3>Mini "mappa mentale shared"</h3>

<ul>
    <li><strong>MtlsHeaders</strong>
        <ul>
            <li>"leggi header dal LB" (trusted boundary)</li>
        </ul>
    </li>
    <li><strong>SanUriSelector</strong>
        <ul>
            <li>"prendi la SAN giusta"</li>
        </ul>
    </li>
    <li><strong>SanUriParser</strong>
        <ul>
            <li>"trasforma URN &rarr; <code>DeviceIdentity</code>"</li>
        </ul>
    </li>
    <li><strong>DeviceIdentity</strong>
        <ul>
            <li>"oggetto identità forte (tenant/site/device)"</li>
        </ul>
    </li>
</ul>

<br>
## alberatura  <br>
l1-control-plane/  <br>
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

## Eventuali problemi su IntelliJ
Clicca Advanced Settings (menzionato nel messaggio blu)  
Disabilita "Open natively"  
Riapri il progetto  

Se non trovi l'opzione  
Vai su File → Settings → Advanced Settings e cerca WSL — dovrebbe esserci un'opzione tipo:  

"Open projects in WSL natively instead of using Remote Development"  

Disabilitala, poi riprova ad aprire il progetto da Remote Development   → WSL.