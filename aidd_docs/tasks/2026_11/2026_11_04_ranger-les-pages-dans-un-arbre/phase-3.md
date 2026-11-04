---
status: done
---

# Instruction: déplacer une page en la glissant dans l'arbre

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/
│   ├── backlog/stories/
│   │   └── ranger-les-pages-du-carnet-dans-un-arbre.md ✏️
│   ├── memory/
│   │   └── architecture.md ✏️
│   └── tasks/2026_11/2026_11_04_ranger-les-pages-dans-un-arbre/
│       └── plan.md ✏️
└── vtt-web/src/components/
    ├── NotebookScreen.vue ✏️
    └── PageTree.vue (créé en phase 2, modifié ici)
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081 / 5174 ; une campagne avec Lore > Cités > Barovie, PNJ et une vingtaine de pages à la racine | Arbre qui dépasse la hauteur du panneau | browser |
| Happy path   | Glisser Cités à la souris sur PNJ, relâcher | Cités et Barovie passent sous PNJ, la branche PNJ s'ouvre | browser |
| Happy path   | Glisser Cités sur la zone « Racine » | Cités revient à la racine avec Barovie | browser |
| Happy path   | Survoler PNJ replié pendant le glisser | La branche s'ouvre après un court délai | browser |
| Happy path   | Glisser près du bas du panneau | L'arbre défile | browser |
| Edge case    | Glisser Lore sur Barovie, ou sur elle-même | Ligne visée marquée refusée, rien n'est envoyé au relâcher | browser |
| Edge case    | Relâcher sur le parent actuel, hors de l'arbre, ou Échap pendant le glisser | Rien n'est envoyé, l'arbre est inchangé | browser |
| Edge case    | Mouvement de moins de 6 px puis relâcher | C'est un clic : la page s'ouvre | browser |
| Edge case    | Après un glisser réussi | Le clic qui suit le relâcher n'ouvre pas la page glissée | browser |
| Edge case    | Au doigt (émulation tactile) | Le doigt fait défiler l'arbre ; aucun glisser ne démarre ; le menu « Déplacer sous… » reste disponible | browser |
| Teardown     | Arrêter 8081 puis 5174 ; effacer le dossier scratchpad | 8080 et 5173 intacts | system |

## Tasks to do

### `1)` Glisser une ligne de l'arbre

> À la souris ou au stylet, une page se dépose sur une autre ou à la racine.

1. `PageTree.vue` : `pointerdown` sur une ligne (souris ou stylet, bouton principal) arme le glisser ; au-delà de 6 px, capture du pointeur, fantôme (titre) qui suit, classe sur `body` qui coupe la sélection de texte.
2. Cible : la ligne sous le pointeur (`document.elementFromPoint`) ; refusée si c'est la page, l'un de ses descendants ou son parent actuel (`tree.ts`) ; zone « Racine » posée sur le titre du panneau (pour ne décaler aucune ligne), visible pendant le glisser seulement, refusée si la page y est déjà.
3. Survol d'une branche repliée plus de 600 ms : elle s'ouvre.
4. Près du haut ou du bas de l'arbre, celui-ci défile.
5. Relâcher sur une cible permise émet `move(id, parentId)` ; `NotebookScreen.vue` le traite comme le dialogue (envoi, relecture, ancêtres ouverts). Échap, `pointercancel` ou perte de capture annulent ; le clic qui suit un glisser est ignoré.
6. Au toucher (`pointerType === 'touch'`), aucun glisser : le défilement natif reste.

### `2)` Clore la story

> Documentation et statuts à jour.

1. `DESIGN.md` : fantôme, cible permise et refusée, zone Racine, ouverture au survol.
2. `architecture.md` : glisser-déposer de l'arbre à la souris et au stylet, menu au toucher et au clavier.
3. Story `status: done` ; plan `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe ; un glisser à la souris déplace une page avec ses sous-pages sous une autre ou à la racine, et l'arbre relu au serveur le confirme ; une cible interdite ne déclenche aucune requête ; un clic court ouvre toujours la page ; au doigt, l'arbre défile. |
| 2 | `DESIGN.md` et `architecture.md` décrivent le glisser ; story et plan clos. |
