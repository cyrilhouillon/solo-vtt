---
status: done
---

# Instruction: Client : lancer le combat, ordre de jeu, créature active

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── vtt-web/src/
    ├── App.vue                       ✏️
    ├── api.ts                        ✏️
    ├── types.ts                      ✏️
    ├── style.css                     ✏️
    ├── components/
    │   ├── CombatControl.vue         ✅
    │   ├── DecisionDialog.vue        ✏️
    │   ├── JournalPanel.vue          ✏️
    │   ├── MapView.vue               ✏️
    │   └── TokenBoard.vue            ✏️
    └── map/
        └── SceneCanvas.ts            ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081 / 5174 ; nouvelle carte, deux personnages et trois monstres posés | « Lancer le combat » actif dans la barre | browser |
| Happy path   | cliquer « Lancer le combat » | tableau trié par initiative, colonne Init. ; ligne active « À son tour » ; jeton actif marqué sur la carte ; barre « Round 1 · Tour de … » ; journal : initiatives, ordre, round, tour | browser |
| Happy path   | cliquer « Fin du tour » jusqu'après la dernière créature | marque déplacée à chaque clic ; « Round 2 » puis la première créature ; journal à jour | browser |
| Edge case    | ex æquo complets | dialogue « Qui joue ? » avec les noms ; le choix rend la créature active | browser |
| Edge case    | attaquer depuis la feuille d'une créature inactive | refus affiché « Ce n'est pas le tour de … » | browser |
| Edge case    | en combat, poser puis retirer une créature | elle entre dans l'ordre (tableau et journal), puis en sort ; retirer la créature active passe à la suivante | browser |
| Edge case    | recharger la page en plein combat | ordre, créature active et round relus du serveur | browser |
| Edge case    | carte vide | « Lancer le combat » désactivé, raison au survol | browser |
| Edge case    | « Nouvelle carte » en combat | contrôle revenu à « Lancer le combat », tableau en ordre de pose | browser |
| Teardown     | arrêter 5174 puis 8081 | ports de l'utilisateur intacts | system |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────────────────────────┐
│ (1) Solo VTT · Carte 30 m × 20 m · 5 créatures   (2) Round 2 · Tour de Loup   │
│                                                      [Fin du tour]  ☾         │
├────┬──────────────────────────────────────────────┬──────────────────────────┤
│Rail│ (3) Sur la carte                             │ Feuille | Cible | Journal │
│    │  Init. Nom          Camp   PV     CA  État   │                          │
│    │  17    Gobelin      Monstre 10/10 15  Joué   │ (5) Journal              │
│    │▶ 15    Loup         Monstre 11/11 12 À son   │  Le combat commence      │
│    │                                      tour    │  Round 2                 │
│    │  12    Guerrière    Perso  12/12  18         │  Tour de Gobelin         │
│    │ ┌──────────────────────────────────────────┐ │  Fin du tour de Gobelin  │
│    │ │ (4) Carte : jeton actif marqué            │ │  Tour de Loup            │
│    │ └──────────────────────────────────────────┘ │                          │
│    │ Barre d'état · Piste à dés                   │                          │
└────┴──────────────────────────────────────────────┴──────────────────────────┘
```

1. Barre : nom, carte, nombre de créatures, comme aujourd'hui.
2. Contrôle de combat : hors combat, « Lancer le combat » ; en combat, le round, la créature active, puis « Fin du tour ».
3. Tableau « Sur la carte » : en combat, trié par initiative, colonne Init. (les ex æquo partagent leur valeur), ligne active marquée (repère, étiquette « À son tour »), « Joué » pour celles qui ont joué ce round ; hors combat, inchangé.
4. Carte : le jeton actif porte une marque distincte de la sélection et de la cible.
5. Journal : lancement, rounds, tours et fins de tour sont des entrées lisibles.

## Tasks to do

### `1)` Types et appels

> Le client lit, lance et termine le tour ; un résultat d'action peut être sans cible.

1. `types.ts` : `Combat` (round, activeId, candidates, groupes avec membres et `played`). `AttackResult` devient `ActionResult` (entries, target facultative, decision, combat).
2. `api.ts` : `combat()` (`null` sur 204), `startCombat()`, `endTurn()` ; `attack` et `answer` rendent `ActionResult`.

### `2)` Contrôle de combat

> Un bouton pour lancer, puis le tour en cours et un bouton pour le terminer.

1. `CombatControl.vue` dans la barre : « Lancer le combat », désactivé sans carte ou sans créature (raison en `title`) ; en combat, « Round N · Tour de X » et « Fin du tour ».
2. `App.vue` : état `combat`, lu au montage et dans `resync`, remis à `null` par `createScene`. Lancer et terminer le tour passent par `attempt`, ajoutent les entrées au journal, ouvrent la décision s'il y en a une, et annoncent le tour (`tell`).
3. `settleAction` lit `ActionResult` : sans cible, ni PV, ni piste de cible, ni onglet Cible ; il met `combat` à jour.
4. Pose et retrait en combat : après la réponse, relire le combat et le journal.

### `3)` Ordre de jeu et créature active

> Savoir d'un coup d'œil à qui c'est le tour.

1. `TokenBoard` : props `combat` ; en combat, lignes dans l'ordre d'initiative, colonne Init., ligne active avec `aria-current="true"` et l'étiquette « À son tour », « Joué » pour celles qui ont joué.
2. `MapView` / `SceneCanvas` : `activeId` dans l'état de rendu et dans le `watch` de `MapView`. La marque du jeton actif a une forme distincte de la sélection (laiton, « The Brass Is Alive Rule ») ; une couleur nouvelle se déclare dans les deux thèmes de `style.css`.
3. `DecisionDialog` : le titre « Réaction · … », câblé, devient propre à la décision (réaction, ou « Qui joue ? »).
4. `JournalPanel` : le message vide mentionne aussi le lancement du combat ; une entrée d'une ligne s'affiche en titre seul.

### `4)` Parcours navigateur

> Le parcours de la story, sur les ports de test.

1. Lancer 8081 et 5174 (`testing.md`), dérouler le Test Scope dans les deux thèmes, puis les arrêter.
2. `npm run build` dans `vtt-web/`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe (`vue-tsc`). |
| 2 | Un clic sur « Lancer le combat » affiche « Round 1 · Tour de … » et les initiatives dans le journal. |
| 3 | La créature active est marquée à la fois dans le tableau trié et sur la carte ; la marque suit « Fin du tour » ; les ex æquo passent par « Qui joue ? ». |
| 4 | Après la dernière créature, « Fin du tour » affiche « Round 2 » et la première créature ; un rechargement garde l'état. |
