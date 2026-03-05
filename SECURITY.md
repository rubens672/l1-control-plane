# SECURITY.md

BertiCloud AI -- L1 Control Plane Security Guide

Questo documento descrive il **modello di sicurezza**, le
**assunzioni**, i **meccanismi**, e le **best practice operative** per
l'MVP **L1** del Control Plane.

Obiettivo L1:

> Autenticazione forte dei device, autorizzazione pragmatica (cache+DB)
> e governance contrattuale, **senza chiavi GCP sui device**.

------------------------------------------------------------------------

# 1. Ambito e non-obiettivi (L1)

## In scope

-   Autenticazione device tramite **mTLS** (X.509 client cert).
-   Identità device derivata SOLO da **SAN URI** nel certificato.
-   Enrollment CSR-based con **bootstrap token** temporaneo.
-   Binding del certificato al device con **fingerprint SHA-256**
    salvata in DB.
-   Autorizzazione ingest basata su:
    -   stato tenant/site/device
    -   stato/validità subscription
    -   fingerprint match

## Out of scope (rimandato a L2)

-   Revocation avanzata (CRL/OCSP) e propagazione near-real-time.
-   Device twin/config push.
-   Rate limiting completo e anomaly detection.
-   MFA/SSO per admin console.
-   Security posture management completo (CSPM), SIEM integrato.

------------------------------------------------------------------------

# 2. Principi di sicurezza

## 2.1 Trust boundary

-   **Trusted**: External HTTPS Load Balancer (GCLB) che termina mTLS e
    valida i cert client.
-   **Untrusted**: payload JSON proveniente dal device (deviceId/siteId
    nel body non sono affidabili).
-   **Trusted** (source of truth): Cloud SQL Postgres (control-plane
    DB).

## 2.2 Least Privilege (IAM)

Ogni servizio Cloud Run deve avere un **Service Account dedicato** con
permessi minimi: - admin-service: Cloud SQL access (via connector) +
(opzionale) Secret Manager per chiave HMAC - enrollment-service: Cloud
SQL access + Secret Manager access ai segreti CA - ingest-service:
Pub/Sub Publisher + Cloud SQL access (solo query authz + update
last_seen)

## 2.3 No device cloud credentials

Vincolo assoluto: - **nessuna chiave GCP sul Raspberry** - il device non
usa `gcloud auth` e non possiede service account key - autenticazione
device avviene solo via **certificato mTLS** firmato dalla CA della
piattaforma

------------------------------------------------------------------------

# 3. Identità Device (SAN URI)

## 3.1 Formato

L'identità device è rappresentata da una URN nel SAN URI del
certificato:

`urn:berticloudai:tenant:<TENANT_ID>:site:<SITE_ID>:device:<DEVICE_ID>`

Esempio:

`urn:berticloudai:tenant:tnt-001:site:site-roma-001:device:rpi-123`

## 3.2 Regole

-   tenantId/siteId/deviceId nel **body JSON** sono "payload-only", non
    trusted.
-   se il body contiene `deviceId` o `siteId` e **non matcha** la SAN →
    **reject** (HTTP 400).
-   ingest tagga Pub/Sub usando **solo** i valori derivati dalla SAN.

------------------------------------------------------------------------

# 4. Autenticazione: mTLS

## 4.1 Meccanismo

-   mTLS termina su **External HTTPS Load Balancer**.
-   Il LB valida il certificato client contro una **CA trust**.
-   Il LB inoltra a Cloud Run e inietta gli header mTLS (informazioni
    del cert).

Header attesi (naming L1): - `client_cert_present` -
`client_cert_chain_verified` - `client_cert_sha256_fingerprint` -
`client_cert_uri_sans` - `client_cert_subject_dn` -
`client_cert_issuer_dn`

## 4.2 Verifiche lato ingest-service

-   se `present=false` o `verified=false` → **401 mtls_required**
-   estrazione SAN URI → `DeviceIdentity`
-   autorizzazione (cache+DB)
-   fingerprint match

## 4.3 Hard requirement

-   Cloud Run non deve essere esposto "direttamente" senza LB mTLS (in
    prod).
-   In produzione, l'endpoint ingest deve essere raggiungibile **solo**
    tramite LB (e policy IAM/ingress).

------------------------------------------------------------------------

# 5. Enrollment: Bootstrap Token + CSR

## 5.1 Perché serve

Prima del certificato mTLS, un device non può autenticarsi. Serve un
bootstrap semplice ma sicuro per ottenere il primo certificato.

## 5.2 Bootstrap token (one-time)

-   emesso dall'admin-service
-   valido per un tempo breve (es. 60 minuti)
-   consumato una sola volta (quando enrollment va a buon fine)
-   salvato nel DB **solo come hash HMAC**:
    -   `bootstrap_token_hash`
    -   `bootstrap_expires_at`

### Protezioni

