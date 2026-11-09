# Run

## Phase 1 · Serveur : combat enregistré, entrée du Carnet, reprise

- Date: 2026-11-09
- Validated on: 55ecadf

- [pass] Task 1 — Après lancement, attaque et fin, `combats/<id>.json` rejoué redonne la scène et les PV finals ; une partie d'avant sans `combatId` se reprend — `mvn test` : `Tests run: 265, Failures: 0` (dont `CombatsEnregistresTest` 9, `JsonFileCombatStoreTest` 3) ; serveur 8081 : lancer, répondre « Qui joue ? », attaquer, terminer → `combats/b13f….json` = `create, place, place, startCombat, answer, attack, endCombat` ; `GET …/combats/{id}` rend la scène finale (PV 12 et 10) ; un déplacement après la fin (200) ne change pas le fichier (dernière commande `endCombat`)
- [pass] Task 2 — Le lancement crée « Combat N » sous « Combats » ; écrire son contenu rend 400 ; renommer, déplacer, supprimer et restaurer marchent comme pour une page — 8081 : `GET …/notebook` rend « Combats » (`section: combats`) et « Combat 1 » (`combatId` b13f…, propre id 30e9…) ; `PUT …/content` 400, sous-page 400, renommer 204, supprimer 204, restaurer 204 puis `GET …/pages/{id}` → « Embuscade », même `combatId` ; tests `NotebookStoreTest`, `JsonFileNotebookStoreTest`, `CombatsApiTest` verts
- [pass] Task 3 — `GET …/combats/{id}` rend état et scène finale ; `resume` d'un combat en cours le remet en partie courante, 409 s'il est terminé, 404 s'il est inconnu — 8081 : en cours `ended False current True` ; après « Nouvelle carte » `current False`, `resume` 204 puis `GET …/combat` round 1 ; après la fin `ended True current False`, `GET …/combat` 204, la scène garde PV et positions ; `resume` d'un terminé 409 ; `GET …/combats/xyz` 404 ; reprise d'une partie illisible sans effet (`CombatsEnregistresTest`)
