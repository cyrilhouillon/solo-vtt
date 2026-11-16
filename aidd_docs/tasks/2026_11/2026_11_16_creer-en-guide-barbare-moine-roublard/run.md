# Run

## Phase 1 · Barbare, Moine, Roublard et outils dans `dnd-engine`

- Date: 2026-11-16
- Validated on: 319177f

- [pass] Task 1 — Chaque classe a le dé de vie, les sauvegardes, les compétences, les valeurs suggérées, les équipements et les maîtrises de sa note SRD ; `Tool` a les outils des notes `Équipement/Outils` ; les historiques gardent leur outil — `mvn test -pl vtt-engine,dnd-engine` : `Tests run: 153, Failures: 0` ; `GuidedValuesTest` (Barbare d12, For/Con ; Moine d8, For/Dex ; Roublard d8, Dex/Int ; équipements A, B, C) ; `BackgroundChoiceTest` (`Background.CRIMINAL.tool() == THIEVES_TOOLS`, « Outils : outils de voleur ») ; `Tool` : 17 artisan, 10 instruments, 6 autres, d'après `Compendio SRD/Français/Équipement`.
- [pass] Task 2 — Chaque refus du Test Scope a sa raison ; les constructions suggérées sont valides — `BuildTest` : `lesConstructionsSuggerees_sontValides`, Style (« Barbare : aucun Style de combat au niveau 1 », « Choisir un Style de combat »), Bottes (« Barbare : pas de Botte d'arme pour Arc long », « Roublard : … Hache à deux mains », Moine 0), Expertise (« Guerrier : aucune Expertise… », « 2 Expertises au plus », « Athlétisme n’est pas maîtrisée », historique acceptée), outil (« Roublard : aucun outil à choisir », « Moine : outils de voleur hors des outils au choix ») ; tous verts.
- [pass] Task 3 — Barbare, Moine, Roublard, options tout en po, Guerrier → Moine — `GuidedValuesTest` verts : Barbare PV 14, CA 13 (10 + Dex 1 + Con 2 ; le plan écrivait 15 par erreur de calcul), Dex 8 → CA 11 ; Moine CA 14, Lance et Dague 1d6 + 2, mains nues +4 1d6 + 2, For 15/Dex 13 → For retenue, « Outils : luth » et luth dans l'équipement, « à choisir » sans outil ; Roublard CA 13, Discrétion et Escamotage en expertise, outils de voleur une fois avec Criminel ; Roublard B « 100 po » CA 12, Guerrier C « 155 po » CA 12 ; Guerrier mains nues +4, 3 contondants ; Guerrier → Moine : Style retiré, CA 14, PV 9, sauvegardes For/Dex, Langues gardées. Libellés d'outils en minuscules (« luth », « outils de voleur »), comme l'outil d'historique d'avant. Suite complète `mvn test` : `BUILD SUCCESS` (vtt-server 292, dnd-server 39).

## Phase 2 · Construction et options de classe au serveur

- Date: 2026-11-16
- Validated on: d503fa7

- [pass] Task 1 — Chaque ligne du Test Scope rend le code et le corps attendus ; une construction d'avant sans `expertise` ni `tool` se relit — `mvn test -pl dnd-server` : `Tests run: 44, Failures: 0` (ressource appelée en JUnit, `ModuleData` en mémoire, comme les tests existants) : `lesOptionsDuBarbareDuMoineEtDuRoublard_disentLeursChoix` (4 classes ; Moine sans Style ni arme, 27 outils dont `LUTE`/« luth »/« Instruments de musique » ; Roublard `expertiseCount` 2, 19 armes dont Rapière, sans Hache à deux mains ; Barbare 28 armes de corps à corps, sans Arc long ; options « 75 po », « 100 po » ; `defaultBuild` suggéré) ; `unRoublardAvecExpertise_seConstruit_etSeRelitALIdentique` (expertise `[STEALTH, SLEIGHT_OF_HAND]`, Discrétion 6, CA 13, relecture identique) ; `unMoineSansStyle_avecSonLuth_seConstruit_etSeRelit` (Style `null`, outil `LUTE`, CA 14, Lance, Dague, mains nues, « Outils : luth ») ; `unDocumentDAvantLEspece_seRelit_etPrendUneEspece` relit une construction gardée sans `expertise` ni `tool` (liste vide, outil `null`) ; `uneConstructionDeGuerrierSansStyle_estRefuseeEn409` (409, fichier intact) ; `unChoixImpossiblePourLaClasse_estRefuseAvecSaRaison` (« Barbare : aucun Style de combat au niveau 1 », « Roublard : 2 Expertises au plus », « Roublard : aucun outil à choisir », rien d'écrit). L'outil d'historique reste servi par son libellé (`lesHistoriquesEtLesDons_disentLeursChoix` vert : « matériel de calligraphe »). Suite complète `mvn test` : `BUILD SUCCESS`.

