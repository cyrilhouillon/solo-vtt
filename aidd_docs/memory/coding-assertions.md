# Vérifications de code

À lancer depuis la racine du dépôt.

## Avant commit

| Ordre | Commande | Vérifie |
| ----- | -------- | ------- |
| 1 | `mvn -q compile` | compilation |
| 2 | `mvn -q test` | tests du moteur et des règles |

## Avant push

| Ordre | Commande | Vérifie |
| ----- | -------- | ------- |
| 1 | `mvn -q verify` | build complet et tests |

## Comportement

Si une correction est nécessaire, lancer un agent par vérification en échec.
