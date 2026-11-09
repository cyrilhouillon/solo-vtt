---
objective: "Le joueur choisit les modules de chaque campagne à sa création et les change ensuite ; un oracle générique sans marque (oui/non à 50/50, d100, tirage dans des listes tenues en pages du Carnet de la campagne) est le second module."
status: pending
---

# Plan: choisir ses modules et un oracle générique

Source: les quatre stories `aidd_docs/backlog/stories/choisir-les-modules-d-une-campagne-a-sa-creation.md`, `activer-ou-desactiver-un-module-d-une-campagne-existante.md` (dépend de la première), `poser-une-question-oui-non-ou-lancer-un-d100-avec-l-oracle-generique.md`, `tirer-un-mot-dans-une-de-ses-listes-de-l-oracle-generique.md` (dépend de la troisième), sans epic.

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Choisir les modules d'une campagne à sa création | [`phase-1.md`](./phase-1.md) | none |
| 2   | Activer ou désactiver un module d'une campagne existante | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | L'oracle générique répond oui/non et lance un d100 | [`phase-3.md`](./phase-3.md) | 1 |
| 4   | L'oracle générique tire une ligne dans une liste du Carnet | [`phase-4.md`](./phase-4.md) | 3 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | Aucune doc externe consultée ; `vtt-server` a `quarkus-rest-jackson` (RESTEasy Reactive, `ErrorMapper` emploie déjà `@ServerExceptionMapper`), donc `@ServerRequestFilter` est disponible ; qu'une `NotFoundException` levée dans le filtre passe par `ErrorMapper` est non vérifié, couvert par le test d'API de la phase 1. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Aucune dépendance nouvelle. Un filtre de requête du cœur lit les modules actifs de la campagne (`campagne.json`) à chaque appel d'une route de module : une lecture du dossier des campagnes par `FileCampaignStore.find`, comme `NotebookResource` aujourd'hui ; coût non mesuré. Nouveau module Maven `oracle-server`, embarqué par `vtt-server/pom.xml` comme `mythic-server` (lu). | 1, 3 |
| Regression | Moyenne. `Campaign` gagne `modules` : construit dans `CampaignStore.inMemory`, `FileCampaignStore`, `CampaignResource` ; `CampaignStore.create(name)` est appelé par `CampaignSessionsTest`, `CampaignStoreTest`, `FileCampaignStoreTest`, `CarnetDeBoutEnBoutTest` (lus par le vérificateur) : une surcharge `create(name)` (tous les modules) les garde inchangés. Une fiche `campagne.json` d'avant, sans `modules`, se lit « tous les modules installés » : Mythic reste actif. `POST /api/campaigns` sans `modules` garde le même sens, donc `Campagnes.creerUneNeuve()` et les tests d'API ne changent pas. `NotebookPages` gagne deux méthodes (phase 4) : implémentée par `NotebookPagesAdapter` et `mythic-server/.../PagesDeTest` (seuls implémenteurs, `grep "implements NotebookPages"`). Le Carnet client filtre ses modules et ses sections par les modules actifs : `NotebookScreen`, `PageTree`, `MovePageDialog` (lus en partie). Pas de phase filet : `CampaignApiTest`, `CampaignStoreTest`, `FileCampaignStoreTest`, `MythicResourceTest`, `NotebookApiTest` verrouillent l'existant. | 1, 2, 4 |
| Architecture | Départ : le serveur ne sait pas quels modules sont installés (un module n'est qu'un jar de ressources) ; le client les découvre par `import.meta.glob` ; tous sont actifs partout. Cible : `vtt-module-api` gagne `InstalledModule` (`id()`), qu'un module déclare en bean ; le cœur les collecte (`InstalledModules`), garde la liste des modules actifs dans la fiche de la campagne, et refuse (404) toute route `/api/campaigns/{id}/<module>/…` d'un module installé mais inactif, par un filtre générique. Le client ne monte panneau, `prepare` et section que des modules actifs. Le cœur ne nomme toujours aucun module. Second module : `oracle-server` + `vtt-web/modules/oracle/`. `NotebookPages` permet de lire les sous-pages et le contenu de toute page de sa section, pas seulement des racines. | 1, 3, 4 |
| Documentation | `architecture.md` (modules actifs par campagne, filtre, `InstalledModule`, oracle générique, nouvelles méthodes du port), `codebase-map.md` (`oracle-server`, `vtt-web/modules/oracle/`, nouveaux fichiers du cœur), `project-brief.md` (choix des modules, oracle générique), `package.md` (`oracle-server`), `testing.md` (tests d'`oracle-server`). Stories `status: done`, plan `status: implemented`. `DESIGN.md` inchangé : cases à cocher, dialogue et panneau reprennent les formes existantes. | 1, 2, 3, 4 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Seuls les assemblages nomment un module (`architecture.md`) | Le filtre et le choix des modules restent génériques ; `grep -rn "mythic\|\"oracle\"" vtt-server/src/main vtt-web/src` reste vide. |
| Moteurs et `vtt-module-api` sans dépendance (`package.md`) | `InstalledModule` est une interface Java pure. |
| Hasard injecté, dés scriptés en test (`testing.md`) | L'oracle tire par le bean `Dice` ; ses tests utilisent `Dice.scripted`. |
| Le serveur tire, le client écrit dans la page en nœuds standard par `writeToOpenPage` (`architecture.md`) | Oui/non, d100 et ligne tirée s'écrivent en citation, comme Mythic. |
| Erreurs par l'`ErrorMapper` du cœur : `IllegalArgumentException` 400, `NotFoundException` 404 | Refus d'un module inconnu, d'une liste vide ou inconnue. |
| Serveurs de test 8081 / 5174, chemins dans le scratchpad, jamais 8080 / 5173 (`testing.md`) | Parcours navigateur des phases 1 à 4, y compris par les sous-agents. |
| Une branche par feature, commit après chaque phase (`vcs.md`) | Branche `feat/choisir-ses-modules`. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Le serveur connaît ses modules par un bean `InstalledModule` que chaque module déclare (`vtt-module-api`) | Sans cela, le cœur ne peut ni valider un choix ni refuser une route de module inactif sans nommer le module. |
| `Campaign.modules` `null` (fiche d'avant, ou création sans liste) = tous les modules installés ; écrit dès qu'un choix est fait | « Les campagnes existantes gardent Mythic actif » sans que le cœur nomme Mythic ; effet assumé : une fiche d'avant voit aussi l'oracle générique actif, jusqu'à ce que le joueur change ses modules (phase 2). |
| Le formulaire de création liste les modules du client (`notebookModules`), cochés par défaut ; le serveur refuse (400) un id qui n'est pas installé | Le client a les titres ; les deux moitiés d'un module sont assemblées ensemble. |
| Route d'un module inactif : 404 « Module inactif dans cette campagne », par un filtre de requête du cœur sur le segment qui suit l'id de campagne, qui lève une `NotFoundException` pour que `ErrorMapper` rende l'`ErrorDto` | Un module inactif se comporte comme un module absent ; aucun module n'a à le vérifier lui-même. Les routes communes `/api/<module>/…` restent ouvertes. |
| Le Carnet relit les modules actifs au serveur à son montage (`GET …/modules`), pas dans le cache du routeur | Ils changent en phase 2 ; « chaque écran relit sa campagne en se montant ». |
| Les pages de la section d'un module inactif sortent de l'arbre et du dialogue Déplacer, mais restent atteignables par leurs liens, la recherche, les pages citantes et un onglet ouvert | Story 2 : disparaît du Carnet, données gardées ; les liens vers elles ne meurent pas. |
| Oracle générique : module `oracle` (titre « Oracle »), un seul module Maven `oracle-server`, sans moteur séparé | Oui/non et d100 sont un `dice.roll` ; un moteur pur n'apporterait rien. |
| Oui/non = un d100, oui de 1 à 50, non de 51 à 100, écrit avec son jet | 50/50 confirmé par l'utilisateur ; le jet reste lisible. |
| Listes : une racine « Listes » de la section `oracle`, créée par `prepare` (idempotent, id gardé dans `modules/oracle.json`) ; chaque sous-page directe vivante est une liste ; ses lignes sont lues comme `ListLines` de Mythic (copie dans `oracle-server`) ; le tirage choisit à chances égales parmi les lignes écrites, les vides ignorées | Story 4 ; un module ne dépend pas d'un autre, et `vtt-module-api` ne peut pas porter Jackson. |
