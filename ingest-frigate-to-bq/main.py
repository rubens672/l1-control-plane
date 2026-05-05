import base64
import json
import functions_framework
from google.cloud import bigquery

# Inizializza il client BigQuery fuori dalla funzione per performance migliori
client = bigquery.Client()

# SOSTITUISCI CON I TUOI DATI REALI
TABLE_ID = "mosqhealthagent.telemetry.frigate_stats"

@functions_framework.cloud_event
def subscribe(cloud_event):
    # 1. Recupera i dati dal messaggio Pub/Sub
    pubsub_message = base64.b64decode(cloud_event.data["message"]["data"]).decode("utf-8")
    data = json.loads(pubsub_message)

    try:
        # 2. Trasformazione CAMERAS (da oggetto a lista di oggetti)
        if 'payload' in data and 'frigate' in data['payload']:
            frigate_data = data['payload']['frigate']
            
            if 'cameras' in frigate_data:
                raw_cameras = frigate_data['cameras']
                normalized_cameras = []
                for cam_name, metrics in raw_cameras.items():
                    metrics['camera_name'] = cam_name
                    normalized_cameras.append(metrics)
                data['payload']['frigate']['cameras'] = normalized_cameras

            # 3. Trasformazione DETECTORS (da oggetto a lista di oggetti)
            if 'detectors' in frigate_data:
                raw_detectors = frigate_data['detectors']
                normalized_detectors = []
                for det_name, metrics in raw_detectors.items():
                    metrics['detector_name'] = det_name
                    normalized_detectors.append(metrics)
                data['payload']['frigate']['detectors'] = normalized_detectors

        # 4. Inserimento in BigQuery
        errors = client.insert_rows_json(TABLE_ID, [data])
        
        if errors == []:
            print(f"OK: Inserito messaggio da device {data.get('deviceId')}")
        else:
            print(f"ERROR BQ: {errors}")

    except Exception as e:
        print(f"CRITICAL ERROR: {str(e)}")