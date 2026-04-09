# Cahier d'alignement — Grille d'évaluation & énoncé AGORA

Document de synthèse pour la **coordination front/back**, l'**état d'avancement** par rapport aux **jalons J1–J8** et aux **objectifs pédagogiques OP1–OP8**. Mis à jour pour refléter le code tel qu'observable dans les dépôts **Agora-back** et **Agora-front** (et les travaux récents : admin réservations, références métier, stats globales, etc.).

**Données** : fictives, sans traitement réel de données personnelles (recommandation PDF soutenance).

---

## 1. État global (lecture « formateur »)

| Zone | Lecture honnête |
|------|-----------------|
| **Cœur technique** | Stack sérieuse : Spring Boot + Angular, Flyway, DTO, séparation controller/service/repository, tests back (unitaires + intégration sur plusieurs flux). |
| **Contrat API** | OpenAPI / client généré côté front (modèles dans `core/api`) — **la synchronisation manuelle** quand le back évolue reste un point de vigilance (régénérer ou documenter les écarts). |
| **J7 (CI deux dépôts)** | **Back** : workflow GitHub Actions (`build`, tests). **Front** : pas de fichier CI repéré à la racine du dépôt — **risque sur le critère « CI sur deux dépôts »** si non assuré ailleurs (GitLab, autre pipeline). |
| **Processus / livrables groupe** | Org Git, calendrier, PDF avant le 10 avril, variantes A/B/C, revues C1–C5 : **non vérifiables depuis le code** ; la charge de preuve reste sur le board, les PR et les mails au formateur. |

**Conclusion franche** : le projet **couvre largement le périmètre métier et technique** attendu pour une soutenance « mairie » crédible, avec des zones fortes (réservations, admin, audit, sécurité JWT). Pour **maximiser la note**, il faut surtout **fermer les preuves process** (CI front, OpenAPI figée, démo scriptée, PDF/calendrier) et **anticiper les questions** sur tout ce qui est partiel ou optionnel (Stripe, E2E complet, mail PJ bout-en-bout en prod de démo).

---

## 2. Jalons J1–J8 — couverture indicative

Notation indicative : **S** = preuve forte dans les dépôts, **P** = partiel ou à documenter en soutenance, **I** = manquant ou non vérifié ici.

| Jalon | Attendu (rappel) | État projet |
|-------|------------------|-------------|
| **J1** Mise en route | Org Git, calendrier, variante, repos, brouillon OpenAPI | **P/S** repos + spec API présents ; **P** reste preuve e-mail / calendrier / variante hors repo. |
| **J2** Fondations | Auth/démo, CRUD/health, CI, README | **S** auth, entités, migrations ; **S** CI back ; **P** README front à croiser ; **P** CI front. |
| **J3** Cœur réservation | API réservations + catalogue, écran front | **S** réservations, ressources, calendrier / parcours citoyen. |
| **J4** Groupes | Adhésions, 3 flags exo., résa soi vs groupe | **S/P** modèle `GroupMembership`, logique réservation liée aux groupes ; **P** expliciter les **3 flags** dans doc/API/UI pour le jury. |
| **J5** Fichiers & mail | Upload → SMTP → suppression + preuve mail | **P/S** flux PJ réservation + tests d'intégration repérables ; **P** preuve SMTP réelle / capture / scénario pour le jury. |
| **J6** Administration | Validation compte, caution/paiement, CRUD exigences PJ | **S** parcours admin réservations, patch statuts, paiements ; **P** boucler « validation compte » + **CRUD exigences PJ** si pas tous les écrans finis. |
| **J7** Qualité transverse | CI ×2, impersonation + audit, intégration/E2E | **S** audit, traces impersonation (MDC) ; **S** tests intégration back ; **P** CI sur **deux** dépôts ; **P** E2E front « un flux critique » (Playwright/Cypress ou script recette). |
| **J8** Finition | README bout-en-bout, OpenAPI à jour, démo | **P** consolider README lancement conjoint + lien OpenAPI + **script de démo** ; préparer répartition **C5** (qui parle de quoi). |

