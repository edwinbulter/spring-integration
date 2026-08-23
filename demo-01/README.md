# demo-01: File → Kafka → File met Spring Integration

Deze demo bestaat uit twee losstaande Spring Boot / Spring Integration apps
en een Kafka broker, allemaal draaiend in namespace `demo-01` van het gedeelde
kind-cluster (zie `../cluster/`).

## Flow

1. **`file-to-kafka-app`** pollt de map `data/input-01`. Voor elk bestand:
   - leest de inhoud van het bestand,
   - bepaalt een nieuwe bestandsnaam: `yyyyMMdd-HHmmss-<originele naam zonder extensie>.json`
     (de originele extensie wordt altijd vervangen door `.json`, en de
     timestamp-prefix voorkomt dat het opnieuw aanbieden van eenzelfde
     bestandsnaam, bv. `test.txt`, tot een naamconflict leidt),
   - bouwt een JSON-bericht met:
     - `payload`: de bestandsinhoud,
     - `filename`: de nieuwe (timestamp-geprefixte, `.json`) bestandsnaam,
     - `logging`: lijst met één object `{ "timestamp": "yyyy-MM-dd HH:mm:ss", "message": "file processed from folder input-01" }`,
   - publiceert dit JSON-bericht naar Kafka-topic `topic-01`,
   - verplaatst het originele bestand, hernoemd naar de nieuwe bestandsnaam, naar `data/processed-01`.
2. **`kafka-to-file-app`** consumeert `topic-01`. Voor elk bericht:
   - leest het veld `filename` uit de JSON,
   - voegt een tweede `logging`-object toe: `{ "timestamp": "yyyy-MM-dd HH:mm:ss", "message": "message received from topic-01" }`,
   - schrijft de bijgewerkte JSON-inhoud naar `data/output-01/<filename>`.

## Draaien

Vanuit de repo-root:

```bash
# 1. (Eén keer, of opnieuw als je de cluster wilt verversen)
./cluster/create-cluster.sh

# 2. Images bouwen en in de cluster laden
./demo-01/scripts/01-build-and-load-images.sh

# 3. Deployen
./demo-01/scripts/02-deploy.sh
```

## Testen

```bash
echo "hello world" > demo-01/data/input-01/test.txt
```

Na een paar seconden:
- `demo-01/data/input-01/test.txt` is verdwenen,
- `demo-01/data/processed-01/<timestamp>-test.json` bevat de originele inhoud,
- `demo-01/data/output-01/<timestamp>-test.json` bevat het volledige JSON-bericht, bv.:

```json
{
  "payload": "hello world\n",
  "filename": "20260823-194000-test.json",
  "logging": [
    { "timestamp": "2026-08-23 19:40:00", "message": "file processed from folder input-01" },
    { "timestamp": "2026-08-23 19:40:01", "message": "message received from topic-01" }
  ]
}
```

Omdat de bestandsnaam met een timestamp wordt geprefixt, kun je hetzelfde
bestand (`test.txt`) net zo vaak opnieuw aanbieden als je wilt — elke keer
ontstaat een uniek bestand in `processed-01` en `output-01`.

Logs bekijken:

```bash
kubectl -n demo-01 logs -l app=file-to-kafka-app -f
kubectl -n demo-01 logs -l app=kafka-to-file-app -f
```

## Opruimen

```bash
./demo-01/scripts/99-cleanup.sh
```

Dit verwijdert alleen de namespace `demo-01` (dus alleen deze demo) — het
gedeelde kind-cluster en eventuele andere demo's blijven ongemoeid.
