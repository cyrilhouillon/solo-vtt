---
status: done
---

# Instruction: Client : écran de combat, reprise et rejeu

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/src/
├── router.ts                        ✏️ /campagnes/:campaignId/combats/:combatId
└── components/
    ├── GameScreen.vue               ✏️ mode rejeu en lecture seule ; rollThenLand awaitable
    ├── ReplayControls.vue           ✅ début, pas arrière, lecture/pause, pas avant, fin, n / N
    ├── JournalPanel.vue             ✏️ message vide du rejeu
    └── NotebookScreen.vue           ✏️ (lien vers la route du combat)
aidd_docs/memory/architecture.md     ✏️
aidd_docs/memory/codebase-map.md     ✏️
aidd_docs/memory/project-brief.md    ✏️
DESIGN.md                            ✏️ si la barre de rejeu ajoute un motif
```

## Test Scope

| Scenario | Action | Observable outcome | Actor |
| -------- | ------ | ------------------ | ----- |
| Setup | Serveurs 8081/5174 ; campagne avec un combat terminé (attaque avec Bouclier), un combat en cours non courant | Prêt | browser |
| Happy path | Ouvrir le combat terminé depuis le Carnet | Écran de combat sur l'état final, barre de rejeu « N / N », aucun outil de jeu | browser |
| Happy path | Début, puis pas avant ×N | Chaque pas montre positions, PV, active, round et journal comme en direct ; une attaque fait rouler ses dés 3D et pose sa bulle | browser |
| Happy path | Pas arrière | Retour instantané à l'état du pas d'avant, journal tronqué | browser |
| Happy path | Lecture | Les pas s'enchaînent, chacun 2 s après la fin de l'animation du précédent ; pause l'arrête ; arrêt à la fin | browser |
| Happy path | Ouvrir le combat en cours non courant | Il est repris : écran de combat en direct, là où il en était | browser |
| Edge case | En rejeu : glisser un jeton, Suppr, D, Ctrl+Z, catalogue, grille, Terminer | Rien ne change ; aucun appel au serveur qui modifie la partie | browser |
| Edge case | `prefers-reduced-motion` en lecture | Pas toutes les 2 s sans animation | browser |
| Edge case | Bouton Carnet depuis le rejeu | Retour au Carnet sur l'entrée | browser |
| Teardown | Arrêter 8081 et 5174 | Ports du joueur intacts | browser |

## Wireframe

```txt
┌────────────┬──────────────────────────────────────────┬─────────────┐
│ (1) Colonne│ (2) Barre : « Combat 1 — rejeu » · zoom   │ (5) Feuille │
│  campagne  ├──────────────────────────────────────────┤  Cible      │
│  Carnet |  │ (3) Carte : jetons, PV, active, dés 3D   │  Journal    │
│  Combat    │                                          │  (jusqu'au  │
│            │                                          │   pas n)    │
│            ├──────────────────────────────────────────┤             │
│            │ (4) ⏮  ◀  ▶/⏸  ▶  ⏭      Pas 7 / 23    │             │
└────────────┴──────────────────────────────────────────┴─────────────┘
```

1. Colonne de la campagne, sans les outils de préparation.
2. Barre : nom du combat et mention rejeu ; pas de contrôle de combat ni de grille.
3. Carte en lecture : zoom, sélection et mesure seulement.
4. Commandes du rejeu et position.
5. Onglets Feuille, Cible, Journal, sans bouton d'attaque.

## Tasks to do

### `1)` Router un combat

> L'adresse d'un combat mène au rejeu, à la reprise ou au direct.

1. Route `combatEntry` `/campagnes/:campaignId/combats/:combatId`, prop `combatId`.
2. À l'entrée : `api.combatEntry` ; terminé → `GameScreen` en rejeu ; en cours et courant → `replace` vers `/combat` ; en cours non courant → `api.resumeCombat` puis `replace` vers `/combat`.

### `2)` Lecture seule

> En rejeu, l'écran montre sans rien modifier.

1. `GameScreen` prop `replay` : charge `api.combatReplay`, pas de `catalog`, `scene`, `combat`, `journal` du direct.
2. Masque outils du rail, grille, `CombatControl`, retrait, attaques (`:actions="false"`), `DecisionDialog` ; `movable` faux ; `onKey` n'accepte que zoom et sélection ; pas d'historique.

### `3)` Avancer, reculer, lire

> Un pas = une action, montré comme en direct.

1. État du pas n : scène, combat, feuilles du pas ; journal = entrées des pas 1..n.
2. Pas avant : journal tronqué au pas n, positions du pas, puis `throwsOf` / `trayFromJournal` / `rollThenLand` sur les entrées du pas, avec la cible et ses PV fournis par le pas (`targetId`, `targetHitPoints`), comme un `ActionResult` ; `rollThenLand` rend une promesse résolue après `showImpact`.
3. Pas arrière, début, fin : état posé d'emblée, dés annulés.
4. `ReplayControls` : lecture enchaîne pas avant, 2 s après la promesse ; pause ; arrêt à la fin ; tout geste manuel met en pause.

### `4)` Documentation

> La mémoire dit le nouveau modèle.

1. `architecture.md`, `codebase-map.md`, `project-brief.md` ; `DESIGN.md` si besoin.

## Test acceptance criteria

| Task | Acceptance criteria |
| ---- | ------------------- |
| 1 | Un combat terminé s'ouvre en rejeu ; un combat en cours s'ouvre en direct, repris s'il n'était pas courant |
| 2 | En rejeu, aucun geste ne modifie la partie (aucune requête autre que GET dans l'onglet Réseau) |
| 3 | Les cinq commandes donnent l'état attendu ; en lecture, chaque pas part 2 s après la fin de l'animation du précédent ; `npm run build` passe |
| 4 | Les trois fichiers de mémoire décrivent combats, rejeu, reprise et section « Combats » |
