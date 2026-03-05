# BertiCloud AI -- L1 Control Plane

MVP Control Plane per piattaforma **IoT / Video Surveillance Edge**
basata su:

-   Raspberry / Edge devices
-   mTLS device authentication
-   Cloud Run services
-   Cloud SQL (Postgres)
-   Pub/Sub ingest pipeline

## Versione **L1**:

> provisioning →
> enrollment → ingest telemetria.

------------------------------------------------------------------------

# Architettura L1

                ┌─────────────────────────┐
                │   Admin / Web Console   │
                └─────────────┬───────────┘
                              │
                              ▼
                     ┌────────────────┐
                     │ admin-service  │
                     │  Spring Boot   │
                     └───────┬────────┘
                             │
                             │ CRUD control-plane
                             │
                             ▼
                    ┌──────────────────┐
                    │   Cloud SQL      │
                    │   PostgreSQL     │
                    └──────────────────┘

Provisioning (Admin → Device)

device PENDING bootstrap token

                             │
                             ▼

                    ┌──────────────────────┐
                    │ enrollment-service   │
                    │ CSR signing (PKI)   │
                    └─────────┬────────────┘
                              │
                              │ verify bootstrap token
                              │ sign CSR
                              │
                              ▼
                     device ACTIVE

                             │
                             ▼

Device → Telemetry

        ┌───────────────────────────┐
        │ External HTTPS LB (mTLS)  │
        └──────────────┬────────────┘
                       │
                       ▼
               ┌────────────────┐
               │ ingest-service │
               │ Spring Boot    │
               └───────┬────────┘
                       │
                       │ publish
                       ▼
                 ┌───────────┐
                 │ Pub/Sub   │
                 └───────────┘

------------------------------------------------------------------------

# Servizi del progetto

Repository contiene 4 moduli Maven.

l1-control-plane │ ├── shared │ ├── admin-service │ ├──
enrollment-service │ └── ingest-service

------------------------------------------------------------------------

# shared

Libreria condivisa con:

-   parsing identità device
-   parsing SAN URI
-   gestione header mTLS

Classi principali:

MtlsHeaders SanUriSelector SanUriParser DeviceIdentity

Identità device:

urn:berticloudai:tenant:`<TENANT_ID>`{=html}:site:`<SITE_ID>`{=html}:device:`<DEVICE_ID>`{=html}

Esempio:

urn:berticloudai:tenant:tnt-001:site:site-roma-001:device:rpi-123

------------------------------------------------------------------------

# admin-service

Gestisce il **control-plane**.

Responsabilità:

-   creare tenant
-   creare subscription
-   creare site
-   registrare device
-   generare bootstrap token

Device lifecycle:

PENDING → ACTIVE

Quando un device viene creato:

status = PENDING

Serve enrollment CSR per diventare ACTIVE.

------------------------------------------------------------------------

## API admin

### Create tenant

POST /v1/admin/tenants

Body

{ "tenantId": "tnt-001", "name": "Cliente Roma", "plan": "PRO" }

------------------------------------------------------------------------

### Create subscription

POST /v1/admin/subscriptions

{ "tenantId": "tnt-001", "status": "ACTIVE", "validFrom":
"2026-01-01T00:00:00Z", "validTo": "2027-01-01T00:00:00Z", "maxDevices":
10 }

------------------------------------------------------------------------

### Create site

POST /v1/admin/sites

