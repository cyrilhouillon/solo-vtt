# Vérifications de code

À lancer depuis la racine du dépôt.

## Avant commit

| Ordre | Commande | Vérifie |
| ----- | -------- | ------- |
| 1 | `mvn -q compile` | compilation |
| 2 | `mvn -q test` | tests du moteur, des règles et du serveur |
| 3 | `npm run build` dans `vtt-web/` | typage (`vue-tsc`) et build du client |

## Avant push

| Ordre | Commande | Vérifie |
| ----- | -------- | ------- |
| 1 | `mvn -q verify` | build complet et tests |
| 2 | `npm run build` dans `vtt-web/` | typage et build du client |

## Comportement

Si une correction est nécessaire, lancer un agent par vérification en échec.
