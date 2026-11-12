# Run

## Phase 1 · Historiques et dons d'origine dans `dnd-engine`

- Date: 2026-11-13
- Validated on: 7dd2b4d

- [pass] Task 1 — Chaque historique a les caractéristiques, le don, les compétences, l'outil et l'équipement de sa note SRD ; chaque don son texte — `BackgroundChoiceTest.lesQuatreHistoriques_ontLeursDonneesDuSrd`, `OriginFeatChoiceTest` (textes de Doué et d'Initié) ; `mvn test` : dnd-engine `Tests run: 133, Failures: 0`
- [pass] Task 2 — Chaque choix impossible du Test Scope est refusé avec sa raison ; un historique avec augmentations et équipement est valide — `desAugmentationsImpossibles_…` (hors historique, +2/+2, +3, +1/+1, aucune), `desSousChoixImpossibles_…` (boîte pour l'Acolyte, incantation Force, incantation pour Vigilant, sans équipement, sans historique, Polyvalent pour un Nain), `uneCompetenceDejaDonneeParLHistorique_…` (classe, espèce, Doué), `OriginFeatChoiceTest.unSousChoixImpossible_…` (4 compétences, liste pour Vigilant). Écarts au Test Scope : « valeur au-dessus de 20 » impossible (aucune méthode ne dépasse 18, +2 donne 20 au plus), vérification retirée ; Doué accepte moins de 3 compétences (le reste « à choisir »), comme les compétences de classe, pour que chaque case cochée s'envoie
- [pass] Task 3 — Soldat For 17, Athlétisme, Arc court, Sauvagerie martiale ; Criminel initiative + 2 ; Soldat → Sage réécrit valeurs, compétences, attaques et dons et garde la CA retouchée ; sans historique la fiche garde historique et dons saisis ; retrait refusé — `unSoldat_…`, `unCriminel_…`, `unAcolyteAvecLEquipementB_…`, `unHumainPolyvalent_…`, `passerDeSoldatASage_…`, `unHistoriqueSaisiALaMain_…`, `unHistoriqueChoisi_seChangeMaisNeSeRetirePas` ; `mvn test` racine : `BUILD SUCCESS` (vtt-server `Tests run: 282, Failures: 0`)
