---
objective: "Le joueur glisse un jeton (ou le déplace au clavier) : en combat, la créature active se déplace dans la limite de sa vitesse restante, affichée pendant le geste, refusée au-delà et rendue pleine à son tour suivant ; toute autre créature, et toute créature hors combat, se repositionne librement ; la pose et le déplacement sont hors grille par défaut, Maj cale sur la case."
status: pending
---

# Plan: déplacer une créature dans la limite de sa vitesse

Source: aidd_docs/backlog/stories/deplacer-une-creature-dans-la-limite-de-sa-vitesse.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Client : hors grille par défaut, Maj cale sur la case | [`phase-1.md`](./phase-1.md) | none |
| 2   | Moteur : déplacer un jeton, dépenser la vitesse de la créature active | [`phase-2.md`](./phase-2.md) | none |
| 3   | Serveur : ressource de déplacement et vitesse restante | [`phase-3.md`](./phase-3.md) | 2 |
| 4   | Client : glisser ou déplacer au clavier, distance parcourue et restante | [`phase-4.md`](./phase-4.md) | 1, 3 |
| 5   | Mettre à jour la mémoire projet et le design | [`phase-5.md`](./phase-5.md) | 4 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf | p. 14 « Movement and Position » : à son tour, une créature se déplace d'une distance au plus égale à sa vitesse ; chaque partie du déplacement se déduit jusqu'à épuisement (« Breaking Up Your Move » : avant et après une action) ; on ne termine pas volontairement un déplacement dans l'espace d'une autre créature. p. 191 Unconscious et p. 187 Restrained : « Speed 0 ». Foncer (Dash, p. 180) et le terrain difficile existent mais restent hors de la story. |
| https://github.com/konvajs/konva/blob/master/src/Stage.ts | Lu dans la copie installée (Konva 10.9.1, `lib/Stage.js`) : `draggable` et `dragDistance` existent sur `Node` ; un glisser annule le clic (`_cancelClick`) : glisser un jeton ne le sélectionne pas. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Regression | Moyenne. `Combat` gagne le déplacement dépensé du tour : `copy()` et `signature()` l'incluent, et `CombatTest.rejouerLeJournalDuCombatRedonneLEtatFinal` le vérifie. `CombatDto` gagne `movementLeft` (champ ajouté, rien ne casse côté `CombatApiTest`, lu). Côté client, inverser Maj change la pose de `onBackgroundClick` (`App.vue` l. 311-356) et l'aperçu (`SceneCanvas.drawPointerPreview`) ; la règle de case occupée passe d'un index de case à un chevauchement (`grid.occupant`), aussi lue par l'annonce clavier de `MapView`. Le client n'a pas de tests (aucun script `test` dans `vtt-web/package.json`) : parcours navigateur sur 8081/5174. Filet : `mvn -q test` avant et après chaque phase (non lancé pendant la planification : à vérifier au début de la phase 2) ; pas de phase de filet dédiée. | 1, 2, 3, 4 |
| Architecture | Départ : un jeton ne bouge plus une fois posé (`Scene` n'a que `place` et `remove`) ; aucune notion de déplacement dans `dnd-engine`. Cible : `Scene.move` (générique, bornes de la carte) ; `dnd.engine.combat.Move`, une résolution qui refuse puis émet `CreatureMoved` pour la seule créature active en combat. La position reste dans la `Scene`, la vitesse dépensée dans `Combat` : le journal rejoue la vitesse, pas les positions (limite pour la story « reprendre une partie »). Les autres déplacements (hors combat, créature non active) sont hors journal, comme la préparation. | 2, 3 |
| Documentation | `architecture.md` : ligne Maj réécrite (hors grille par défaut, Maj cale ; chevauchement refusé par le client), règles du déplacement, piège des positions hors journal. `codebase-map.md` : `Move`, `Scene.move`, route `scene/tokens/{id}/move`. `project-brief.md` : fonctionnalité. `DESIGN.md` : aperçu de glisser (fantôme, trait, cercle de vitesse restante, bulle), vitesse restante sur la feuille. Story en `status: done`. | 5 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| L'état ne change que via `Engine.emit` ; rejouer le journal redonne l'état (`architecture.md`) | Le déplacement de la créature active est l'événement `CreatureMoved` ; la vitesse dépensée se rejoue. |
| Le client mesure, le serveur tranche (`architecture.md`) | L'aperçu calcule la distance ; le serveur la recalcule et refuse au-delà de la vitesse restante. |
| Une règle D&D vit dans `dnd-engine` et vaut pour tout appelant ; `IllegalArgumentException` avec la raison, sans événement (`AttackAction.of`) | `Move.of` refuse avant toute émission ; le serveur rend 400, pas 409 (un 409 fait recharger la scène au client). |
| Une décision en attente bloque toute action (409, `DecisionPendingException`) | Le déplacement est refusé tant que « Qui joue ? » ou Bouclier attend. |
| Seul le client refuse une case occupée (`architecture.md`) | Le chevauchement reste une vérification client, à la pose comme au déplacement. |
| Dés scriptés, noms de tests en français, pas de mock ; règle nouvelle = test de la situation de table (`testing.md`) | Vitesse, fractionnement, refus, vitesse rendue, rejeu, API. |
| Ne jamais tester sur 8080/5173 (`testing.md`) | Parcours navigateur sur 8081/5174. |
| « The Brass Is Alive Rule » et « The Two Lights Rule » (`DESIGN.md`) | Le déplacement en cours est un état vivant (laiton) ; le refus est en `--danger`, comme la case occupée. |
| Règles D&D tirées du SRD 5.2.1 officiel (`project-brief.md`) | Vitesse, fractionnement, vitesse 0 à 0 PV et entravé. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| En combat, seule la créature active est limitée par sa vitesse, et son déplacement est un événement du journal. Toute autre créature, et toute créature hors combat, se repositionne librement, hors journal. | Choix de l'utilisateur : le joueur tient aussi le rôle du MJ. |
| La distance d'un déplacement est la ligne droite euclidienne du départ à l'arrivée, sans chemin ni terrain difficile ; un tour se fractionne en autant de déplacements que la vitesse le permet. | Même mesure que la portée (`architecture.md`) ; SRD p. 14 « Breaking Up Your Move ». |
| La vitesse dépensée vit dans `Combat` et revient à zéro à `TurnStarted` de la créature. | Elle n'a de sens qu'en combat et que pour le tour en cours ; rejouable avec le reste du combat. |
| À 0 PV ou entravée, la créature active a une vitesse de 0 : tout déplacement est refusé avec la raison. | SRD p. 187 et 191 ; même lecture que le refus d'attaque à 0 PV. |
| Pose et déplacement sont hors grille par défaut ; Maj cale sur le centre de la case. Une position est refusée par le client si un autre jeton est à moins d'une case de centre à centre. | Choix de l'utilisateur (usage le plus courant) ; une règle de chevauchement vaut pour les positions libres comme calées. |
| Clavier : D déplace la créature sélectionnée avec le curseur de case de la pose (flèches, Entrée, Échap). | Choix de l'utilisateur : glisser plus une alternative clavier. |
| Foncer (Dash) et le terrain difficile restent hors de cette story. | Non demandés par la story ; l'epic exclut ce qui dépasse le combat de base. |
| La limite de déplacement est facultative dans le contrat client : `movementLeft` vaut `null` pour « illimité », et le client n'affiche cercle, reste ou refus de distance que si une limite existe. | Choix de l'utilisateur : un autre système que D&D peut ne pas limiter le déplacement ; le client ne doit coder aucune règle D&D. |
