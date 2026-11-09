---
status: done
---

# Instruction: le panneau ouvre et clôt une scène dans la page

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-web/modules/mythic/
│   ├── api.ts ✏️
│   ├── result.ts ✏️
│   └── MythicPanel.vue ✏️
├── DESIGN.md ✏️
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   ├── codebase-map.md ✏️
│   └── project-brief.md ✏️
└── aidd_docs/backlog/stories/
    └── ouvrir-et-clore-une-scene.md ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `mvn -q install -DskipTests`, serveurs 8081 / 5174 (chemins du scratchpad), campagne neuve, tables de test importées, Personnages : « Garde Ylo », page « Session 1 » ouverte | Panneau Mythic prêt, chaos 5 | browser |
| Happy path   | « On entre au donjon », Tester, jusqu'à une scène attendue | Citation : « Scène attendue : On entre au donjon (chaos 5) », « d10 : n → Scène attendue » | browser |
| Happy path   | Chaos 9, Tester jusqu'à une scène modifiée | Ligne « Ajustement : … (d10 : n) », ou « Ajustements : … · … (d10 : 8, n, n) » | browser |
| Happy path   | Chaos 9, Tester jusqu'à une scène interrompue | Ligne « Événement aléatoire : … » et, si le focus vise une liste, l'entrée tirée, comme pour une question | browser |
| Happy path   | Clore, Contrôle gardé, à 5 | « Fin de scène : contrôle gardé → chaos 5 → 4 » dans la page ; le panneau affiche 4 | browser |
| Happy path   | Recharger | Chaos 4 gardé | browser |
| Edge case    | Contrôle perdu à 9 | « … contrôle perdu → chaos 9 (déjà au maximum) » | browser |
| Edge case    | Aucune page ouverte, Tester ou Clore | Refus dans le panneau, aucun appel, chaos inchangé | browser |
| Edge case    | Page verrouillée | Écrit à la fin, page toujours verrouillée | browser |
| Edge case    | Ctrl+Z après un test | La citation part en un pas | browser |
| Teardown     | Arrêt de 8081 puis 5174 | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌──────────────────────────────┐
│ (1) MYTHIC                   │
│ Chaos          [−]  5  [+]   │
├──────────────────────────────┤
│ (2) Scène attendue           │
│ [ On entre au donjon       ] │
│ [Tester la scène]            │
│ Clore la scène               │
│ [Contrôle gardé] [perdu]     │
├──────────────────────────────┤
│ (3) Question · Probabilité   │
│ [Demander]                   │
├──────────────────────────────┤
│ (4) Sens · [Tirer]           │
│ (5) Dernier résultat         │
└──────────────────────────────┘
```

1. Titre du module et chaos, inchangés.
2. Section Scène, nouvelle, avant l'oracle (la scène précède les questions) : champ facultatif, « Tester la scène » (gras, `wood-raised`) ; sous « Clore la scène » en `ink-muted`, « Contrôle gardé » et « Contrôle perdu ».
3. Oracle, inchangé.
4. Sens, inchangé.
5. Dernier résultat : l'issue de la scène en gras (et ajustement ou événement), ou « Chaos 5 → 4 » après une clôture ; une erreur à sa place.

## Tasks to do

### `1)` Les appels

> Le client teste et clôt.

1. `api.ts` : types `Scene`, `SceneEnd` ; `testScene(expected)`, `endScene(inControl)`.

### `2)` L'écrit dans la page

> Une citation par geste.

1. `result.ts` : sortir les paragraphes de l'événement d'`answerBlocks` (l. 56-61) ; `sceneBlocks` (« Scène attendue : … (chaos n) », « d10 : n → issue », ajustement(s) et jets, ou l'événement) ; `sceneEndBlocks` (« Fin de scène : contrôle gardé / perdu → chaos a → b », « (déjà au minimum / maximum) » si inchangé).

### `3)` Le panneau

> Section Scène selon le wireframe.

1. `MythicPanel.vue` : refus sans page avant l'appel ; Tester écrit puis vide le champ ; Clore écrit puis met à jour `chaos` ; `last` gagne `{ scene }` et `{ sceneEnd }`.

### `4)` Clôture

> La doc et le backlog disent la story faite.

1. `DESIGN.md` (section Scène du panneau Mythic), `architecture.md` (routes, dés, écrit), `codebase-map.md` (`SceneCheck`, `SceneAdjustment`), `project-brief.md` ; story `status: done`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` vert dans `vtt-web/`. |
| 2 | Chaque issue et la clôture s'écrivent en citation dans la page, que la recherche trouve. |
| 3 | Le parcours du Test Scope passe au navigateur sur 5174 ; le chaos affiché suit la clôture. |
| 4 | Mémoire et story à jour ; `grep -ri mythic vtt-server/src vtt-web/src` vide ; `mvn -q verify` vert. |
