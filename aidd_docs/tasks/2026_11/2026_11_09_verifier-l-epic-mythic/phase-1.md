---
status: done
---

# Instruction: base verte et cœur sans Mythic

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `mvn -q test` à la racine, puis `npm run build` dans `vtt-web/` | exit 0 pour les deux | cli |
| Happy path   | `grep -rni mythic vtt-server/src vtt-web/src` | aucune ligne | cli |
| Happy path   | `grep -rni mythic` sur `pom.xml`, `vtt-server/pom.xml` et `vtt-web/` hors `node_modules` et `dist` | Mythic cité seulement par les deux `pom.xml` et `vtt-web/modules/mythic/` | cli |

## Tasks to do

### `1)` Filet de sécurité

> Partir d'une base verte.

1. Lancer `mvn -q test` à la racine et `npm run build` dans `vtt-web/` ; si rouge, s'arrêter et le signaler, sans toucher la production.

### `2)` Prouver que le cœur ne nomme pas Mythic

> Observer la sixième preuve de l'epic.

1. Lancer les deux recherches du Test Scope sur le commit vérifié.
2. Consigner la preuve 6 dans `run.md` : commandes, sorties, commit ; la phase 2 la reporte dans `verification.md`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `mvn -q test` et `npm run build` passent. |
| 2 | `run.md` dit, sorties à l'appui, que `vtt-server/src` et `vtt-web/src` ne citent pas Mythic et que seuls les assemblages le citent. |
