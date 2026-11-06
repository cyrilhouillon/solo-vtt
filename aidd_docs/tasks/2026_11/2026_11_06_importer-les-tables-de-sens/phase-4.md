---
status: done
---

# Instruction: le panneau Mythic importe et écrit le sens dans la page

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/
│   ├── backlog/stories/importer-ses-tables-de-mots-et-tirer-un-sens.md ✏️
│   ├── memory/
│   │   ├── architecture.md ✏️
│   │   ├── codebase-map.md ✏️
│   │   └── project-brief.md ✏️
│   └── tasks/2026_11/2026_11_06_importer-les-tables-de-sens/plan.md ✏️
└── vtt-web/modules/mythic/
    ├── api.ts ✏️
    ├── result.ts ✏️
    └── MythicPanel.vue ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs 8081 / 5174 (trois chemins dans le scratchpad) ; campagne neuve, page ouverte | Section Sens : « Tables de sens non importées », bouton Importer ; oracle oui/non utilisable | browser |
| Setup        | L'utilisateur colle ses tables (« 1: mot2: mot… ») ; Claude écrit le fichier traduit au format, dans le scratchpad | Fichier `.txt` hors du dépôt | system |
| Happy path   | Importer ce fichier | Liste Actions, Descriptions, éléments ; Tirer actif | browser |
| Happy path   | Actions, Tirer, page déverrouillée | Au curseur : « Sens (Actions) : mot · mot », « d100 : n / n » | browser |
| Happy path   | Un élément, Tirer, page verrouillée | À la fin de la page, verrou gardé | browser |
| Happy path   | Question qui déclenche un événement (chaos 9, rejouer) | Ligne de l'événement suivie de ses deux mots d'action | browser |
| Happy path   | Seconde campagne ; redémarrage du serveur 8081 | Tables toujours importées | browser |
| Edge case    | Importer un fichier à 99 entrées | Raison et ligne dans le panneau ; tables d'avant gardées | browser |
| Edge case    | Aucune page ouverte, Tirer | Refus avec la raison ; aucun `POST …/meanings` | browser |
| Edge case    | Sans tables, question qui déclenche un événement | Événement écrit sans sens, comme avant | browser |
| Edge case    | Recherche d'un mot tiré | La page qui le porte est trouvée | browser |
| Teardown     | Arrêter 8081 puis 5174 | Données du joueur (`~/.solo-vtt/`) intactes | system |

## Wireframe

```txt
┌─────────────────────────┐
│ (1) Mythic              │
├─────────────────────────┤
│ (2) Chaos · Question …  │
├─────────────────────────┤
│ (3) Sens                │
│ [Actions            ▾]  │
│ (4) [ Tirer ]           │
│ (5) Remplacer les tables│
├─────────────────────────┤
│ (6) Dernier résultat    │
└─────────────────────────┘
```

1. Titre du module.
2. Chaos et question oui/non, inchangés.
3. Table du sens : Actions, Descriptions, puis un groupe Éléments dans l'ordre du fichier. Sans tables, à sa place : « Tables de sens non importées : importer le fichier de vos tables. » et le bouton Importer.
4. Tirer : tire deux mots, puis les écrit dans la page ouverte.
5. Remplacer les tables : choisit un nouveau fichier `.txt` (même import).
6. Rappel du dernier résultat (réponse ou sens), ou la raison d'un refus ou d'un import refusé.

## Tasks to do

### `1)` Le client du sens

> Le panneau importe, liste et tire.

1. `api.ts` : `meaningTables()`, `importMeaningTables(text)` (`PUT`, `Content-Type: text/plain; charset=utf-8`), `drawMeaning(table, element)` sur `/api/mythic/…` ; `RandomEvent` gagne `meaning`.
2. `result.ts` : `meaningBlocks(meaning)` (citation : « Sens (table) : » en gras, les deux mots, « d100 : n / n ») ; la ligne de l'événement ajoute « — mot · mot » s'il a un sens.
3. `MythicPanel.vue` : section Sens selon le Wireframe ; fichier lu par `file.text()` ; refus sans page avant l'appel ; erreurs dans le panneau ; styles aux jetons de `src/style.css`.

### `2)` Parcours et fin

> La story est vérifiée de bout en bout puis close.

1. L'utilisateur colle ses tables ; Claude les met au format et les traduit en français dans le scratchpad, jamais dans le dépôt.
2. `npm run build`, `mvn -q verify` ; parcours sur 5174 / 8081 selon le Test Scope ; `grep -ri mythic vtt-server/src vtt-web/src` vide ; `git status` sans fichier de tables.
3. `architecture.md` (tables communes, routes `/api/mythic`, sens d'un événement), `codebase-map.md` (fichiers du moteur et du serveur), `project-brief.md` (tables de sens), `DESIGN.md` (section Sens).
4. Story `status: done` ; décision d'epic « format du fichier d'import » notée tranchée ; plan `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Sur 5174, un fichier importé une fois sert à toutes les campagnes et passe un redémarrage ; Tirer écrit deux mots de la table choisie dans la page ; un événement écrit ses deux mots d'action ; sans tables, le panneau dit pourquoi et l'oracle répond. |
| 2 | Builds verts ; aucun mot du livre dans le dépôt ; mémoire, `DESIGN.md`, story, epic et plan à jour. |
