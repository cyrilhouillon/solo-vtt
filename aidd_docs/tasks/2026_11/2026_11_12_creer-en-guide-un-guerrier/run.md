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

## Phase 3 · Section « Création guidée » de la fiche

- Date: 2026-11-12
- Validated on: d798345 (deux corrections après relecture du code de l'agent : construction de départ tirée du serveur, `ClassOptionsDto.defaultBuild`, cc844bb ; fiche figée pendant un choix pour ne perdre aucune frappe, d798345)

- [pass] Task 1 — `npm run build` passe ; les types suivent les DTO du serveur — `npm run build` : vue-tsc puis « ✓ built in 1.26s » ; `GET …/dnd/classes` sur 8081 rend `defaultBuild` (FIGHTER, standard suggéré, DEFENSE, FIGHTER_A, GREATSWORD/FLAIL/JAVELIN), lu tel quel par le client.
- [pass] Task 2 — Choisir Guerrier, puis chaque choix, remplit la fiche dans le navigateur ; un achat au-delà de 27 est refusé avant l'envoi — Chrome, Vite 5174 → 8081 (scratchpad), campagne « Guerrier guide », fiche Aria ; captures d'écran indisponibles (CDP en délai dépassé), vérifié par le DOM et le journal réseau : Classe → Guerrier : CA 17, PV 11/11, init +2, Épée à deux mains 4 2d6+2, Fléau d’armes 4 1d8+2, Javeline 4 1d6+2 « 1,5 m ; lancer 9/36 m », « Enregistrée » ; Athlétisme et Perception cochés : Athlétisme +4, Perception +2, Histoire éteinte ; méthode Achat de points puis Dex 15 : « Points restants -2 / 27 », alerte « Achat de points : 29 points dépensés, 27 au plus », Appliquer éteint, aucun PUT envoyé ; méthode Dés puis « Lancer les dés » : POST ability-rolls 200 puis PUT build 200, six jets avec le dé écarté barré, totaux 15/12/10/15/8/15 rangés For 15 Dex 15 Con 15 Cha 12 Sag 10 Int 8, bouton de lancer disparu ; méthode Standard puis Con → 8 : échange avec Int (Con 8, Int 13), PV max 9 ; Bottes : Arc long à la place de l’Épée à deux mains, Capacités « Bottes d’arme : Arc long (Ralentissement), Fléau d’armes (Sape), Javeline (Ralentissement) » ; Style Archerie : CA 14, Arc long +6 ; Classe « — » : le reste de la section se cache.
- [pass] Task 3 — Une frappe suivie d'un choix donne deux envois dans l'ordre et aucun troisième ; recharger l'onglet redonne la même fiche — CA tapée 20 puis Équipement B 300 ms après : journal réseau `PUT …/sheets/{id}` 200 puis `PUT …/build` 200, rien d'autre ; CA 15 (cuir clouté 12 + Dex 2 + Défense 1), Cimeterre, Épée courte, Arc long ; rechargement : classe FIGHTER, méthode ROLLED, 6 jets barrés, Athlétisme et Perception cochés, équipement B, CA 15, pas de bouton de lancer ; `modules/dnd.json` abîmé puis choix Archerie : alerte « Fiches de personnage illisibles : modules/dnd.json est abîmé », CA et attaques inchangées ; fichier restauré puis Archerie : appliqué.

## Phase 4 · Stories des capacités non jouées et mémoire

- Date: 2026-11-12
- Validated on: 0c6cd35

- [pass] Task 1 — Chaque capacité de niveau 1 du Guerrier est jouée, portée par la fiche, ou renvoie à une story — `dnd-engine/src/main/java/dnd/engine/rules/` ne contient que `DuelingStyle`, `Bless`, `RangeRules`, `ConditionRules`, `ShieldReaction` et les réactions : ni Second souffle, ni botte, ni style du Guerrier ; stories `utiliser-second-souffle-en-combat`, `appliquer-les-bottes-d-arme-en-combat` (les 8 bottes vérifiées dans les fiches d'armes du SRD), `jouer-les-styles-de-combat-du-guerrier`, rattachées à l'epic des personnages comme sa tâche (choix du joueur) ; tâche `lister-les-capacites-…` : tableau de 6 lignes, Défense « portée par la fiche ».
- [pass] Task 2 — La mémoire nomme chaque nouveau type, route et événement — `architecture.md`, `codebase-map.md`, `project-brief.md` mis à jour ; `Build`, `GuidedValues`, `AbilityMethod`, `AbilityRoll`, `SrdWeapon`, `SrdArmor`, `FightingStyle`, `CharacterClass`, `StartingEquipment`, `SheetState`, `SheetBuilt`, `AbilitiesRolled`, `ClassOptionsDto`, `defaultBuild`, `suggested`, `GuidedCreation`, `ability-rolls` tous retrouvés dans `dnd-engine`, `dnd-server`, `vtt-web/modules/dnd` ; story au statut `done`.

## Final

- Date: 2026-11-12
- Validated on: 0c6cd35

- [pass] `mvn -q verify` — exit 0 ; surefire : dnd-engine 91, dnd-server 17, mythic-engine 65, mythic-server 43, oracle-server 10, vtt-engine 27, vtt-server 275 tests, 0 échec, 0 erreur.
- [pass] `npm run build` dans `vtt-web/` — vue-tsc puis Vite : ✓ built in 1.14s.
