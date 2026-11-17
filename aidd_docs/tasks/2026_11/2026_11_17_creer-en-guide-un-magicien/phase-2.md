---
status: done
---

# Instruction: sorts au serveur et emplacements suivis par la fiche

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-server/src/main/java/dnd/server/
├── DndDtos.java          ✏️ SheetValuesDto + spellcastingAbility, cantrips, spellbook, preparedSpells ; DerivedDto + spellSaveDc, spellAttackBonus ; BuildDto + cantrips, spellbook, prepared ; ClassOptionsDto + incantation (nombres, sorts au choix) ; SpellOptionsDto
├── SheetResource.java    ✏️ GET spells ; sustained prend les événements de fiche
├── SheetStore.java       ✏️ événement « expended » ; construction gardée sans sorts relue comme vide
└── SheetCharacters.java  ✏️ sustained : DamageDealt et SpellSlotExpended
vtt-server/src/main/java/solovtt/server/
└── SceneSession.java     ✏️ garde aussi SpellSlotExpended d'un personnage posé
dnd-server/src/test/java/dnd/server/
├── SheetResourceTest.java    ✏️ Magicien construit ; refus ; GET spells ; fiche d'avant relue
└── SheetCharactersTest.java  ✏️ emplacement dépensé suivi
vtt-server/src/test/java/solovtt/server/
└── FicheSuitLeCombatTest.java ✏️ Bouclier lancé : emplacement envoyé à la fiche ; rejeu : rien ; reprise : le personnage posé garde Bouclier et ses emplacements
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | campagne en mémoire avec D&D, fiche créée | fiche vide, sans sort | api |
| Happy path   | `PUT sheets/{id}/build` Magicien suggéré | 200 : valeurs avec sorts, `derived.spellSaveDc` 12, `spellAttackBonus` 4, emplacement niveau 1 max 2 | api |
| Happy path   | `GET …/dnd/classes` | Magicien : 3 sorts mineurs, grimoire 6, 4 préparés, sorts au choix par niveau ; autres classes sans incantation | api |
| Happy path   | `GET …/dnd/spells` | 45 sorts, id, libellé, niveau, école, C / R | api |
| Happy path   | `PUT sheets/{id}` fiche libre avec Bouclier préparé, Int | 200, relue identique | api |
| Happy path   | `SheetCharacters.sustained` avec `SpellSlotExpended(1)` et `DamageDealt(3)` | histoire + `expended` et `lost` ; fiche : 1 dépensé, PV − 3 | api |
| Happy path   | combat : Gobelin touche le personnage Magicien, « Lancer Bouclier » | après la réponse, la fiche reçoit `SpellSlotExpended(1)` | system |
| Happy path   | rejeu du combat (`replayed`, `CombatReplay`) | aucun envoi à la fiche | system |
| Happy path   | reprise de la partie (rejeu des commandes) après la pose d'un Magicien qui prépare Bouclier | la créature recréée connaît Bouclier et a ses emplacements restants | system |
| Edge case    | construction refusée (préparé hors grimoire, 7 sorts de grimoire) | 400 avec la raison, rien d'écrit | api |
| Edge case    | `modules/dnd.json` d'avant, fiche et construction sans champs de sort | 200, sorts vides, caractéristique `null` | api |
| Edge case    | événement `expended` sans niveau | 409 | api |
| Teardown     | campagne en mémoire jetée | aucun fichier | system |

## Tasks to do

### `1)` DTO et lecture des sorts

> Le client lit et envoie sorts, DD et attaque de sort, et la liste des sorts.

1. `SheetValuesDto`, `DerivedDto`, `BuildDto` : nouveaux champs ; `null` lu comme vide.
2. `ClassOptionsDto` : caractéristique d'incantation, nombres, sorts au choix (mineurs, 1er niveau), construction suggérée ; `SpellOptionsDto` et `GET …/dnd/spells`.
3. `SheetStore` : `expended` avec `level` (409 sans niveau) ; construction d'avant sans sorts relue.

### `2)` Emplacements suivis

> Un emplacement dépensé en combat baisse sur la fiche, après la garde, jamais au rejeu.

1. `SceneSession` : l'abonnement garde `SpellSlotExpended` d'un personnage posé (entrée à `profile() != null`), comme `DamageDealt`.
2. `SheetCharacters.sustained` : `DamageDealt` → `HitPointsLost`, `SpellSlotExpended` → `SpellSlotExpended`, dans l'ordre ; `SheetResource.sustained` écrit ces événements ensemble, sous le verrou.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Magicien construit : DD 12, attaque +4, 2 emplacements ; `GET spells` rend les 45 sorts ; une fiche d'avant se relit sans sort ; refus en 400 |
| 2 | Bouclier lancé en combat : la fiche a 1 emplacement dépensé ; rejeu et reprise : aucun envoi ; PV et emplacements d'une même action écrits ensemble |
