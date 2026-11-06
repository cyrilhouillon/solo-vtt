---
status: done
---

# Instruction: le panneau Mythic écrit l'oracle dans la page

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/
│   ├── backlog/stories/poser-une-question-a-l-oracle-depuis-le-carnet.md ✏️
│   ├── memory/
│   │   ├── architecture.md ✏️
│   │   ├── codebase-map.md ✏️
│   │   └── project-brief.md ✏️
│   └── tasks/2026_11/2026_11_06_poser-une-question-a-l-oracle/plan.md ✏️
└── vtt-web/modules/mythic/
    ├── index.ts ✅
    ├── api.ts ✅
    ├── result.ts ✅
    └── MythicPanel.vue ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs 8081 / 5174 (scratchpad) ; campagne neuve, page « Session 6 » ouverte | Panneau Mythic, chaos 5 | browser |
| Happy path   | Chaos à 7 | 7 affiché ; relu après rechargement | browser |
| Happy path   | « Le garde est-il corrompu ? », « Probable », Demander, page déverrouillée | Au curseur : question, probabilité, chaos, jet, réponse ; page enregistrée | browser |
| Happy path   | Jet double ≤ chaos (rejouer jusqu'à l'obtenir, ou campagne au chaos 9) | Événement aléatoire et son focus écrits avec la réponse | browser |
| Happy path   | Même question sur une page verrouillée | Résultat à la fin de la page, verrou gardé | browser |
| Edge case    | Aucune page ouverte, Demander | Refus avec la raison dans le panneau ou le bandeau ; aucun appel `POST …/questions` | browser |
| Edge case    | Chaos 1 et −, 9 et + | Boutons éteints aux bornes | browser |
| Edge case    | Seconde campagne | Son chaos est 5 | browser |
| Edge case    | Serveur sans `mythic-server` (ou erreur réseau) | Erreur dans le bandeau, page inchangée | browser |
| Edge case    | `vtt-web/modules/mythic/` retiré, `npm run build` | Build vert, panneau absent, Carnet comme avant | system |
| Edge case    | Recherche « corrompu » | La page qui porte le résultat est trouvée | browser |
| Teardown     | Arrêter 8081 puis 5174 ; remettre `modules/mythic/` | Données du joueur intactes | system |

## Wireframe

```txt
┌─────────────────────────┐
│ (1) Mythic              │
├─────────────────────────┤
│ (2) Chaos   [−]  5  [+] │
├─────────────────────────┤
│ (3) Question            │
│ [Le garde est-il …    ] │
│ (4) Probabilité         │
│ [Probable           ▾]  │
│ (5) [ Demander ]        │
├─────────────────────────┤
│ (6) Dernière réponse    │
└─────────────────────────┘
```

1. Titre du module.
2. Facteur de chaos de la campagne, 1 à 9, boutons éteints aux bornes.
3. Question facultative, écrite telle quelle dans la page.
4. Probabilité, d'« Impossible » à « Certain », lue au serveur (`GET …/mythic/odds`).
5. Demander : pose la question, puis écrit le résultat dans la page ouverte.
6. Rappel de la dernière réponse (réponse, jet, événement), ou la raison d'un refus.

## Tasks to do

### `1)` Le module client Mythic

> Le panneau pose la question et écrit le résultat dans la page.

1. `modules/mythic/api.ts` : `chaos`, `setChaos`, `odds`, `ask` sur `/api/campaigns/{id}/mythic/…`, par `request` du cœur.
2. `modules/mythic/result.ts` : le résultat en nœuds standard, une citation : « Question : … (Probable, chaos 5) », « d100 : 23 → Oui », et « Événement aléatoire : Action d'un PNJ » s'il y a lieu.
3. `modules/mythic/MythicPanel.vue` : chaos (− / +, `PUT` à chaque pas), question, probabilité, Demander ; refuse sans page avant tout appel ; erreurs dans le panneau ; styles aux jetons de `src/style.css`.
4. `modules/mythic/index.ts` : `NotebookModule` `{ id: 'mythic', title: 'Mythic', panel }`.

### `2)` Parcours et fin

> La story est vérifiée de bout en bout puis close.

1. `npm run build`, `mvn -q verify` ; parcours navigateur sur 5174 / 8081 selon le Test Scope ; `grep -ri mythic vtt-server/src vtt-web/src` vide.
2. `architecture.md` (Mythic : chaos par campagne, question, écriture), `codebase-map.md` (`vtt-web/modules/mythic/`), `project-brief.md` (oracle Mythic dans les fonctionnalités), `DESIGN.md` (panneau Mythic).
3. Story `status: done` ; plan `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Sur 5174, une question écrit question, probabilité, chaos, jet, réponse (et événement avec focus sur un double ≤ chaos) dans la page ouverte, au curseur ou en fin de page verrouillée ; le chaos se garde par campagne et passe un redémarrage. |
| 2 | Builds verts ; sans `modules/mythic/`, le Carnet marche comme avant ; mémoire, `DESIGN.md`, story et plan à jour. |
