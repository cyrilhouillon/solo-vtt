---
status: done
---

# Instruction: PV perdus dans l'histoire de la fiche et port `sustained`

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-engine/src/main/java/dnd/engine/
├── catalog/PlayerCharacters.java   ✏️ default boolean sustained(campaignId, id, List<GameEvent> events)
└── sheet/
    ├── SheetEvent.java             ✏️ record HitPointsLost(int amount), amount > 0
    └── SheetHistory.java           ✏️ pli : PV actuels − amount, plancher 0 ; construction et jets gardés
dnd-engine/src/test/java/dnd/engine/sheet/
└── SheetHistoryTest.java           ✏️
dnd-server/src/main/java/dnd/server/
├── SheetStore.java                 ✏️ EventFile.amount ; type "lost"
├── SheetResource.java              ✏️ @Singleton ; synchronized sustained(campaignId, pageId, amounts)
└── SheetCharacters.java            ✏️ constructeur + SheetResource ; sustained : DamageDealt → HitPointsLost
dnd-server/src/test/java/dnd/server/
├── SheetCharactersTest.java        ✏️
└── SheetResourceTest.java          ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | fiche Guerrier construite (PV 12 / 12), `ModuleData` en mémoire | `take` rend un profil à 12 PV | system |
| Happy path   | `sustained` avec `DamageDealt(…, 5)` | `true` ; `GET` de la fiche : PV actuels 7 ; `dnd.json` porte `{ "type": "lost", "amount": 5 }` | system |
| Happy path   | nouvelles instances de `SheetResource` et `SheetCharacters` sur le même `ModuleData`, puis `take` | profil à 7 PV | system |
| Happy path   | `PUT` de la fiche à 10 PV, puis un coup de 3 | 7 PV : le delta se compose avec la retouche | system |
| Edge case    | coup de 20 sur 7 PV | 0 PV, jamais négatif ; la fiche se relit | system |
| Edge case    | `sustained` pour un id qui n'est pas une fiche vivante (inconnue, à la corbeille) | `false`, rien écrit | system |
| Edge case    | `sustained` sans `DamageDealt` sur ce personnage | `true`, rien écrit | system |
| Edge case    | `dnd.json` abîmé | `ClientErrorException` 409, fichier intact | system |
| Edge case    | `HitPointsLost(0)` ou négatif ; `lost` sans `amount` dans `dnd.json` | `IllegalArgumentException` ; fiche illisible 409 | system |

## Tasks to do

### `1)` La fiche perd des PV

> Un coup est un événement de la fiche, un delta plié en PV ≥ 0.

1. `SheetEvent.HitPointsLost(int amount)`, `amount > 0` sinon `IllegalArgumentException`.
2. `SheetHistory.replay` : valeurs avec `hitPoints = max(0, hitPoints − amount)` (`CharacterSheet.withHitPoints` si absente), construction, caractéristiques lues et jets gardés.
3. `SheetHistoryTest` : perte, plancher, perte après une saisie, construction gardée.

### `2)` Le port des personnages reçoit les coups

> Le cœur envoie à la source d'un PJ ce qui lui arrive, sans connaître la fiche.

1. `PlayerCharacters.sustained(String campaignId, String id, List<GameEvent> events)` : `default false` ; `true` si `id` est l'un de ses personnages et que la fiche a suivi ; lève sinon, avec la raison.
2. `SheetStore.EventFile` : champ `amount` (`NON_NULL`), type `lost` lu et écrit ; `lost` sans `amount` lève `IllegalStateException` (409).
3. `SheetResource` : `@Singleton` ; `synchronized void sustained(campaignId, pageId, List<Integer> amounts)` ajoute un `HitPointsLost` par montant positif, en une seule écriture, sous le verrou des autres écritures.
4. `SheetCharacters(ModuleData, NotebookPages, SheetResource)` ; `sustained` : `false` si la page n'est pas une entrée vivante du module ; sinon les montants des `DamageDealt` qui ciblent ce personnage, passés à `SheetResource.sustained`, puis `true`.
5. Tests : `SheetCharactersTest` (constructeur, cas du Test Scope), `SheetResourceTest` (fiche relue avec `lost`).

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Une histoire avec `HitPointsLost` se plie en PV baissés, jamais sous 0, construction et jets intacts |
| 2 | Un coup envoyé à `SheetCharacters` baisse la fiche lue par `GET` et par `take`, relue par une autre instance ; une page qui n'est pas une fiche rend `false` sans écrire ; un document abîmé lève 409 sans être écrasé |
