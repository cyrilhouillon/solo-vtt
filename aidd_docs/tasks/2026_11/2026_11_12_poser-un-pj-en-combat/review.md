# Review: poser un PJ en combat depuis le catalogue

- Rounds: 1
- Started: 2026-11-12

## Round 1 · r-7c3e

- Date: 2026-11-12
- By: Cyril Houillon
- Diff: `main...5fd34ad` plus the working tree (`vtt-web/vite.config.ts`, `.mcp.json` : changements locaux du joueur, hors de la feature)
- Axes: code, functional, relevancy
- Verdict: changes-requested
- Score: 7/13 met, 0 unmet, 6 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1·1 `DamageType.values()` compte 13 types — `dnd-engine/src/main/java/dnd/engine/state/DamageType.java:4-17`
- [x] P1·2 Fiche guidée en attaques structurées conformes au SRD ; chaque refus donne sa raison — `GuidedValues.java:149-157`, `SheetAttack.java:20-38`
- [x] P1·3 `profile().create(...)` aux valeurs de la fiche, PV actuels compris ; 0 PV max refusé — `CharacterSheet.java:109-113`, `CreatureProfile.java:31-41`, `Creature.java:45`
- Unverified at runtime: P1·4 `CatalogEntry.character` et `mvn -q test -pl dnd-engine` passe — code présent (`CatalogEntry.java:37-39`) ; run sur `a28a1b3`, autre head
- Unverified at runtime: P2·1 `mvn -q test -pl dnd-server`, 400 avec raison, fiche d'avant 409 — run sur `c97600b`, autre head
- Unverified at runtime: P2·2 `npm run build` et saisie dans le navigateur — run sur `c97600b`, autre head
- Unverified at runtime: P3·1 `mvn -q test -pl dnd-server`, liste suit la corbeille, copie suit la fiche — code présent (`SheetCharacters.java:47-78`) ; run sur `48de503`, dont le working tree diffère par `vtt-web/vite.config.ts` (config : autre head, bien que le Vite de test ne la lise pas)
- [x] P3·2 PJ au catalogue seulement si D&D actif ; `/api/catalog` 404 — `CampaignCatalog.java:78-83`, `CatalogResource.java` sans `@Path`
- [x] P3·3 Retouche, renommage, suppression de la fiche sans effet sur reprise et rejeu — `SceneSession.java:341-345` (`placed` ne lit que `Place.profile`), `SavedGame.java:232`
- Unverified at runtime: P3·4 `mvn -q verify` passe ; SRD sans prétirés — retrait visible `SrdCatalog.java:26` ; run sur `48de503`, autre head
- Unverified at runtime: P3·5 `npm run build` ; PJ glissé, en combat, déplacé, attaquant — run sur `48de503`, autre head
- [x] P4·1 Aucune mention de `/api/catalog` commun ni des prétirés au catalogue dans `aidd_docs/memory` — `grep` : `/api/catalog` absent ; prétirés cités seulement comme retirés et dans `testing.md` (`TestCharacters`)
- [x] P4·2 Story `done`, plan `implemented` — `poser-un-pj-en-combat-depuis-le-catalogue.md:3`, `plan.md:3`

### Findings

- `dnd-engine/src/main/java/dnd/engine/sheet/SheetAttack.java:14` : `damageBonus` saisi sans borne entre au moteur, où `RollDamage` (`CoreAttackRules.java:147`) peut rendre un total négatif que `Creature.takeDamage` (`Math.max(0, hitPoints - amount)`) transforme en soin, au-delà du maximum (PJ à 1d4-3 qui tire 1 : la cible gagne 2 PV) → plancher à 0 dans `RollDamage` (`Math.max(0, …)`), SRD 5.2.1 p. 16 « Damage Rolls » : « it's possible to deal 0 damage but not negative damage », et test de la situation de table — 🟡 major, backend
- `dnd-engine/src/main/java/dnd/engine/sheet/GuidedValues.java:155` : la Sarbacane guidée reçoit `flatDamage() + ability`, alors que le SRD 5.2.1 p. 16 « Damage Rolls » dit : « you don't add your ability modifier to a fixed damage amount that doesn't use a roll, such as the damage of a Blowgun » ; le plan (phase 1, « 1 + modificateur ») et `CharacterProfileTest.laSarbacaneGuidee_infligeDesDegatsFixes` portent la même erreur → `damageBonus` = `flatDamage()` seul quand `diceCount() == 0`, test corrigé (1 dégât quelle que soit la Dextérité) — 🟡 major, backend
- `dnd-engine/src/main/java/dnd/engine/state/CreatureProfile.java:45` : `new ArrayList<>(weapons)` copie une liste que le constructeur compact recopie aussitôt (`List.copyOf`) → passer `weapons` tel quel — 🟢 minor, code-health
- `vtt-web/modules/dnd/sheet.ts:45` : `DAMAGE_TYPES` est trié par id anglais, si bien que la liste affichée ne suit pas l'ordre des libellés (« d'acide, contondants, de froid, de feu… ») → trier par libellé (ou garder l'ordre du SRD) — 🟢 minor, frontend
