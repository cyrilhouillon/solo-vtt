---
objective: "Le joueur lance le combat : chaque créature tire son initiative, l'ordre obtenu s'affiche, la créature active est mise en évidence sur la carte et dans l'ordre de jeu, le joueur termine le tour quand il veut (nouveau round après la dernière créature), et le journal indique le début de chaque tour et de chaque round."
status: pending
---

# Plan: enchaîner les tours selon l'initiative

Source: aidd_docs/backlog/stories/enchainer-les-tours-selon-l-initiative.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Moteur générique : tour, candidats et politique de tour | [`phase-1.md`](./phase-1.md) | none |
| 2   | Règles D&D : initiative, groupes, tours, arrivées et départs | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Serveur : ressource combat, pose et retrait en combat | [`phase-3.md`](./phase-3.md) | 2 |
| 4   | Client : lancer le combat, ordre de jeu, créature active | [`phase-4.md`](./phase-4.md) | 3 |
| 5   | Mettre à jour la mémoire projet | [`phase-5.md`](./phase-5.md) | 4 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf | p. 13 : au début du combat, chaque participant fait un test de Dextérité (d20 + modificateur d'initiative) ; ordre du plus haut au plus bas, inchangé d'un round à l'autre ; égalités départagées par le MJ et les joueurs. Initiative des fiches : Gobelin guerrier +2, Squelette +3, Loup +2. Tableau « Standard Array by Class » : Guerrier Dex 14 (+2), cohérent avec For 17 / Con 14 de la Guerrière ; Magicienne Dex 14 (+2), déjà notée dans le catalogue. Une créature récupère sa réaction au début de son tour. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Regression | Moyenne. `GameState` gagne l'état du combat : `copy()` et `signature()` doivent l'inclure, sinon les tests de rejeu de `GameState` (`AttackRulesTest`, `RangeRulesTest`) passent sans le vérifier ; `EngineTest` rejoue un état jouet et ne couvre pas le combat. `AttackAction.of` refuse en combat l'attaque d'une créature inactive : hors combat, rien ne change, et aucun test existant ne lance de combat (lus). `AttackResultDto` devient un résultat d'action dont la cible est facultative : `DecisionResource.answer` le rend aussi pour « Qui joue ? », qui n'a pas de cible, et le client (`settleAction`) doit le lire. Le titre « Réaction · … » est câblé dans `DecisionDialog`. Hors combat, pose et retrait ne tirent aucun dé : les dés scriptés de `SceneApiTest`, `AttackApiTest` et `DecisionApiTest` ne bougent pas. Filet : la suite existante (`mvn -q test`) passe avant et après chaque phase ; pas de phase de filet dédiée. | 1, 2, 3, 4 |
| Architecture | Départ : aucune structure de tour (`architecture.md`, piège « Pas encore de structure de tour ») ; `GameState` ne tient que des créatures ; la préparation de scène se fait hors journal. Cible : `vtt-engine` porte le vocabulaire générique du tour (`TurnOrder` : round, candidats qui peuvent jouer, créature active) et l'interface `TurnPolicy<S>`, par laquelle le système dit qui peut jouer après chaque fin de tour. Le moteur ne sait rien de l'initiative. `dnd-engine` l'implémente par groupes d'initiative et pilote le combat par des résolutions soumises au moteur, dont les événements placent l'état : le combat est rejouable. Un système sans initiative (Daggerheart : joueurs, puis MJ) écrit sa propre politique. Pose et retrait en combat passent par le moteur pour l'ordre de jeu, mais la créature elle-même reste créée et supprimée hors journal (limite connue pour la story « reprendre une partie »). | 1, 2, 3 |
| Documentation | `architecture.md` : vocabulaire du tour et politique, groupes d'initiative et choix « Qui joue ? », attaque réservée à la créature active, arrivées et départs en combat ; piège des tours réécrit (la réaction revient, Bouclier n'expire toujours pas). `codebase-map.md` : `TurnOrder`, `TurnPolicy`, paquet `dnd.engine.combat`, ressource `combat`, `CombatControl`. `project-brief.md` : fonctionnalité. `DESIGN.md` : contrôle de combat, colonne Init. et ligne active, marque du jeton actif. Story en `status: done`. | 5 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| L'état ne change que via `Engine.emit` ; les jets de dés sont des événements ; rejouer le journal redonne l'état (`architecture.md`) | Initiative, tours, arrivées et départs sont des événements ; un test de rejeu couvre le combat. |
| Le moteur générique ne connaît aucun type de jeu ; aucune dépendance runtime (`architecture.md`, `package.md`) | `TurnOrder` et `TurnPolicy` ne manipulent que des ids ; l'initiative reste dans `dnd-engine` (epic). |
| Une décision suspend le moteur ; une seule à la fois ; 409 tant qu'elle attend (`architecture.md`) | « Qui joue ? » est une décision : pose, retrait, attaque, lancement et fin de tour sont refusés tant qu'elle attend. |
| Le hasard est injecté (`Dice`) | Les d20 d'initiative passent par `engine.roll`. |
| Dés scriptés, noms de tests en français, pas de mock ; règle nouvelle = test de la situation de table (`testing.md`) | Initiative, égalité, round, arrivée, départ, attaque hors tour, réaction rendue, rejeu, API. |
| Ne jamais tester sur 8080/5173 (`testing.md`) | Le parcours navigateur se fait sur 8081/5174. |
| Univers visuel de `DESIGN.md` (« The Brass Is Alive Rule », « The Two Lights Rule ») | La créature active est un état vivant ; toute couleur nouvelle se déclare dans les deux thèmes. |
| Règles D&D tirées du SRD 5.2.1 officiel (`project-brief.md`) | Modificateurs d'initiative, réaction rendue au début du tour. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| `vtt-engine` décrit un tour sans parler d'initiative : `TurnOrder` (round, candidats, créature active) et `TurnPolicy<S>`. Après chaque fin de tour, le système rend les candidats suivants et dit si un round commence. | Choix de l'utilisateur : faire cohabiter des systèmes sans initiative ou à groupes (Daggerheart, où jouent soit les joueurs, soit le MJ). Un seul candidat devient actif d'office ; s'il y en a plusieurs, le joueur choisit. |
| Plusieurs candidats : le moteur pose la décision « Qui joue ? » dans le dialogue existant. La créature choisie joue, « Fin du tour », puis la politique rend les candidats restants. | Choix de l'utilisateur. Il y a toujours une seule créature active, et la console comme le client réutilisent les décisions. |
| D&D : groupes d'initiative. Le total le plus haut joue d'abord, puis le modificateur le plus haut à total égal ; à égalité complète, les créatures forment un groupe dont chaque membre joue une fois par round, dans l'ordre que choisit le joueur. | Choix de l'utilisateur ; le SRD laisse les égalités au MJ et aux joueurs. |
| Lancement, fin de tour, arrivée et départ sont des `Resolution` simples, pas des pipelines à phases. | Choix de l'utilisateur. Contrepartie : Vigilance (Alert), la surprise ou l'avantage d'initiative demanderont une conversion en pipeline. |
| En combat, une créature posée tire son initiative et entre dans l'ordre. Si son rang vient après le groupe en cours, elle joue ce round-ci ; sinon, au suivant. Une créature retirée quitte l'ordre ; si elle était active, son tour se termine d'office et la politique rend les candidats suivants. | Choix de l'utilisateur : tout permettre. Pendant une décision, pose et retrait restent refusés (409). |
| En combat, seule la créature active peut attaquer ; sinon `AttackAction.of` refuse avec la raison (400). Hors combat, toute créature attaque. | Choix de l'utilisateur. La règle est D&D, donc elle vit dans `dnd-engine` et vaut pour tout appelant. |
| Une créature à 0 PV garde sa place dans l'ordre. | Choix de l'utilisateur. Le joueur termine son tour ; les jets contre la mort ne sont pas modélisés. |
| Au début de son tour, une créature récupère sa réaction (`ReactionRegained`). Le +5 de Bouclier n'expire toujours pas. | Choix de l'utilisateur : pas d'effet à durée (exclu par l'epic). |