-   DB dump non rivela token plaintext.
-   TTL riduce finestra di attacco.
-   Enrollment usa `SELECT ... FOR UPDATE` per prevenire race e doppio
    consumo.

## 5.3 CSR validation

Il CSR deve contenere la SAN URI nel campo `extensionRequest`. Regole: -
deviceId nel path == deviceId in SAN - tenantId/siteId in SAN ==
tenantId/siteId registrati sul device in DB - se mismatch → reject (HTTP
400)

## 5.4 CA key custody

-   issuing CA private key è custodita in **Secret Manager**
-   il container non deve contenere file di key
-   accesso al segreto solo dal service account di enrollment-service

------------------------------------------------------------------------

# 6. Certificate Binding (Fingerprint)

## 6.1 Perché

Il SAN da solo non basta: un attaccante potrebbe tentare di usare un
cert diverso con SAN "finto". Binding tramite fingerprint impedisce
certificati non attesi.

## 6.2 Regola

-   enrollment salva `expected_fingerprint_sha256` su `devices`
-   ingest confronta fingerprint proveniente dal LB con
    `expected_fingerprint_sha256`
-   mismatch → reject (HTTP 403 fingerprint_mismatch) e negative caching

------------------------------------------------------------------------

# 7. Autorizzazione (AuthZ) e Cache

## 7.1 AuthZ Source of Truth

Cloud SQL Postgres è l'unico source of truth per: - tenant status - site
status - device status - subscription status - subscription valid_to -
expected fingerprint - limiti messaggi/min

## 7.2 Caching policy (L1)

In ingest-service usiamo Caffeine: - allow cache TTL: 5--15 min (revoca
"lenta" accettata in L1) - deny cache TTL: 60--120 sec (protezione
contro flood/misconfig)

Tradeoff: - più performance e costi DB più bassi - revoca non istantanea
(accettabile in MVP)

------------------------------------------------------------------------

# 8. API Security

## 8.1 admin-service (control-plane APIs)

Queste API devono essere considerate "privilegiate". In produzione: -
proteggi con IAM (IAP / Identity Platform / OAuth) oppure rete privata -
non esporre pubblicamente senza auth - logga audit delle operazioni
admin

## 8.2 enrollment-service

-   richiede bootstrap token
-   rifiuta device non PENDING
-   logga tentativi falliti (senza loggare segreti)

## 8.3 ingest-service

-   accessibile solo via mTLS
-   payload non trusted
-   publish Pub/Sub con attributi derivati da SAN

------------------------------------------------------------------------

# 9. Secret Management

Segreti da gestire: - `BOOTSTRAP_HMAC_KEY_BASE64` (condiviso tra admin e
enrollment) - CA private key PEM - CA cert PEM - CA chain PEM
(opzionale)

Best practices: - Secret Manager + versioning - rotation programmata
(L2) - mai committare `.pem`, `.key`, `.p12`, `.jks` nel repo - usare
`.gitignore` per proteggere file locali

------------------------------------------------------------------------

# 10. Logging & PII

Linee guida: - non loggare token plaintext - non loggare CSR completo
(al massimo hash/size) - loggare `deviceId/tenantId/siteId` derivati da
SAN (ok) - se telemetria contiene PII, valutare masking o separazione
dataset (L2)

------------------------------------------------------------------------

# 11. Threat Model minimo (L1)

## Minacce principali

1)  Device spoofing via payload JSON

-   Mitigazione: identità solo da SAN (mTLS) + mismatch reject

2)  Token replay / bruteforce

-   Mitigazione: TTL breve + HMAC + one-time consumption + FOR UPDATE

3)  Cert spoofing / cert non autorizzato

-   Mitigazione: fingerprint match contro DB

4)  Flood su ingest endpoint

-   Mitigazione: deny cache + Cloud Run autoscaling + (L2) rate limit

5)  Compromissione segreti CA

-   Mitigazione: Secret Manager + IAM least privilege + rotation (L2)

------------------------------------------------------------------------

# 12. Checklist di sicurezza operativa (prima della produzione)

✅ Cloud Run: - ingress: solo tramite LB / internal as designed -
service account dedicati per servizio - min IAM per Secret Manager /
PubSub / Cloud SQL - no env var con segreti in chiaro se non necessario
(preferire Secret Manager)

✅ Database: - Cloud SQL private IP + connector - password DB in Secret
Manager - audit log abilitati (se possibile)

✅ PKI: - CA key in Secret Manager - backup sicuro della CA key
(offline) - policy per rotation (L2)

✅ Repo hygiene: - `.gitignore` include cert/key/secrets - scanning
segreti (pre-commit o CI) (L2)

------------------------------------------------------------------------

# 13. Security Contacts & Reporting

In L1 (solo dev), tutte le segnalazioni vanno indirizzate al maintainer
del progetto.

------------------------------------------------------------------------

Author\
Berti AI & Cloud Architecture\
Technical Cloud & AI Architecture on Google Cloud
