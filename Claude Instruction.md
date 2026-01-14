# Instructions globales à donner à Claude

Ce document constitue **le contrat complet (conceptuel + technique)** que Claude doit suivre pour concevoir et
implémenter la plateforme.

👉 Ces instructions **remplacent toute autre hypothèse**.
👉 Toute simplification non justifiée est **interdite**.

---

## 🎯 OBJECTIF FONDAMENTAL

Construire une **plateforme SaaS de transport multi-tenant**, prête production, avec :

* Sécurité Zero-Trust
* Isolation stricte des tenants
* Architecture modulaire découpable
* Code auditable et maintenable

Claude doit raisonner **comme un Senior / Staff Software Architect**, jamais comme un tutoriel.

---

## 🧠 MODÈLE MENTAL (NON NÉGOCIABLE)

* **Auth décide QUI**

* **Agency décide QUELS TENANTS EXISTENT**

* **Parcel décide CE QUI SE PASSE MÉTIER**

* **RBAC** → quelles actions

* **ABAC** → sur quelles ressources

* **Events** → propagation de faits, jamais des ordres

---

## 🧱 STYLE D’ARCHITECTURE IMPOSÉ

* **Monolithe modulaire strict** (pas microservices)
* Bounded Contexts hermétiques
* Zéro confiance interne (Zero Trust Monolith)
* Découpage futur vers microservices sans refonte

### Contexts obligatoires

* `auth`
* `agency`
* `parcel`
* `shared`

❌ Aucun import direct entre contexts métier

---

## 🧩 STRUCTURE D’UN CONTEXTE (DDD STRICT)

Chaque contexte doit suivre exactement :

```
<context>
├── api            // Controllers uniquement
├── application    // Use cases / orchestration
├── domain         // Métier pur + invariants
└── infrastructure // DB, adapters techniques
```

### Interdictions absolues

* `domain` → `api`
* `domain` → `infrastructure`
* `parcel` → `auth.domain`
* `parcel` → `agency.domain`

---

## 🧪 GOUVERNANCE D’ARCHITECTURE

* Règles d’architecture exécutées (ArchUnit)
* Toute violation = build cassé
* Les frontières sont **vérifiées**, pas documentées seulement

---

## 🔐 SÉCURITÉ — CONCEPT + TECHNIQUE

### Principe fondamental

> Les services métier **ne connaissent jamais l’utilisateur**, seulement le **JWT décodé**.

---

### Stack sécurité imposée

* Spring Boot 3.x / Spring 6+
* **Spring Resource Server**
* OAuth2 / OIDC
* JWT stateless uniquement

❌ Pas de session
❌ Pas de UserDetailsService côté métier

---

### JWT CONTRACT (OBLIGATOIRE)

Chaque JWT **DOIT** contenir :

* `sub` → user_id
* `actor_type` → AGENCY_EMPLOYEE | CUSTOMER | PLATFORM_ADMIN
* `agency_id` → uniquement pour employés
* `roles` → rôles scopés agence

Claude **NE DOIT JAMAIS** recharger ces données depuis la DB.

---

## 🧩 RBAC + ABAC (OBLIGATOIRE)

### RBAC — Actions

* Implémenté via annotations (`@RequireRole`, `@RequireActor`)
* Appliqué dans `api` ou `application`
* Jamais dans le domaine

### ABAC — Ressources

* Implémenté via **policies de domaine**
* Exemple : `ParcelAccessPolicy`
* Vérifie :

    * `agency_id`
    * ownership (sender / receiver)
    * actor_type

---

## 🌍 MULTI-TENANCE — DÉFENSE EN PROFONDEUR

Claude doit implémenter **les 4 niveaux** suivants :

1. JWT (`agency_id`)
2. TenantContext (request-scoped)
3. Repository filtré par `agency_id`
4. Invariants métier explicites

👉 Toute entité métier **DOIT** contenir `agency_id`.

---

## 📦 MODÉLISATION MÉTIER (CONCEPT + IMPLÉMENTATION)

### Shipment

* Agrégat distinct
* Représente une opération d’envoi groupé
* Lifecycle : `OPEN → CONFIRMED`
* Non tracké

### Parcel

* Unité opérationnelle
* Trackée individuellement
* Lifecycle indépendant
* Associée à un Shipment

❌ Interdiction de modéliser `1 Parcel = 1 Shipment`

---

## 🔁 ÉVÉNEMENTS — STRATÉGIE OBLIGATOIRE

### Concept

* Commandes = synchrones
* Événements = faits métier
* Consommés uniquement pour des effets de bord

### Implémentation

* Event Bus interne
* Publication **après commit** uniquement
* Events immuables
* Incluent toujours `agency_id`

### Interdictions

* Event-driven authorization
* Write métier dans un listener
* Publication avant commit

---

## 🛠️ STACK TECHNIQUE IMPOSÉE

Claude **DOIT** utiliser exclusivement :

* **Java 21**

    * records pour DTOs
    * sealed interfaces si pertinent

* **Spring Boot 3.x / Spring 6+**

    * Web
    * Data JPA
    * Security
    * Resource Server

* **Gradle** (Kotlin DSL recommandé)

* **MapStruct**

    * `componentModel = "spring"`
    * aucun mapping manuel

* **PostgreSQL**

    * via Docker Compose
    * Flyway ou Liquibase accepté

---

## 🧪 TESTS (OBLIGATOIRES)

Claude doit fournir :

* Tests domaine (invariants)
* Tests sécurité (RBAC / ABAC)
* Tests multi-tenant (fuite interdite)
* Tests d’architecture (ArchUnit)

---

## 🚫 INTERDICTIONS ABSOLUES

Claude **NE DOIT PAS** :

* Simplifier la sécurité
* Introduire des dépendances inter-contextes
* Mettre de la logique métier dans les controllers
* Gérer les rôles hors Auth Context
* Utiliser H2 ou DB en mémoire
* Utiliser les événements comme commandes

---

## 🧠 MODE DE RÉPONSE ATTENDU DE CLAUDE

Claude doit :

* Justifier chaque choix
* Respecter strictement ces instructions
* Refuser toute implémentation dangereuse
* Produire du code prêt production
* Raisonner plateforme, pas feature

---

**Fin des instructions globales**
