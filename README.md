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

## Verification

- Health API: http://localhost:${BACKEND_PORT:-8080}/api/health
- Swagger UI: http://localhost:${BACKEND_PORT:-8080}/swagger-ui/index.html

## Variables d'environnement

Voir `.env.example`.

## Resolution de problemes

- Port déjà pris (8080/5432): change `BACKEND_PORT` / `DB_PORT` dans `.env` (ou copie `.env.example` → `.env`).
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
# AGORA Backend — Monitoring

## Zabbix Agent intégré

### Configuration

Modifier dans docker-compose.yml :

ZBX_SERVER_HOST=IP_DU_SERVEUR_ZABBIX

### Hostname

agora-app

### Port

10050 doit être accessible depuis le serveur Zabbix.

### Endpoints

- http://localhost:${BACKEND_PORT:-8080}/actuator/health
- http://localhost:${BACKEND_PORT:-8080}/v3/api-docs