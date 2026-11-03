# Exécutions

## 2026-11-03 · head `e49b6b1`

Arbre de travail identique au head au moment des commandes.

| Commande | Résultat |
| --- | --- |
| `mvn -q test` (racine) | sortie 0 ; `vtt-engine` 25, `dnd-engine` 68, `vtt-server` 110 tests, 0 échec, 0 erreur (dont `MapApiTest`, `MapStoreTest`, `JsonFileMapStoreTest`, `ResumeGameTest`, `SceneWithoutMapTest`) |
| `npm run build` (`vtt-web/`) | sortie 0 ; `vue-tsc` sans erreur, build Vite fait (avertissement connu : chunk `DiceRoller` > 500 kB) |

Non fait : parcours navigateur de la phase 3 sur 8081 / 5174.
