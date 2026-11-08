---
status: done
---

# Instruction: le panneau écrit l'entrée tirée avec l'événement

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-web/modules/mythic/
│   ├── api.ts ✏️
│   ├── result.ts ✏️
│   └── MythicPanel.vue ✏️
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   └── project-brief.md ✏️
└── aidd_docs/backlog/stories/
    └── laisser-un-evenement-aleatoire-tirer-dans-ses-listes.md ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs 8081 / 5174 (chemins du scratchpad), campagne neuve, Personnages : « Garde Ylo » deux fois et un lien `[[Vessa]]`, une page « Session 1 » ouverte | Section Mythic prête | browser |
| Happy path   | Chaos 9, questions jusqu'à un événement PNJ | Sous « Événement aléatoire : … », « Personnages : <entrée> (d10 : n → ligne n) » dans la page et le dernier résultat du panneau | browser |
| Happy path   | Entrée tirée = le lien | Lien vivant dans la page, qui ouvre « Vessa » | browser |
| Happy path   | Recherche « Ylo » | « Session 1 » trouvée | browser |
| Edge case    | Ligne vide tirée | « Choisir : l'entrée la plus logique, ou relancez » | browser |
| Edge case    | Fils vide, focus de fil | « Loin d'un fil → Contexte actuel (liste Fils vide) » | browser |
| Edge case    | Ctrl+Z | La citation entière part en un pas | browser |
| Teardown     | Arrêt de 8081 puis 5174 | 8080 et 5173 intacts | system |

## Tasks to do

### `1)` Les types

> Le client lit le tirage.

1. `api.ts` : `RandomEvent.effectiveFocus`, `effectiveFocusLabel`, `list: ListDraw | null` (`entry: JSONContent[]`).

### `2)` L'écrit dans la page

> Une ligne de plus dans la citation.

1. `result.ts` : focus effectif (« focus tiré → Contexte actuel (liste X vide) ») ; paragraphe « <Liste> : » + nœuds de l'entrée, ou « Choisir … », ou « liste pleine : à nettoyer », puis les dés.

### `3)` Le panneau

> Le dernier résultat montre l'entrée.

1. `MythicPanel.vue` (l. 204-209) : focus effectif et `entryText`, ou « Choisir ».

### `4)` Clôture

> La doc et le backlog disent la story faite.

1. `architecture.md` (écrit de l'entrée), `project-brief.md` (tirage dans les listes) ; story `status: done`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` vert dans `vtt-web/`. |
| 2 | L'entrée, « Choisir » ou Contexte actuel s'écrit dans la page, lien compris, et la recherche la trouve. |
| 3 | Le panneau montre la même entrée que la page. |
| 4 | La mémoire et la story disent le tirage ; `grep -ri mythic vtt-server/src vtt-web/src` vide. |
