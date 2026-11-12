---
status: pending
---

# Instruction: choix de l'historique et de Polyvalent sur la fiche

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/modules/dnd/
├── api.ts                    ✏️ backgrounds(), originFeats()
├── sheet.ts                  ✏️ SheetValues + feats ; Build + background (BackgroundChoice) ; SpeciesChoice + originFeat (OriginFeatChoice) ; BackgroundOptions, OriginFeatOptions
├── GuidedCreation.vue        ✏️ partie « Historique » ; don de Polyvalent dans la partie Espèce ; compétences de l'historique éteintes ailleurs
└── CharacterSheetView.vue    ✏️ champ « Dons » (textes)
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081 / 5174, campagne neuve avec D&D, « Aria » construite en Guerrier, Athlétisme et Perception | partie « Historique » à « — » | browser |
| Happy path   | Historique → Soldat | Athlétisme décochée et éteinte, Intimidation éteinte ; augmentations +1 For/Dex/Con ; Historique « Soldat », Dons « Sauvagerie martiale », attaques Arc court et Lance | browser |
| Happy path   | augmentations → +2 For / +1 Con ; boîte de jeux → dés ; équipement → B | For 17, Con 14 ; Maîtrises + boîte de jeux (dés) ; Équipement « 50 po » ; Arc court et Lance retirés | browser |
| Happy path   | Espèce → Humain, Polyvalent → Doué, trois compétences | compétences de classe et de l'historique éteintes dans Doué ; trois compétences maîtrisées ; Dons + Doué | browser |
| Happy path   | Historique → Acolyte, incantation Sagesse | Dons « Initié à la magie (Clerc) — incantation Sagesse ; … (à choisir) » ; For 15 | browser |
| Edge case    | une fiche dont l'historique était saisi à la main, puis Classe → Guerrier | l'historique saisi reste ; « — » proposé dans Historique | browser |
| Edge case    | recharger l'onglet | historique, augmentations, don et valeurs reviennent | browser |
| Edge case    | refus du serveur (400, valeur au-dessus de 20 aux dés) | raison affichée dans la section, choix remis à la construction gardée | browser |
| Teardown     | arrêter 8081 et 5174 | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────────┐
│ (1) Création guidée                                       [▾] │
│  Classe [Guerrier ▾]                                          │
│  (2) Espèce [Humain ▾] Taille [M ▾]  Compétent [… ▾]          │
│      Polyvalent [Doué ▾]  ☐ ☐ ☐ compétences de Doué (3)       │
│  (3) Historique [Soldat ▾]                                    │
│      Augmentations  [+2/+1 ▾]  +2 [Force ▾]  +1 [Constit. ▾]  │
│      Compétences : Athlétisme, Intimidation · Outil [dés ▾]   │
│      Don : Sauvagerie martiale — texte                        │
│      Équipement (•) A : texte   ( ) B : 50 po                 │
│  (4) Caractéristiques · Compétences · Style · Équipement ·    │
│      Bottes (inchangés)                                       │
│  (5) raison d'un refus                                        │
├──────────────────────────────────────────────────────────────┤
│ (6) Identité : Classe · Niveau · Espèce · Taille · Historique │
│ (7) Caractéristiques · Combat · Attaques (inchangés)          │
│ (8) Textes : Capacités · Traits d'espèce · Dons · Équipement… │
└──────────────────────────────────────────────────────────────┘
```

1. Section existante.
2. Partie Espèce existante ; pour l'Humain, liste Polyvalent (« à choisir » puis les 4 dons), puis les sous-choix du don : liste de sorts et incantation pour Initié, trois cases de compétences pour Doué (celles déjà données éteintes).
3. Nouvelle partie, après l'espèce : 4 historiques (« — » tant qu'aucun n'est choisi) ; mode d'augmentation (+2/+1 ou +1/+1/+1) et caractéristiques parmi les trois de l'historique ; compétences et outil en texte, boîte de jeux en liste pour le Soldat ; don et son texte, incantation pour Initié ; équipement A ou B.
4. Choix de classe existants ; une compétence de l'historique y est éteinte.
5. Raison d'un refus, inchangée.
6. Identité inchangée ; Historique s'y remplit.
7. Fiche existante.
8. Textes : + « Dons », après « Traits d'espèce ».

## Tasks to do

### `1)` Client de l'historique

> Les types et les appels des options d'historique et de don.

1. `sheet.ts` : `BackgroundChoice`, `OriginFeatChoice`, `BackgroundOptions`, `OriginFeatOptions`, `Build.background`, `SpeciesChoice.originFeat`, `SheetValues.feats`.
2. `api.ts` : `backgrounds()`, `originFeats()`.

### `2)` Partie « Historique » de la création guidée

> Le joueur choisit l'historique et ses sous-choix.

1. `GuidedCreation.vue` : lit `backgrounds()` et `originFeats()` au montage, avec `classes()` et `species()` ; choisir un historique émet la construction avec +1 aux trois, équipement A, et sans les compétences de classe, d'espèce ou de Doué qu'il donne ; chaque sous-choix émet la construction entière ; « — » seulement sans historique ; changer de classe garde l'historique.
2. Compétences de l'historique éteintes dans la classe, l'espèce et Doué.

### `3)` Don de Polyvalent

> L'Humain choisit son don d'origine.

1. Partie Espèce : liste Polyvalent pour l'Humain, sous-choix du don ; changer d'espèce retire le don.

### `4)` Champ de la fiche

> Les dons se voient et se retouchent.

1. `CharacterSheetView.vue` : « Dons » dans les textes, enregistré comme les autres.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe ; les options d'historique et de don arrivent au montage |
| 2 | Choisir Soldat remplit la fiche et éteint Athlétisme et Intimidation dans la classe ; changer les augmentations, la boîte ou l'équipement met la fiche à jour ; un refus s'affiche et remet les choix |
| 3 | Un Humain choisit Doué et trois compétences, ou Initié à la magie, sa liste et son incantation ; la fiche les montre |
| 4 | « Dons » s'affiche, se retouche et survit au rechargement |