## Phase 3 · Choix des trois classes sur la fiche

- Date: 2026-11-16
- Validated on: f1dcf44

- [pass] Task 1 — `npm run build` passe — `npm run build` dans `vtt-web/` : exit 0 (`vue-tsc` puis build Vite).
- [pass] Task 2 — Chaque ligne du Test Scope s'observe sur 5174 ; aucun refus du serveur — Chrome sur Vite 5174 → serveur de test 8081 (campagnes, images, modules dans le scratchpad), campagne « Test classes » avec D&D, fiche « Kael » : Classe → Barbare : pas de Style, Bottes (2) parmi 28 armes de corps à corps (aucun arc), attaques Hache à deux mains, Hachette, Attaque à mains nues, CA 13, PV 14 ; Athlétisme et Perception cochées, aucun refus. Espèce Halfelin, historique Criminel, puis Classe → Moine : espèce et historique gardés ; ni Style ni Bottes, liste Outil groupée (« Outils d’artisan » 17, « Instruments de musique » 10) ; CA 15 (Dex 16 avec +1 du Criminel, Sag 14), Lance, Dague et mains nues +5 1d6 + 3 ; Outil → luth : « Outils : luth, outils de voleur », luth dans l'Équipement ; Équipement B : « 50 po », Lance retirée (la Dague du Criminel reste), outil gardé. Classe → Roublard, quatre compétences : partie Expertise (2) ne propose que les six compétences maîtrisées (dont Discrétion et Escamotage du Criminel) ; Discrétion et Athlétisme : la troisième case s'éteint, Discrétion en EXPERTISE à +7 (Dex 3 + 2 × 2), CA 14 (cuir 11 + 3) ; décocher Athlétisme dans Compétences : Expertise réduite à Discrétion dans le même envoi, aucun refus. Rechargement : Roublard, trois compétences, Discrétion cochée en Expertise. Seconde fiche « Aria » → Guerrier : Style affiché, Bottes (3), trois équipements (A, B, C), ni Expertise ni Outil, CA 17, attaques + mains nues. Serveurs 8081 et 5174 arrêtés ; 8080 et 5173 non touchés. Écart vu, hors critère : le libellé de l'option « Équipement A » du Moine garde « outil … (à choisir) » après le choix de l'outil (texte servi par les options de classe), alors que la fiche écrit bien le luth.

## Phase 4 · Stories des capacités non jouées et mémoire

- Date: 2026-11-16
- Validated on: b482770

- [pass] Task 1 — Chaque capacité de niveau 1 du Barbare, du Moine et du Roublard figure dans la liste ; chaque « non » renvoie à une story existante — `lister-les-capacites-de-niveau-1-que-le-moteur-ne-joue-pas.md` : Rage, Défense sans armure, Bottes (Guerrier, Barbare, Roublard), Arts martiaux (dé et Attaques lestes ; action Bonus), Attaque à mains nues, Expertise, Attaque sournoise, Argot des voleurs, outils de Doué ; chaque lien `../stories/*.md` de la liste résolu sur disque (script : aucun « MISSING »). Stories créées : `entrer-en-rage-en-combat`, `porter-une-attaque-sournoise`, `frapper-a-mains-nues-en-action-bonus-avec-arts-martiaux`, `choisir-des-outils-avec-doue` (`proposed`, rangs 23 à 26), `tenir-l-inventaire-du-personnage` (`ready`, rang 8 ; les stories suivantes de l'epic décalées d'un rang) ; Bottes d'arme étendue au Barbare et au Roublard.
- [pass] Task 2 — La mémoire ne décrit plus la création guidée comme Guerrier seul ; la story est `done` — `architecture.md` (création guidée des quatre classes, aptitudes de `CharacterClass`, `Tool`, option tout en po, Expertise, CA et attaques depuis ce qui est porté, mains nues, DTO), `codebase-map.md` (`CharacterClass`, `Tool`), `project-brief.md` (Barbare, Moine, Roublard) ; `creer-en-guide-un-barbare-un-moine-ou-un-roublard.md` : `status: done`.

## Final

- Date: 2026-11-16
- Validated on: f1dcf44

- [pass] `mvn verify` — `BUILD SUCCESS` ; vtt-engine 27, dnd-engine 153, mythic-engine 65, mythic-server 43, oracle-server 10, dnd-server 44, vtt-server 292 tests, 0 échec (lancé sur b482770, qui ne diffère de f1dcf44 que par du Markdown).
- [pass] `npm run build` dans `vtt-web/` — exit 0.
- [pass] Parcours navigateur de la phase 3 (8081 / 5174) — voir la section Phase 3.
