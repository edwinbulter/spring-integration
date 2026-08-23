# demo-01: Quote → File → Kafka → File/DB met Spring Integration

Deze demo bestaat uit vier losstaande Spring Boot / Spring Integration apps,
een Kafka broker en een PostgreSQL database, allemaal draaiend in namespace
`demo-01` van het gedeelde kind-cluster (zie `../cluster/`).

## Flow

0. **`quote-to-file-app`** haalt periodiek (standaard elke 30s) een random
   quote op bij [ZenQuotes](https://zenquotes.io/api/random) en schrijft
   alleen de waarde van het `"q"`-veld naar `data/input-01/quote-<xx>.txt`,
   met `xx` een teller 00 t/m 99 die daarna weer bij 00 begint. De periode
   wordt continu (elke seconde) opnieuw gelezen uit `data/config.yml`, dus
   een wijziging daar wordt binnen ~1 seconde overgenomen zonder herstart
   (zie "Fetch-periode live aanpassen" hieronder). De weggeschreven bestanden
   worden automatisch door `file-to-kafka-app` opgepikt, waarmee de hele
   keten quote → Kafka → file/db gedemonstreerd wordt.
1. **`file-to-kafka-app`** pollt de map `data/input-01`. Voor elk bestand:
   - leest de inhoud van het bestand,
   - bepaalt een timestamp-prefix `yyyyMMdd-HHmmss` (voorkomt dat het opnieuw
     aanbieden van eenzelfde bestandsnaam, bv. `test.txt`, tot een
     naamconflict leidt),
   - bouwt een JSON-bericht met:
     - `payload`: de bestandsinhoud,
     - `filename`: `<timestamp>-<originele naam zonder extensie>.json` (de
       originele extensie wordt in dit veld altijd vervangen door `.json`),
     - `logging`: lijst met één object `{ "timestamp": "yyyy-MM-dd HH:mm:ss", "message": "file processed from folder input-01" }`,
   - publiceert dit JSON-bericht naar Kafka-topic `topic-01`,
   - verplaatst het originele bestand naar `data/processed-01`, hernoemd naar
     `<timestamp>-<originele bestandsnaam>` (met behoud van de originele
     extensie, want de inhoud daar is geen JSON).
2. **`kafka-to-file-app`** consumeert `topic-01` (consumergroup `kafka-to-file-app`). Voor elk bericht:
   - leest het veld `filename` uit de JSON,
   - voegt een tweede `logging`-object toe: `{ "timestamp": "yyyy-MM-dd HH:mm:ss", "message": "message received from topic-01" }`,
   - schrijft de bijgewerkte, netjes geformatteerde JSON naar `data/output-01/<filename>`.
3. **`kafka-to-db-app`** consumeert hetzelfde `topic-01` onafhankelijk (eigen
   consumergroup `kafka-to-db-app`, dus dit bericht ontvangt zowel deze app
   als `kafka-to-file-app`, ze "delen" de berichten niet). Voor elk bericht:
   - leest de velden `filename` en `payload` uit de JSON,
   - voegt een rij toe aan de PostgreSQL-tabel `messages` met kolommen
     `filename`, `payload` (de originele bestandsinhoud) en `creation_date`
     (moment van verwerking).

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
- `demo-01/data/processed-01/<timestamp>-test.txt` bevat de originele inhoud (originele extensie behouden),
- `demo-01/data/output-01/<timestamp>-test.json` bevat het volledige, netjes geformatteerde JSON-bericht, bv.:

```json
{
  "payload" : "hello world\n",
  "filename" : "20260823-194000-test.json",
  "logging" : [ {
    "timestamp" : "2026-08-23 19:40:00",
    "message" : "file processed from folder input-01"
  }, {
    "timestamp" : "2026-08-23 19:40:01",
    "message" : "message received from topic-01"
  } ]
}
```

- er is een nieuwe rij toegevoegd aan de `messages`-tabel in PostgreSQL, te controleren met:

```bash
kubectl -n demo-01 exec -it deploy/postgres -- psql -U demo01 -d demo01 \
  -c "SELECT id, filename, creation_date FROM messages ORDER BY creation_date DESC LIMIT 5;"
```

Omdat de bestandsnaam met een timestamp wordt geprefixt, kun je hetzelfde
bestand (`test.txt`) net zo vaak opnieuw aanbieden als je wilt — elke keer
ontstaat een uniek bestand in `processed-01`/`output-01` en een nieuwe rij in
de database.

Logs bekijken:

```bash
kubectl -n demo-01 logs -l app=quote-to-file-app -f
kubectl -n demo-01 logs -l app=file-to-kafka-app -f
kubectl -n demo-01 logs -l app=kafka-to-file-app -f
kubectl -n demo-01 logs -l app=kafka-to-db-app -f
```

> **Let op:** het PostgreSQL-wachtwoord (`demo01`) staat hardcoded in
> `k8s/postgres.yaml` en `k8s/kafka-to-db-app.yaml`. Dat is alleen bedoeld
> voor deze wegwerpbare, lokale demo-cluster — gebruik dit patroon niet in
> een echte omgeving.

## Fetch-periode live aanpassen

`quote-to-file-app` leest zijn ophaal-periode uit `demo-01/data/config.yml`:

```yaml
QuoteToFile:
  periodInSeconds: 30
```

Pas dit bestand aan (bv. `periodInSeconds: 5` om sneller quotes te zien
binnenkomen) terwijl de app draait — geen herstart of herdeploy nodig. De
app leest dit bestand elke seconde opnieuw, dus de nieuwe waarde wordt
uiterlijk na ~1 seconde overgenomen. Ontbreekt het bestand, is het niet
leesbaar, of bevat het geen geldig getal, dan valt de app terug op de
standaardwaarde van 30 seconden (en logt dat eenmalig, niet elke seconde).

## Opruimen

```bash
./demo-01/scripts/99-cleanup.sh
```

Dit verwijdert alleen de namespace `demo-01` (dus alleen deze demo) — het
gedeelde kind-cluster en eventuele andere demo's blijven ongemoeid.
