---
status: done
---

# Instruction: fiche, attaques structurées de bout en bout

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-server/src/
├── main/java/dnd/server/DndDtos.java           ✏️ AttackDto structuré
└── test/java/dnd/server/SheetResourceTest.java ✏️
vtt-web/modules/dnd/
├── sheet.ts                                    ✏️ type SheetAttack, types de dégâts, texte des dégâts
└── CharacterSheetView.vue                      ✏️ ligne d'attaque en champs structurés
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081 / 5174, campagne neuve avec D&D | Section « Personnages joueurs » | browser |
| Happy path   | `PUT …/dnd/sheets/{id}` avec une attaque structurée | 200, l'attaque relue à l'identique | api |
| Happy path   | Création guidée Guerrier | Les attaques s'affichent en champs remplis (1d8 +3 tranchants, 1,5 m) | browser |
| Happy path   | Ajouter une attaque, choisir 1d6, perforants, portée 24/96 | Enregistrée seule après une seconde ; relue après rechargement | browser |
| Edge case    | Ni allonge ni portée | Refus 400 affiché sur la fiche, saisie gardée | browser |
| Edge case    | `modules/dnd.json` d'avant (attaques en texte) | `GET` de la fiche : 409 | api |
| Teardown     | Arrêter 8081 puis 5174 | Ports libres | system |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────────────────────────┐
│ (1) Attaques                                                                 │
│ Nom           Bonus Dés        Bonus dég. Type            Allonge Portée     │
│ [Épée longue] [+5]  [1]d[8 ▾]  [+3]       [tranchants ▾]  [1,5]   [  ]/[  ] ×│
│ (2) 1d8+3 tranchants · 1,5 m                                                 │
│ [Javeline   ] [+5]  [1]d[6 ▾]  [+3]       [perforants ▾]  [1,5]   [9 ]/[36] ×│
│     1d6+3 perforants · 1,5 m ; lancer 9/36 m                                 │
│ (3) [+ Attaque]                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

1. Tableau des attaques : un champ par donnée ; dé en liste (d4 à d20), type en liste des 13 types ; allonge et portées vides si l'attaque n'en a pas.
2. Sous chaque ligne, le résumé lisible tiré des champs.
3. Ajout d'une attaque, comme aujourd'hui.

## Tasks to do

### `1)` Format d'échange

> Le serveur reçoit et rend des attaques structurées.

1. `AttackDto(name, bonus, diceCount, dieSides, damageBonus, damageType, reach, normalRange, longRange)`, `damageType` en nom d'enum.
2. Mapper dans les deux sens ; `EventFile` suit `SheetValuesDto`, si bien qu'un document d'avant rend 409.
3. Adapter `SheetResourceTest` ; ajouter le refus d'une attaque sans allonge ni portée (400) et la lecture d'un document d'avant (409).

### `2)` Saisie sur la fiche

> Le joueur saisit une attaque champ par champ.

1. `sheet.ts` : type `SheetAttack` structuré, liste des types de dégâts (nom, libellé), `damageText(attack)`.
2. `CharacterSheetView.vue` : champs du wireframe ; une nouvelle attaque part de 1d6, contondants, allonge 1,5 m ; champ vide → `null` à l'envoi.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `mvn -q test -pl dnd-server` passe ; attaque invalide 400 avec la raison ; fiche d'avant 409 |
| 2 | `npm run build` passe ; dans le navigateur (5174), une attaque saisie s'enregistre et se relit, une invalide affiche le refus |
