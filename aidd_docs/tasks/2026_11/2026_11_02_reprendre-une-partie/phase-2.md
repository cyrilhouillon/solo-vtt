---
status: pending
---

# Instruction: Brancher la sauvegarde sur un fichier configurable, valider dans le navigateur, mettre à jour la mémoire

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── vtt-server/src/main/java/solovtt/server/SceneSession.java   ✏️ reçoit un GameStore injecté
├── vtt-server/src/main/java/solovtt/server/persistence/GameStoreProducer.java   ✅ produit le GameStore selon la configuration
├── vtt-server/src/main/resources/application.properties        ✏️ chemin par défaut, aucun en test
└── aidd_docs/
    ├── backlog/stories/reprendre-une-partie-a-l-identique.md   ✏️ status: done
    └── memory/
        ├── architecture.md      ✏️ décision et pièges de la sauvegarde
        ├── codebase-map.md      ✏️ SavedGame, GameStore, persistence/, fichier de sauvegarde
        ├── project-brief.md     ✏️ fonctionnalité
        └── testing.md           ✏️ chemin de sauvegarde des serveurs de test
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `mvn -q install -DskipTests`, serveur sur 8081 avec `-Dsolovtt.save.path=<scratchpad>/partie.json`, Vite sur 5174 | page de préparation ouverte sur 5174 | system |
| Happy path   | Poser trois créatures, en déplacer une, lancer le combat, jouer un tour, attaquer la Magicienne jusqu'au dialogue Bouclier ; arrêter le serveur 8081, le relancer, recharger la page | même carte, positions, PV, ordre de jeu, tour en cours, journal et piste à dés ; le dialogue Bouclier est ouvert sur la même question ; y répondre termine l'attaque | browser |
| Edge case    | `mvn -q test` | aucun fichier de sauvegarde écrit ; tests verts | system |
| Edge case    | Fichier de sauvegarde rendu illisible, puis relance du serveur 8081 | page sans carte ; fichier `.illisible` à côté | browser |
| Teardown     | Arrêter 8081 puis 5174 ; supprimer le fichier du scratchpad | ports libérés ; aucun fichier créé par le test dans `~/.solo-vtt` | system |

## Tasks to do

### `1)` Configurer le fichier de sauvegarde

> L'application sauve par défaut dans `~/.solo-vtt/partie.json` ; les tests, nulle part.

1. `GameStoreProducer` lit `@ConfigProperty(name = "solovtt.save.path") Optional<Path>` et produit un `JsonFileGameStore`, ou `GameStore.none()` sans chemin ; `SceneSession` reçoit `@Inject GameStore` et ignore la configuration.
2. `application.properties` : `solovtt.save.path=${user.home}/.solo-vtt/partie.json` et, en profil de test, aucun chemin (`%test.solovtt.save.path=` vide, vérifier qu'il donne bien `Optional.empty()`).

### `2)` Valider dans le navigateur

> Le parcours de la story, avec arrêt et relance réels du serveur de test.

1. Dérouler le Test Scope sur 8081/5174, jamais 8080/5173.

### `3)` Mettre à jour la mémoire et la story

> La documentation dit comment la partie est sauvée et ce que cela suppose.

1. `architecture.md` : décision « sauvegarde = commandes et dés rejoués, derrière le port `GameStore` » ; retirer « La persistance n'existe pas encore » et le piège de la décision non sauvegardable ; ajouter le piège « une sauvegarde suppose le code de règles qui l'a écrite ».
2. `codebase-map.md`, `project-brief.md`, `testing.md` (option `-Dsolovtt.save.path` du serveur 8081) ; story en `status: done`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Le serveur sans option écrit `~/.solo-vtt/partie.json` ; `mvn -q test` n'écrit aucun fichier de sauvegarde. |
| 2 | Après relance du serveur, la page montre la partie et la décision telles qu'avant l'arrêt. |
| 3 | La mémoire ne dit plus que la persistance manque ; elle nomme le fichier, sa surcharge et le piège du code de règles. |
