# Run

## Phase 1 · Construction du Guerrier dans `dnd-engine`

- Date: 2026-11-12
- Validated on: cb0554e

- [pass] Task 1 — Chaque arme et armure du Guerrier a les valeurs de sa fiche SRD (dégâts, portée, CA, botte) — `mvn test -pl dnd-engine` : Tests run: 86, Failures: 0 ; `GuidedValuesTest.unGuerrierA_Defense_remplitLaFiche` : Épée à deux mains 2d6 tranchants (Écorchure), Fléau d’armes 1d8 contondants (Sape), Javeline 1d6 perforants 9/36 m (Ralentissement), Cotte de mailles 16 ; `unGuerrierB_Archerie_aCA14_etLArcLongA6` : cuir clouté 12 + Dex, Arc long 1d8 45/180 m.
- [pass] Task 2 — Une construction impossible est refusée avec sa raison ; `Build.fighter()` est valide — `BuildTest` 8/8 : standard hors permutation, achat à plus de 27 points (avec le coût), valeur hors 8–15, dés sans jets ou valeurs hors totaux, trois compétences ou hors liste, Bottes en double ou quatre, refusés ; `leGuerrierSuggere_estValide_avecAthletismeEtPerception` ; dés scriptés 6-6-6-1 ×6 → six jets de 18.
- [pass] Task 3 — Guerrier A Défense : CA 17, PV 11 et les trois attaques attendues ; passer à B recalcule CA et attaques et garde les retouches qui n'en dépendent pas — `GuidedValuesTest` 4/4 : CA 17, PV 11/11, init +2, attaques +4 2d6+2 / 1d8+2 / 1d6+2 « 1,5 m ; lancer 9/36 m » ; A → B avec CA retouchée 20 et PV max 30 : CA 15, PV max 30 gardé ; Con ↔ Int : PV 9, expertise de Perception gardée.

## Phase 2 · Histoire de la fiche et routes de construction

- Date: 2026-11-12
- Validated on: 2b096e1 (tâches 2 et 3 dans un seul commit : les routes émettent des événements que seule la persistance sait écrire)

- [pass] Task 1 — Rejouer une histoire créée, construite, saisie redonne valeurs, construction et jets — `mvn test -pl dnd-engine,dnd-server -am` : `SheetHistoryTest` Tests run: 5, Failures: 0 ; `CharacterSheetTest` 6/6 ; second `AbilitiesRolled` refusé.
- [pass] Task 2 — Une construction valide remplit la fiche, une invalide rend 400 sans rien écrire ; les dés ne se lancent qu'une fois — `SheetResourceTest` Tests run: 17, Failures: 0 (`uneConstruction_remplitLaFiche_etSeRelitALIdentique`, `uneRetouche_survitAUnChoixDontElleNeDependPas_pasAUnChoixDontElleDepend`, `lesDes_seLancentUneFois_puisDonnentLesValeurs`, `uneConstructionImpossible_estRefuseeAvecSaRaison_sansRienEcrire`, `construireOuLancerSurUnePageQuiNEstPasUneFiche_rend404`, `uneSaisieApresUneConstruction_gardeLaConstructionEtLesJets`) ; API réelle 8081 (`mvn install`, `quarkus:dev` sur le scratchpad) : `GET …/dnd/classes` 200 Guerrier, 2 compétences, 38 armes, 4 styles, A et B, budget 27 ; `PUT …/build` Guerrier A Défense 200 → CA 17, PV max 11, Épée à deux mains +4 2d6+2, Fléau d’armes +4 1d8+2, Javeline +4 1d6+2 ; trois compétences → 400 « Guerrier : 2 compétences au plus » ; `POST …/ability-rolls` 200 six totaux ; second → 409 « Les dés de caractéristique de cette fiche sont déjà lancés » ; page inconnue → 404.
- [pass] Task 3 — Une fiche construite se relit à l'identique après un nouveau `SheetResource` ; un `dnd.json` d'avant se relit — `uneConstruction_remplitLaFiche_etSeRelitALIdentique`, `unDocumentDAvantLaConstruction_seRelitSansConstruction` (JSON `created` + `edited` écrit à la main), `uneConstructionOuDesJetsIllisibles_sontRefusesEn409_sansEtreEcrases` ; `mvn -q verify` exit 0 (vtt-server 275, dnd-server 17, dnd-engine 91 tests, 0 échec).
