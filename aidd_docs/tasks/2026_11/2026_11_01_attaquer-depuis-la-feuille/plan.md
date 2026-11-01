---
objective: "Depuis la feuille d'une créature (personnage ou adversaire), le joueur lance une attaque, choisit sa cible sur la carte, et le moteur la résout selon les règles D&D 2024 : allonge, portée normale et longue, lancer. Le jet, le résultat et les dégâts apparaissent dans le journal, et les PV de la cible baissent."
status: implemented
---

# Plan: attaquer depuis la feuille en ciblant sur la carte

Source : aidd_docs/backlog/stories/attaquer-depuis-la-feuille-en-ciblant-sur-la-carte.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Portées D&D dans `dnd-engine` : allonge, portée, lancer | [`phase-1.md`](./phase-1.md) | none |
| 2   | Serveur : moteur en session, attaque, journal | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Client : attaque depuis la feuille, ciblage, journal | [`phase-3.md`](./phase-3.md) | 2 |
| 4   | Mettre à jour la mémoire projet | [`phase-4.md`](./phase-4.md) | 3 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf (p. 15, « Ranged Attacks », « Range », « Reach ») | Avec deux portées, l'attaque a le désavantage au-delà de la portée normale et devient impossible au-delà de la longue. L'allonge vaut 5 ft, sauf mention contraire. « Ranged Attacks in Close Combat » existe : désavantage à 5 ft d'un ennemi qui voit l'attaquant (hors périmètre). |
| https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf (p. 89-90, « Properties » et tableau « Weapons ») | Thrown : attaque à distance, avec le même modificateur qu'au corps à corps. La Dague est Finesse, Light, Thrown (Range 20/60) ; l'Arc court est Ammunition (Range 80/320 ; Arrow), Two-Handed. |
| https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf (fiches Goblin Warrior, Skeleton, Wolf) | Gobelin : CA 15, Cimeterre +4 1d6+2, Arc court +4 1d6+2 80/320 ft (conforme au catalogue, plus l'arc). Squelette : CA 14, Épée courte +5 1d6+3, Arc court +5 1d6+3 80/320 ft (le catalogue a CA 13 et +4 1d6+2). Loup : CA 12, Morsure +4 1d6+2 (le catalogue a CA 13 et 2d4+2). Le dé « +1d4 si avantage » du Gobelin reste hors périmètre, comme aujourd'hui. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Regression | Moyenne. `Weapon` change de forme et `Creature` passe d'une arme à une liste d'armes. Ça touche `CoreAttackRules`, `DuelingStyle`, `AttackAction`, `demo/Main`, `Dtos`, `SceneApiTest` (`weapon.name`, `weapon.damage`), `AttackRulesTest` et `SrdCatalogTest`, ainsi que côté client `types.ts` (`weapon`) et `CreatureSheet.vue` (`sheet.weapon.*`). Le constructeur de copie de `Creature` est aussi concerné. La correction des fiches SRD change les assertions de CA du Squelette dans `SrdCatalogTest:41` et `SceneApiTest:63`. Tout cela a été lu, et cherché avec `grep weapon`.. Le filet, ce sont les 50 tests existants (`grep -c @Test` observé), dont les 14 d'`AttackRulesTest` : ils doivent passer sans changer de comportement. Pas de phase de filet dédiée. | 1, 2, 3 |
| Architecture | Départ : le serveur ne tient qu'une `Scene` et un `GameState`, et n'a ni moteur ni journal. Les armes n'ont pas de portée, et `AttackAction` ignore la distance. Cible : la règle de portée vit dans `dnd-engine` et reçoit une distance en mètres. `Scene` reste sans règle, et le serveur ne fait que mesurer et convertir. `SceneSession` tient un `Engine` par carte et un journal des descriptions d'événements. | 1, 2 |
| Runtime | Aucune nouvelle dépendance | none |
| Documentation | `project-brief.md` : aucune source officielle du SRD n'est indiquée (`grep SRD` observé, seul `dnd-engine/README.md` cite Open5e). Le PDF officiel 5.2.1 y devient la référence des règles et du catalogue. Également `architecture.md` (portées, moteur en session, Bouclier absent du serveur), `codebase-map.md` (ressources `attacks` et `journal`, `JournalPanel`), `project-brief.md` (fonctionnalité), `testing.md` (dés scriptés côté serveur), `dnd-engine/README.md` (ligne `state/`, si `Attack` y entre) | 4 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Le client mesure, le serveur tranche (`architecture.md`) | Les cercles de portée du client sont indicatifs. Le refus de portée vient du serveur. |
| La scène est générique : positions et distances euclidiennes, de centre à centre (`architecture.md`) | Aucune règle de portée dans `solovtt.scene`. La diagonale n'a pas de tolérance : choix de l'utilisateur. |
| Tout est listener, un ordre imposé devient une phase (`architecture.md`) | Le désavantage de portée longue est un listener `MODIFY_ROLL`. |
| Vitesses et longueurs du moteur en mètres, 1,5 m pour 5 ft (`architecture.md`) | Allonges et portées sont stockées en mètres et converties dans l'unité de la carte pour la feuille. |
| Aucune dépendance runtime dans les moteurs (`package.md`) | Rien n'est ajouté à `dnd-engine`. |
| Dés toujours scriptés, noms de tests en français, pas de mock (`testing.md`) | Tests des règles et de l'API d'attaque |
| Ne jamais tester sur 8080/5173 (`testing.md`) | Le parcours navigateur se fait sur 8081/5174. |
| Contenu tiré du SRD 5.2.1 officiel (CC-BY-4.0) | Portées de la Dague et de l'Arc court, et correction des fiches du Squelette et du Loup |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Les règles de portée (allonge, portée normale et longue, lancer) restent dans `dnd-engine`. Chaque attaque y expose ses longueurs en mètres, et l'appelant fournit la distance en mètres. | Choix de l'utilisateur : rien de D&D ne fuit dans la scène ni dans le serveur, qui ne fait que mesurer, convertir et relayer. |
| Une cible au-delà de l'allonge ou de la portée longue est refusée avant la soumission (`IllegalArgumentException`, puis 400 avec la raison), sans événement | Rien ne s'est passé dans la partie : le journal reste celui des actions réelles. |
| Distance euclidienne stricte : un voisin en diagonale (2,12 m) est hors d'une allonge de 1,5 m | Choix de l'utilisateur : la grille n'est qu'indicative et les jetons peuvent être posés librement. |
| Le serveur enregistre toutes les règles sauf `ShieldReaction` jusqu'à la story « répondre à une décision » | Choix de l'utilisateur : le moteur ne s'arrête jamais sur une décision que l'interface ne sait pas encore poser. |
| Les attaques à distance au contact d'un ennemi (désavantage, SRD p. 15) sont hors périmètre | Il faudrait savoir qui est hostile et connaître toutes les positions. L'utilisateur a limité la story à la portée normale, à l'allonge et au lancer. |
