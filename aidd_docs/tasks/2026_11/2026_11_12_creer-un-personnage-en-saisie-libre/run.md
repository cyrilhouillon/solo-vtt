# Run

## Phase 2 · Fiche de personnage à événements dans `dnd-engine`

- Date: 2026-11-12
- Validated on: e16e0a6

- [pass] Task 1 — Les dérivés suivent les formules du SRD ; toute valeur hors bornes est refusée avec sa raison — `mvn -q test -pl dnd-engine` : `CharacterSheetTest` Tests run: 6, Failures: 0 (`lesDerives_suiventLesFormulesDuSrd` : mod. Dex +2, sauvegarde Dex +4, Discrétion en expertise +6, Perception +3, Perception passive 13 ; `unModificateur_arrondiALInferieur` : 9 → −1, 1 → −5 ; `uneValeurHorsBornes_estRefuseeAvecSaRaison` : « Dextérité entre 1 et 30 : 31 », « Niveau entre 1 et 20 : 21 », « Valeur manquante : Dextérité »).
- [pass] Task 2 — Plier `SheetCreated` puis des `SheetEdited` donne les valeurs du dernier ; une histoire sans `SheetCreated` est refusée ; `dnd-engine` sans dépendance runtime — `laDerniereSaisie_estLEtatDeLaFiche`, `uneHistoireSansCreation_ouAvecDeuxCreations_estRefusee` verts ; module entier : run 74, fail 0, err 0 ; `mvn dependency:tree -pl dnd-engine -Dscope=runtime` : seule `solovtt:vtt-engine`.
