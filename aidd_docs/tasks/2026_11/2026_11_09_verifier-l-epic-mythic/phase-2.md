---
status: done
---

# Instruction: parcours navigateur de l'epic

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── aidd_docs/tasks/2026_11/2026_11_09_verifier-l-epic-mythic/
    └── verification.md ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `mvn -q install -DskipTests`, serveur de test 8081 avec `solovtt.campaigns.path`, `solovtt.images.path` et `solovtt.modules.path` dans le scratchpad, Vite 5174 (`testing.md`) ; fichier de tables aux mots inventés généré dans le scratchpad (format `TablesDeTest`) | accueil sur 5174, aucune campagne ; `~/.solo-vtt/` non touché | system |
| Happy path   | Interface seule : créer « Barovie » ; le Carnet montre la section « Mythic » avec « Fils » et « Personnages » ; importer le fichier de tables dans la section Sens | le panneau propose les tables (Actions, Descriptions, Éléments) | browser |
| Happy path   | Écrire « Trouver l'évêque » et « Sauver Ireena » dans Fils ; créer « Ismark » et le lier par `[[` dans Personnages ; créer « Session 6 » ; chaos 5, poser « Le garde est-il corrompu ? » en « Probable » | une citation dans Session 6 : question, probabilité, chaos 5, d100 et réponse | browser |
| Happy path   | Chaos 9, reposer des questions jusqu'à un événement dont le focus vise un fil | citation « Événement aléatoire : <focus> — mot · mot » puis « Fils : <entrée> (d10 : n → ligne n) » ; nombre d'essais noté | browser |
| Happy path   | Au chaos 9, « Contrôle gardé » (« Contrôle perdu » n'y change rien : borne), puis tester une scène attendue jusqu'à une issue modifiée | « Fin de scène : contrôle gardé → chaos 9 → 8 », chaos affiché 8, puis « d10 : n → Scène modifiée » et ses ajustements | browser |
| Edge case    | Revenir à l'accueil puis rouvrir « Barovie » | section Mythic, Fils, Personnages et chaos inchangés | browser |
| Edge case    | Relever en `GET` campagnes, `…/notebook`, chaque page, `…/mythic/chaos`, `/api/mythic/meaning-tables` ; arrêter et relancer 8081 ; relever de nouveau ; recharger l'onglet | relevés identiques ; tables toujours importées sans nouvel import ; Fils et Personnages dans l'arbre, avec leur contenu | browser |
| Teardown     | Tuer les processus sur 8081 puis 5174 (`testing.md`) | 8080 et 5173 intacts | system |

## Tasks to do

### `1)` Lancer les serveurs de test

> Isoler le parcours des données du joueur.

1. Suivre `testing.md`, étapes 1 à 3, avec les trois chemins dans le scratchpad.
2. Générer le fichier de tables (sections `# Actions 1` à `# Descriptions 2`, plus un `# Élément : …`, 100 mots inventés chacune) dans le scratchpad.

### `2)` Jouer le scénario de l'epic dans le navigateur

> Observer les preuves 1 à 4 de l'epic.

1. Jouer les lignes Happy path et le retour par l'accueil, sans Swagger ni fichier édité.
2. Créer `verification.md`, y reporter la preuve 6 de la phase 1, puis consigner chaque preuve (tenue ou non), le nombre d'essais des tirages répétés et chaque écart (bloquant, gênant, cosmétique) avec capture.

### `3)` Vérifier la reprise

> Observer la preuve 5 de l'epic, et que l'import ne se refait pas.

1. Relever les `GET` du Test Scope avant l'arrêt de 8081 ; relancer ; comparer.
2. Recharger l'onglet, puis rouvrir depuis l'accueil ; consigner le résultat dans `verification.md`.

### `4)` Arrêter les serveurs de test

> Rendre la machine telle qu'avant.

1. Tuer 8081 puis 5174, jamais 8080 ni 5173.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | 5174 répond et appelle 8081 ; `~/.solo-vtt/campagnes` et `~/.solo-vtt/modules` inchangés. |
| 2 | `verification.md` dit, preuve à l'appui, si chacune des preuves 1 à 4 de l'epic tient. |
| 3 | `verification.md` dit, preuve à l'appui, si listes, chaos et tables reviennent à l'identique après redémarrage. |
| 4 | Plus rien n'écoute sur 8081 ni 5174 ; 8080 et 5173 inchangés. |
