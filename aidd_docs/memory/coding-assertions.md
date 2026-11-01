# Vérifications de code

À lancer depuis `dnd-engine/`.

## Avant commit

| Ordre | Commande | Vérifie |
| ----- | -------- | ------- |
| 1 | `mvn -q compile` | compilation |
| 2 | `mvn -q test` | tests de règles |

## Avant push

| Ordre | Commande | Vérifie |
| ----- | -------- | ------- |
| 1 | `mvn -q verify` | build complet et tests |

## Comportement

Si une correction est nécessaire, lancer un agent par vérification en échec.
