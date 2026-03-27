# AGORA - Backend de reservation de ressources

Backend Spring Boot pour la gestion des reservations de ressources municipales (projet scolaire Ynov).

## Prerequis

| Outil | Version minimale |
|---|---|
| Java | 21 |
| Docker Desktop | recent |

## Lancement local

### 1) Démarrer la stack Docker (PostgreSQL + backend)

```powershell
docker compose up -d
```

### Alternative: lancer Spring Boot en local (en gardant Postgres via Docker)

- Démarre uniquement la DB :

```powershell
docker compose up -d postgres
```

- Ou en 1 commande via les scripts :
  - macOS/Linux : `./scripts/dev-run-local.sh`
  - Windows (PowerShell) : `.\scripts\dev-run-local.ps1`

- Charge les variables `.env` puis démarre Spring Boot.
  - Sur macOS/Linux (bash/zsh) :

```bash
set -a && source .env && set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

  - Sur Windows (PowerShell) :

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*#') { return }
  if ($_ -match '^\s*$') { return }
  $kv = $_.Split('=',2)
  [System.Environment]::SetEnvironmentVariable($kv[0], $kv[1])
}
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

## Verification

- Health API: http://localhost:${BACKEND_PORT:-8081}/api/health
- Swagger UI: http://localhost:${BACKEND_PORT:-8081}/swagger-ui/index.html

## Variables d'environnement

Voir `.env.example`.

## Resolution de problemes

- Port déjà pris (8080/5432): change `BACKEND_PORT` / `DB_PORT` dans `.env` (ou copie `.env.example` → `.env`).
- Windows: si tu as une erreur de bind sur `127.0.0.1:8080` ("access permissions"), mets `BIND_ADDR=0.0.0.0` dans `.env` (ou change `BACKEND_PORT`).
- Flyway / migrations incohérentes après des changements d'historique: reset du volume DB dev :

```powershell
docker compose down -v
docker compose up -d
```

- JAVA_HOME manquant: definis `JAVA_HOME` ou garde Java accessible via `PATH`.

## Architecture

```
com.agora
 |- controller   -> REST endpoints (DTO, validation HTTP)
 |- service      -> Logique metier + transactions
 |- repository   -> Acces aux donnees (Spring Data JPA)
 |- entity       -> Entites JPA
 |- dto          -> Data Transfer Objects
 |- mapper       -> MapStruct mappers
 |- config       -> Configuration Spring (Security, OpenAPI)
 |- exception    -> Gestion globale des erreurs
```
## Monitoring (Zabbix)

Le backend intègre un agent Zabbix pour le monitoring.

### Configuration
Modifier les variables d'environnement dans le fichier `compose.yaml` (ou `.env`) :
- `ZBX_SERVER_HOST`: IP du serveur Zabbix.
- `ZBX_HOSTNAME`: `agora-app` (par défaut).

### Accès
- **Port** : `10050` doit être accessible depuis le serveur Zabbix.
- **Endpoints Actuator** :
  - Health : `http://localhost:${BACKEND_PORT:-8081}/actuator/health`
  - OpenAPI : `http://localhost:${BACKEND_PORT:-8081}/v3/api-docs`

## Accès à la base de données (PostgreSQL)

### Via Docker (CLI)
Pour ouvrir un shell `psql` directement dans le conteneur :
```powershell
docker exec -it agora-db psql -U agora -d agora
```

Quelques commandes utiles :
- `\dt` : Lister les tables.
- `select count(*) from users;` : Compter les utilisateurs.
- `\q` : Quitter psql.

### Via un client externe (DBeaver, DataGrip, pgAdmin)
- **Host** : `127.0.0.1`
- **Port** : `${DB_PORT}` (par défaut `5432`)
- **Database** : `${DB_NAME}` (par défaut `agora`)
- **User** : `${DB_USER}` (par défaut `agora`)
- **Password** : `${DB_PASS}` (par défaut `agora`)

Toutes ces valeurs sont définies dans votre fichier `.env` à la racine du projet.