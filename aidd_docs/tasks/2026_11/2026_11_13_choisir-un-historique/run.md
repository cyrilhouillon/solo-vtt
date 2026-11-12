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

## Phase 3 · Choix de l'historique et de Polyvalent sur la fiche

- Date: 2026-11-13
- Validated on: e8ee101

- [pass] Task 1 — `npm run build` passe ; les options d'historique et de don arrivent au montage — `npm run build` (vue-tsc + vite) : `✓ built in 940ms`, aucune erreur ; dans le navigateur (Chrome, 5174 → 8081), la liste Historique propose « — (saisie libre) », Acolyte, Criminel, Sage, Soldat et Polyvalent les 4 dons, sans « illisibles » au bandeau
- [pass] Task 2 — Choisir Soldat remplit la fiche et éteint Athlétisme et Intimidation dans la classe ; augmentations, boîte, équipement mettent la fiche à jour ; un refus s'affiche et remet les choix — fiche « Brune » (Guerrier Athlétisme + Perception) : Historique → Soldat : Athlétisme décochée et éteinte, Intimidation éteinte, +1 aux trois, Dons « Sauvagerie martiale — … », attaques + Arc court, Lance ; « +2 et +1 » : +2 Force / +1 Dextérité, Force 17 ; +1 → Constitution : Con 14 ; boîte → dés : Maîtrises « …\nOutils : boîte de jeux (dés) » ; équipement B : Équipement « …, 4 po\n50 po », Arc court et Lance retirés. Refus : envoi forcé (fetch patché, augmentation Sagesse +3) → bandeau « Acolyte : augmenter une caractéristique de 2 et une autre de 1, ou les trois de 1 », Augmentations remises à « +1 aux trois », Sagesse 11 inchangée. Fiche « Corin » saisie à la main (Historique « Marin », Dons « Chanceux ») puis Classe → Guerrier : « Marin » et « Chanceux » restent, « — (saisie libre) » proposé. Rechargement de l'onglet : Acolyte, +1 aux trois, Humain, Doué, Dons reviennent. Écart au Test Scope : le refus « au-dessus de 20 » n'existe plus (phase 1), remplacé par un refus d'augmentation
- [pass] Task 3 — Un Humain choisit Doué et trois compétences, ou Initié à la magie, sa liste et son incantation ; la fiche les montre — Espèce → Humain, Polyvalent → Doué : Athlétisme, Intimidation, Perception éteintes ; Histoire, Médecine, Religion cochées → toutes les autres éteintes (3 au plus), Dons + « Doué : maîtrise de Histoire, Médecine, Religion », Traits « Polyvalent : Doué » ; Compétent éteint ces six compétences ; puis Historique → Acolyte : Religion (donnée par l'Acolyte) retirée de Doué (« 1 à choisir »), Dons « Initié à la magie (Clerc) — incantation à choisir … », Force 15, Sagesse 11, Charisme 13 ; Incantation → Sagesse : Dons « … incantation Sagesse … ». Initié à la magie par Polyvalent (liste, incantation) couvert côté serveur (`OriginFeatChoiceTest`, `BackgroundChoiceTest.unHumainPolyvalent_…`), non cliqué dans le navigateur
- [pass] Task 4 — « Dons » s'affiche, se retouche et survit au rechargement — champ « Dons » après « Traits d'espèce », rempli par la construction, « Chanceux » saisi via `PUT` relu dans le champ, contenu identique après rechargement

## Phase 4 · Stories des dons non joués et mémoire

- Date: 2026-11-13
- Validated on: ec9ee79

- [pass] Task 1 — Chaque don d'origines non joué renvoie à une story existante du backlog — liste de la tâche : Sauvagerie martiale → `relancer-les-degats-avec-sauvagerie-martiale.md` (créée, ordre 20) ; Vigilant, Échange d'Initiative → `echanger-son-initiative-avec-vigilant.md` (créée, ordre 21) ; sorts d'Initié à la magie → `creer-en-guide-les-autres-lanceurs-de-sorts.md` (critère ajouté) ; Doué, Vigilant (initiative) et Polyvalent « portés par la fiche » ; les trois fichiers existent (`test -f` : ok)
- [pass] Task 2 — La mémoire nomme les nouvelles classes et routes ; les deux stories sont `done` — `architecture.md` (puce Historique : `BackgroundChoice`, `OriginFeatChoice`, `Build.abilities`, `feats`, `backgrounds`, `origin-feats`, refus) et `codebase-map.md` (`Background`, `BackgroundChoice`, `BackgroundEquipment`, `GamingSet`, `OriginFeat`, `OriginFeatChoice`, `SpellList`), `project-brief.md` (historique, don de l'Humain) ; `status: done` pour `choisir-un-historique-en-creation-guidee.md` et `creer-un-personnage-en-saisie-libre-dans-personnages-joueurs.md`

## Final

- Date: 2026-11-13
- Validated on: e8ee101 (dernier commit de code ; seuls des fichiers Markdown ont changé depuis)

- [pass] `mvn -q compile` — exit 0, aucune erreur
- [pass] `mvn verify` — `BUILD SUCCESS` ; dnd-engine `Tests run: 133, Failures: 0`, dnd-server `Tests run: 33, Failures: 0`, vtt-server `Tests run: 282, Failures: 0`, autres modules 27, 65, 43, 10 sans échec
- [pass] `npm run build` dans `vtt-web/` — vue-tsc et vite : `✓ built in 1.04s`, aucune erreur
