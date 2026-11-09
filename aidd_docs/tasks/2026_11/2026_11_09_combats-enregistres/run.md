# Run

## Phase 1 · Serveur : combat enregistré, entrée du Carnet, reprise

- Date: 2026-11-09
- Validated on: 55ecadf

- [pass] Task 1 — Après lancement, attaque et fin, `combats/<id>.json` rejoué redonne la scène et les PV finals ; une partie d'avant sans `combatId` se reprend — `mvn test` : `Tests run: 265, Failures: 0` (dont `CombatsEnregistresTest` 9, `JsonFileCombatStoreTest` 3) ; serveur 8081 : lancer, répondre « Qui joue ? », attaquer, terminer → `combats/b13f….json` = `create, place, place, startCombat, answer, attack, endCombat` ; `GET …/combats/{id}` rend la scène finale (PV 12 et 10) ; un déplacement après la fin (200) ne change pas le fichier (dernière commande `endCombat`)
- [pass] Task 2 — Le lancement crée « Combat N » sous « Combats » ; écrire son contenu rend 400 ; renommer, déplacer, supprimer et restaurer marchent comme pour une page — 8081 : `GET …/notebook` rend « Combats » (`section: combats`) et « Combat 1 » (`combatId` b13f…, propre id 30e9…) ; `PUT …/content` 400, sous-page 400, renommer 204, supprimer 204, restaurer 204 puis `GET …/pages/{id}` → « Embuscade », même `combatId` ; tests `NotebookStoreTest`, `JsonFileNotebookStoreTest`, `CombatsApiTest` verts
- [pass] Task 3 — `GET …/combats/{id}` rend état et scène finale ; `resume` d'un combat en cours le remet en partie courante, 409 s'il est terminé, 404 s'il est inconnu — 8081 : en cours `ended False current True` ; après « Nouvelle carte » `current False`, `resume` 204 puis `GET …/combat` round 1 ; après la fin `ended True current False`, `GET …/combat` 204, la scène garde PV et positions ; `resume` d'un terminé 409 ; `GET …/combats/xyz` 404 ; reprise d'une partie illisible sans effet (`CombatsEnregistresTest`)

## Phase 2 · Serveur : rejeu pas à pas d'un combat terminé

- Date: 2026-11-09
- Validated on: 3aca379

- [pass] Task 1 — Le combat scripté donne exactement les pas attendus, le repositionnement libre dans le pas suivant, l'attaque et sa réponse dans un seul pas ; l'état du dernier pas égale celui de `GET …/combats/{id}` — `CombatReplayTest` vert (6 pas : départ sans combat, lancement, déplacement de l'active, fin de tour avec le repositionnement libre de la Magicienne, attaque + « Ne rien faire » avec cible PV 2, fin) ; 8081 : le combat « Combat 1 » donne 4 pas, le lancement et la réponse à « Qui joue ? » en un seul pas, l'attaque ratée avec sa cible (Guerrière, PV 12, comme la scène finale de `GET …/combats/{id}`)
- [pass] Task 2 — La route rend le rejeu d'un combat terminé, 409 pour un combat en cours, 404 pour un inconnu — 8081 : `GET …/combats/{id}/replay` 200 (terminé), 409 (« Combat 2 » en cours), 404 (`xyz`) ; `CombatsApiTest` vert

## Phase 3 · Client : l'entrée combat dans le Carnet

- Date: 2026-11-09
- Validated on: 79f4e3a

- [pass] Task 1 — La section « Combats » s'affiche même sans module ; aucune sous-page ne peut être créée ni déposée sous un combat ; `npm run build` passe — Chrome, 5174/8081, campagne sans module : l'arbre montre la section « Combats » (Combat 2, Embuscade) ; le menu d'une entrée n'a que Renommer, Déplacer sous…, Supprimer… ; le dialogue « Déplacer “Notes” » éteint Combat 2 et Embuscade ; `npm run build` ✓ built
- [pass] Task 2 — Ouvrir une entrée combat (arbre, lien `[[`, onglet, recherche) montre la miniature de son état actuel ou final ; le clic mène à l'écran du combat — Chrome : adresse de l'entrée « Embuscade » → titre, « Terminé », carte en petit avec 2 pastilles, « Revoir le combat », « Pages qui renvoient ici » ; « Combat 2 » depuis l'arbre → « En cours », jeton déplacé à sa place ; `[[Comb` dans « Notes » propose Combat 2, le lien posé ouvre l'entrée (onglet) qui cite Notes ; « Reprendre le combat » → `/combat` en direct ; après une nouvelle carte posée par l'API, « Reprendre le combat » reprend Combat 2 (carte 30 × 20 m, round 1)
