---
status: done
---

# Instruction: section « Création guidée » de la fiche

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/modules/dnd/
├── api.ts                    ✏️ classes(), build(pageId, build), rollAbilities(pageId)
├── sheet.ts                  ✏️ types Build, AbilityRoll, ClassOptions ; Sheet + build, rolls ; coût de l'achat de points
├── GuidedCreation.vue        ✅ la section des choix guidés
└── CharacterSheetView.vue    ✏️ monte GuidedCreation en tête ; remplace ses valeurs par la réponse sans les renvoyer
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081 / 5174, campagne neuve avec D&D, personnage « Aria » créé | fiche vide, section « Création guidée » avec Classe « — » | browser |
| Happy path   | Classe → Guerrier | la fiche se remplit (CA 17, PV 11, trois attaques), la section montre méthode, compétences, style, équipement, Bottes | browser |
| Happy path   | cocher Athlétisme et Perception | ces compétences passent en maîtrise, bonus recalculés | browser |
| Happy path   | retoucher la CA à 20, puis Équipement → B | CA 15 (Défense), attaques du cimeterre, de l'épée courte et de l'arc | browser |
| Happy path   | Méthode → achat de points, monter Dex | « Points restants » suit la frappe ; au-delà de 27, le bouton d'application est éteint avec la raison | browser |
| Happy path   | Méthode → dés, « Lancer les dés » | six totaux et leurs dés (le plus bas barré) ; affectation par listes ; plus de bouton de lancer | browser |
| Edge case    | recharger l'onglet | choix, jets et valeurs reviennent | browser |
| Edge case    | taper une valeur puis changer un choix moins d'une seconde après | la valeur tapée part d'abord, puis la construction ; aucun enregistrement en double | browser |
| Edge case    | refus du serveur (400) | raison affichée dans la section, fiche inchangée | browser |
| Teardown     | arrêter 8081 et 5174 | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────────┐
│ (1) Enregistrée                                               │
├──────────────────────────────────────────────────────────────┤
│ (2) Création guidée                                       [▾] │
│  Classe [Guerrier ▾]                                          │
│  (3) Caractéristiques  Méthode [Standard ▾]                   │
│      FOR [15▾] DEX [14▾] CON [13▾] INT [8▾] SAG [10▾] CHA [12▾]│
│      Points restants 0 / 27   ·  [Lancer les dés]  4-4-3-(1)… │
│  (4) Compétences (2)  ☑ Athlétisme ☐ Acrobaties ☑ Perception …│
│  (5) Style de combat  [Défense ▾]                             │
│  (6) Équipement  ◉ A : cotte de mailles, épée à deux mains…   │
│                  ○ B : armure de cuir clouté, cimeterre…      │
│  (7) Bottes d'arme (3)  [Épée à deux mains ▾] [Fléau ▾] […▾]  │
│  (8) raison d'un refus                                        │
├──────────────────────────────────────────────────────────────┤
│ (9) Identité · Caractéristiques · Combat · Textes (inchangés) │
└──────────────────────────────────────────────────────────────┘
```

1. État d'enregistrement, inchangé.
2. Section repliable (`FoldSection`) ; Classe « — » cache le reste : la fiche est en saisie libre.
3. Méthode et répartition : listes de valeurs (standard, dés) ou champs 8–15 (achat) ; points restants à la frappe ; jets montrés avec le dé écarté.
4. Cases des 9 compétences du Guerrier ; une troisième est éteinte.
5. Les quatre styles du SRD.
6. Équipement A ou B, en texte du SRD.
7. Trois listes d'armes courantes et de guerre, chacune avec sa botte.
8. Raison d'un refus du serveur.
9. La fiche existante, que la construction remplit et qui reste retouchable.

## Tasks to do

### `1)` Client de la construction

> Les types et appels des nouvelles routes.

1. `sheet.ts` : `Build`, `AbilityRoll`, `ClassOptions`, `Sheet.build`, `Sheet.rolls` ; `pointBuyCost(scores, costs)`.
2. `api.ts` : `classes()`, `build(pageId, build)`, `rollAbilities(pageId)`.

### `2)` Section « Création guidée »

> Le joueur fait ses choix sur la fiche.

1. `GuidedCreation.vue` : lit `classes()` au montage ; rend les choix du wireframe ; émet la construction à chaque choix (une valeur d'achat de points à la sortie du champ), le lancer de dés, et montre la raison d'un refus.
2. Bouton éteint pour un achat au-delà de 27 points ; liste d'affectation sans doublon pour standard et dés.

### `3)` Appliquer un choix sans conflit avec l'enregistrement

> Un choix remplace les valeurs à l'écran par celles du serveur.

1. `CharacterSheetView.vue` : sur un choix, attendre `saved()`, envoyer la construction, puis remplacer valeurs, construction et jets par la réponse sans que le `watch` le prenne pour une frappe ; choix éteints pendant l'envoi.
2. Même chemin pour « Lancer les dés ».

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe ; les types suivent les DTO du serveur |
| 2 | Choisir Guerrier, puis chaque choix, remplit la fiche dans le navigateur ; un achat au-delà de 27 est refusé avant l'envoi |
| 3 | Une frappe suivie d'un choix donne deux envois dans l'ordre et aucun troisième ; recharger l'onglet redonne la même fiche |
