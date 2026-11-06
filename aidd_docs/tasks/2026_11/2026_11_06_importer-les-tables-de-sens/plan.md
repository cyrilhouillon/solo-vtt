---
objective: "Le joueur importe une fois ses tables de sens Mythic (fichier texte à titres, gardé hors du dépôt, commun à ses campagnes, refusé avec la raison s'il est mal formé), tire deux mots dans la table choisie depuis le panneau, et chaque événement aléatoire tire aussi ses deux mots dans les tables d'actions ; sans tables, le panneau dit pourquoi et l'oracle oui/non reste utilisable."
status: in-progress
---

# Plan: importer ses tables de mots et tirer un sens

Source: aidd_docs/backlog/stories/importer-ses-tables-de-mots-et-tirer-un-sens.md (epic `aidd_docs/backlog/epics/jouer-sa-campagne-avec-mythic-gme-depuis-le-carnet.md`, order 2 ; tranche la décision d'epic « format du fichier d'import des tables de mots »).

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Le cœur garde les données communes d'un module | [`phase-1.md`](./phase-1.md) | none |
| 2   | Le moteur Mythic lit les tables et tire un sens | [`phase-2.md`](./phase-2.md) | none |
| 3   | Le serveur Mythic importe les tables et tire un sens | [`phase-3.md`](./phase-3.md) | 1, 2 |
| 4   | Le panneau Mythic importe et écrit le sens dans la page | [`phase-4.md`](./phase-4.md) | 3 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://wordmillgames.itch.io/mythic-game-master-emulator-second-edition | Mythic GME 2e est un livre commercial : les tables de sens ne sont ni livrées ni recopiées dans le dépôt. Leur forme (Actions et Descriptions en deux colonnes de 100, une table d'éléments = une colonne de 100 tirée deux fois ; sens d'un événement tiré dans les Actions) vient de la connaissance du livre, non vérifiée sur une source publique ; l'utilisateur la confirme en collant ses tables (phase 4). |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Corps `text/plain` reçu en `String` par Quarkus REST (`@Consumes(TEXT_PLAIN)`) : non vérifié dans ce dépôt (seuls JSON et octets bruts d'image y sont reçus) ; vérifié par le parcours sur 8081. Nouvelle propriété `solovtt.modules.path` (défaut `${user.home}/.solo-vtt/modules`, vide en test) sur le modèle lu de `solovtt.images.path` (`application.properties` l. 6-7, `ImageStoreProducer`). Aucune dépendance nouvelle. | 1, 3 |
| Regression | Faible. `FateQuestion.ask` gagne un paramètre (tables facultatives) : 9 appels lus dans `FateQuestionTest`, 1 dans `MythicResource` ; ordre des dés inchangé sans tables. `MythicResource` gagne une dépendance au constructeur (1 appel de test). `vtt-module-api` gagne une interface, `ModuleData` inchangé. `request` (`src/api.ts` l. 35-57) déjà prêt pour un autre type de corps. Pas de phase filet : `FateQuestionTest` et `MythicResourceTest` verrouillent l'oracle existant. | 2, 3, 4 |
| Architecture | Départ : un module ne garde que des données par campagne (`ModuleData`, `<campagne>/modules/<id>.json`) et ne sert que des routes sous `/api/campaigns/{id}/<module>/`. Cible : port `solovtt.module.CommonModuleData` (un document par module, commun aux campagnes), servi par le cœur dans `solovtt.modules.path/<id>.json` ; un module peut servir des routes communes sous `/api/<module>/…`, comme `images` côté cœur. Le cœur ne nomme toujours pas Mythic. | 1, 3 |
| Documentation | `architecture.md` (port commun, routes communes d'un module, tables de sens, sens d'un événement), `codebase-map.md` (`CommonModuleData`, adaptateur, `MeaningResource`, fichiers du moteur), `project-brief.md` (tables de sens), `testing.md` (option `-Dsolovtt.modules.path=<scratchpad>/modules` des serveurs de test), `package.md` (inchangé : aucune dépendance), `DESIGN.md` (section Sens du panneau). Story `status: done` ; plan `status: implemented`. | 1, 4 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Les tables de mots ne sont jamais dans le dépôt (epic, story) | Tests et exemples n'emploient que des mots inventés ; le fichier du joueur vit dans `~/.solo-vtt/modules/` ou le scratchpad. |
| Seuls les assemblages nomment un module ; ni `vtt-server/src` ni `vtt-web/src` (`architecture.md`) | Le port commun est générique ; `grep -ri mythic vtt-server/src vtt-web/src` reste vide. |
| Les moteurs n'ont aucune dépendance runtime ; le hasard est injecté (`Dice`) | Lecture du fichier et tirage dans `mythic-engine`, en Java pur, dés injectés. |
| `IllegalArgumentException` → 400 par l'`ErrorMapper` du cœur | Fichier mal formé : 400 avec la raison et le numéro de ligne. |
| Écriture atomique, fichier illisible jamais écrasé (adaptateurs JSON) | `solovtt.modules.path/<id>.json`. |
| Serveurs de test sur 8081 / 5174 avec les chemins dans le scratchpad (`testing.md`) | Sans `-Dsolovtt.modules.path`, le serveur de test écraserait les tables du joueur. |
| Dés scriptés ; noms de tests en français (`testing.md`) | Tests de `mythic-engine`, `mythic-server` et de l'adaptateur. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Format d'import : texte à titres. `# Actions 1`, `# Actions 2`, `# Descriptions 1`, `# Descriptions 2` obligatoires, puis zéro ou plusieurs `# Élément : <nom>` (nom unique, sans casse) ; 100 entrées non vides par section, une par ligne ; lignes vides ignorées, BOM et CRLF tolérés ; titre inconnu, texte avant le premier titre ou compte faux : refus avec le numéro de ligne | Décision de l'utilisateur ; proche d'un copier-coller du livre. |
| L'utilisateur colle ses tables du livre (« 1: mot2: mot… ») ; Claude les met au format et les traduit en français, dans un fichier hors du dépôt, que le joueur importe | Demande de l'utilisateur ; l'application ne traduit rien. |
| Les tables sont des données communes du module, derrière un nouveau port `CommonModuleData` ; l'import remplace les tables précédentes | La story les veut communes aux campagnes ; `ModuleData` est par campagne. |
| Routes communes du module sous `/api/mythic/…` (`meaning-tables`, `meanings`), hors de `/api/campaigns/{id}` | Les tables ne dépendent d'aucune campagne, comme `images`. |
| Le sens d'un événement aléatoire se tire toujours dans les Actions (deux d100 après le focus) ; sans tables, l'événement s'écrit sans sens | Décision de l'utilisateur (règle par défaut de Mythic 2e). |
| Le serveur tire, le client écrit dans la page en nœuds standard (citation), comme l'oracle | Même contrat que la story précédente : recherche et enregistrement lisent le résultat. |
