# Run

## Phase 1 · Base verte et cœur sans Mythic

- Date: 2026-11-09
- Validated on: 407344c

- [pass] Task 1 — `mvn -q test` et `npm run build` passent — `mvn -q test` : exit 0, rapports surefire : 435 tests, 0 échec, 0 erreur ; `npm run build` dans `vtt-web/` : exit 0 (seul l'avertissement habituel de taille de chunk).
- [pass] Task 2 — `run.md` dit, sorties à l'appui, que `vtt-server/src` et `vtt-web/src` ne citent pas Mythic et que seuls les assemblages le citent — `grep -rni mythic vtt-server/src vtt-web/src` : 0 ligne. `grep -ni mythic pom.xml vtt-server/pom.xml` : `pom.xml` l. 17-18 (`<module>`), 46-52 (versions), `vtt-server/pom.xml` l. 45-46 (dépendance). `grep -rli mythic vtt-web` hors `node_modules` et `dist` : 4 fichiers sous `vtt-web/modules/mythic/`, plus `vtt-web/.impeccable/hook.cache.json`, cache d'outil ignoré par git (`.gitignore:11`), pas du code.

## Phase 2 · Parcours navigateur de l'epic

- Date: 2026-11-09
- Validated on: bd58ec8

- [pass] Task 1 — 5174 répond et appelle 8081 ; `~/.solo-vtt/campagnes` et `~/.solo-vtt/modules` inchangés — `mvn -q install -DskipTests` exit 0 ; `GET :8081/api/campaigns` et `GET :5174/api/campaigns` → `[]` ; md5 des fichiers de `~/.solo-vtt/campagnes` et `~/.solo-vtt/modules` identique avant et après (`02d36682…`) ; campagne et `mythic.json` écrits dans le scratchpad.
- [pass] Task 2 — `verification.md` dit, preuve à l'appui, si chacune des preuves 1 à 4 de l'epic tient — Chrome 1568 × 733, thème clair, 5174 → 8081 : tables importées (Sens passe de « non importées » à Actions / Descriptions / Lieux) ; Session 6 « Oracle : Le garde est-il corrompu ? (Probable, chaos 5) / d100 : 8 → Oui exceptionnel » ; au chaos 9, 18 questions jusqu'à « Événement aléatoire : Vers un fil — action1-040 · action2-037 / Fils : Sauver Ireena (d10 : 3 → ligne 2) » ; « Fin de scène : contrôle gardé → chaos 9 → 8 » puis « d10 : 7 → Scène modifiée / Ajustement : Intensifier une activité (d10 : 4) » ; détail dans `verification.md`.
- [pass] Task 3 — `verification.md` dit, preuve à l'appui, si listes, chaos et tables reviennent à l'identique après redémarrage — relevé `GET` (campagnes, notebook, chaque page, `mythic/chaos`, `mythic/meaning-tables`) avant et après redémarrage de 8081 : `cmp` identique ; rechargement sur l'adresse de Fils : Fils, Personnages (lien Ismark), 4 onglets, chaos 8, tables importées ; console sans erreur.
- [pass] Task 4 — Plus rien n'écoute sur 8081 ni 5174 ; 8080 et 5173 inchangés — `Get-NetTCPConnection` après arrêt : seuls 8080 (PID 32248) et 5173 (PID 32860), les mêmes qu'avant.

## Phase 3 · Clore l'epic ou reporter ses écarts

- Date: 2026-11-09
- Validated on: 03ae67c

- [pass] Task 1 — Le statut de l'epic correspond à `verification.md` ; chaque écart bloquant ou gênant a un item dans `aidd_docs/backlog/` — `verification.md` : six preuves tenues, aucun écart bloquant ni gênant (deux cosmétiques listés) ; epic `jouer-sa-campagne-avec-mythic-gme-depuis-le-carnet.md` passée à `status: done` ; aucun item créé.
- [pass] Task 2 — `plan.md` et ses phases portent `status: implemented` — `plan.md` `status: implemented`, phases 1 à 3 `status: done` (convention du skill pour les phases ; comme le plan de l'epic Carnet, le plan porte `implemented`).

## Final

- Date: 2026-11-09
- Validated on: 03ae67c

- [pass] `mvn -q test` — exit 0, 435 tests, 0 échec (phase 1, aucun code modifié depuis).
- [pass] `npm run build` dans `vtt-web/` — exit 0 (phase 1, aucun code modifié depuis).
- [pass] Parcours navigateur 8081/5174 et reprise après redémarrage — phase 2, six preuves tenues.
