# Run

## Phase 1 · Construction du Guerrier dans `dnd-engine`

- Date: 2026-11-12
- Validated on: cb0554e

- [pass] Task 1 — Chaque arme et armure du Guerrier a les valeurs de sa fiche SRD (dégâts, portée, CA, botte) — `mvn test -pl dnd-engine` : Tests run: 86, Failures: 0 ; `GuidedValuesTest.unGuerrierA_Defense_remplitLaFiche` : Épée à deux mains 2d6 tranchants (Écorchure), Fléau d’armes 1d8 contondants (Sape), Javeline 1d6 perforants 9/36 m (Ralentissement), Cotte de mailles 16 ; `unGuerrierB_Archerie_aCA14_etLArcLongA6` : cuir clouté 12 + Dex, Arc long 1d8 45/180 m.
- [pass] Task 2 — Une construction impossible est refusée avec sa raison ; `Build.fighter()` est valide — `BuildTest` 8/8 : standard hors permutation, achat à plus de 27 points (avec le coût), valeur hors 8–15, dés sans jets ou valeurs hors totaux, trois compétences ou hors liste, Bottes en double ou quatre, refusés ; `leGuerrierSuggere_estValide_avecAthletismeEtPerception` ; dés scriptés 6-6-6-1 ×6 → six jets de 18.
- [pass] Task 3 — Guerrier A Défense : CA 17, PV 11 et les trois attaques attendues ; passer à B recalcule CA et attaques et garde les retouches qui n'en dépendent pas — `GuidedValuesTest` 4/4 : CA 17, PV 11/11, init +2, attaques +4 2d6+2 / 1d8+2 / 1d6+2 « 1,5 m ; lancer 9/36 m » ; A → B avec CA retouchée 20 et PV max 30 : CA 15, PV max 30 gardé ; Con ↔ Int : PV 9, expertise de Perception gardée.
