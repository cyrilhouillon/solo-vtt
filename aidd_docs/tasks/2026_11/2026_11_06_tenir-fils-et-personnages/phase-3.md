---
status: done
---

# Instruction: le Carnet montre la section et la prépare à l'ouverture

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/
│   ├── memory/
│   │   ├── architecture.md ✏️
│   │   └── project-brief.md ✏️
│   ├── backlog/stories/tenir-ses-fils-et-personnages-dans-la-section-mythic-du-carnet.md ✏️
│   └── tasks/2026_11/2026_11_06_tenir-fils-et-personnages/plan.md ✏️
└── vtt-web/
    ├── modules/mythic/
    │   ├── api.ts ✏️
    │   └── index.ts ✏️
    └── src/
        ├── types.ts ✏️
        ├── modules/notebookModule.ts ✏️
        ├── notebook/tree.ts ✏️
        └── components/
            ├── NotebookScreen.vue ✏️
            ├── PageTree.vue ✏️
            ├── PageBranch.vue ✏️
            └── MovePageDialog.vue ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081 / 5174 (`testing.md`), campagne neuve | Accueil | browser |
| Happy path   | Ouvrir la campagne | L'arbre montre l'en-tête « Mythic » sous les pages ordinaires, avec Fils et Personnages | browser |
| Happy path   | Ouvrir Fils, déverrouiller, écrire deux entrées dont une en double ; dans Personnages, une entrée `[[Aldric]]` | Liste à puces enregistrée ; le lien ouvre Aldric ; la recherche trouve l'entrée ; Fils est en onglet | browser |
| Happy path   | Glisser « Aldric » sous Personnages, puis à la racine | Permis, la page suit | browser |
| Happy path   | Recharger, puis redémarrer le serveur 8081 | Section et contenu identiques, aucune page de plus | browser |
| Edge case    | Menu de Fils ; glisser Fils ; dialogue Déplacer d'une autre page | Ni « Déplacer sous… » ni « Supprimer… » ; Fils ne se glisse pas ; Fils et Personnages proposés comme destinations | browser |
| Edge case    | `POST …/mythic/lists` refusé (route rendant une erreur) | Bandeau d'erreur, le Carnet s'ouvre quand même | browser |
| Edge case    | `grep -ri mythic vtt-web/src vtt-server/src` | Vide | cli |
| Teardown     | Arrêter 8081 puis 5174 | Ports libres ; 8080 et 5173 intacts | cli |

## Wireframe

```txt
┌─ PAGES ───────────────────────┐
│ (1) ▸ Aldric                ⋯ │
│     ▾ Session 6             ⋯ │
│         Le garde            ⋯ │
│                               │
│ (2) MYTHIC                    │
│     ─────────────             │
│ (3)   Fils                  ⋯ │
│     ▸ Personnages           ⋯ │
├───────────────────────────────┤
│ (4) Nouvelle page [Titre][Créer]
│     Corbeille                 │
└───────────────────────────────┘
```

1. Pages ordinaires : l'arbre d'aujourd'hui, inchangé.
2. En-tête de section : titre du module en petites capitales et filet, comme « Pages ».
3. Racines de section : lignes ordinaires, menu ⋯ réduit à Nouvelle sous-page et Renommer ; leurs sous-pages ont le menu complet.
4. Pied du panneau inchangé : « Nouvelle page » crée à la racine ordinaire.

## Tasks to do

### `1)` Le contrat

> Une page dit sa section ; un module se prépare avant la lecture du Carnet.

1. `types.ts` : `PageSummary.section: string | null`.
2. `NotebookModule.prepare?: (campaignId: string) => Promise<void>` ; Javadoc : idempotent, attendu avant la première lecture du Carnet.
3. `NotebookScreen` : au montage, `Promise.allSettled` des `prepare` de `notebookModules`, refus au bandeau, puis lecture du Carnet comme avant.

### `2)` L'arbre

> Les sections viennent après les pages ordinaires, sous un en-tête.

1. `tree.ts` : `buildTree` range les racines ordinaires, puis celles de chaque section (par id de section, puis titre) ; `canMove` refuse une racine de section.
2. `PageTree` : en-tête de chaque section (titre du module trouvé dans `notebookModules`, sinon l'id), en petites capitales comme « Pages » ; un appui sur une racine de section n'arme pas de glisser.
3. `PageBranch` : menu d'une racine de section sans « Déplacer sous… » ni « Supprimer… ».
4. `MovePageDialog` : mêmes destinations, dans l'ordre de l'arbre.

### `3)` Mythic se prépare

> Fils et Personnages existent dès la première ouverture.

1. `modules/mythic/api.ts` : `prepareLists(campaignId)` (`POST …/mythic/lists`) ; `index.ts` : `prepare`.

### `4)` Vérifier et documenter

> Le parcours passe, la doc suit.

1. `npm run build`, puis le Test Scope sur 5174.
2. `DESIGN.md` : en-tête de section dans l'arbre, menu d'une racine de section.
3. `architecture.md` : `prepare`, section dans l'arbre client ; `project-brief.md` : section Mythic.
4. Story `status: done`, plan `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | À la première ouverture d'une campagne, Fils et Personnages apparaissent sans recharger ; un échec de préparation n'empêche pas d'ouvrir le Carnet. |
| 2 | La section Mythic suit les pages ordinaires ; Fils et Personnages ne se déplacent ni ne se suppriment depuis l'interface ; toute autre page entre et sort de la section. |
| 3 | Rouvrir la campagne ne crée aucune page de plus. |
| 4 | `npm run build` vert ; parcours du Test Scope passé ; le cœur ne cite pas Mythic. |
