# Appunti: Rimozione Bilanciatore di Carico

Questi appunti documentano la modifica architetturale effettuata per eliminare il bilanciatore di carico HTTP(S) esterno (al fine di ridurre i costi di networking) ed esporre i servizi Cloud Run direttamente tramite *Cloud Run Domain Mapping*.

## 1. Modifiche Infrastrutturali
Il bilanciatore di carico precedente instradava il traffico su 4 servizi backend.

I record DNS per i servizi esposti (`console`, `enrollment`, `ingest`) devono puntare ai server di Google (es. `ghs.googlehosted.com`) come richiesto dal mapping nativo di Cloud Run. I certificati SSL sono ora gestiti automaticamente da Cloud Run.

## 2. Gestione dell'Accesso a `admin-service`

- **Autenticazione IAM:** Nel file `cloudbuild.yaml`, è stato rimosso il flag `--no-invoker-iam-check` per `admin-service`. Di conseguenza, Cloud Run richiede ora un token OIDC (Identity) di Google Cloud per permettere l'invocazione.
- **Comunicazione Server-to-Server:** Il frontend (Web App / Console), implementato in `webapp-service`, ha bisogno di interrogare l'`admin-service`. Poiché `admin-service` richiede autenticazione, la `webapp-service` è stata aggiornata per supportare la generazione del token OIDC:
  - È stato aggiunto un **`GcpOidcInterceptor`** (`ai.berticloud.webapp.client.GcpOidcInterceptor`).
  - Questo intercettatore interroga il server dei metadati nativo di Google (`http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/identity?audience=...`) richiedendo un token per l'audience dell'`admin-service`.
  - Il token viene automaticamente allegato ad ogni richiesta del `RestTemplate` nell'header `Authorization: Bearer <token>`.

Questa architettura consente all'`admin-service` di rimanere isolato da richieste non autorizzate su Internet, accettando solo le connessioni autenticate (come quelle provenienti da `webapp-service` tramite l'account di servizio autorizzato).