---

## 3. Objectifs pédagogiques OP1–OP8 (couverture)

| OP | Synthèse |
|----|----------|
| **OP1** Coordination | Daily/board/mails : **hors code**. Côté technique : évolution API + front (ex. stats admin, filtres) montre une synchro réelle. |
| **OP2** Interdépendances | Erreurs HTTP + messages ; client API typé ; pagination admin réservations. |
| **OP3** Interfaces | OpenAPI / DTO ; **veille** lors des ajouts de champs (`bookingReference`, `userEmail`, `/stats`). |
| **OP4** CI/CD | Back : **oui**. Front : **à confirmer ou ajouter**. Secrets : `.env` non commité (bon réflexe à maintenir). |
| **OP5** Tests | Back : unitaires + intégration nombreux. Front : tests composants **partiels** selon fichiers `*.spec.ts`. |
| **OP6** Performance | Pagination liste admin ; stats globales via endpoint dédié (évite stats « page courante »). |
| **OP7** Conflits intégration | Historique PR / tickets ; exemples : garde-routes Angular, contrainte `booking_reference`, alignement grille admin. |
| **OP8** Documentation | README back ; ce cahier ; tickets `docs/` — peuvent être regroupés pour le PDF soutenance. |

---

## 4. Périmètre métier énoncé — rappel vs implémentation

| Exigence | Commentaire |
|----------|-------------|
| Mono-mairie | Respecté (pas de multi-tenant dans le modèle observé). |
| Ressources (salles / matériel) | Catalogue unifié avec type ressource. |
| Autonomie / tutelle + impersonation | Modèle utilisateur + audit ; parcours « en tant que » à **démontrer** clairement en soutenance. |
| Groupes + réservation pour soi / groupe | Présent côté API et règles ; à **verbaliser** (qui voit quoi). |
| 3 flags d'exonération | À **pointer explicitement** (noms, champs, écrans) pour éviter un « P » sur J4. |
| PJ + SMTP + suppression | Code et tests d'intégration ; **preuve utilisateur** (mail reçu, log) pour J5. |
| Paiement sans PSP | Statuts caution / admin patch ; évolution Stripe **optionnelle** — la mention dans README/backlog valorise si non fait. |
| Secrétaire non révocable, délégation, conseil municipal | **À vérifier** dans les règles métier et la démo (sinon risque question-jury). |

---

## 5. Évolutions notables (code récent et rétroactif)

- **Réservations admin** : liste paginée, filtres **côté API** (plusieurs `status`), indicateurs **globaux** via `GET /api/admin/reservations/stats`, suppression du message trompeur « stats = page courante ».
- **Référence métier réservation** : `booking_reference` (format `yyMMdd` + séquence 5 chiffres), seed et création alignés.
- **Détail réservateur** : libellé + email dans les réponses admin ; modale sans UUID brut.
- **Robustesse front** : historique paiements avec garde `Array.isArray` ; `ApiService` supporte paramètres GET répétés (`status` multiple).
- **Tests back** : adaptation des jeux de données (booking reference obligatoire), `ReservationServiceImplTest` avec service de référence mocké explicitement.

---

## 6. Avant la soutenance (check-list courte)

1. **CI front** (ou preuve équivalente) + badge / lien dans README.
2. **Régénérer / valider OpenAPI** vs contrôleur réel (stats admin, champs résumé réservation).
3. **Script ou scénario démo** (parcours citoyen → admin → audit / impersonation si applicable).
4. **Phrase données fictives** dans le PDF.
5. **Matrice C5** : qui présente API, CI, métier, coordination, démo.

---

*Dernière mise à jour : alignement post-travail admin réservations / référence / stats globales (contenu reflétant l'état des dépôts au moment de la rédaction).*
