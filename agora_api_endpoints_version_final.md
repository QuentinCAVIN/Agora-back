# AGORA — API Endpoints & Mock JSON (Front)

> **Base URL** : `http://localhost:8080`
> **Auth** : Bearer Token JWT via `POST /api/auth/login` — 15 min. Refresh cookie HttpOnly 7j.
> 🔒 = endpoint authentifié

---

## 0. OpenAPI (contrat pour le client)

- **JSON** : `GET /v3/api-docs` (Springdoc). Swagger UI : `/swagger-ui.html`.
- **Synchroniser le front** : dans `Agora-front`, `npm run openapi:sync` (export automatique via le test `OpenApiExportTest` du back, puis génération TypeScript). Détail : `Agora-front/docs/openapi-sync.md`.

### Évolutions récentes (cahier de suivi UI / contrat)

- **Réservations — référence métier** : chaque réservation porte un `bookingReference` unique, format **`yyMMdd` + 5 chiffres** (séquence par jour de réservation à la création / seed). Présent dans les DTO **résumé** et **détail** (`ReservationSummaryResponseDto`, `ReservationDetailResponseDto`).
- **GET `/api/admin/reservations`** : filtre `status` **répétable** (ex. `status=PENDING_VALIDATION&status=PENDING_DOCUMENT`) pour un **OU** logique côté serveur ; réponses enrichies `userName`, `userEmail`, `bookingReference` (pagination inchangée).
- **GET `/api/admin/reservations/stats`** : compteurs **globaux** (toute la base, hors pagination) pour cartes / onglets admin — voir §11.
- **GET `/api/admin/audit`** : paramètre optionnel `reservationId` pour la timeline filtrée côté serveur ; le détail des entrées peut inclure `reservationDisplayRef` (libellé court) en complément de `reservationId` (UUID).
- **Gestion utilisateurs (`/admin/users`)** : bloc d’aide « Pouvoirs administrateur » placé **au-dessus** de la barre de recherche ; recherche avec **suggestions API** (`q` + onglet courant) ; liste paginée filtrée **serveur** via `q` ; **renvoi d’activation** : modal de confirmation + message d’erreur métier si pas d’e-mail (réponse **400** attendue : *Aucun email pour renvoyer l'activation*).
- **Groupes admin** : création — champ « type » en **combobox** (liste explicite + saisie libre), normalisation **insensible à la casse / accents** pour Service, Association, Autre ; autres libellés → type `AUTRE` en base ; ajout de membres par **recherche utilisateur** (`/api/admin/users?q=`) sans saisir d’UUID.
- **Superadmin — admin support** : recherche avec **debounce** (auto-requête) ; masquage des **UUID** dans les tableaux ; bouton **Promouvoir** plus compact ; mise en page élargie.
- **Journal d’audit** : affichage de références réservation lisibles, cartes repliables, modal timeline ; correction template stricte (`bookingLabel ?? undefined`).

---

## 1. AUTH

---

### `POST /api/auth/register`
Inscription. Le compte est créé directement en `ACTIVE` et assigné au groupe Public.

**Body :**
```json
{
  "email": "jean.dupont@gmail.com",
  "password": "MonMotDePasse123!",
  "firstName": "Jean",
  "lastName": "Dupont",
  "phone": "0612345678"
}
```

**201 :**
```json
{
  "id": "u001",
  "email": "jean.dupont@gmail.com",
  "firstName": "Jean",
  "lastName": "Dupont",
  "accountType": "AUTONOMOUS",
  "status": "ACTIVE"
}
```

**409 :**
```json
{ "code": "EMAIL_ALREADY_EXISTS", "message": "Cet email est déjà associé à un compte." }
```

---

### `POST /api/auth/login`
Connexion → retourne JWT + user résumé.

**Body :**
```json
{
  "email": "jean.dupont@gmail.com",
  "password": "MonMotDePasse123!"
}
```

**200 :**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhMWIyYzNkNCIsInJvbGUiOiJVU0VSIn0.mock",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "firstName": "Jean",
    "lastName": "Dupont",
    "accountType": "AUTONOMOUS",
    "status": "ACTIVE"
  }
}
```

---

### `POST /api/auth/refresh`
Rafraîchit le token via cookie HttpOnly. Pas de body.

**200 :** même structure que login.

---

### `POST /api/auth/logout` 🔒
Déconnexion, invalide le refresh token. Pas de body.

**204 :** vide

---

### `GET /api/auth/me` 🔒
Profil complet de l'utilisateur connecté, avec ses groupes et leurs droits/tarifs.

**200 :**
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "email": "jean.dupont@gmail.com",
  "firstName": "Jean",
  "lastName": "Dupont",
  "accountType": "AUTONOMOUS",
  "status": "ACTIVE",
  "phone": "0612345678",
  "groups": [
    {
      "id": "g001",
      "name": "Habitants commune",
      "isPreset": true,
      "canBookImmobilier": true,
      "canBookMobilier": false,
      "discountType": "PERCENTAGE",
      "discountValue": 50,
      "discountAppliesTo": "ALL",
      "discountLabel": "Réduction 50%"
    }
  ],
  "createdAt": "2026-01-15T09:30:00Z"
}
```

