# Tests

## Stratégie

- Des tests de règles unitaires : chacun décrit une situation de table exacte et vérifie les événements émis et l'état final.
- La rejouabilité est testée : rejouer le journal sur une copie de l'état initial doit redonner la même `GameState.signature()`.

## Outils

- JUnit Jupiter, avec les assertions JUnit. Pas de librairie de mock.
- `vtt-server` : `@QuarkusTest` et RestAssured contre l'API réelle ; RestAssured lit les nombres JSON en `float` (`equalTo(5.0f)`).
- Chaque `@BeforeEach` d'un test d'API qui crée une carte appelle d'abord `Campagnes.creerUneNeuve()`, puis vise ses routes par `campagne() + "/scene"` : chaque test joue dans sa propre campagne.
- `vtt-web` : pas de tests automatisés ; `vue-tsc` vérifie les types et le parcours se valide dans le navigateur.

## Conventions

- Les tests sont dans `vtt-engine/src/test/java/` (axiomes du moteur sur un système jouet, scène), `dnd-engine/src/test/java/` (règles D&D, catalogue), `mythic-engine/src/test/java/` (table de destin, focus, événements aléatoires), `mythic-server/src/test/java/` (ressource Mythic en JUnit simple, `ModuleData` en mémoire, dés scriptés ; l'assemblage se vérifie sur le serveur 8081) et `vtt-server/src/test/java/` (API ; reprise de partie avec un `GameStore` en mémoire, adaptateur JSON sur `@TempDir`).
- Les dés sont toujours scriptés (`Dice.scripted`, ou `ScriptedDice` dans les tests `@QuarkusTest` du serveur), dans l'ordre de consommation : d20 (un ou deux), dés bonus au jet, puis dés de dégâts. Il n'y a jamais de hasard réel dans un test.
- Les noms de méthodes sont en français et décrivent la règle (`avantageEtDesavantageSAnnulent_unSeulD20`).
- Toute nouvelle règle arrive avec le test de la situation de table qu'elle change.

## Lancer

- `mvn test` depuis la racine.
- Le serveur (8080) et le client (5173) de développement sont lancés par l'utilisateur depuis les lanceurs IntelliJ. Ne jamais les arrêter ni y faire de tests navigateur : un test modifie la session en mémoire du serveur visé et écrase la scène de l'utilisateur. Cela vaut aussi pour les sous-agents (revue, critique) : leur donner les ports de test.
- Serveurs de test, lancés en arrière-plan :
  1. `mvn -q install -DskipTests` à la racine, pour que le serveur voie les moteurs modifiés.
  2. `mvn quarkus:dev -pl vtt-server -Dquarkus.http.port=8081 -Dsolovtt.campaigns.path=<scratchpad>/campagnes -Dsolovtt.images.path=<scratchpad>/images -Dsolovtt.modules.path=<scratchpad>/modules` : sans ces options, le serveur de test écrirait les campagnes (parties et cartes enregistrées), les images et les données communes des modules du joueur (`~/.solo-vtt/`).
  3. Dans `vtt-web/`, Vite sur 5174 avec `/api` vers 8081, sans toucher `vite.config.ts` : `node --input-type=module -e "import { createServer } from 'vite'; import vue from '@vitejs/plugin-vue'; const s = await createServer({ configFile: false, root: process.cwd(), plugins: [vue()], server: { port: 5174, strictPort: true, proxy: { '/api': 'http://localhost:8081' } } }); await s.listen()"`.
  4. Pour arrêter, tuer le processus qui écoute sur 8081 puis celui sur 5174 (`Get-NetTCPConnection -LocalPort <port> -State Listen` puis `Stop-Process`), jamais 8080 ni 5173.
