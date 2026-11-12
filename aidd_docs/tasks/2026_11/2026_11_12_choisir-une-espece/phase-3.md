---
status: done
---

# Instruction: choix de l'espèce sur la fiche

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/modules/dnd/
├── api.ts                    ✏️ species()
├── sheet.ts                  ✏️ SheetValues + size, speciesTraits ; Build + species (SpeciesChoice) ; SpeciesOptions
├── GuidedCreation.vue        ✏️ partie « Espèce » : espèce, taille, option, compétence, incantation ; compétences de classe et d'espèce exclusives
└── CharacterSheetView.vue    ✏️ champs « Taille » (identité) et « Traits d'espèce » (textes)
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081 / 5174, campagne neuve avec D&D, « Aria » construite en Guerrier, Athlétisme et Perception | partie « Espèce » à « — » | browser |
| Happy path   | Espèce → Nain | Espèce « Nain », Taille « M (moyenne) », Vitesse 9, PV 12, Traits d'espèce remplis | browser |
| Happy path   | Espèce → Elfe, lignage Elfe sylvestre, Sagesse, Sens aiguisés | Vitesse 10,5, PV 11 ; Perception éteinte dans Sens aiguisés (déjà maîtrisée) ; Intuition choisie passe en maîtrise et s'éteint dans les compétences de classe | browser |
| Happy path   | retoucher la vitesse à 12, puis lignage → Haut-elfe | vitesse 12 → 9 réécrite (sa valeur dérivée passe de 10,5 à 9) | browser |
| Edge case    | une fiche dont l'espèce était saisie à la main, puis Classe → Guerrier | l'espèce saisie reste ; « — » proposé dans Espèce | browser |
| Edge case    | recharger l'onglet | espèce, sous-choix et valeurs reviennent | browser |
| Edge case    | refus du serveur (400) | raison affichée dans la section, choix remis à la construction gardée | browser |
| Teardown     | arrêter 8081 et 5174 | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────────┐
│ (1) Création guidée                                       [▾] │
│  Classe [Guerrier ▾]                                          │
│  (2) Espèce [Elfe ▾]   Taille M (moyenne)                     │
│      Lignage elfique [Elfe sylvestre ▾]  texte du lignage     │
│      Incantation [Sagesse ▾]  Sens aiguisés [Intuition ▾]     │
│  (3) Caractéristiques · Compétences · Style · Équipement ·    │
│      Bottes (inchangés)                                       │
│  (4) raison d'un refus                                        │
├──────────────────────────────────────────────────────────────┤
│ (5) Identité : Classe · Niveau · Espèce · Taille · Historique │
│ (6) Caractéristiques · Combat · Attaques (inchangés)          │
│ (7) Textes : Capacités · Traits d'espèce · Équipement · …     │
└──────────────────────────────────────────────────────────────┘
```

1. Section existante ; la partie Espèce ne s'affiche qu'une classe choisie.
2. Liste des 9 espèces (« — » tant qu'aucune n'est choisie) ; taille en liste pour Humain et Tieffelin, en texte sinon ; option, incantation et compétence seulement si l'espèce en a, chacune avec « à choisir ».
3. Choix de classe existants ; une compétence prise par l'espèce y est éteinte.
4. Raison d'un refus, inchangée.
5. Identité : + « Taille », texte retouchable.
6. Fiche existante.
7. Textes : + « Traits d'espèce », après « Capacités ».

## Tasks to do

### `1)` Client de l'espèce

> Les types et l'appel des options d'espèce.

1. `sheet.ts` : `SpeciesChoice`, `SpeciesOptions`, `Build.species`, `SheetValues.size`, `SheetValues.speciesTraits`.
2. `api.ts` : `species()`.

### `2)` Partie « Espèce » de la création guidée

> Le joueur choisit l'espèce et ses sous-choix.

1. `GuidedCreation.vue` : lit `species()` au montage ; choisir une espèce émet la construction avec l'espèce seule ; chaque sous-choix émet la construction entière ; « — » seulement sans espèce.
2. Compétence d'espèce et compétences de classe s'éteignent l'une l'autre.

### `3)` Champs de la fiche

> Taille et traits se voient et se retouchent.

1. `CharacterSheetView.vue` : « Taille » dans l'identité, « Traits d'espèce » dans les textes, enregistrés comme les autres.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe ; les types suivent les DTO du serveur |
| 2 | Choisir chaque espèce du Test Scope remplit la fiche dans le navigateur ; une compétence déjà maîtrisée n'est pas proposable |
| 3 | Taille et traits retouchés survivent au rechargement |
