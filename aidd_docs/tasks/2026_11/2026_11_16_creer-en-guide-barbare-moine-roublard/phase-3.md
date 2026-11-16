---
status: done
---

# Instruction: choix des trois classes sur la fiche

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/modules/dnd/
├── sheet.ts              ✏️ Build + expertise, tool ; fightingStyle facultatif ; ClassOptions + expertiseCount, tools
└── GuidedCreation.vue    ✏️ Style et Bottes masqués sans choix ; partie Expertise ; outil au choix ; Expertise retirée d'une compétence qui perd sa maîtrise
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081 / 5174, campagne neuve avec D&D, personnage « Kael » | Création guidée, Classe « — » | browser |
| Happy path   | Classe → Barbare, deux compétences | PV 14, CA 15, Hache à deux mains, Hachette, Attaque à mains nues ; Bottes (2) parmi les armes de corps à corps ; pas de Style | browser |
| Happy path   | Classe → Moine, outil → Luth | ni Style ni Bottes ; CA 14 ; Attaque à mains nues 1d6 ; « Outils : Luth » ; luth dans l'Équipement | browser |
| Happy path   | Moine, équipement B | Équipement « 50 po » ; Lance et Dague retirées ; outil gardé dans Maîtrises | browser |
| Happy path   | Classe → Roublard, quatre compétences, Expertise Discrétion et Escamotage | Expertise ne propose que les compétences maîtrisées, deux au plus ; bonus de Discrétion à +2 × 2 + Dex | browser |
| Happy path   | espèce et historique choisis, puis changer de classe | espèce et historique gardés | browser |
| Edge case    | Roublard, Expertise Athlétisme, puis décocher Athlétisme | Expertise Athlétisme retirée dans le même envoi, aucun refus | browser |
| Edge case    | recharger l'onglet | classe, Expertise, outil et valeurs reviennent | browser |
| Edge case    | Guerrier existant | Style et Bottes (3) comme avant ; équipement C proposé | browser |
| Teardown     | arrêter 8081 et 5174 | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────────┐
│ (1) Création guidée                                       [▾] │
│  Classe [Moine ▾]                                             │
│  (2) Espèce · Historique · Caractéristiques (inchangés)       │
│  (3) Compétences (2)  ☑ ☑ ☐ …                                 │
│  (4) Expertise (2)    ☑ Discrétion ☑ Escamotage ☐ … (Roublard)│
│  (5) Outil [à choisir ▾] (Moine : artisan | instrument)       │
│  (6) Style de combat [Défense ▾]   (Guerrier seul)            │
│  (7) Équipement (•) A : texte  ( ) B : 50 po                  │
│  (8) Bottes d'arme (2) [Dague ▾] [Arc court ▾]  (sauf Moine)  │
│  (9) raison d'un refus                                        │
└──────────────────────────────────────────────────────────────┘
```

1. Section existante.
2. Parties existantes, inchangées.
3. Compétences de la classe, au nombre de la classe.
4. Nouvelle partie, si `expertiseCount` > 0 : cases des compétences maîtrisées (classe, espèce, historique, Doué), éteintes au-delà du nombre.
5. Nouvelle liste, si la classe choisit un outil : « à choisir » puis les outils groupés par catégorie.
6. Style de combat, seulement si la classe en propose.
7. Équipement, avec l'option tout en po de la classe.
8. Bottes, seulement si `masteryCount` > 0, parmi les armes servies pour la classe.
9. Raison d'un refus, inchangée.

## Tasks to do

### `1)` Types

> Le client suit le contrat du serveur.

1. `Build.expertise: Skill[]`, `tool: string | null`, `fightingStyle: string | null` ; `ClassOptions.expertiseCount`, `tools`.

### `2)` Expertise, outil, classes sans Style ni Bottes

> Le joueur construit chacune des trois classes sans refus du serveur.

1. Style de combat et Bottes affichés seulement si la classe en a.
2. Partie Expertise : compétences maîtrisées par la construction affichée, `expertiseCount` au plus.
3. Liste d'outil, groupée par catégorie (`optgroup`), si la classe en choisit un.
4. Tout envoi qui change compétences, espèce ou historique retire de `expertise` les compétences qui ne sont plus maîtrisées.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe |
| 2 | Chaque ligne du Test Scope s'observe sur 5174 ; aucun refus du serveur pour une construction faite à l'écran |
