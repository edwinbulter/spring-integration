# spring-integration

Monorepo met losstaande Spring Integration demo's, elk in hun eigen submap
(`demo-01`, `demo-02`, ...). Demo's delen alleen de root-`pom.xml` (Spring
Boot BOM / Java-versie) en de kind-cluster in `cluster/`; verder zijn ze
volledig onafhankelijk van elkaar te bouwen, te deployen en op te ruimen.

## Structuur

```
spring-integration/
├── pom.xml            # Alleen dependency-/plugin-beheer, GEEN <modules>
├── cluster/            # Gedeeld kind-cluster (alle demo's draaien hierin)
│   ├── kind-config.yaml
│   ├── create-cluster.sh
│   └── delete-cluster.sh
└── demo-01/            # Eerste demo (zie demo-01/README.md)
    ├── pom.xml         # Aggregator-pom voor deze demo (parent = root pom)
    ├── file-to-kafka-app/
    ├── kafka-to-file-app/
    ├── k8s/
    ├── scripts/
    └── data/
```

Er is bewust géén `<modules>`-sectie in de root `pom.xml`: elke demo wordt
apart gebouwd, bijvoorbeeld:

```bash
mvn -f demo-01/pom.xml package
```

## Gedeeld kind-cluster opzetten

Alle demo's draaien in dezelfde kind-cluster `single-node`, met een brede
mount van de bovenliggende workspace-map naar `/data/projects` op de node
(zie `cluster/kind-config.yaml`). Dit maakt het mogelijk dat elke demo zijn
eigen `data/`-submap op de node kan mounten zonder dat er iets
gebruikersnaam- of machine-specifieks in de configuratie hoeft te staan.

```bash
./cluster/create-cluster.sh
```

**Let op:** dit script verwijdert en herbouwt de bestaande `single-node`
cluster. Alles wat daar al in draaide gaat verloren.

Cluster volledig verwijderen (i.p.v. één demo opruimen):

```bash
./cluster/delete-cluster.sh
```

## Een demo draaien

Zie de README in de demo-map zelf, bijvoorbeeld `demo-01/README.md`.

## Een nieuwe demo toevoegen

Kopieer het patroon van `demo-01`:
1. Nieuwe map `demo-02/` met eigen `pom.xml` (packaging `pom`, `<parent>` =
   root pom, eigen `<modules>`).
2. Eigen `k8s/`, `scripts/` en `data/` submappen.
3. Eigen Kubernetes-namespace met dezelfde naam als de mapnaam (`demo-02`),
   zodat opruimen altijd `kubectl delete ns demo-02` is.
4. Hergebruik het gedeelde `cluster/` — geen nieuw kind-cluster nodig.
