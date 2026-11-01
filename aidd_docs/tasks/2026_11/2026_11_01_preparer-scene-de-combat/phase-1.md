---
status: done
---

# Instruction: scène générique et catalogue SRD

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-engine/src/main/java/solovtt/scene/
│   ├── DistanceUnit.java          ✅ enum METRE, FOOT avec libellé (« m », « ft »)
│   ├── Position.java              ✅ record (x, y) dans l'unité de la carte
│   ├── Token.java                 ✅ record (id, entityId, position)
│   └── Scene.java                 ✅ carte (largeur, hauteur, unité) + jetons, place, distance
├── vtt-engine/src/test/java/solovtt/scene/
│   └── SceneTest.java             ✅
├── dnd-engine/src/main/java/dnd/engine/state/
│   └── Creature.java              ✏️ ajout de speed (défaut 9 m)
├── dnd-engine/src/main/java/dnd/engine/catalog/
│   ├── CatalogEntry.java          ✅ record (id, nom, kind PERSONNAGE|MONSTRE, fabrique par id d'instance)
│   └── SrdCatalog.java            ✅ 2 personnages + 3 monstres SRD 5.2
└── dnd-engine/src/test/java/dnd/engine/catalog/
    └── SrdCatalogTest.java        ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `new Scene(30, 20, METRE)` | carte vide, aucun jeton | system |
| Happy path   | placer deux jetons à (0,0) et (3,4), demander la distance | 5.0 dans l'unité de la carte | system |
| Happy path   | instancier « gobelin » deux fois depuis `SrdCatalog` | deux `Creature` d'ids distincts, valeurs SRD | system |
| Edge case    | placer un jeton hors des bornes de la carte | `IllegalArgumentException` avec la raison | system |
| Edge case    | placer un second jeton sur un id déjà utilisé | `IllegalArgumentException` | system |
| Edge case    | distance avec un jeton inconnu | `IllegalArgumentException` « jeton inconnu » | system |
| Edge case    | id de catalogue inconnu | `IllegalArgumentException` | system |

## Tasks to do

### `1)` Scène générique

> `vtt-engine` sait représenter une carte vide et des jetons, et mesurer une distance, sans aucun type de jeu.

1. Créer `DistanceUnit`, `Position`, `Token` (records/enum).
2. Créer `Scene` : dimensions positives, unité, jetons ordonnés (`LinkedHashMap`), `place(Token)` qui valide bornes et unicité, `tokens()`, `token(id)`, `distance(idA, idB)` euclidienne centre à centre.
3. Écrire `SceneTest` couvrant le Test Scope.

### `2)` Vitesse des créatures

> La feuille peut afficher la vitesse, prérequis de la story « déplacer ».

1. Ajouter `speed` (en mètres, défaut 9) au builder et à la lecture de `Creature`, copié par `copy()`.
2. Lancer `mvn -q test` : les tests existants passent sans modification.

### `3)` Catalogue SRD

> Le joueur choisit des créatures prêtes à l'emploi, sans rien saisir.

1. Créer `CatalogEntry` et `SrdCatalog.entries()` / `SrdCatalog.create(entryId, instanceId)`.
2. Personnages niveau 1 calculés selon les règles SRD 5.2 (pas les valeurs de la démo, qui ne sont pas de niveau 1) : une guerrière (épée longue, style Duel) et une magicienne (dague, Bouclier, 2 emplacements de niveau 1), sans condition. Chaque entrée a une arme, car `AttackAction` lit `weapon()` sans contrôle de null.
3. Monstres : Gobelin guerrier, Squelette (vulnérable aux dégâts contondants), Loup ; CA, PV, vitesse et attaque principale relevés dans le SRD 5.2, avec un commentaire d'attribution CC-BY-4.0. Les immunités (poison du Squelette) ne sont pas modélisées : aucun dégât de poison n'existe dans le moteur ; le signaler en commentaire.
4. Écrire `SrdCatalogTest` : chaque entrée s'instancie, deux instances ont des ids distincts, valeurs clés d'un monstre vérifiées.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `SceneTest` passe ; `vtt-engine` n'importe aucun type `dnd` (`grep`) et reste sans dépendance runtime |
| 2 | `mvn -q test` passe ; `Creature.builder(...).build().speed()` vaut 9 |
| 3 | `SrdCatalogTest` passe ; chaque entrée a une arme et une attaque scriptée de l'une sur une autre se résout sans erreur |
