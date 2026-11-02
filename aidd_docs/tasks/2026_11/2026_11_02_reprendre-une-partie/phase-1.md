---
status: done
---

# Instruction: Serveur : la session enregistre ses commandes et ses dés derrière un port, puis les rejoue

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-server/src/main/java/solovtt/server/
├── SceneSession.java          ✏️ enregistre chaque commande réussie et ses dés, se restaure par rejeu ; ne connaît que GameStore
├── SavedGame.java             ✅ la partie sauvée : commandes (sealed) avec leurs dés ; sans annotation technique
├── GameStore.java             ✅ le port : load(), save(SavedGame), setAside() ; GameStore.none() ne garde rien
└── persistence/
    └── JsonFileGameStore.java ✅ l'adaptateur : JSON (Jackson, mixins) dans un fichier, écriture atomique
vtt-server/src/test/java/solovtt/server/
├── InMemoryGameStore.java     ✅ faux écrit à la main (pas de mock) pour les tests de session
├── ResumeGameTest.java        ✅ reprise d'une partie par rejeu, sans Quarkus ni disque
└── persistence/
    └── JsonFileGameStoreTest.java ✅ aller-retour JSON, fichier absent, mise de côté, sur @TempDir
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `mvn -q test` avant toute modification | tests verts | system |
| Happy path   | Session A sur un `InMemoryGameStore` : carte, Guerrière, Gobelin, Magicienne, déplacement libre, lancement du combat, déplacement de la créature active, fin de tour, attaque du Gobelin sur la Magicienne qui pose Bouclier. Session B neuve sur le même store | B a la même scène (dimensions, unité, jetons et positions), la même `GameState.signature()`, le même journal (textes, numéros d'action, rounds, faits), le même combat et la même décision en attente (id, options) ; répondre à B reprend l'attaque comme dans A | system |
| Happy path   | `JsonFileGameStore` : `save` puis `load` d'une partie contenant chaque variante de commande | partie relue égale à l'originale | system |
| Edge case    | Store vide | la session démarre sans carte | system |
| Edge case    | Rejeu en échec (dé manquant ou restant, `answer` sans décision correspondante) | la session démarre sans carte ; `setAside()` appelé, rien n'est sauvé | system |
| Edge case    | Fichier JSON illisible | `load()` lève une erreur ; la session le traite comme un rejeu en échec ; le fichier est renommé `partie.json.<horodatage>.illisible` | system |
| Edge case    | Commande refusée (attaque hors d'allonge, action pendant une décision, option invalide) | aucune sauvegarde | system |
| Edge case    | « Nouvelle carte » après une partie | la partie sauvée ne contient que la nouvelle carte | system |
| Edge case    | `new SceneSession(dice)` | utilise `GameStore.none()` ; les tests existants passent | system |
| Teardown     | `@TempDir` supprimé par JUnit | aucun fichier hors du dossier temporaire | system |

## Tasks to do

### `1)` Décrire la partie sauvée et son port

> La session ne voit qu'une liste de commandes et une interface à trois méthodes.

1. `SavedGame` : record avec `List<SavedCommand>` ; `SavedCommand` sealed, une variante par méthode qui modifie la session (`Create`, `Place` par id de catalogue, `Remove`, `Move`, `Attack`, `StartCombat`, `EndTurn`, `EndCombat`, `Answer`), chacune avec `List<Integer> rolls`. Aucune annotation Jackson.
2. `GameStore` : `Optional<SavedGame> load()` (vide si aucune partie, exception si illisible), `void save(SavedGame)`, `void setAside()` (une partie qui ne se rejoue plus est gardée à part, jamais écrasée). `GameStore.none()` : ne charge rien, ne garde rien.

### `2)` Écrire l'adaptateur fichier JSON

> Tout le technique (Jackson, chemin, disque) reste dans `persistence/`.

1. `JsonFileGameStore(Path)` : son propre `ObjectMapper`, avec des mixins `@JsonTypeInfo`/`@JsonSubTypes` pour `SavedCommand`.
2. `save` crée le dossier, écrit un fichier temporaire voisin puis `Files.move` en `ATOMIC_MOVE` et `REPLACE_EXISTING` ; `setAside` renomme en `<nom>.<horodatage>.illisible`.
3. `JsonFileGameStoreTest` sur `@TempDir`.

### `3)` Enregistrer les commandes dans `SceneSession`

> Chaque appel réussi entre dans la partie, avec ses dés, puis passe par `GameStore.save`.

1. Le moteur reçoit un `Dice` qui délègue à la source courante et note chaque valeur dans les dés de la commande en cours.
2. Chaque méthode publique qui modifie la session vide les dés notés à son entrée et, seulement après succès, ajoute sa commande et appelle `save` ; les `run(JoinCombat)` et `run(LeaveCombat)` internes de `place` et `remove` rattachent leurs dés (initiative) à la commande `Place` ou `Remove` ; `create` remet la liste à zéro ; `answer` n'enregistre que si une décision a répondu.
3. Constructeur `SceneSession(Dice, GameStore)` ; `SceneSession(Dice)` délègue avec `GameStore.none()`.

### `4)` Restaurer par rejeu

> Une session neuve rejoue la partie chargée et retrouve l'état exact, décision comprise.

1. À la construction : `load()` ; pour chaque commande, une source de dés de rejeu sur ses dés (qui sait s'il en reste), sauvegarde suspendue, appel de la même méthode publique, puis vérification que tous ses dés ont été consommés ; ensuite retour aux dés réels.
2. Est un échec : `load()` en erreur, exception, dé manquant ou restant, `answer` qui rend `Optional.empty()`. Alors session remise sans carte, `setAside()`, avertissement dans le log.

### `5)` Tester la reprise

> La situation de table de la story, vérifiée de bout en bout sans Quarkus ni disque.

1. `InMemoryGameStore` (garde la dernière `SavedGame`, compte les `setAside`) et `ResumeGameTest` : les scénarios du Test Scope, avec `Dice.scripted`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `SceneSession`, `SavedGame` et `GameStore` n'importent ni Jackson, ni `java.nio.file`, ni la configuration (vérifié par lecture des imports). |
| 2 | Une `SavedGame` sauvée puis chargée est égale à l'originale, toutes variantes comprises ; un fichier absent se charge vide ; un fichier mis de côté porte le suffixe `.illisible` et le fichier d'origine n'existe plus. |
| 3 | Après chaque commande réussie, le store contient la commande avec ses dés ; après un refus, il est inchangé ; `new SceneSession(dice)` ne sauve rien. |
| 4 | Une session neuve sur le store a la même scène, signature, journal, combat et décision ; répondre à la décision reprend l'attaque et continue de sauver ; un rejeu en échec donne une session sans carte et appelle `setAside()`. |
| 5 | `mvn -q test` passe, nouveaux et anciens tests compris. |
