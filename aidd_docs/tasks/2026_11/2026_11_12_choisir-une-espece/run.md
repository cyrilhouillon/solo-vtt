# Run

## Phase 1 · Espèces et choix d'espèce dans `dnd-engine`

- Date: 2026-11-12
- Validated on: bc34385

- [pass] Task 1 — Chaque espèce a la taille, la vitesse, la vision et les traits de sa note SRD ; chaque option son texte et son type de dégâts — `SpeciesChoiceTest` (`lesNeufEspeces_ontLeursOptionsDuSrd`, `lesDragonsDonnentLeurTypeDeDegats`, `uneEspeceDUneSeuleTaille_laPrendDOffice`, `laVitesse_estCelleDeLEspece_ouDeSonOption`, `lesTraits_disentChaqueSousChoix…`) ; `mvn test -pl dnd-engine` : `Tests run: 116, Failures: 0, Errors: 0`
- [pass] Task 2 — Chaque choix impossible du Test Scope est refusé avec sa raison ; une espèce seule est valide — `SpeciesChoiceTest.unSousChoixImpossible_estRefuseAvecLaRaison` (Drow pour un Nain, taille P pour un Nain, Acrobaties hors Sens aiguisés, incantation Force, compétence pour un Halfelin), `uneCompetenceDejaMaitriseeParLaClasse_estRefusee`, `uneEspeceSeule_estValide`, `BuildTest.uneEspeceImpossible_refuseLaConstruction`
- [pass] Task 3 — Nain donne PV 12 ; Elfe sylvestre vitesse 10,5 et sa compétence ; changer d'espèce réécrit vitesse, PV et traits et garde une retouche dont la valeur dérivée ne change pas ; sans espèce, la fiche garde espèce et vitesse saisies — `GuidedValuesTest` (`unNain_…`, `unElfeSylvestre_…`, `unGoliathSansAscendance_…`, `passerDeNainAHautElfe_gardeLaVitesseRetouchee_…`, `passerDElfeSylvestreANain_…`, `uneConstructionSansEspece_…`) ; suite complète `mvn test` : BUILD SUCCESS (282 tests serveur, 0 échec)
