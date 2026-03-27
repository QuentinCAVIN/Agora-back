Parfait 👌 on va te faire un README **propre, clair, prêt pour ton équipe** — genre tu poses ça dans ton repo et tout le monde comprend direct 💼🔥

---

# 📘 README — Système d’Audit Intelligent (AGORA)

## 🎯 Objectif

Le système d’audit permet de **tracer automatiquement les actions métier critiques** sans polluer le code applicatif.

Il repose sur :

* une **annotation `@Audited`**
* un **Aspect AOP (`AuditAspect`)**
* un **service central (`AuditService`)**

👉 Résultat :
toutes les actions importantes sont historisées proprement.

---

# ⚙️ Fonctionnement global

```text
Controller / Service
        ↓
   @Audited
        ↓
   AuditAspect (AOP)
        ↓
   AuditService
        ↓
   Base de données (AuditLog)
```

---

# 🏷️ Annotation `@Audited`

Permet de marquer une méthode à auditer.

```java
@Audited(action = "CREATE_RESOURCE")
```

### 🔧 Options disponibles

```java
@Audited(
    action = "CREATE_RESERVATION",
    logParams = true,
    logResult = true,
    logError = true
)
```

| Paramètre   | Description                          |
| ----------- | ------------------------------------ |
| `action`    | Nom métier de l’action (obligatoire) |
| `logParams` | Log les paramètres d’entrée          |
| `logResult` | Log le résultat                      |
| `logError`  | Log les erreurs                      |

---

# 🧠 AuditAspect

L’aspect intercepte automatiquement les méthodes annotées.

### ✔️ Ce qui est capturé

* 📥 paramètres d’entrée
* 📤 résultat (optionnel)
* ❌ erreurs
* 🧵 traceId / correlationId (MDC)
* 👤 utilisateur (placeholder → JWT plus tard)

---

# 📦 Exemple d’utilisation

## Cas simple

```java
@Audited(action = "DELETE_RESOURCE")
public void delete(UUID id) {
    resourceRepository.deleteById(id);
}
```

---

## Cas avancé

```java
@Audited(
    action = "CREATE_RESERVATION",
    logParams = true,
    logResult = true
)
public Reservation create(ReservationRequest request) {
    return reservationRepository.save(...);
}
```

---

# 🗃️ Données stockées (exemple)

```json
{
  "action": "CREATE_RESERVATION",
  "adminUser": "SYSTEM",
  "details": {
    "traceId": "abc123",
    "correlationId": "xyz456",
    "params": {
      "resourceId": "123",
      "date": "2026-04-01"
    },
    "result": {
      "id": "res-789"
    }
  }
}
```

---

# ❌ Exemple en cas d’erreur

```json
{
  "action": "CREATE_RESERVATION_FAILED",
  "details": {
    "params": {...},
    "error": "Slot already taken"
  }
}
```

---

# 🔐 Sécurité & bonnes pratiques

### 🚫 Ne jamais logger :

* mots de passe
* tokens JWT
* données sensibles (PII critique)

### ✅ Bonnes pratiques :

* privilégier des actions métier claires (`CREATE_RESERVATION`)
* ne pas abuser de `logResult` (peut être lourd)
* utiliser l’audit uniquement pour les actions importantes

---

# 🧩 Intégration avec le reste du projet

Le système est conçu pour s’intégrer avec :

* 📊 `/api/admin/audit` → consultation des logs
* 🔐 Security (JWT → utilisateur réel)
* 📦 GlobalExceptionHandler → cohérence erreurs
* 📡 Monitoring / observabilité

---

# 🚀 Évolutions prévues

* 👤 récupération automatique du user via JWT
* 🔄 audit différentiel (before / after)
* 📈 export des logs (CSV / BI)
* ⚡ filtrage intelligent des champs sensibles
* 🔍 audit par type d’action (RESERVATION, PAYMENT, ADMIN)

---

# 🧠 Résumé

| Feature           | Statut |
| ----------------- | ------ |
| Audit automatique | ✅      |
| Paramètres        | ✅      |
| Résultat          | ✅      |
| Erreurs           | ✅      |
| Trace ID          | ✅      |
| Extensible        | ✅      |

---

# 💬 TL;DR

👉 Tu ajoutes juste :

```java
@Audited(action = "SOME_ACTION")
```

👉 Et ton système log tout proprement derrière.

---

Si tu veux aller encore plus loin (et là on passe en mode architecte 🧠🔥), je peux te faire :

* 🔥 Audit différentiel (ce qui a changé)
* 🔥 Audit DB automatique (Hibernate)
* 🔥 Dashboard audit temps réel

Dis-moi 😏