{ "siteId": "site-roma-001", "tenantId": "tnt-001", "name": "Villa
Roma", "timezone": "Europe/Rome" }

------------------------------------------------------------------------

### Register device

POST /v1/admin/devices

{ "deviceId": "rpi-123", "tenantId": "tnt-001", "siteId":
"site-roma-001", "model": "raspberry-pi4" }

Device stato iniziale:

PENDING

------------------------------------------------------------------------

### Generate bootstrap token

POST /v1/admin/devices/{deviceId}:bootstrapToken

Response:

{ "deviceId": "rpi-123", "bootstrapToken": "abcXYZ....", "expiresAt":
"2026-03-06T10:00:00Z" }

⚠️ Il token viene mostrato **una sola volta**.

DB salva solo:

bootstrap_token_hash

------------------------------------------------------------------------

# enrollment-service

Gestisce **PKI enrollment dei device**.

Flusso:

Device │ generate keypair │ generate CSR │ POST
/v1/enrollments/{deviceId}:signCsr │ verify bootstrap token │ validate
SAN │ sign certificate │ device ACTIVE

------------------------------------------------------------------------

## API

POST /v1/enrollments/{deviceId}:signCsr

Header

Authorization: Bearer `<bootstrapToken>`{=html}

Body

{ "csrPem": "-----BEGIN CERTIFICATE REQUEST-----..." }

Response

{ "clientCertPem": "-----BEGIN CERTIFICATE-----...", "chainPem":
"-----BEGIN CERTIFICATE-----..." }

------------------------------------------------------------------------

# ingest-service

Riceve **telemetria device**.

Endpoint:

POST /v1/telemetry

Accesso **solo via mTLS**.

------------------------------------------------------------------------

## Sicurezza ingest

Il Load Balancer valida il certificato client e inoltra header:

client_cert_present client_cert_chain_verified
client_cert_sha256_fingerprint client_cert_uri_sans

L'ingest-service:

1.  legge SAN URI
2.  costruisce DeviceIdentity
3.  verifica DB (cache + Cloud SQL)
4.  pubblica su Pub/Sub

------------------------------------------------------------------------

## Esempio payload telemetria

{ "eventType": "telemetry.frigate.stats", "schemaVersion": "1.0",
"siteId": "site-roma-001", "deviceId": "rpi-123", "timestamp":
"2026-02-27T21:21:45Z", "payload": { "system": { "cpu": { "percent": 10
}, "ram": { "percent": 60 } } } }

Pub/Sub attributes:

tenantId siteId deviceId eventType schemaVersion

------------------------------------------------------------------------

# Database

Schema principale:

control_plane

Tabelle:

tenants subscriptions sites devices device_cert_history

------------------------------------------------------------------------

# Setup locale

Prerequisiti:

Java 21\
Maven\
Docker (opzionale)\
Postgres locale

Compilazione:

mvn clean package

Avvio servizio:

cd admin-service\
mvn spring-boot:run

------------------------------------------------------------------------

# Variabili ambiente

Admin

BOOTSTRAP_HMAC_KEY_BASE64

Enrollment

BOOTSTRAP_HMAC_KEY_BASE64 CA_KEY_SECRET CA_CERT_SECRET CA_CHAIN_SECRET

Ingest

PUBSUB_TOPIC DB_URL

------------------------------------------------------------------------

# Test manuale con Postman

Ordine test consigliato:

1 create tenant\
2 create subscription\
3 create site\
4 create device\
5 bootstrap token\
6 enrollment CSR\
7 ingest telemetria

------------------------------------------------------------------------

# Convenzione identità device

SAN URI del certificato:

urn:berticloudai:tenant:`<TENANT>`{=html}:site:`<SITE>`{=html}:device:`<DEVICE>`{=html}

Esempio:

urn:berticloudai:tenant:tnt-001:site:site-roma-001:device:rpi-123

------------------------------------------------------------------------

# Obiettivo L1

Provisioning\
Enrollment\
Telemetry ingest\
Pub/Sub pipeline

tutto **funzionante end-to-end**.

------------------------------------------------------------------------

# Evoluzione futura (L2)

-   revocation cert
-   twin/config device
-   rate limit ingest
-   Dataflow → BigQuery
-   Web admin console
-   CA rotation
-   device metrics dashboard

------------------------------------------------------------------------

# Autore

Berti AI & Cloud Architecture\
Technical Cloud & AI Architecture on Google Cloud
