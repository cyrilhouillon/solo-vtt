---
objective: "Depuis le Carnet, le joueur pose une question oui/non à l'oracle Mythic 2e (probabilité, facteur de chaos de la campagne, gardé et modifiable de 1 à 9) ; réponse, jet et éventuel événement aléatoire avec son focus s'écrivent dans la page ouverte, et Mythic vit dans ses propres modules, que le cœur ne nomme jamais."
status: implemented
---

# Plan: poser une question à l'oracle depuis le Carnet

Source: aidd_docs/backlog/stories/poser-une-question-a-l-oracle-depuis-le-carnet.md (epic `aidd_docs/backlog/epics/jouer-sa-campagne-avec-mythic-gme-depuis-le-carnet.md`, order 1), avec le socle de module du spike `aidd_docs/backlog/spikes/definir-les-points-d-extension-d-un-module-du-carnet.md` (décision de l'utilisateur : le socle entre dans ce plan ; la section de l'arbre attend la story « fils et personnages »).

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Le cœur garde les données d'un module | [`phase-1.md`](./phase-1.md) | none |
| 2   | Le moteur Mythic répond à une question | [`phase-2.md`](./phase-2.md) | none |
| 3   | Le serveur Mythic tient le chaos et pose la question | [`phase-3.md`](./phase-3.md) | 1, 2 |
| 4   | Le Carnet accueille le panneau d'un module | [`phase-4.md`](./phase-4.md) | none |
| 5   | Le panneau Mythic écrit l'oracle dans la page | [`phase-5.md`](./phase-5.md) | 3, 4 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://wordmillgames.itch.io/mythic-game-master-emulator-second-edition | Mythic GME 2e est un livre commercial (Word Mill Games) : aucune source web ne donne la table de destin ni la table de focus ; l'utilisateur les transcrit du livre (phase 2). |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Découverte d'un jar voisin par Quarkus 3.40.1.1 grâce à un `META-INF/beans.xml` vide, ressource `/api/campaigns/{campaignId}/mythic` servie à côté du localisateur de `CampaignResource` : observée dans l'essai du spike (4/4 `@QuarkusTest`), non rejouée dans ce dépôt. Découverte client par `import.meta.glob` hors de `src/` : build observé dans l'essai, panneau jamais vu dans un navigateur. Écriture Tiptap (`insertContentAt`) sur un éditeur non éditable : non vérifiée. Aucune dépendance nouvelle : Jackson, `quarkus-rest-jackson`, Tiptap déjà présents. | 3, 4, 5 |
| Regression | Faible à moyenne. Serveur : `CampaignStore` (lu) gagne deux méthodes, ses deux implémentations aussi ; tests `CampaignStoreTest`, `FileCampaignStoreTest`. Client : `NotebookScreen.vue` (grille `17rem minmax(0,1fr)` lue l. 831-838, 945-947) gagne une colonne seulement si un module a un panneau ; `PageEditor.vue` gagne `insertBlocks`, sans toucher `onUpdate`. Pas de phase filet : les tests serveur existants verrouillent le Carnet ; client validé par `npm run build` et parcours sur 8081 / 5174. | 1, 4 |
| Architecture | Départ : trois modules Maven, aucun point d'extension ; le Carnet est fermé. Cible : `vtt-module-api` (port `ModuleData`, sans dépendance) implémenté par le cœur ; `mythic-engine` (pur, dés injectés, dépend de `vtt-engine`) et `mythic-server` (jar à `beans.xml`, ressource sous `/api/campaigns/{id}/mythic`) ; côté client, contrat `NotebookModule` / `NotebookContext` dans `src/modules/`, modules découverts dans `vtt-web/modules/*/index.ts`. Seuls `pom.xml` racine, `vtt-server/pom.xml` et le dossier `vtt-web/modules/` nomment Mythic. | 1, 3, 4, 5 |
| Documentation | `architecture.md` (modules, port `ModuleData`, règle de nommage, panneau et écriture dans la page, Mythic), `codebase-map.md` (nouveaux modules Maven, `src/modules/`, `modules/mythic/`, route `mythic`), `package.md` (nouvelles librairies sans dépendance runtime), `project-brief.md` (oracle), `testing.md` (tests de `mythic-engine` et `mythic-server`), `DESIGN.md` (colonne du panneau de module, panneau Mythic). Story `status: done`, epic inchangée ; plan `status: implemented`. | 1, 2, 3, 4, 5 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Les moteurs n'ont aucune dépendance runtime (`architecture.md`, `package.md`) | `vtt-module-api` et `mythic-engine` n'en ont aucune (hormis `vtt-engine` pour `Dice`). |
| Le hasard est injecté (`Dice`), jamais `Random` (`architecture.md`) | Le moteur Mythic tire ses d100 par `Dice` ; le serveur reçoit le bean de `DiceProducer`. |
| Seuls les assemblages nomment un module : `pom.xml` racine, `vtt-server/pom.xml`, `vtt-web/modules/` ; ni `vtt-server/src` ni `vtt-web/src` (spike) | Vérifié en phase 3 et 5 par recherche de `mythic` dans ces deux dossiers. |
| Lire le Carnet ne rejoue pas la partie (`architecture.md`) | La ressource Mythic n'utilise ni `CampaignSessions` ni `SceneSession`. |
| Campagne inconnue : 404 ; `IllegalArgumentException` → 400 (`ErrorMapper`) | La ressource Mythic lève `NotFoundException` pour une campagne inconnue, 400 pour un chaos hors de 1..9. |
| Écriture atomique, fichier illisible jamais écrasé (adaptateurs JSON) | `<campagne>/modules/<id>.json`. |
| Dés scriptés, noms de tests en français décrivant la règle (`testing.md`) | Tests de `mythic-engine` et `mythic-server`. |
| Client sans tests automatisés ; jamais de test navigateur sur 8080 / 5173 (`testing.md`) | Parcours sur 8081 / 5174, campagnes et images dans le scratchpad. |
| `vtt-server` impose JUnit 6 par le BOM Quarkus (`architecture.md`) | `mythic-server` importe le même BOM et surcharge `junit.version` de même. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Le socle de module (port serveur, découverte client, écriture dans la page) est livré dans ce plan ; la section de l'arbre et la lecture des pages par un module viennent avec la story « fils et personnages » | Décision de l'utilisateur ; seule la partie qu'utilise l'oracle est construite. |
| Port `solovtt.module.ModuleData` : `exists(campaignId)`, `read(campaignId, moduleId)` → `Optional<String>` (JSON), `write(campaignId, moduleId, json)` ; le cœur le sert depuis `CampaignStore` dans `<campagne>/modules/<moduleId>.json` | Le module garde son format sans que le cœur l'interprète ; aucune dépendance (pas de Jackson) dans l'API. |
| Le serveur tire le jet et garde le chaos ; le client écrit le résultat dans la page | Le serveur ne connaît pas la page ouverte ; le jet reste injectable et testable. Le jet n'entre pas dans le journal de la partie. |
| Page verrouillée : le résultat s'ajoute à la fin de la page et s'enregistre ; déverrouillée : au curseur | Décision de l'utilisateur ; le verrou protège la frappe, pas l'oracle. |
| Le résultat s'écrit en nœuds standard (une citation de paragraphes), sans extension Tiptap | Recherche, liens entrants et enregistrement le lisent sans changement (spike). |
| Table de destin et table de focus transcrites du livre par l'utilisateur, en constantes de `mythic-engine` | Décision de l'utilisateur ; aucune source publique fiable. |
| Le panneau occupe une troisième colonne, à droite de la page ; empilé sous la page en écran étroit ; absente sans module | Décision de l'utilisateur. |
