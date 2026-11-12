# Run

## Phase 1 · Espèces et choix d'espèce dans `dnd-engine`

- Date: 2026-11-12
- Validated on: bc34385

- [pass] Task 1 — Chaque espèce a la taille, la vitesse, la vision et les traits de sa note SRD ; chaque option son texte et son type de dégâts — `SpeciesChoiceTest` (`lesNeufEspeces_ontLeursOptionsDuSrd`, `lesDragonsDonnentLeurTypeDeDegats`, `uneEspeceDUneSeuleTaille_laPrendDOffice`, `laVitesse_estCelleDeLEspece_ouDeSonOption`, `lesTraits_disentChaqueSousChoix…`) ; `mvn test -pl dnd-engine` : `Tests run: 116, Failures: 0, Errors: 0`
- [pass] Task 2 — Chaque choix impossible du Test Scope est refusé avec sa raison ; une espèce seule est valide — `SpeciesChoiceTest.unSousChoixImpossible_estRefuseAvecLaRaison` (Drow pour un Nain, taille P pour un Nain, Acrobaties hors Sens aiguisés, incantation Force, compétence pour un Halfelin), `uneCompetenceDejaMaitriseeParLaClasse_estRefusee`, `uneEspeceSeule_estValide`, `BuildTest.uneEspeceImpossible_refuseLaConstruction`
- [pass] Task 3 — Nain donne PV 12 ; Elfe sylvestre vitesse 10,5 et sa compétence ; changer d'espèce réécrit vitesse, PV et traits et garde une retouche dont la valeur dérivée ne change pas ; sans espèce, la fiche garde espèce et vitesse saisies — `GuidedValuesTest` (`unNain_…`, `unElfeSylvestre_…`, `unGoliathSansAscendance_…`, `passerDeNainAHautElfe_gardeLaVitesseRetouchee_…`, `passerDElfeSylvestreANain_…`, `uneConstructionSansEspece_…`) ; suite complète `mvn test` : BUILD SUCCESS (282 tests serveur, 0 échec)

## Phase 2 · Fiche, construction et options d'espèce au serveur

- Date: 2026-11-12
- Validated on: 40c7b0a

- [pass] Task 1 — Une construction avec espèce s'écrit et se relit à l'identique ; un `dnd.json` d'avant se lit sans 409 — `SheetResourceTest.uneConstructionAvecEspece_remplitLaFiche_etSeRelitALIdentique` (Nain : taille M, vitesse 9, PV 12, traits, `"species":{"species":"DWARF"` dans `dnd.json`, relue égale), `unDocumentDAvantLEspece_seRelit_etPrendUneEspece` (`built` sans `species`, `edited` sans `size` ni `speciesTraits` : lue, textes vides ; Orc appliqué, vitesse 10,5 → 9), `uneOptionDUneAutreEspece_estRefuseeEn400_sansRienEcrire` ; `mvn test -pl dnd-server` : `Tests run: 29, Failures: 0, Errors: 0`
- [pass] Task 2 — `GET …/dnd/species` rend les 9 espèces et leurs options ; campagne inconnue 404 — `SheetResourceTest.lesEspeces_disentLeursSousChoix` (9 espèces, Elfe : 3 lignages, Sens aiguisés, incantation ; Humain : 2 tailles, 18 compétences ; `NotFoundException` pour une campagne inconnue) ; suite complète `mvn test` : BUILD SUCCESS

## Phase 3 · Choix de l'espèce sur la fiche

- Date: 2026-11-12
- Validated on: 1d77211

- [pass] Task 1 — `npm run build` passe ; les types suivent les DTO du serveur — `npm run build` dans `vtt-web/` : `✓ built`, sans erreur `vue-tsc`
- [pass] Task 2 — Choisir chaque espèce du Test Scope remplit la fiche dans le navigateur ; une compétence déjà maîtrisée n'est pas proposable — Chrome, 5174 → 8081 (campagne neuve « Test especes », Aria construite en Guerrier, Athlétisme et Perception). Nain : Espèce Nain, Taille « M (moyenne) », Vitesse 9, PV 12 / 12, quatre traits. Elfe, Elfe sylvestre, Sagesse, Intuition : Vitesse 10.5, PV 11, Intuition en maîtrise sur la fiche, case Intuition des compétences de classe éteinte, option Perception éteinte dans Sens aiguisés. Vitesse retouchée à 12 puis lignage Haut-elfe : vitesse 9 à l'écran et au serveur ; `dnd.json` montre `edited` (12) puis `built` (9), sans envoi en trop. Perception forcée dans Sens aiguisés : « Perception déjà maîtrisée par la classe » affiché, liste remise à Intuition. Rechargement : espèce, lignage, incantation, compétence et valeurs reviennent ; « — » n'est plus proposé. Brin, espèce saisie « Elfe des bois » et vitesse 10.5, puis Classe → Guerrier : espèce et vitesse saisies gardées, « — (saisie libre) » proposé. Humain : Taille « à choisir » (liste M / P), Compétent avec 18 compétences, traits « à choisir »
- [pass] Task 3 — Taille et traits retouchés survivent au rechargement — Brin : Taille et Traits d'espèce saisis, enregistrés seuls (`GET` : `size` « M (moyenne) », `speciesTraits` saisi), relus identiques après rechargement ; serveurs 8081 et 5174 arrêtés ensuite, 8080 et 5173 intacts

## Phase 4 · Stories des traits non joués et mémoire

- Date: 2026-11-12
- Validated on: 4fa0005

- [pass] Task 1 — Chaque trait de niveau 1 des 9 espèces est joué, porté par la fiche, hors combat, ou renvoie à une story — `rules/` relu : résistance jouée dans l'attaque mais absente de `CreatureProfile`, aucun jet de sauvegarde au moteur (`Bless` : « non modélisés »), pas d'éclairage ; la tâche `lister-les-capacites-…` liste 26 lignes pour les 9 espèces et leurs options, chacune avec une story (six nouvelles, ordres 14 à 19 ; sorts d'espèce → lanceurs de sorts ; Polyvalent → historique) ou la raison (portée par la fiche, hors combat, niveau 5)
- [pass] Task 2 — La mémoire nomme chaque nouveau type, champ et route — `architecture.md` : `SpeciesChoice`, `Species`, `SpeciesOption`, `CreatureSize`, `size`, `speciesTraits`, `GET …/dnd/species`, refus d'une compétence déjà maîtrisée ; `codebase-map.md` : les quatre types ; `project-brief.md` : choix de l'espèce ; story au statut `done`

## Final

- Date: 2026-11-12
- Validated on: 4fa0005

- [pass] `mvn verify` — exit 0, `BUILD SUCCESS` ; `Tests run: 116, Failures: 0` (dnd-engine), `29` (dnd-server), `282` (vtt-server), aucun échec
- [pass] `npm run build` dans `vtt-web/` — `✓ built`, sans erreur `vue-tsc` (arbre propre, hors `vtt-web/vite.config.ts` et `.mcp.json`, modifications du joueur antérieures à la branche, non committées)
