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

## Phase 4 · Client : écran de combat, reprise et rejeu

- Date: 2026-11-09
- Validated on: b144269

- [pass] Task 1 — Un combat terminé s'ouvre en rejeu ; un combat en cours s'ouvre en direct, repris s'il n'était pas courant — Chrome 5174/8081 : « Revoir le combat » d'« Embuscade » → `/combats/<id>/rejeu`, « Pas 3 / 3 » ; « Reprendre le combat » de « Combat 2 » courant → `/combat` ; après une nouvelle carte, le même bouton reprend Combat 2 (carte 30 × 20 m, round 1) ; bouton Carnet du rejeu → l'entrée du combat. Écart au plan : le choix rejeu / direct / reprise se fait dans `CombatEntryView`, et le rejeu est un écran à part (`ReplayScreen`) plutôt qu'un mode de `GameScreen`
- [pass] Task 2 — En rejeu, aucun geste ne modifie la partie — Chrome : `fetch` espionné après le chargement, puis Début, →×3, ←, glisser un jeton, Suppr, Fin : aucune requête (`calls: []`) ; le jeton glissé ne bouge pas ; tableau sans colonne de retrait, feuilles sans bouton d'attaque, aucun outil ni contrôle de combat
- [pass] Task 3 — Les cinq commandes donnent l'état attendu ; en lecture, chaque pas part 2 s après la fin de l'animation du précédent ; `npm run build` passe — Chrome, combat de 19 pas (3 créatures, 6 attaques) : ouverture sur « Pas 19/19 » (Gobelin à terre, Guerrière 1/12) ; Début → « Lancement », PV pleins, aucune piste ; pas avant → déplacement du Gobelin, puis attaque : d20 3D sur la carte, piste posée à l'arrêt ; pas arrière instantané (« Pas 8/19 », journal tronqué) ; lecture mesurée : pas sans dés à +2,00 s, +2,05 s ; pas d'attaque 5,4 s (animation puis 2 s) ; Pause arrête (« Pas 9/19 » trois secondes après) ; écran en direct inchangé après l'extraction de `dice/tray.ts` (piste relue du journal) ; `npm run build` ✓ built. Non vérifié au navigateur : `prefers-reduced-motion` (le lancer se résout alors aussitôt, `MapView.rollDice`)
- [pass] Task 4 — Les trois fichiers de mémoire décrivent combats, rejeu, reprise et section « Combats » — `architecture.md` (agrégat combat, entrée du Carnet, routes, rejeu client, « nouvelle carte » qui garde le combat), `codebase-map.md` (`CombatStore`, `CombatsResource`, `CombatReplay`, routes, `ReplayScreen`, `CombatEntryView`, `CombatThumbnail`, `dice/tray.ts`), `project-brief.md` (fonctionnalité), `DESIGN.md` (écran de rejeu, entrée du Carnet), commit 959c6d4

## Final

- Date: 2026-11-09
- Validated on: b144269

- [pass] `mvn -q verify` — exit 0
- [pass] `mvn test` — `Tests run: 268, Failures: 0, Errors: 0, Skipped: 0` (vtt-server)
- [pass] `npm run build` dans `vtt-web/` — ✓ built, `vue-tsc` sans erreur
