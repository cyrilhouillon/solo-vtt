---
status: pending
---

# Instruction: module client `vtt-web/modules/dnd`

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── vtt-web/modules/dnd/
    ├── index.ts                  ✅ NotebookModule « dnd », titre « Personnages joueurs »
    ├── api.ts                    ✅ sections, création, lecture, saisie
    ├── sheet.ts                  ✅ types de la fiche, compétences et caractéristiques en français
    ├── DndPanel.vue              ✅ « Nouveau personnage »
    └── CharacterSheetView.vue    ✅ la fiche, éditable, enregistrée seule
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081 et 5174 ; campagne neuve avec D&D coché | Carnet ouvert ; l'arbre montre « Personnages joueurs » | browser |
| Happy path   | panneau D&D : « Nouveau personnage », nom « Aria » | une entrée « Aria » sous la section, ouverte en onglet sur une fiche vide | browser |
| Happy path   | saisir Dex 14, cocher Discrétion en expertise, CA 15, une attaque, du texte en Capacités | dérivés mis à jour aussitôt (mod. +2, Discrétion +6) ; enregistré une seconde après la dernière frappe | browser |
| Happy path   | recharger l'onglet, puis redémarrer le serveur 8081 | la fiche revient à l'identique | browser |
| Happy path   | `[[Aria` dans une autre page ; recherche « aria » ; renommer depuis l'arbre | le lien ouvre la fiche ; la recherche la trouve ; l'onglet et le lien suivent le nouveau nom | browser |
| Edge case    | saisir Dex 31 | refus affiché sur la fiche, valeur gardée à l'écran, rien d'enregistré | browser |
| Edge case    | « Modules » : désactiver D&D, puis réactiver | la section disparaît puis revient avec ses fiches | browser |
| Edge case    | campagne sans D&D, lien vers une fiche ouvert | « Module dnd inactif dans cette campagne. » | browser |
| Teardown     | arrêter 8081 et 5174 | 8080 et 5173 jamais touchés | system |

## Wireframe

```txt
┌───────────────┬──────────────────────────────────────────────┬──────────────┐
│ (1) Arbre     │ (2) Onglets : Session 3 | Aria                │ (6) Modules  │
│ Pages         ├──────────────────────────────────────────────┤ ┌──────────┐ │
│ …             │ (3) Aria                                      │ │Personnages│ │
│ Personnages   │ Classe [Guerrier] Niveau [1] Espèce [Elfe]    │ │ joueurs   │ │
│ joueurs       │ Historique [Soldat]                           │ │[Nouveau   │ │
│  ▸ Aria       ├──────────────┬───────────────────────────────┤ │personnage]│ │
│  ▸ Borin      │ (4) Caract.  │ (5) Combat                    │ └──────────┘ │
│               │ FOR [15] +2  │ CA [16] Init [+2] Vit. [9] m  │              │
│               │ DEX [14] +2  │ PV [12]/[12]  DV [1d10]       │              │
│               │ …            │ Attaques : nom bonus dégâts   │              │
│               │ Maîtrise [+2]│  [Épée longue][+5][1d8+3 tr.] │              │
│               │ Sauvegardes  │ Emplacements : niv. [1] [2]/[0]│              │
│               │ ☑ FOR +4 …   ├───────────────────────────────┤              │
│               │ Compétences  │ (7) Capacités · Équipement ·  │              │
│               │ ◐ Discr. +6  │ Maîtrises · Langues (textes)  │              │
│               │ Perc. passive│                               │              │
│               │ 13           │                               │              │
│               ├──────────────┴───────────────────────────────┤              │
│               │ (8) Pages qui citent Aria                     │              │
└───────────────┴──────────────────────────────────────────────┴──────────────┘
```

1. Arbre du Carnet : la section « Personnages joueurs » et ses fiches, comme des pages.
2. Onglets : une fiche ouverte a son onglet.
3. Titre (nom du personnage, renommé depuis l'arbre) et identité saisie.
4. Caractéristiques et modificateurs, maîtrise, sauvegardes et compétences (aucune, maîtrise, expertise) avec leurs bonus calculés, Perception passive.
5. Valeurs de combat saisies, attaques en lignes, emplacements par niveau.
6. Panneau du module : créer un personnage.
7. Textes libres.
8. Pages citantes, comme pour toute page.

## Tasks to do

### `1)` Déclarer le module

> Le module D&D prépare sa section et propose « Nouveau personnage ».

1. `index.ts` : `id: 'dnd'`, `title: 'Personnages joueurs'`, `prepare` appelle `POST …/dnd/sections`, `panel: DndPanel`, `entryView: CharacterSheetView`.
2. `api.ts` sur `request` de `src/api.ts`.
3. `DndPanel.vue` : champ nom + « Nouveau personnage » ; crée la fiche puis l'ouvre par `context.openPage`.

### `2)` Afficher et saisir la fiche

> La fiche se lit et se modifie sur place, enregistrée seule.

1. `CharacterSheetView.vue` suit le wireframe ; champs étiquetés, accessibles au clavier ; style par les jetons de `src/style.css` (`DESIGN.md`).
2. Dérivés recalculés à l'écran à chaque frappe (`sheet.ts`), le serveur faisant foi à l'enregistrement.
3. Enregistrement une seconde après la dernière frappe, un envoi à la fois ; refus du serveur affiché sur la fiche, valeurs gardées et renvoyées au changement suivant.
4. Validation dans le navigateur (8081 / 5174) du Test Scope.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Dans une campagne à D&D actif, la section « Personnages joueurs » apparaît, et « Nouveau personnage » crée une fiche ouverte en onglet ; aucun `modules/dnd` dans `vtt-web/src`. |
| 2 | Chaque rubrique de la fiche D&D 2024 se saisit et se garde après rechargement et redémarrage ; les dérivés suivent la saisie ; un refus s'affiche sans perte de saisie ; la fiche se lie, se cherche et se renomme comme une page ; désactiver puis réactiver D&D la rend intacte. |
