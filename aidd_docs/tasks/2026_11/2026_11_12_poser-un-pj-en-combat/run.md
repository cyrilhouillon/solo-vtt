# Run record

## Phase 1 · Moteur : attaques structurées et profil de créature

- Date: 2026-11-12
- Validated on: a28a1b3

- [pass] Task 1 — `DamageType.values()` compte 13 types — `mvn -q test -pl dnd-engine` exit 0, `CharacterProfileTest.lesTreizeTypesDeDegatsDuSrd`
- [pass] Task 2 — Une fiche guidée a des attaques structurées conformes au SRD ; chaque refus donne sa raison — `GuidedValuesTest` (Épée à deux mains 2d6+2 tranchants 1,5 m ; Javeline 1,5 m et 9/36 m ; Arc long 45/180 m sans allonge), `CharacterProfileTest.uneAttaqueImpossible_estRefuseeAvecSaRaison`, `laSarbacaneGuidee_infligeDesDegatsFixes`
- [pass] Task 3 — `profile().create(...)` rend une créature aux valeurs de la fiche, PV actuels compris ; fiche à 0 PV max refusée — `laCreatureDUneFiche_aSesValeurs_etUneAttaqueParMode`, `unPJBlesse_gardeSesPVActuels`, `uneFicheAZeroPVMax_neSePosePas`
- [pass] Task 4 — `CatalogEntry.character` crée la créature du profil sous le nom donné ; `mvn -q test -pl dnd-engine` passe — `uneEntreeDePersonnage_creeSaCopieSousLeNomDonne` ; surefire : tests=99 failures=0 errors=0

## Phase 2 · Fiche : attaques structurées de bout en bout

- Date: 2026-11-12
- Validated on: c97600b

- [pass] Task 1 — `mvn -q test -pl dnd-server` passe ; attaque invalide 400 avec la raison ; fiche d'avant 409 — `mvn -q test -pl dnd-engine,dnd-server` exit 0 (`uneAttaqueSansAllongeNiPortee_estRefuseeEn400_avecSaRaison`, `uneFicheDAvantLesAttaquesStructurees_estIllisibleEn409`) ; API 8081 : `PUT …/dnd/sheets/{id}` Arc court 1d6+2 PIERCING 24/96 → 200, relue à l'identique ; sans portée → 400 `{"message":"Arc court : une allonge ou une portée"}` ; `modules/dnd.json` d'avant (attaque en texte) → `GET` 409 « Fiches de personnage illisibles : modules/dnd.json est abîmé »
- [pass] Task 2 — `npm run build` passe ; dans le navigateur (5174), une attaque saisie s'enregistre et se relit, une invalide affiche le refus — `npm run build` exit 0 ; Chrome, Vite 5174 → 8081 : Classe Guerrier → attaques en champs (Épée à deux mains 2d6+2 tranchants · 1,5 m ; Javeline 1d6+2 perforants · 1,5 m ; lancer 9/36 m) ; « Ajouter une attaque », Arc court, allonge vidée, perforants, portée 24/96 → « Enregistrée », résumé « 1d6 perforants · 24/96 m » ; rechargement : les 4 attaques relues ; portées vidées → « Non enregistrée : Arc court : une allonge ou une portée », saisie gardée. `vtt-web/vite.config.ts` (modification locale du joueur) diffère du commit mais n'est pas lu par le Vite de test (`configFile: false`).
