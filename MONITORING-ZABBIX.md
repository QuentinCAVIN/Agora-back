# 📡 Monitoring Zabbix — AGORA Backend

Ce projet expose des métriques via un agent Zabbix.

Les triggers (alertes) doivent être configurés côté serveur Zabbix.

---

## 🧠 Principe

- L'application expose des métriques via l'agent Zabbix (`UserParameter`)
- Zabbix récupère ces métriques
- Les **triggers permettent de déclencher des alertes**

---

## ⚙️ Prérequis

- Le conteneur `zabbix-agent` doit être lancé
- Le port `10050` doit être accessible
- Le host doit être créé dans Zabbix avec :

---

## 🔧 Étapes de configuration

### 1. Accéder au host

Dans l'interface Zabbix :

---

### 2. Vérifier les items

Aller dans :

Vous devez voir :

- `api.health`
- `api.status`
- `api.response_time`
- `db.ping`
- `agora.groups.count`
- `agora.reservations.count`

---

### 3. Créer les triggers

Aller dans :
Triggers → Create Trigger
---

## 🚨 Triggers recommandés

---

### 🔴 API DOWN

**Name** 
API AGORA DOWN
**Expression**
{agora-app:api.health.last()}=0

**Severity**
High
---

### 🔴 DATABASE DOWN

**Name**
DATABASE DOWN

**Expression**
{agora-app:db.ping.last()}=0

**Severity**
Disaster

---

### 🟠 API HTTP ERROR

**Name**
API HTTP ERROR

**Expression**
{agora-app:api.status.last()}<>200

**Severity**
High

---

### 🟠 API LENTE

**Name**

API RESPONSE SLOW


**Expression**

{agora-app:api.response_time.last()}>0.5


**Severity**

Warning

---

### 🟡 ANOMALIE MÉTIER — GROUPES

**Name**

NO GROUPS FOUND


**Expression**

{agora-app:agora.groups.count.last()}=0


**Severity**

Average


---

### 🟡 ANOMALIE MÉTIER — RÉSERVATIONS

**Name**

NO RESERVATIONS FOUND


**Expression**

{agora-app:agora.reservations.count.last()}=0


**Severity**

Average


---

## 📊 Résumé

| Élément | Localisation |
|--------|-------------|
| Métriques | Zabbix Agent |
| Items | Zabbix UI |
| Triggers | Zabbix UI |

---

## 🧠 Choix techniques

Les triggers sont définis côté Zabbix pour :

- séparer la supervision de l'application
- permettre une adaptation sans modification du code
- centraliser la gestion des alertes

---

## 🚀 Résultat attendu

Une fois configuré :

- Zabbix détecte automatiquement :
    - API down
    - base de données indisponible
    - lenteurs
    - incohérences métier

---

