# Brief du projet

## Ce que c'est

- `solo-vtt` : un Virtual TableTop web modulaire, conçu spécialement pour le jeu de rôle en solo.
- Aujourd'hui, le dépôt contient `vtt-engine` (moteur de règles et scène génériques), `dnd-engine` (squelette de règles D&D 2024 qui s'appuie dessus), `vtt-server` (serveur Quarkus qui les expose) et `vtt-web` (client Vue).

## Pourquoi il existe

- Les VTT existants sont pensés pour une table multijoueur avec un MJ humain. `solo-vtt` vise le joueur seul : le moteur arbitre les règles, et le joueur ou une IA répond aux décisions.
- `dnd-engine` valide l'architecture du moteur avant d'y brancher du contenu et une interface.

## Vocabulaire du domaine

Le code, les commentaires et les messages sont en français ; les identifiants sont en anglais.

| Terme | Sens |
| ----- | ---- |
| SRD 5.2 | Référence de règles D&D 2024 sous licence CC-BY-4.0, base des règles implémentées. Source à consulter pour toute règle ou fiche : le PDF officiel 5.2.1, https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf (pas de wiki tiers) |
| Phase | Étape ordonnée d'une action (`AttackPhase`) ; l'ordre de l'enum est l'ordre d'exécution |
| Listener | Une règle qui agit dans une phase ; règles de base et exceptions ont le même statut |
| RuleBook | Registre des listeners actifs ; activer un module = enregistrer ses listeners |
| Résolution | Ce qui est en cours sur la pile du moteur (une attaque, un sort…) |
| Décision | Question posée à un joueur ou une IA ; suspend le moteur jusqu'à `Engine.answer` |
| Réaction | Capacité déclenchée pendant l'action d'un autre (Bouclier), via une décision |
| Événement | `GameEvent` : seul moyen de modifier l'état ; le journal est rejouable |
| CA | Classe d'armure |
| Scène | Une carte (dimensions, unité) et les jetons posés dessus ; générique, sans règle |
| Jeton | Pion d'une scène, qui référence une entité du système par son id |
| Avantage / désavantage | Lancer 2d20 et garder le meilleur / le pire ; les deux s'annulent |

## Fonctionnalités clés

- Attaque complète en 12 phases, du jet de d20 aux dégâts (résistance puis vulnérabilité).
- Conditions, Bénédiction, réaction Bouclier, style de combat Duel.
- Moteur suspendable sur décision, pile de résolutions, journal d'événements rejouable.
- Démo console (`dnd.engine.demo.Main`), avec un mode interactif.
- Préparation de scène dans le navigateur : carte vide en mètres ou en pieds, créatures du catalogue SRD posées au clic, distance entre deux créatures, feuille de chaque créature.
- Attaque depuis la feuille : ciblage sur la carte, allonge, portée normale et longue, lancer ; résultat dans le journal et PV mis à jour.
- Décision du moteur dans le navigateur : la réaction Bouclier arrête l'attaque, le joueur choisit dans un dialogue, puis l'attaque reprend.
