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

## Phase 3 · Catalogue de campagne et pose d'un PJ

- Date: 2026-11-12
- Validated on: 48de503

- [pass] Task 1 — `mvn -q test -pl dnd-server` passe ; la liste suit la corbeille ; la copie suit la fiche du moment — `mvn -q verify` exit 0 ; `SheetCharactersTest` (liste par nom sans la fiche supprimée, copie au titre renommé et aux valeurs du moment, fiche vierge refusée, document abîmé : liste vide et copie 409)
- [pass] Task 2 — `GET …/catalog` rend les PJ seulement si D&D est actif ; `/api/catalog` rend 404 — `PlayerCharacterApiTest.leCatalogue_…`, `sansLeModuleDnd_…` (catalogue sans Aria, pose 404) ; serveur 8081 : `GET …/catalog` → `[Aria CHARACTER, gobelin, squelette, loup]`, `GET /api/catalog` → 404
- [pass] Task 3 — Après retouche, renommage et suppression de la fiche, la reprise et le rejeu montrent le PJ tel que posé — `PlayerCharacterApiTest.retoucherRenommerSupprimerLaFiche_…` (CA d'origine et nom « Aria » sur la créature, sur une `SceneSession` rejouée depuis la partie gardée et au dernier pas de `…/combats/{id}/replay`) ; `JsonFileGameStoreTest` aller-retour d'un `place` avec profil ; `ResumeGameTest.unePartieDAvantQuiPoseUnPretire_sansSaCopie_estMiseDeCote`
- [pass] Task 4 — `mvn -q verify` passe ; le catalogue SRD n'a plus que des monstres — `mvn -q verify` exit 0 (vtt-server : 281 tests puis 282, 0 échec) ; `SrdCatalogTest.leCatalogueSrdNeProposeQueTroisMonstres_…` ; `uneCarteEnregistree_…` (rechargée : CA retouchée de la fiche ; fiche supprimée : 409, carte inchangée) ; `uneFicheAZeroPVMax_neSePosePas` (400 avec la raison)
- [pass] Task 5 — `npm run build` passe ; dans le navigateur (5174), le PJ se glisse sur la carte, entre en combat, se déplace et attaque — `npm run build` exit 0 ; Chrome, Vite 5174 → 8081 : nouvelle carte 30 × 20 m ; tiroir « Catalogue » : Personnages « Aria », Monstres Gobelin, Squelette, Loup ; Aria glissée sur la carte (Personnage, PV 11/11, CA 17), Gobelin à côté ; « Lancer le combat » → « Initiative : Aria 12 · Gobelin guerrier 9 », tour d'Aria, vitesse 9 m ; Aria glissée d'une case → « reste 7,3 m » ; Épée à deux mains sur le Gobelin à 2,6 m → « hors d'allonge (1,5 m) » ; Arc court → « Raté, 2 contre CA 15 » au journal
