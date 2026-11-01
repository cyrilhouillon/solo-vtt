# Brief du projet

## Ce que c'est

- `solo-vtt` : un Virtual TableTop web modulaire, conçu spécialement pour le jeu de rôle en solo.
- Aujourd'hui, le dépôt contient `vtt-engine`, le moteur de règles générique, et `dnd-engine`, un squelette de règles D&D 2024 qui s'appuie dessus. Les deux serviront de librairies à l'application.

## Pourquoi il existe

- Les VTT existants sont pensés pour une table multijoueur avec un MJ humain. `solo-vtt` vise le joueur seul : le moteur arbitre les règles, et le joueur ou une IA répond aux décisions.
- `dnd-engine` valide l'architecture du moteur avant d'y brancher du contenu et une interface.

## Vocabulaire du domaine

Le code, les commentaires et les messages sont en français ; les identifiants sont en anglais.

| Terme | Sens |
| ----- | ---- |
| SRD 5.2 | Référence de règles D&D 2024 sous licence CC-BY-4.0, base des règles implémentées |
| Phase | Étape ordonnée d'une action (`AttackPhase`) ; l'ordre de l'enum est l'ordre d'exécution |
| Listener | Une règle qui agit dans une phase ; règles de base et exceptions ont le même statut |
| RuleBook | Registre des listeners actifs ; activer un module = enregistrer ses listeners |
| Résolution | Ce qui est en cours sur la pile du moteur (une attaque, un sort…) |
| Décision | Question posée à un joueur ou une IA ; suspend le moteur jusqu'à `Engine.answer` |
| Réaction | Capacité déclenchée pendant l'action d'un autre (Bouclier), via une décision |
| Événement | `GameEvent` : seul moyen de modifier l'état ; le journal est rejouable |
| CA | Classe d'armure |
| Avantage / désavantage | Lancer 2d20 et garder le meilleur / le pire ; les deux s'annulent |

## Fonctionnalités clés

- Attaque complète en 12 phases, du jet de d20 aux dégâts (résistance puis vulnérabilité).
- Conditions, Bénédiction, réaction Bouclier, style de combat Duel.
- Moteur suspendable sur décision, pile de résolutions, journal d'événements rejouable.
- Démo console (`dnd.engine.demo.Main`), avec un mode interactif.
