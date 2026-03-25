# AGORA - Backend de reservation de ressources

Backend Spring Boot pour la gestion des reservations de ressources municipales (projet scolaire Ynov).

## Prerequis

| Outil | Version minimale |
|---|---|
| Java | 21 |
| Docker Desktop | recent |

## Lancement local

### 1) Demarrer PostgreSQL avec Docker

```powershell
docker compose up -d postgres
```

### 2) Demarrer l'application Spring Boot

```powershell
.\mvnw.cmd spring-boot:run
```

## Verification

- Health API: http://localhost:8080/api/health
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## Variables d'environnement

Voir `.env.example`.

## Resolution de problemes

- Port 5432 deja pris: change le mapping dans `compose.yaml` (ex: `5433:5432`) puis adapte `SPRING_DATASOURCE_URL`.
- Base non prete au demarrage: attends l'etat `healthy` du conteneur puis relance Spring Boot.
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

- http://localhost:8080/actuator/health
- http://localhost:8080/v3/api-docs