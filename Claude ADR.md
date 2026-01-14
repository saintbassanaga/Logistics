# Architecture Decision Records (ADR)

Ce document regroupe les **ADR officiels** de la plateforme de transport multi-tenant.
Ils constituent la **référence d’architecture** pour le développement, l’audit et l’évolution du système.

---

## ADR-001 — Adoption d’un Monolithe Modulaire

### Statut

Accepted

### Contexte

La plateforme vise un haut niveau de sécurité, de multi-tenance et de cohérence métier. Une approche microservices
introduirait une complexité opérationnelle prématurée (réseau, orchestration, latence, observabilité).

### Décision

Adopter un **Monolithe Modulaire strictement cloisonné**, structuré par Bounded Contexts DDD.

### Conséquences

**Positives**

* Déploiement simplifié
* Transactions locales fortes
* Latence minimale
* Évolutivité vers microservices sans refonte

**Négatives**

* Discipline architecturale obligatoire
* Tests d’architecture requis

---

## ADR-002 — Séparation stricte des Bounded Contexts

### Statut

Accepted

### Contexte

Les responsabilités Auth, Agency et Parcel ne doivent jamais se contaminer, même dans un monolithe.

### Décision

Chaque Bounded Context est isolé par :

* packages dédiés
* règles d’imports strictes
* dépendances unidirectionnelles

### Conséquences

* Prévention du Big Ball of Mud
* Découpage microservices futur facilité

---

## ADR-003 — Authorization Server comme source unique de vérité

### Statut

Accepted

### Contexte

La duplication de logique d’identité ou de rôles est une source majeure de failles de sécurité.

### Décision

Le contexte Auth est la **seule autorité** pour :

* utilisateurs
* rôles
* affiliations agence
* émission de JWT

Les services métier ne chargent jamais d’utilisateurs depuis la base de données.

### Conséquences

* Sécurité renforcée
* JWT riche obligatoire
* Zéro dépendance Auth → Business

---

## ADR-004 — Utilisation de JWT riches (Context Propagation)

### Statut

Accepted

### Contexte

Les services métier ne doivent jamais effectuer des appels synchrones pour vérifier l’identité ou les rôles.

### Décision

Chaque JWT contient :

* `sub`
* `actor_type`
* `agency_id` (employés uniquement)
* `roles`

### Conséquences

* Autorisation locale et rapide
* Auditabilité accrue
* Tokens plus volumineux

---

## ADR-005 — Modèle d’autorisation RBAC + ABAC

### Statut

Accepted

### Contexte

RBAC seul ne permet pas de garantir l’isolation multi-tenant ni l’accès aux ressources spécifiques.

### Décision

* **RBAC** pour les actions (annotations)
* **ABAC** pour l’accès aux ressources (policies de domaine)

### Conséquences

* Autorisation fine
* Règles métier explicites
* Complexité contrôlée

---

## ADR-006 — Multi-tenance logique par `agency_id`

### Statut

Accepted

### Contexte

La plateforme est SaaS multi-tenant, chaque agence devant être strictement isolée.

### Décision

* `agency_id` obligatoire dans toutes les entités métier
* Application du tenant à 4 niveaux : JWT, API, Repository, Domaine

### Conséquences

* Isolation forte
* Prévention des fuites inter-agences

---

## ADR-007 — Shipment comme agrégat distinct de Parcel

### Statut

Accepted

### Contexte

Un envoi réel implique plusieurs colis envoyés dans une même opération métier.

### Décision

Introduire **Shipment** comme agrégat métier distinct, avec un cycle de vie court (OPEN → CONFIRMED).

### Conséquences

* Modélisation réaliste
* Meilleure cohérence métier
* Requêtes plus complexes

---

## ADR-008 — Event-Driven uniquement pour les effets de bord

### Statut

Accepted

### Contexte

L’event-driven mal utilisé complexifie inutilement les invariants critiques.

### Décision

* Commandes et invariants : synchrones
* Événements : faits métier, post-commit, effets de bord uniquement

### Conséquences

* Cohérence forte
* Évolutivité
* Pas d’event-driven authorization

---

## ADR-009 — Event Bus interne transactionnel

### Statut

Accepted

### Contexte

Même en monolithe, les événements sont nécessaires pour découpler les effets secondaires.

### Décision

Mettre en place un **bus d’événements interne**, transaction-aware, sans broker externe.

### Conséquences

* Découplage interne
* Migration future facilitée

---

## ADR-010 — Zéro Trust Monolith

### Statut

Accepted

### Contexte

Un monolithe ne doit jamais supposer la confiance entre modules.

### Décision

* Aucun accès direct aux données d’un autre contexte
* Vérifications de sécurité systématiques
* Pas de logique implicite

### Conséquences

* Robustesse maximale
* Discipline de développement requise

---

## ADR-011 — Tests d’architecture obligatoires

### Statut

Accepted

### Contexte

Les règles d’architecture doivent être vérifiées automatiquement.

### Décision

Introduire des **tests d’architecture (ArchUnit)** exécutés en CI.

### Conséquences

* Détection précoce des violations
* Préservation de l’intégrité du design

---

## ADR-012 — Documentation par ADR comme source de vérité

### Statut

Accepted

### Contexte

Les décisions architecturales doivent survivre aux changements d’équipe.

### Décision

Les ADR sont la **documentation officielle** des décisions structurantes.

### Conséquences

* Continuité d’équipe
* Audit facilité
* Alignement technique durable

---

**Fin des ADR**
