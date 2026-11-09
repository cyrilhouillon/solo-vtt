# Run

## Phase 1 · Base verte et cœur sans Mythic

- Date: 2026-11-09
- Validated on: 407344c

- [pass] Task 1 — `mvn -q test` et `npm run build` passent — `mvn -q test` : exit 0, rapports surefire : 435 tests, 0 échec, 0 erreur ; `npm run build` dans `vtt-web/` : exit 0 (seul l'avertissement habituel de taille de chunk).
- [pass] Task 2 — `run.md` dit, sorties à l'appui, que `vtt-server/src` et `vtt-web/src` ne citent pas Mythic et que seuls les assemblages le citent — `grep -rni mythic vtt-server/src vtt-web/src` : 0 ligne. `grep -ni mythic pom.xml vtt-server/pom.xml` : `pom.xml` l. 17-18 (`<module>`), 46-52 (versions), `vtt-server/pom.xml` l. 45-46 (dépendance). `grep -rli mythic vtt-web` hors `node_modules` et `dist` : 4 fichiers sous `vtt-web/modules/mythic/`, plus `vtt-web/.impeccable/hook.cache.json`, cache d'outil ignoré par git (`.gitignore:11`), pas du code.
