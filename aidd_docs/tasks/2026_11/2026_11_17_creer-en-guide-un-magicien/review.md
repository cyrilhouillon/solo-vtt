# Review: créer en guidé un Magicien

- Rounds: 1
- Started: 2026-11-17

## Round 1 · r-7c2e

- Date: 2026-11-17
- By: Cyril Houillon
- Diff: `main...8d1b876`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 10/10 met, 0 unmet, 0 unverified at runtime, 0 out of the diff

### Criteria

- [x] Phase 1 · T1 — `Spell` porte les 45 sorts de la liste du Magicien, Bouclier d'id `shield` ; Magicien d6, Int/Sag, 7 compétences, valeurs suggérées, équipements A et B — `Spell.java:23-68`, `CharacterClass.java:46`, `StartingEquipment.java:33`, `MagicienTest.java:57` ; run.md Final (`mvn -q verify`, 684 tests, 0 échec, sur 8b29583, seul du Markdown depuis)
- [x] Phase 1 · T2 — chaque refus a sa raison, la construction suggérée est valide — `Build.java:176`, `MagicienTest.java:65,128`
- [x] Phase 1 · T3 — PV 7, DD 12, attaque +4, 2 emplacements ; Bouclier préparé connu au profil avec emplacements restants ; jamais au-delà du maximum ; Magicien → Guerrier — `CharacterSheet.java:135,147,152,213`, `GuidedValues.java:92,166`, `SheetHistory.java:36`, `MagicienTest.java:80,162,180,202,219`
- [x] Phase 2 · T1 — Magicien construit (DD 12, +4, 2 emplacements), `GET spells` 45 sorts, fiche d'avant relue sans sort, refus 400 — `SheetResource.java:109`, `DndDtos.java:399,411`, `SheetStore.java:115` ; tests cités dans run.md Phase 2, repassés par le run Final
- [x] Phase 2 · T2 — Bouclier en combat : 1 emplacement dépensé ; rejeu et reprise sans envoi ; PV et emplacements écrits ensemble — `SceneSession.java:292`, `SheetCharacters.java:92`, `SheetResource.java:247` ; `FicheSuitLeCombatTest`, `SheetCharactersTest`, run Final
- [x] Phase 3 · T1 — `npm run build` passe ; champs de sort relus au rechargement — run.md Phase 3 (8b29583) : `built`, fiche « Elminster » relue
- [x] Phase 3 · T2 — Intelligence, DD 12, +4 ; retouche libre gardée ; « — » sans caractéristique — run.md Phase 3 : « DD 12 », « Attaque +4 », Trait de feu gardé au rechargement
- [x] Phase 3 · T3 — sorts recommandés cochés, Bouclier préparé passe à la fiche, préparé décoché du grimoire, Magicien → Guerrier, 1 emplacement dépensé après Bouclier — run.md Phase 3 : « Sorts préparés (3 / 4) », fiche « 2 / 1 » après « Lancer Bouclier »
- [x] Phase 4 · T1 — chaque sort et règle du Magicien dans la liste, chaque ligne non jouée renvoie à une story — run.md Phase 4 (b77f43a, seul du Markdown depuis) : manquants `[]`, 4 stories créées, repos complété
- [x] Phase 4 · T2 — mémoire : `Spell`, champs de sort, `GET …/dnd/spells`, suivi des emplacements, piège Bouclier corrigé — `architecture.md` (paragraphe Magicien, piège Bouclier), `codebase-map.md`, `project-brief.md`

### Findings

- `vtt-web/modules/dnd/CharacterSheetView.vue:253` : `onSpells` a été inséré entre le commentaire « Un geste d'inventaire part aussitôt… » et `onInventory` : deux docblocks s'empilent sur `onSpells`, et `onInventory` perd le sien → remonter `onSpells` au-dessus du commentaire d'inventaire — 🟢 minor, standards
- `dnd-engine/src/main/java/dnd/engine/sheet/GuidedValues.java:226` : « Sorts (Intelligence) » est écrit en dur alors que la caractéristique vient de `casting.ability()` → la lire de `casting` (son libellé), pour que la prochaine classe lanceuse n'hérite pas d'un texte faux — 🟢 minor, code-health
