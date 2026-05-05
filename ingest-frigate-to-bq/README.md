# Ingest Frigate to BigQuery (Cloud Function)

Questo modulo contiene una **Google Cloud Function** scritta in Python che agisce come sottoscrittore (subscriber) di un topic Pub/Sub. Il suo scopo principale è ricevere messaggi contenenti statistiche di telemetria provenienti da istanze Frigate, normalizzare i dati e inserirli in una tabella BigQuery per l'analisi.

## Funzionamento

La funzione viene attivata da un evento Pub/Sub. Quando riceve un nuovo messaggio, esegue i seguenti passaggi:

1. **Decodifica del Messaggio**: Estrae il payload in base64 dal messaggio Pub/Sub e lo decodifica in un oggetto JSON.
2. **Normalizzazione dei Dati (Trasformazione Schema)**:
   I dati nativi di Frigate spesso contengono dizionari con chiavi dinamiche (es. il nome della telecamera). BigQuery gestisce in modo più efficiente questi dati se sono strutturati come array (liste) di record ripetuti.
   - **Cameras**: Cerca l'oggetto `payload.frigate.cameras`. Se presente, lo converte da un dizionario (chiave: nome telecamera, valore: metriche) a una lista di oggetti, aggiungendo il campo `camera_name` a ciascun record.
   - **Detectors**: Cerca l'oggetto `payload.frigate.detectors`. Se presente, lo converte da un dizionario a una lista di oggetti, aggiungendo il campo `detector_name` a ciascun record.
3. **Inserimento in BigQuery**: Utilizza il client Python di BigQuery per inserire l'oggetto JSON normalizzato come una nuova riga nella tabella di destinazione specificata (`mosqhealthagent.telemetry.frigate_stats`).

## Prerequisiti

Il file `requirements.txt` definisce le dipendenze necessarie per il funzionamento:
- `functions-framework`: Fornisce l'ambiente di esecuzione standard per le Cloud Functions.
- `google-cloud-bigquery`: La libreria client ufficiale per interagire con l'API di BigQuery.

## Configurazione

All'interno di `main.py`, la variabile `TABLE_ID` definisce la destinazione dei dati. Attualmente è configurata come:
```python
TABLE_ID = "mosqhealthagent.telemetry.frigate_stats"
```
*Nota: l'account di servizio associato all'esecuzione di questa Cloud Function abbia deve avere i permessi IAM necessari per scrivere in questa tabella BigQuery (es. `roles/bigquery.dataEditor`).*