---

### `GET /api/auth/activate?token=xxx`
Vérifie si un token d'activation (lien email) est valide.

**200 :**
```json
{ "valid": true, "targetEmail": "jean.dupont@gmail.com" }
```

---

### `POST /api/auth/activate`
L'utilisateur définit son mot de passe via le lien reçu. Connecté directement après.

**Body :**
```json
{ "token": "abc123-activation-token", "newPassword": "NouveauMdp456!" }
```

**200 :** même structure que login.

---

## 2. RESOURCES

---

### `GET /api/resources`
Catalogue public des ressources actives. Pas d'auth requise.

**Query params :** `type` (IMMOBILIER|MOBILIER), `minCapacity`, `available` (bool), `date`, `page`, `size`

**200 :**
```json
{
  "content": [
    {
      "id": "r001",
      "name": "Salle des fêtes — Grande salle",
      "resourceType": "IMMOBILIER",
      "capacity": 250,
      "description": "Grande salle pour événements jusqu'à 250 personnes",
      "depositAmountCents": 15000,
      "imageUrl": "https://mairie-exemple.fr/images/salle-fetes.jpg",
      "accessibilityTags": ["PMR_ACCESS", "PARKING", "SOUND_SYSTEM"],
      "isActive": true
    },
    {
      "id": "r002",
      "name": "Vidéoprojecteur Epson EB-X51",
      "resourceType": "MOBILIER",
      "capacity": null,
      "description": "Vidéoprojecteur portable avec câble HDMI",
      "depositAmountCents": 5000,
      "imageUrl": "https://mairie-exemple.fr/images/videoproj.jpg",
      "accessibilityTags": [],
      "isActive": true
    }
  ],
  "totalElements": 12,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

---

### `POST /api/resources` 🔒 Admin
Créer une ressource.

**Body :**
```json
{
  "name": "Salle des fêtes — Grande salle",
  "resourceType": "IMMOBILIER",
  "capacity": 250,
  "depositAmountCents": 15000,
  "description": "Grande salle pour événements jusqu'à 250 personnes",
  "imageUrl": "https://mairie-exemple.fr/images/salle-fetes.jpg",
  "accessibilityTags": ["PMR_ACCESS", "PARKING", "SOUND_SYSTEM"]
}
```

**201 :**
```json
{
  "id": "r001",
  "name": "Salle des fêtes — Grande salle",
  "resourceType": "IMMOBILIER",
  "capacity": 250,
  "description": "Grande salle pour événements jusqu'à 250 personnes",
  "depositAmountCents": 15000,
  "imageUrl": "https://mairie-exemple.fr/images/salle-fetes.jpg",
  "accessibilityTags": ["PMR_ACCESS", "PARKING", "SOUND_SYSTEM"],
  "isActive": true
}
```

---

### `GET /api/resources/{resourceId}`
Détail d'une ressource.

**200 :** même structure que ci-dessus (un seul objet).

---

### `PUT /api/resources/{resourceId}` 🔒 Admin
Modifier une ressource. Même body que POST.

**200 :** `ResourceDto` mis à jour.

---

### `DELETE /api/resources/{resourceId}` 🔒 Admin
Soft delete (passe `isActive` à false).

**204 :** vide

---

### `GET /api/resources/{resourceId}/slots?date=2026-04-10`
Créneaux dispos pour une ressource à une date donnée.

**200 :**
```json
[
  { "slotStart": "08:00", "slotEnd": "10:00", "isAvailable": true },
  { "slotStart": "10:00", "slotEnd": "12:00", "isAvailable": false },
  { "slotStart": "14:00", "slotEnd": "16:00", "isAvailable": true },
  { "slotStart": "16:00", "slotEnd": "18:00", "isAvailable": true }
]
```

---

### `GET /api/calendar?year=2026&month=4`
Vue calendrier mensuelle : créneaux + fermetures, toutes ressources.

**200 :**
```json
{
  "year": 2026,
  "month": 4,
  "days": [
    {
      "date": "2026-04-10",
      "isBlackout": false,
      "blackoutReason": null,
      "slots": [
        {
          "resourceId": "r001",
          "resourceName": "Salle des fêtes — Grande salle",
          "resourceType": "IMMOBILIER",
          "slotStart": "08:00",
          "slotEnd": "10:00",
          "isAvailable": true
        }
      ]
    },
    {
      "date": "2026-04-14",
      "isBlackout": true,
      "blackoutReason": "Jour férié",
      "slots": []
    }
  ]
}
```

---

## 3. RESERVATIONS

---

### `GET /api/reservations` 🔒
Mes réservations (paginé).

**Query params :** `status` (PENDING_VALIDATION|CONFIRMED|CANCELLED|REJECTED|PENDING_DOCUMENT), `page`, `size`

**200 :**
```json
{
  "content": [
    {
      "id": "res001",
      "resourceName": "Salle des fêtes — Grande salle",
      "resourceType": "IMMOBILIER",
      "date": "2026-04-10",
      "slotStart": "14:00",
      "slotEnd": "18:00",
      "status": "CONFIRMED",
      "depositStatus": "DEPOSIT_PENDING",
      "depositAmountCents": 7500,
      "depositAmountFullCents": 15000,
      "discountLabel": "Réduction 50% (Habitants commune)",
      "createdAt": "2026-03-24T10:30:00Z",
      "userName": "Jean Dupont",
      "recurringGroupId": null,
      "bookingReference": "26041000001",
      "userEmail": "jean.dupont@gmail.com"
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

> Champs **résumé** : `userName` (prénom + nom, ou email si nom vide), `userEmail`, `bookingReference`, `recurringGroupId` (séries).

---

### `POST /api/reservations` 🔒
Créer une réservation. Tarif auto-calculé selon les groupes de l'utilisateur.

**Body :**
```json
{
  "resourceId": "r001",
  "date": "2026-04-10",
  "slotStart": "14:00",
  "slotEnd": "18:00",
  "purpose": "Réunion associative mensuelle",
  "groupId": null
}
```

> `groupId` : null = pour soi, renseigné = au nom du groupe.

**201 :**
```json
{
  "id": "res002",
  "resourceName": "Salle des fêtes — Grande salle",
  "resourceType": "IMMOBILIER",
  "date": "2026-04-10",
  "slotStart": "14:00",
  "slotEnd": "18:00",
  "status": "CONFIRMED",
  "depositStatus": "DEPOSIT_PENDING",
  "depositAmountCents": 7500,
  "depositAmountFullCents": 15000,
  "discountLabel": "Réduction 50% (Habitants commune)",
  "createdAt": "2026-03-24T11:00:00Z",
  "resource": {
    "id": "r001",
    "name": "Salle des fêtes — Grande salle",
    "resourceType": "IMMOBILIER",
    "capacity": 250,
    "depositAmountCents": 15000,
    "imageUrl": "https://mairie-exemple.fr/images/salle-fetes.jpg"
  },
  "userName": "Jean Dupont",
  "groupName": "Habitants commune",
  "purpose": "Réunion associative mensuelle",
  "documents": [],
  "recurringGroupId": null,
  "bookingReference": "26041000001",
  "userEmail": "jean.dupont@gmail.com"
}
```

**403 :**
```json
{ "code": "RESERVATION_FORBIDDEN_NO_GROUP", "message": "Aucun de vos groupes n'autorise la réservation de ressources MOBILIER." }
```

**409 :**
```json
{ "code": "SLOT_UNAVAILABLE", "message": "Le créneau 14h00-18h00 du 10/04/2026 est déjà réservé." }
```

---

### `GET /api/reservations/{reservationId}` 🔒
Détail complet d'une réservation.

**200 :** même structure que la réponse 201 du POST ci-dessus.

---

### `DELETE /api/reservations/{reservationId}` 🔒
Annuler une réservation.

**204 :** vide

---

### `POST /api/reservations/recurring` 🔒
Créer une série récurrente (hebdo, bi-hebdo, mensuel).

**Body :**
```json
{
  "resourceId": "r001",
  "slotStart": "18:00",
  "slotEnd": "20:00",
  "purpose": "Cours de yoga hebdomadaire",
  "groupId": "g002",
  "frequency": "WEEKLY",
  "startDate": "2026-04-01",
  "endDate": "2026-06-30",
  "excludedDates": ["2026-05-01", "2026-05-08"]
}
```

**201 :** tableau de `ReservationSummaryDto`
```json
[
  {
    "id": "res010",
    "resourceName": "Salle des fêtes — Grande salle",
    "resourceType": "IMMOBILIER",
    "date": "2026-04-01",
    "slotStart": "18:00",
    "slotEnd": "20:00",
    "status": "CONFIRMED",
    "depositStatus": "EXEMPT",
    "depositAmountCents": 0,
    "depositAmountFullCents": 15000,
    "discountLabel": "Exonération totale (Association locale)",
    "createdAt": "2026-03-24T12:00:00Z",
    "userName": "Jean Dupont",
    "recurringGroupId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "bookingReference": "26040100001",
    "userEmail": "jean.dupont@gmail.com"
  },
  {
    "id": "res011",
    "resourceName": "Salle des fêtes — Grande salle",
    "resourceType": "IMMOBILIER",
    "date": "2026-04-08",
    "slotStart": "18:00",
    "slotEnd": "20:00",
    "status": "CONFIRMED",
    "depositStatus": "EXEMPT",
    "depositAmountCents": 0,
    "depositAmountFullCents": 15000,
    "discountLabel": "Exonération totale (Association locale)",
    "createdAt": "2026-03-24T12:00:00Z",
    "userName": "Jean Dupont",
    "recurringGroupId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "bookingReference": "26040800001",
    "userEmail": "jean.dupont@gmail.com"
  }
]
```

---

### `GET /api/reservations/recurring/{recurringGroupId}` 🔒
Occurrences d'une série.

**200 :** même tableau de `ReservationSummaryDto`.

---

### `DELETE /api/reservations/recurring/{recurringGroupId}` 🔒
Annule les occurrences futures.

**204 :** vide

---

## 4. DOCUMENTS

---

### `POST /api/reservations/{reservationId}/documents` 🔒
Upload PJ. **Cahier :** zéro stockage binaire applicatif — le fichier doit être relayé vers un canal d’envoi (ex. Brevo). **Implémentation actuelle :** validation taille (max 5 Mo), types MIME autorisés, réservation appartenant à l’utilisateur authentifié ; persistance des **métadonnées** en base (`reservation_documents`) ; relayage via `ReservationAttachmentRelay` (**simulation** `BREVO_SIMULATED` : logs applicatifs `PJ_RELAY` + idempotence documentée sur `reservationDocumentId`) ; **journal d’audit** ligne `RESERVATION_DOCUMENT_UPLOADED` dans `audit_logs` (détails JSON : ids, `relayChannel`, etc.). Voir tests `ReservationDocument*`.

**Body :** `multipart/form-data`
- `file` : binaire (PDF, JPEG, PNG, DOC, DOCX, XLS, XLSX)
- `docType` : `ASSOCIATION_PROOF` | `IDENTITY` | `SOCIAL_PROOF` | `MANDATE_PROOF` | `WORD_EXCEL_DOC` | `OTHER`

**201 :**
```json
{
  "id": "doc001",
  "docType": "ASSOCIATION_PROOF",
  "originalFilename": "statuts_association.pdf",
  "mimeType": "application/pdf",
  "sizeBytes": 245760,
  "status": "SENT",
  "sentAt": "2026-03-24T14:00:00Z"
}
```

**400 :**
```json
{ "code": "INVALID_MIME_TYPE", "message": "Format non accepté : application/x-msdownload" }
```

---

## 5. GROUPS (utilisateur)

---

### `GET /api/groups` 🔒
Liste des groupes visibles par l'utilisateur connecté.

**200 :**
```json
[
  {
    "id": "g001",
    "name": "Habitants commune",
    "isPreset": true,
    "canBookImmobilier": true,
    "canBookMobilier": false,
    "discountType": "PERCENTAGE",
    "discountValue": 50,
    "discountAppliesTo": "ALL",
    "discountLabel": "Réduction 50%",
    "memberCount": 124
  },
  {
    "id": "g002",
    "name": "Association sportive locale",
    "isPreset": false,
    "canBookImmobilier": true,
    "canBookMobilier": true,
    "discountType": "FULL_EXEMPT",
    "discountValue": 0,
    "discountAppliesTo": "ALL",
    "discountLabel": "Exonération totale",
    "memberCount": 18
  }
]
```

---

### `GET /api/groups/{groupId}` 🔒
Détail d'un groupe avec ses membres.

**200 :**
```json
{
  "id": "g001",
  "name": "Habitants commune",
  "isPreset": true,
  "canBookImmobilier": true,
  "canBookMobilier": false,
  "discountType": "PERCENTAGE",
  "discountValue": 50,
  "discountAppliesTo": "ALL",
  "discountLabel": "Réduction 50%",
  "memberCount": 124,
  "members": [
    {
      "firstName": "Jean",
      "lastName": "Dupont",
      "role": "MEMBER",
      "joinedAt": "2026-01-15T09:30:00Z"
    },
    {
      "firstName": "Sophie",
      "lastName": "Bernard",
      "role": "MANAGER",
      "joinedAt": "2026-02-01T10:00:00Z"
    }
  ],
  "createdByName": "Système",
  "createdAt": "2025-06-01T00:00:00Z"
}
```

---

## 6. WAITLIST

---

### `GET /api/waitlist` 🔒
Mes inscriptions en liste d'attente.

**200 :**
```json
[
  {
    "id": "w001",
    "resourceName": "Salle des fêtes — Grande salle",
    "slotDate": "2026-04-15",
    "slotStart": "14:00",
    "slotEnd": "18:00",
    "position": 2,
    "status": "WAITING",
    "notifiedAt": null
  }
]
```

---

### `POST /api/waitlist` 🔒
S'inscrire en file d'attente sur un créneau occupé.

**Body :**
```json
{
  "resourceId": "r001",
  "slotDate": "2026-04-15",
  "slotStart": "14:00",
  "slotEnd": "18:00"
}
```

**201 :**
```json
{
  "id": "w002",
  "resourceName": "Salle des fêtes — Grande salle",
  "slotDate": "2026-04-15",
  "slotStart": "14:00",
  "slotEnd": "18:00",
  "position": 3,
  "status": "WAITING",
  "notifiedAt": null
}
```

---

### `DELETE /api/waitlist/{waitlistId}` 🔒
Se retirer de la liste.

**204 :** vide

---

## 7. ADMIN — ACCOUNTS

---

### `GET /api/admin/users` 🔒 Admin
Tous les comptes, paginé.

**Query params :** `status`, `accountType` (AUTONOMOUS|TUTORED), `page`, `size`

**200 :**
```json
{
  "content": [
    {
      "id": "u001",
      "email": "jean.dupont@gmail.com",
      "firstName": "Jean",
      "lastName": "Dupont",
      "accountType": "AUTONOMOUS",
      "status": "ACTIVE",
      "phone": "0612345678",
      "createdAt": "2026-01-15T09:30:00Z"
    },
    {
      "id": "u002",
      "email": null,
      "firstName": "Germaine",
      "lastName": "Perrier",
      "accountType": "TUTORED",
      "status": "ACTIVE",
      "internalRef": "PERR-1948-042",
      "phone": "0556781234",
      "notesAdmin": "Accompagnée par le CCAS",
      "createdAt": "2026-02-10T11:00:00Z"
    }
  ],
  "totalElements": 87,
  "totalPages": 5
}
```

---

### `GET /api/admin/users/{userId}` 🔒 Admin
Détail complet d'un compte (inclut groupes).

**200 :**
```json
{
  "id": "u001",
  "email": "jean.dupont@gmail.com",
  "firstName": "Jean",
  "lastName": "Dupont",
  "accountType": "AUTONOMOUS",
  "status": "ACTIVE",
  "phone": "0612345678",
  "notesAdmin": null,
  "groups": [
    { "id": "g001", "name": "Habitants commune", "discountLabel": "Réduction 50%" }
  ],
  "createdAt": "2026-01-15T09:30:00Z"
}
```

---

### `POST /api/admin/users/tutored` 🔒 Admin
Créer un compte sous tutelle (sans email/mdp). Identifiant interne auto-généré. Groupe Public auto-attribué.

**Body :**
```json
{
  "firstName": "Germaine",
  "lastName": "Perrier",
  "birthYear": 1948,
  "phone": "0556781234",
  "notesAdmin": "Personne âgée, accompagnée par le CCAS"
}
```

**201 :**
```json
{
  "id": "u002",
  "firstName": "Germaine",
  "lastName": "Perrier",
  "accountType": "TUTORED",
  "status": "ACTIVE",
  "internalRef": "PERR-1948-042",
  "phone": "0556781234",
  "notesAdmin": "Personne âgée, accompagnée par le CCAS",
  "groups": [
    { "id": "g000", "name": "Public", "discountLabel": "Plein tarif" }
  ],
  "createdAt": "2026-03-24T14:30:00Z"
}
```

---

### `PATCH /api/admin/users/{userId}/tutored` 🔒 Admin
Modifier la fiche d'un compte tutelle. Même body que création.

**200 :** profil mis à jour.

---

### `POST /api/admin/users/{userId}/activate-autonomous` 🔒 Admin
Passer un compte tutelle → autonome. Envoie un email d'invitation (lien valide 72h).

**Body :**
```json
{ "email": "germaine.perrier@gmail.com" }
```

**200 :** confirmation.

---

### `POST /api/admin/users/{userId}/resend-activation` 🔒 Admin
Renvoyer le lien d'activation (invalide l'ancien). Pas de body.

**200 :** confirmation.

---

### `POST /api/admin/users/{userId}/impersonate` 🔒 Admin
Ouvrir une session d'impersonation sur un compte TUTORED. Token court 30 min. Toutes les actions sont journalisées dans l'audit.

**200 :**
```json
{
  "sessionToken": "eyJ...impersonation_token",
  "targetUser": {
    "firstName": "Germaine",
    "lastName": "Perrier",
    "accountType": "TUTORED",
    "internalRef": "PERR-1948-042"
  },
  "expiresAt": "2026-03-24T15:30:00Z"
}
```

**403 :**
```json
{ "code": "IMPERSONATION_FORBIDDEN", "message": "L'impersonation est réservée aux comptes de type TUTORED." }
```

---

### `GET /api/admin/users/{userId}/print-summary` 🔒 Admin
Fiche PDF imprimable. Retourne un `application/pdf` binaire (téléchargement ou nouvel onglet).

---

## 8. ADMIN — GROUPS (SUPERADMIN + ADMIN_SUPPORT)

---

### `GET /api/admin/groups` 🔒
Liste tous les groupes avec leur configuration.

**200 :**
```json
[
  {
    "id": "g000",
    "name": "Public",
    "isPreset": true,
    "canBookImmobilier": false,
    "canBookMobilier": false,
    "discountType": "NONE",
    "discountValue": 0,
    "memberCount": 87
  },
  {
    "id": "g001",
    "name": "Habitants commune",
    "isPreset": true,
    "canBookImmobilier": true,
    "canBookMobilier": false,
    "discountType": "PERCENTAGE",
    "discountValue": 50,
    "memberCount": 124
  },
  {
    "id": "g003",
    "name": "Conseillers municipaux",
    "isPreset": true,
    "canBookImmobilier": true,
    "canBookMobilier": true,
    "discountType": "FULL_EXEMPT",
    "discountValue": 0,
    "memberCount": 15
  },
  {
    "id": "g004",
    "name": "Personnel mairie",
    "isPreset": false,
    "canBookImmobilier": true,
    "canBookMobilier": true,
    "discountType": "PERCENTAGE",
    "discountValue": 30,
    "memberCount": 8
  }
]
```

---

### `POST /api/admin/groups` 🔒
Créer un groupe custom (`isPreset=false`, supprimable).

**Body :**
```json
{
  "name": "Personnel mairie",
  "canViewImmobilier": true,
  "canBookImmobilier": true,
  "canViewMobilier": true,
  "canBookMobilier": true,
  "discountType": "PERCENTAGE",
  "discountValue": 30,
  "discountAppliesTo": "ALL"
}
```

**201 :**
```json
{
  "id": "g004",
  "name": "Personnel mairie",
  "isPreset": false,
  "canViewImmobilier": true,
  "canBookImmobilier": true,
  "canViewMobilier": true,
  "canBookMobilier": true,
  "discountType": "PERCENTAGE",
  "discountValue": 30,
  "discountAppliesTo": "ALL",
  "discountLabel": "Réduction 30%",
  "memberCount": 0,
  "members": [],
  "createdByName": "Admin Dupont",
  "createdAt": "2026-03-24T16:00:00Z"
}
```

---

### `GET /api/admin/groups/{groupId}` 🔒
Détail complet + membres.

**200 :** même structure que 201 du POST, avec `members` peuplé (voir section Groups utilisateur).

---

### `PUT /api/admin/groups/{groupId}` 🔒
Modifier un groupe. Même body que POST.

**200 :** `GroupDetailDto` mis à jour.

---

### `DELETE /api/admin/groups/{groupId}` 🔒
Supprimer un groupe. Impossible si `isPreset=true`.

**204 :** vide

**409 :**
```json
{ "code": "PRESET_GROUP_DELETION_FORBIDDEN", "message": "Les groupes prédéfinis ne peuvent pas être supprimés." }
```

---

### `GET /api/admin/groups/{groupId}/members` 🔒
Liste des membres d'un groupe.

**200 :**
```json
[
  {
    "userId": "u001",
    "firstName": "Jean",
    "lastName": "Dupont",
    "email": "jean.dupont@gmail.com",
    "role": "MEMBER",
    "joinedAt": "2026-01-15T09:30:00Z"
  },
  {
    "userId": "u005",
    "firstName": "Sophie",
    "lastName": "Bernard",
    "email": "sophie.bernard@gmail.com",
    "role": "MANAGER",
    "joinedAt": "2026-02-01T10:00:00Z"
  }
]
```

---

### `POST /api/admin/groups/{groupId}/members` 🔒
Affecter un utilisateur à un groupe.

**Body :**
```json
{ "userId": "u001", "role": "MEMBER" }
```

**201 :** vide

**409 :**
```json
{ "code": "ALREADY_MEMBER", "message": "Cet utilisateur est déjà membre de ce groupe." }
```

---

### `DELETE /api/admin/groups/{groupId}/members/{userId}` 🔒
Retirer un membre.

**204 :** vide

---

## 9. ADMIN — PAYMENTS

---

### `GET /api/admin/payments` 🔒 Admin
Liste des cautions (paginé).

**Query params :** `status`, `dateFrom`, `dateTo`, `page`, `size`

**200 :**
```json
{
  "content": [
    {
      "reservationId": "res001",
      "status": "DEPOSIT_PENDING",
      "amountCents": 7500,
      "paymentMode": null,
      "updatedByName": "Système",
      "updatedAt": "2026-03-24T10:30:00Z"
    },
    {
      "reservationId": "res003",
      "status": "DEPOSIT_PAID",
      "amountCents": 15000,
      "paymentMode": "CHECK",
      "checkNumber": "1234567",
      "comment": "Chèque à l'ordre de la commune",
      "updatedByName": "Marie Secrétaire",
      "updatedAt": "2026-03-20T14:30:00Z"
    }
  ],
  "totalElements": 24,
  "totalPages": 2
}
```

---

### `PATCH /api/admin/payments/{reservationId}` 🔒 Admin
Mettre à jour le statut caution (guichet physique). `paymentMode` obligatoire si `status=DEPOSIT_PAID`.

**Body (espèces) :**
```json
{ "status": "DEPOSIT_PAID", "amountCents": 5000, "paymentMode": "CASH", "comment": "Réglé au guichet" }
```

**Body (chèque) :**
```json
{ "status": "DEPOSIT_PAID", "amountCents": 15000, "paymentMode": "CHECK", "checkNumber": "1234567" }
```

**Body (remboursement) :**
```json
{ "status": "REFUNDED", "amountCents": 5000, "comment": "Annulation — remboursé en espèces" }
```

**200 :**
```json
{
  "reservationId": "res001",
  "status": "DEPOSIT_PAID",
  "amountCents": 5000,
  "paymentMode": "CASH",
  "comment": "Réglé au guichet",
  "updatedByName": "Marie Secrétaire",
  "updatedAt": "2026-03-24T16:00:00Z"
}
```

**409 :**
```json
{ "code": "INVALID_STATUS_TRANSITION", "message": "Transition REFUNDED → DEPOSIT_PAID non autorisée." }
```

---

### `GET /api/admin/payments/{reservationId}/history` 🔒 Admin
Historique complet de la caution (insert-only, par date desc).

**200 :**
```json
[
  {
    "status": "DEPOSIT_PAID",
    "amountCents": 5000,
    "paymentMode": "CASH",
    "comment": "Réglé au guichet",
    "updatedByName": "Marie Secrétaire",
    "updatedAt": "2026-03-24T16:00:00Z"
  },
  {
    "status": "DEPOSIT_PENDING",
    "amountCents": 5000,
    "paymentMode": null,
    "updatedByName": "Système",
    "updatedAt": "2026-03-24T10:30:00Z"
  }
]
```

---

## 10. ADMIN — AUDIT

---

### `GET /api/admin/audit` 🔒 Admin
Journal d'audit complet.

**Query params :** `adminUserId` (email, sous-chaîne ou UUID utilisateur → email résolu), `targetUserId` (idem), `impersonationOnly` (`true` = uniquement lignes impersonation), `dateFrom` / `dateTo` (ISO date, fuseau Europe/Paris pour le bornage), `page`, `size`

**Rôles :** `SUPERADMIN`, `SECRETARY_ADMIN`, `ADMIN_SUPPORT` (identique aux autres routes `/api/admin/**`).

**200 :**
```json
{
  "content": [
    {
      "id": "audit001",
      "adminName": "Marie Secrétaire",
      "targetName": "Germaine Perrier",
      "action": "RESERVATION_CREATED",
      "details": { "resourceName": "Salle polyvalente" },
      "isImpersonation": true,
      "performedAt": "2026-03-24T15:15:00Z"
    }
  ],
  "totalElements": 156,
  "totalPages": 8
}
```

---

## 11. ADMIN — CONFIG

---

### `GET /api/admin/reservations` 🔒 Admin
Toutes les réservations (vue admin), **pagination serveur**.

**Query params :**
- `status` — **optionnel**, **répétable** : chaque valeur est un `ReservationStatus`. Si plusieurs paramètres `status` sont passés (ex. filtre UI « en attente »), le serveur applique un **OU** (`IN (...)`). Une seule valeur = filtre simple.
- `resourceId`, `dateFrom`, `dateTo`, `page`, `size`

**Rôles :** `SUPERADMIN`, `SECRETARY_ADMIN`, `ADMIN_SUPPORT`.

**200 :** même enveloppe paginée que `GET /api/reservations` ; chaque élément de `content` inclut notamment :
- `userName` — libellé affichable (nom + prénom, ou email si besoin),
- `userEmail` — email du réservateur si connu,
- `bookingReference` — référence métier (`yyMMdd` + 5 chiffres).

Exemple (extrait) :
```json
{
  "content": [
    {
      "id": "07cd5824-e9e5-412e-9340-b0204546bf7e",
      "resourceName": "Salle polyvalente — Petite salle",
      "resourceType": "IMMOBILIER",
      "date": "2026-04-15",
      "slotStart": "00:00",
      "slotEnd": "00:00",
      "status": "CONFIRMED",
      "depositStatus": "DEPOSIT_PENDING",
      "depositAmountCents": 8000,
      "depositAmountFullCents": 8000,
      "discountLabel": "Aucune remise",
      "createdAt": "2026-04-08T10:00:00Z",
      "userName": "Camille Martin",
      "recurringGroupId": null,
      "bookingReference": "26041500003",
      "userEmail": "camille.martin@example.com"
    }
  ],
  "totalElements": 42,
  "totalPages": 5,
  "page": 0,
  "size": 10
}
```

---

### `GET /api/admin/reservations/stats` 🔒 Admin
Compteurs **globaux** sur toutes les réservations (indépendants de la pagination de la liste).

**Rôles :** `SUPERADMIN`, `SECRETARY_ADMIN`, `ADMIN_SUPPORT`.

**200 :**
```json
{
  "total": 42,
  "pendingGroup": 5,
  "confirmed": 28,
  "cancelled": 6,
  "rejected": 1,
  "depositPending": 12,
  "exemptOrWaived": 4
}
```

| Champ | Signification |
|-------|----------------|
| `total` | Nombre total de réservations |
| `pendingGroup` | `PENDING_VALIDATION` ou `PENDING_DOCUMENT` |
| `confirmed` / `cancelled` / `rejected` | Par statut métier |
| `depositPending` | Caution encore `DEPOSIT_PENDING` |
| `exemptOrWaived` | `EXEMPT` ou `WAIVED` |

---

### `PATCH /api/admin/reservations/{reservationId}/status` 🔒 Admin
Changer le statut d'une réservation.

**Body :**
```json
{ "status": "CONFIRMED", "comment": "Pièces vérifiées" }
```

**200 :** confirmation.

---

### `GET /api/admin/blackouts` 🔒 Admin
Périodes de fermeture.

**200 :**
```json
[
  {
    "id": "blk001",
    "resourceId": null,
    "resourceName": null,
    "dateFrom": "2026-12-24",
    "dateTo": "2026-12-26",
    "reason": "Fermeture fêtes de Noël",
    "createdByName": "Marie Secrétaire"
  },
  {
    "id": "blk002",
    "resourceId": "r001",
    "resourceName": "Salle des fêtes — Grande salle",
    "dateFrom": "2026-05-10",
    "dateTo": "2026-05-12",
    "reason": "Travaux de peinture",
    "createdByName": "Marie Secrétaire"
  }
]
```

---

### `POST /api/admin/blackouts` 🔒 Admin
Créer une fermeture. `resourceId: null` = fermeture globale.

**Body :**
```json
{
  "resourceId": null,
  "dateFrom": "2026-12-24",
  "dateTo": "2026-12-26",
  "reason": "Fermeture fêtes de Noël"
}
```

**201 :** `BlackoutPeriodDto`.

---

### `DELETE /api/admin/blackouts/{blackoutId}` 🔒 Admin

**204 :** vide

---

### `GET /api/admin/stats/dashboard` 🔒 Admin
Métriques du tableau de bord.

**200 :**
```json
{
  "todayReservations": 12,
  "pendingDeposits": 8,
  "pendingDocuments": 3,
  "tutoredAccounts": 14,
  "totalGroups": 6
}
```

---

### `GET /api/admin/exports/reservations?dateFrom=2026-01-01&dateTo=2026-03-31` 🔒 Admin
Export CSV des réservations. Retourne `text/csv`.

---

### `GET /api/admin/exports/payments?dateFrom=2026-01-01&dateTo=2026-03-31` 🔒 Admin
Export CSV des cautions. Retourne `text/csv`.

---

## 12. SUPERADMIN

---

### `GET /api/superadmin/admin-support` 🔒 SUPERADMIN
Liste des ADMIN_SUPPORT actifs.

**200 :**
```json
[
  {
    "id": "u010",
    "email": "paul.assiste@mairie.fr",
    "firstName": "Paul",
    "lastName": "Assisté",
    "status": "ACTIVE"
  }
]
```

---

### `POST /api/superadmin/admin-support` 🔒 SUPERADMIN
Promouvoir un USER actif en ADMIN_SUPPORT.

**Body :**
```json
{ "userId": "u001" }
```

**201 :** profil utilisateur avec rôle mis à jour.

**409 :** déjà ADMIN_SUPPORT.

---

### `DELETE /api/superadmin/admin-support/{userId}` 🔒 SUPERADMIN
Révoquer le rôle ADMIN_SUPPORT.

**204 :** vide

---

## ANNEXES

### Enums

| Enum | Valeurs |
|------|---------|
| ResourceType | `IMMOBILIER` `MOBILIER` |
| AccountType | `AUTONOMOUS` `TUTORED` |
| AccountStatus | `ACTIVE` `INACTIVE` `SUSPENDED` |
| ReservationStatus | `PENDING_VALIDATION` `CONFIRMED` `CANCELLED` `REJECTED` `PENDING_DOCUMENT` |
| DepositStatus | `DEPOSIT_PENDING` `DEPOSIT_PAID` `EXEMPT` `WAIVED` `REFUNDED` |
| PaymentMode | `CASH` `CHECK` |
| MemberRole | `MEMBER` `MANAGER` |
| DocType | `ASSOCIATION_PROOF` `IDENTITY` `SOCIAL_PROOF` `MANDATE_PROOF` `WORD_EXCEL_DOC` `OTHER` |
| DiscountType | `NONE` `PERCENTAGE` `FIXED_AMOUNT` `FULL_EXEMPT` |
| DiscountAppliesTo | `ALL` `IMMOBILIER_ONLY` `MOBILIER_ONLY` |
| WaitlistStatus | `WAITING` `NOTIFIED` `CONFIRMED` `EXPIRED` |
| RecurrenceFrequency | `WEEKLY` `BIWEEKLY` `MONTHLY` |

### Codes erreur

| Code | HTTP | Quand |
|------|------|-------|
| EMAIL_ALREADY_EXISTS | 409 | Inscription avec email existant |
| SLOT_UNAVAILABLE | 409 | Créneau déjà pris |
| RESERVATION_FORBIDDEN_NO_GROUP | 403 | Aucun groupe n'autorise cette catégorie |
| ALREADY_MEMBER | 409 | Utilisateur déjà dans le groupe |
| PRESET_GROUP_DELETION_FORBIDDEN | 409 | Tentative de supprimer un groupe prédéfini |
| SUPERADMIN_ALREADY_EXISTS | 409 | Un SUPERADMIN existe déjà |
| IMPERSONATION_FORBIDDEN | 403 | Cible n'est pas TUTORED |
| INVALID_STATUS_TRANSITION | 409 | Transition caution interdite |
| INVALID_MIME_TYPE | 400 | Type fichier non accepté |

### Calcul tarifaire multi-groupes

- **Droits** : union — si UN groupe autorise, c'est autorisé
- **Tarif** : le plus avantageux gagne → `FULL_EXEMPT` > `PERCENTAGE` (le + fort) > `FIXED_AMOUNT` (le + fort) > `NONE`

### Transitions caution autorisées

`DEPOSIT_PENDING` → `DEPOSIT_PAID` | `EXEMPT` | `WAIVED`
`DEPOSIT_PAID` → `REFUNDED`
