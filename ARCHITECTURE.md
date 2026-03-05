# ARCHITECTURE.md

BertiCloud AI -- L1 Control Plane Architecture

This document describes the **logical architecture, security model, and
main data flows** for the L1 MVP of the BertiCloud AI Control Plane.

Goal of L1:

> Make provisioning → enrollment → telemetry ingestion work end‑to‑end
> in a secure and scalable way.

------------------------------------------------------------------------

# 1. System Overview

The system manages **IoT / Edge devices (Raspberry based)** that send
telemetry data to the cloud.

The architecture is separated into two logical planes:

**Control Plane** Responsible for device lifecycle and identity.

**Data Plane** Responsible for high‑throughput telemetry ingestion.

------------------------------------------------------------------------

# 2. High Level Architecture

                ┌──────────────────────────────┐
                │        Admin / Console       │
                │     (future Web App)         │
                └──────────────┬───────────────┘
                               │
                               ▼
                        ┌──────────────┐
                        │ admin-service │
                        │ Spring Boot   │
                        └───────┬───────┘
                                │
                                │ CRUD Control Plane
                                │
                                ▼
                        ┌───────────────┐
                        │  Cloud SQL     │
                        │ PostgreSQL     │
                        └───────────────┘

Provisioning phase Device created → PENDING

                                │
                                ▼

                        ┌───────────────────┐
                        │ enrollment-service │
                        │ CSR Signing (PKI) │
                        └─────────┬─────────┘
                                  │
                                  │ verify bootstrap token
                                  │ validate SAN
                                  │ sign certificate
                                  │
                                  ▼
                          Device becomes ACTIVE

                                  │
                                  ▼

Telemetry phase

Edge Device (Raspberry)

      │
      │ mTLS
      ▼

┌─────────────────────────────────────┐ │ External HTTPS Load Balancer
(GCP) │ │ mTLS Client Authentication │
└───────────────┬─────────────────────┘ │ ▼ ┌──────────────┐ │
ingest-service│ │ Spring Boot │ └──────┬────────┘ │ │ publish ▼
┌───────────┐ │ Pub/Sub │ └───────────┘

------------------------------------------------------------------------

# 3. Components

## shared

Shared library used by all services.

Responsibilities:

• device identity parsing\
• SAN URI parsing\
• mTLS header utilities

Main classes:

MtlsHeaders\
SanUriParser\
SanUriSelector\
DeviceIdentity

------------------------------------------------------------------------

## admin-service

Control plane management API.

Responsibilities:

• create tenant\
• create subscription\
• create site\
• register device\
• issue bootstrap token

Device lifecycle:

PENDING → ACTIVE

Devices start as **PENDING** and become **ACTIVE** after CSR enrollment.

------------------------------------------------------------------------

## enrollment-service

Handles **PKI enrollment** of devices.

Responsibilities:

• verify bootstrap token\
• validate CSR SAN identity\
• sign device certificate\
• store certificate metadata in database

Security critical component.

Private CA key is stored in **Secret Manager**.

------------------------------------------------------------------------

## ingest-service

Handles **telemetry ingestion** from edge devices.

Responsibilities:

• verify mTLS headers • extract device identity from SAN • authorize
device using Cloud SQL + cache • publish telemetry to Pub/Sub

Performance considerations:

• Caffeine cache for auth context\
• minimal DB queries during hot path

------------------------------------------------------------------------

# 4. Identity Model

Device identity is encoded inside the certificate **SAN URI**.

Format:

urn:berticloudai:tenant:`<TENANT_ID>`{=html}:site:`<SITE_ID>`{=html}:device:`<DEVICE_ID>`{=html}

Example:

urn:berticloudai:tenant:tnt-001:site:site-roma-001:device:rpi-123

This identity is considered **cryptographically strong** because it is:

• embedded inside an X.509 certificate\
• signed by the platform CA

------------------------------------------------------------------------

# 5. Security Model

## mTLS Authentication

Devices authenticate using **mutual TLS**.

Flow:

1.  Device presents client certificate
2.  Load Balancer validates certificate chain
3.  LB forwards request to Cloud Run
4.  LB injects mTLS headers

Headers used by backend:

client_cert_present\
client_cert_chain_verified\
client_cert_sha256_fingerprint\
client_cert_uri_sans

------------------------------------------------------------------------

## Bootstrap Token

Before obtaining a certificate, devices authenticate using a **bootstrap
token**.

Properties:

• one-time usage\
• limited TTL (default 60 minutes)\
• stored in DB only as **HMAC hash**

Workflow:

Admin generates token → device uses token → enrollment consumes token.

------------------------------------------------------------------------

## Certificate Binding

After enrollment:

Device certificate fingerprint is stored in database.

During ingest:

fingerprint from mTLS header must match expected fingerprint in DB.

This prevents certificate spoofing.

------------------------------------------------------------------------

# 6. Database Schema

Schema:

control_plane

Tables:

tenants\
subscriptions\
sites\
devices\
device_cert_history

devices table stores:

• device status\
• certificate fingerprint\
• certificate metadata\
• bootstrap token hash

------------------------------------------------------------------------

# 7. Telemetry Flow

Device sends JSON telemetry payload.

Example:

{ "eventType": "telemetry.frigate.stats", "schemaVersion": "1.0",
"deviceId": "rpi-123" }

Ingest service:

1.  extracts SAN identity
2.  authorizes device
3.  publishes raw JSON to Pub/Sub

Pub/Sub attributes:

tenantId\
siteId\
deviceId\
eventType\
schemaVersion

------------------------------------------------------------------------

# 8. Scaling Model

Architecture is horizontally scalable.

Cloud Run provides:

• automatic scaling\
• stateless services\
• container deployment

Pub/Sub decouples ingestion from downstream processing.

Future pipeline:

Pub/Sub → Dataflow → BigQuery.

------------------------------------------------------------------------

# 9. Future Improvements (L2)

Planned enhancements:

• certificate revocation\
• device configuration management\
• device twin state\
• telemetry rate limiting\
• Dataflow pipeline to BigQuery\
• Web Admin Console\
• certificate rotation\
• observability and metrics dashboards

------------------------------------------------------------------------

# 10. Design Philosophy

L1 focuses on:

simplicity\
security\
end‑to‑end functionality

Complexity will be introduced incrementally in L2.

------------------------------------------------------------------------

Author\
Berti AI & Cloud Architecture
