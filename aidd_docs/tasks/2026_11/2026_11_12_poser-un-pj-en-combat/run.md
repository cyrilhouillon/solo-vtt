# Run record

## Phase 1 · Moteur : attaques structurées et profil de créature

- Date: 2026-11-12
- Validated on: a28a1b3

- [pass] Task 1 — `DamageType.values()` compte 13 types — `mvn -q test -pl dnd-engine` exit 0, `CharacterProfileTest.lesTreizeTypesDeDegatsDuSrd`
- [pass] Task 2 — Une fiche guidée a des attaques structurées conformes au SRD ; chaque refus donne sa raison — `GuidedValuesTest` (Épée à deux mains 2d6+2 tranchants 1,5 m ; Javeline 1,5 m et 9/36 m ; Arc long 45/180 m sans allonge), `CharacterProfileTest.uneAttaqueImpossible_estRefuseeAvecSaRaison`, `laSarbacaneGuidee_infligeDesDegatsFixes`
- [pass] Task 3 — `profile().create(...)` rend une créature aux valeurs de la fiche, PV actuels compris ; fiche à 0 PV max refusée — `laCreatureDUneFiche_aSesValeurs_etUneAttaqueParMode`, `unPJBlesse_gardeSesPVActuels`, `uneFicheAZeroPVMax_neSePosePas`
- [pass] Task 4 — `CatalogEntry.character` crée la créature du profil sous le nom donné ; `mvn -q test -pl dnd-engine` passe — `uneEntreeDePersonnage_creeSaCopieSousLeNomDonne` ; surefire : tests=99 failures=0 errors=0
