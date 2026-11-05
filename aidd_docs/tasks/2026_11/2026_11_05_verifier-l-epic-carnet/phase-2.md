---
status: done
---

# Instruction: parcours navigateur du Carnet

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── aidd_docs/tasks/2026_11/2026_11_05_verifier-l-epic-carnet/
    └── verification.md ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `mvn -q install -DskipTests`, serveur de test 8081 avec `solovtt.campaigns.path` et `solovtt.images.path` dans le scratchpad, Vite 5174 (`testing.md`) | accueil sur 5174, aucune campagne | system |
| Happy path   | Interface seule : créer « Barovie » ; créer « Sessions », « PNJ », « Ismark », « Ireena » ; créer « Session 5 » sous « Sessions », y écrire une scène qui lie Ismark et Ireena par `[[` ; ouvrir le combat, poser deux créatures, lancer puis terminer le combat ; revenir par Carnet | Session 5 ouverte, onglets intacts, texte et liens présents | browser |
| Happy path   | Renommer « Ismark » en « Ismark le Petit » | le lien de Session 5 et l'onglet affichent le nouveau titre ; la page d'Ismark liste Session 5 parmi les pages qui la citent | browser |
| Happy path   | Chercher un mot du texte de Session 5 | Session 5 en résultat, mot surligné ; un clic l'ouvre | browser |
| Happy path   | Supprimer « PNJ » (la confirmation dit 2 sous-pages et la page qui y renvoie), puis restaurer depuis la Corbeille | onglet Ismark fermé, liens barrés ; après restauration, arbre et liens revivants | browser |
| Edge case    | Revenir à l'accueil puis rouvrir « Barovie » | arrivée sur la dernière page active | browser |
| Edge case    | Arrêter et relancer 8081, recharger l'onglet, puis rouvrir depuis l'accueil | même page à l'adresse, mêmes onglets, arbre, contenus, liens et pages citantes ; réponses `GET …/notebook` identiques avant et après | browser |
| Teardown     | Tuer les processus sur 8081 puis 5174 (`testing.md`) | 8080 et 5173 intacts | system |

## Tasks to do

### `1)` Lancer les serveurs de test

> Isoler le parcours des campagnes de l'utilisateur.

1. Suivre `testing.md`, étapes 1 à 3, campagnes et images dans le scratchpad.

### `2)` Jouer le scénario de l'epic dans le navigateur

> Observer les quatre premières preuves de l'epic.

1. Jouer les lignes Happy path et le retour par l'accueil, sans Swagger ni fichier édité.
2. Consigner dans `verification.md` chaque preuve (tenue ou non) et chaque écart (bloquant, gênant, cosmétique) avec capture.

### `3)` Vérifier la reprise

> Observer la cinquième preuve de l'epic.

1. Relever `GET /api/campaigns/{id}/notebook`, `…/notebook/trash` et chaque page avant l'arrêt de 8081 ; relancer ; comparer.
2. Recharger l'onglet, puis rouvrir depuis l'accueil ; consigner le résultat dans `verification.md`.

### `4)` Arrêter les serveurs de test

> Rendre la machine telle qu'avant.

1. Tuer 8081 puis 5174, jamais 8080 ni 5173.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | 5174 répond et appelle 8081 ; `~/.solo-vtt/campagnes` inchangé. |
| 2 | `verification.md` dit, preuve à l'appui, si chacune des quatre premières preuves de l'epic tient. |
| 3 | `verification.md` dit, preuve à l'appui, si le Carnet reprend à l'identique après redémarrage. |
| 4 | Plus rien n'écoute sur 8081 ni 5174 ; 8080 et 5173 inchangés. |
