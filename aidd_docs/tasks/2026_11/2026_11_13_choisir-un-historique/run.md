# Run

## Phase 1 · Historiques et dons d'origine dans `dnd-engine`

- Date: 2026-11-13
- Validated on: 7dd2b4d

- [pass] Task 1 — Chaque historique a les caractéristiques, le don, les compétences, l'outil et l'équipement de sa note SRD ; chaque don son texte — `BackgroundChoiceTest.lesQuatreHistoriques_ontLeursDonneesDuSrd`, `OriginFeatChoiceTest` (textes de Doué et d'Initié) ; `mvn test` : dnd-engine `Tests run: 133, Failures: 0`
- [pass] Task 2 — Chaque choix impossible du Test Scope est refusé avec sa raison ; un historique avec augmentations et équipement est valide — `desAugmentationsImpossibles_…` (hors historique, +2/+2, +3, +1/+1, aucune), `desSousChoixImpossibles_…` (boîte pour l'Acolyte, incantation Force, incantation pour Vigilant, sans équipement, sans historique, Polyvalent pour un Nain), `uneCompetenceDejaDonneeParLHistorique_…` (classe, espèce, Doué), `OriginFeatChoiceTest.unSousChoixImpossible_…` (4 compétences, liste pour Vigilant). Écarts au Test Scope : « valeur au-dessus de 20 » impossible (aucune méthode ne dépasse 18, +2 donne 20 au plus), vérification retirée ; Doué accepte moins de 3 compétences (le reste « à choisir »), comme les compétences de classe, pour que chaque case cochée s'envoie
- [pass] Task 3 — Soldat For 17, Athlétisme, Arc court, Sauvagerie martiale ; Criminel initiative + 2 ; Soldat → Sage réécrit valeurs, compétences, attaques et dons et garde la CA retouchée ; sans historique la fiche garde historique et dons saisis ; retrait refusé — `unSoldat_…`, `unCriminel_…`, `unAcolyteAvecLEquipementB_…`, `unHumainPolyvalent_…`, `passerDeSoldatASage_…`, `unHistoriqueSaisiALaMain_…`, `unHistoriqueChoisi_seChangeMaisNeSeRetirePas` ; `mvn test` racine : `BUILD SUCCESS` (vtt-server `Tests run: 282, Failures: 0`)

## Phase 2 · Fiche, construction et options d'historique au serveur

- Date: 2026-11-13
- Validated on: c843a58

- [pass] Task 1 — Une construction avec historique et Polyvalent s'écrit, se relit et se rend à l'identique ; une fiche d'avant se relit sans historique, `feats` vide ; un historique tronqué rend 409 — `SheetResourceTest.uneConstructionAvecHistorique_…` (Soldat, puis Humain Vigilant : initiative 4, relu à l'identique par une ressource neuve), `unDocumentDAvantLEspece_…` (+ `build.background` null, `feats` ""), `unHistoriqueGardeTronque_…` (sans historique, augmentations vides : 409, fichier intact), `uneConstructionAvecHistoriqueImpossible_…` (doublon, +3, Polyvalent d'un Nain, retrait : 400, rien d'écrit) ; `mvn test` racine `BUILD SUCCESS`. API réelle 8081 : `PUT …/dnd/sheets/{id}/build` Guerrier + Soldat +2 For/+1 Con, A, dés → 200, Historique « Soldat », For 17, attaques + Arc court, Lance, `feats` « Sauvagerie martiale… », `build.background` rendu ; même construction avec Athlétisme → 400 « Athlétisme déjà donnée par l’historique », `GET` de la fiche toujours « Soldat »
- [pass] Task 2 — `GET backgrounds` rend les 4 historiques et `GET origin-feats` les 4 dons, avec leurs sous-choix ; campagne inconnue 404 — `lesHistoriquesEtLesDons_disentLeursChoix` ; API réelle 8081 : `GET …/dnd/backgrounds` 200 `[ACOLYTE, CRIMINAL, SAGE, SOLDIER]`, boîtes du Soldat (`DICE` « dés »…) ; `GET …/dnd/origin-feats` 200 : Doué 3 compétences, Initié 3 listes ; `GET /api/campaigns/inconnue/dnd/backgrounds` 404
