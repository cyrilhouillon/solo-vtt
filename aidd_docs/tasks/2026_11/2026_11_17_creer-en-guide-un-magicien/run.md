# Run

## Phase 1 · Magicien, sorts et emplacements dans `dnd-engine`

- Date: 2026-11-17
- Validated on: 4254e37

- [pass] Task 1 — `Spell` a les 45 sorts de la liste du Magicien, Bouclier d'id `shield` ; le Magicien a le dé de vie, les sauvegardes, les compétences, les valeurs suggérées, les équipements de sa note SRD — `mvn test -pl vtt-engine,dnd-engine` : `Tests run: 187, Failures: 0` ; `MagicienTest` (13 tests) : 15 sorts mineurs et 30 du 1er niveau, ids distincts, `Spell.SHIELD.id() == ShieldReaction.SPELL` ; valeurs 8/12/13/15/14/10, PV 7 (d6 + 1), sauvegardes Int et Sag, équipement A (Dague, Bâton de combat, 5 po) et B (55 po), d'après `Compendio SRD/Français/Personnages/Classes/Magicien.md`.
- [pass] Task 2 — Chaque refus du Test Scope a sa raison ; la construction suggérée est valide — `MagicienTest.desSortsImpossibles_sontRefusesAvecLaRaison` (4 sorts mineurs, 7 au grimoire, 5 préparés, préparé hors grimoire, Bouclier en sort mineur, Lumière au grimoire, doublon, sort inconnu, sorts d'un Guerrier) et `leMagicienSuggere_estValide_avecLesSortsRecommandes`.
- [pass] Task 3 — Magicien suggéré : PV 7, DD 12, attaque +4, 2 emplacements ; profil avec Bouclier préparé : `knowsSpell("shield")` et emplacements restants ; un emplacement dépensé ne dépasse jamais le maximum ; Magicien → Guerrier retire les sorts et garde une retouche indépendante — `unMagicien_remplitLaFiche`, `unMagicienQuiPrepareBouclier_leConnaitEnCombat_avecSesEmplacementsRestants` (2 puis 1 emplacement), `unEmplacementDepense_seCompteDansLHistoire_jamaisAuDelaDuMaximum` (1, 2, 2 ; niveau 3 ignoré), `passerDeMagicienAGuerrier_retireLesSorts_etGardeUneRetoucheIndependante`, `changerUnSortPrepare_neReecritQueLesPrepares_etGardeLesEmplacementsDepenses` ; `mvn -q test` à la racine : exit 0 ; `npm run build` : `built`.
