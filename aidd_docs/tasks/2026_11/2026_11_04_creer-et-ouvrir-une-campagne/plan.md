---
objective: "En ouvrant l'application, le joueur choisit ou crée une campagne nommée ; chacune garde sa propre partie et ses propres cartes enregistrées, la retrouve à l'identique en y revenant, y compris après redémarrage du serveur."
status: implemented
---

# Plan: créer et ouvrir une campagne depuis l'accueil

Source: aidd_docs/backlog/stories/creer-et-ouvrir-une-campagne-depuis-l-accueil.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Garder les campagnes | [`phase-1.md`](./phase-1.md) | none |
| 2   | Ouvrir une campagne par l'API | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Accueil des campagnes dans le navigateur | [`phase-3.md`](./phase-3.md) | 2 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | none |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Nouveau dossier `~/.solo-vtt/campagnes/<id>/` (`campagne.json`, `partie.json`, `cartes.json`), propriété `solovtt.campaigns.path`, vide en profil de test (mémoire). Les adaptateurs `JsonFileGameStore` et `JsonFileMapStore` sont réutilisés tels quels sur ces chemins (constructeur `Path`, `JsonFileGameStore.java:33`, `JsonFileMapStore.java:28`). `~/.solo-vtt/partie.json` et `cartes.json` ne sont ni lus ni touchés : entre cette story et la suivante (« retrouver sa partie d'avant »), la partie du joueur n'est plus visible, sans être perdue. Non vérifié à l'exécution. | 1, 2 |
| Regression | Élevé côté serveur : le constructeur CDI de `SceneSession` (`SceneSession.java:153`) devient `(Dice, CampaignStore)`, sans rejeu ni campagne ouverte, et refuse une carte sans campagne ouverte ; tous les `@QuarkusTest` qui créent une carte (`AttackApiTest`, `CombatApiTest`, `DecisionApiTest`, `MapApiTest`, `MoveApiTest`, `SceneApiTest`) doivent ouvrir une campagne dans leur `@BeforeEach`. `MapResource` ne reçoit plus un `MapStore` injecté mais celui de la campagne ouverte (`MapResource.java:25`, `SceneWithoutMapTest.java:21`). `ResumeGameTest`, `CombatDeBoutEnBoutTest` et `SceneWithoutMapTest` construisent `new SceneSession(dice, store)` ou `new SceneSession(dice)` (`ResumeGameTest.java:175`, `:240`, `:247`, `SceneWithoutMapTest.java:17`) : ces deux constructeurs restent et ouvrent directement une campagne sans nom sur `store` (ou `GameStore.none()`), donc `create` y reste permis. Pas de phase filet : ces tests existants sont le filet, ils doivent repasser. Moyen côté client : `App.vue` (1643 lignes) devient `GameScreen.vue` sans changement de logique, sauf la barre et `resync` (`App.vue:320`). | 2, 3 |
| Architecture | Départ : une seule session (`SceneSession`) sur une seule partie (`GameStore`) et une seule liste de cartes (`MapStore`), produites au démarrage (`GameStoreProducer`, `MapStoreProducer`). Cible : un port `CampaignStore` (adaptateur `persistence.FileCampaignStore`, mémoire en test) liste et crée les campagnes et donne le `GameStore` et le `MapStore` de chacune ; la session reste unique et tient la campagne ouverte : l'ouvrir remet la session à zéro puis rejoue la partie de cette campagne. Les deux producteurs disparaissent. | 1, 2 |
| Documentation | `architecture.md` (campagne ouverte, port `CampaignStore`, dossier par campagne, refus sans campagne, piège du redémarrage), `codebase-map.md` (`CampaignResource`, `CampaignStore`, `FileCampaignStore`, `/api/campaigns`, `HomeScreen`, `GameScreen`), `testing.md` (`-Dsolovtt.campaigns.path=<scratchpad>/campagnes` remplace les options de partie et de cartes), `project-brief.md` (fonctionnalité, chemins `~/.solo-vtt/`), `DESIGN.md` (écran d'accueil, bouton Accueil de la barre). Story : `status: done`. Plan : `status: implemented`. | 2, 3 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Les moteurs n'ont aucune dépendance runtime (`architecture.md`) | Tout reste dans `vtt-server` et `vtt-web`. |
| La partie se garde par ses commandes et se rejoue au lancement (`architecture.md`) | Ouvrir une campagne = remettre la session à zéro puis rejouer ses commandes : aucun format nouveau de partie. |
| Port et adaptateur dans `persistence/`, profil de test sans fichier, écriture atomique (`architecture.md`) | `CampaignStore` suit `GameStore`, `MapStore`, `ImageStore`. |
| Les images restent communes, par empreinte (epic, hypothèse acceptée) | `ImageStore` ne change pas. |
| Ne jamais tester sur 8080/5173 ni écrire les fichiers du joueur (`testing.md`) | Le serveur de test reçoit `-Dsolovtt.campaigns.path=<scratchpad>/campagnes`. |
| Tests `@QuarkusTest` + RestAssured, noms en français, dés scriptés (`testing.md`) | Nouveaux tests d'adaptateur et d'API. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Une seule session côté serveur, qui tient la campagne ouverte (`POST /api/campaigns/{id}/open`) ; les routes existantes (`/api/scene`, `/api/maps`…) visent cette campagne, sans id dans l'URL | Suit « une seule session en mémoire » (jeu local et solo) et laisse intactes les routes et `api.ts`. Contrepartie : deux onglets sur deux campagnes se marchent dessus, et après un redémarrage du serveur aucune campagne n'est ouverte (le client retourne à l'accueil). L'alternative, `/api/campaigns/{id}/scene…` et une session par campagne, toucherait chaque ressource, chaque test et chaque appel du client. |
| Révisée après revue (round 1) : une `SceneSession` par campagne (`CampaignSessions`), routes de partie sous `/api/campaigns/{id}/…` ; `open`, `/current` et le retour forcé à l'accueil disparaissent | L'id de campagne entre dans l'API avant sa première livraison ; deux campagnes ne se marchent plus dessus et un redémarrage est invisible au client. Le coût était surtout dans les routes, à payer de toute façon. |
| Une campagne = un dossier `campagnes/<uuid>/` avec `campagne.json` (id, nom), `partie.json` et `cartes.json` | Réutilise les deux adaptateurs existants sans changer leur format ; un UUID évite d'assainir un nom pour Windows ; les noms en double sont permis (aucun critère ne les interdit). |
| Le client sépare `HomeScreen.vue` et `GameScreen.vue` (l'actuel `App.vue` déplacé), montés par un `App.vue` réduit, l'écran de jeu reçoit `:key` = id de campagne | Changer de campagne remonte l'écran de jeu : tout l'état client repart de zéro sans liste de remises à zéro à tenir. Pas de routeur : aucune dépendance nouvelle, et l'accueil est l'écran d'ouverture voulu. |
