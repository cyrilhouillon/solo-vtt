# Run

## Phase 2 · Fiche de personnage à événements dans `dnd-engine`

- Date: 2026-11-12
- Validated on: e16e0a6

- [pass] Task 1 — Les dérivés suivent les formules du SRD ; toute valeur hors bornes est refusée avec sa raison — `mvn -q test -pl dnd-engine` : `CharacterSheetTest` Tests run: 6, Failures: 0 (`lesDerives_suiventLesFormulesDuSrd` : mod. Dex +2, sauvegarde Dex +4, Discrétion en expertise +6, Perception +3, Perception passive 13 ; `unModificateur_arrondiALInferieur` : 9 → −1, 1 → −5 ; `uneValeurHorsBornes_estRefuseeAvecSaRaison` : « Dextérité entre 1 et 30 : 31 », « Niveau entre 1 et 20 : 21 », « Valeur manquante : Dextérité »).
- [pass] Task 2 — Plier `SheetCreated` puis des `SheetEdited` donne les valeurs du dernier ; une histoire sans `SheetCreated` est refusée ; `dnd-engine` sans dépendance runtime — `laDerniereSaisie_estLEtatDeLaFiche`, `uneHistoireSansCreation_ouAvecDeuxCreations_estRefusee` verts ; module entier : run 74, fail 0, err 0 ; `mvn dependency:tree -pl dnd-engine -Dscope=runtime` : seule `solovtt:vtt-engine`.

## Phase 3 · Module serveur `dnd-server`

- Date: 2026-11-12
- Validated on: 5216afb

- [pass] Task 1 — `mvn -q verify` passe ; `GET /api/modules` du serveur 8081 liste `dnd` ; aucun `dnd.server` dans `vtt-server/src` — `mvn -q verify` : run 500, fail 0, err 0 (après mise à jour de `CampaignApiTest`, qui listait les modules installés en dur) ; `GET http://localhost:8081/api/modules` → 200 `["dnd","mythic","oracle"]` ; `grep -rln "dnd.server|modules/dnd" vtt-server/src vtt-web/src` : aucun résultat.
- [pass] Task 2 — Une fiche se crée sous « Personnages joueurs », se lit vide, se modifie et se relit avec ses dérivés, même après redémarrage ; une valeur hors bornes rend 400 sans rien changer ; une page qui n'est pas une fiche rend 404 — `SheetResourceTest` 6/6 ; sur 8081 : `POST …/dnd/sections` 200 `{"playersPageId":…}` ; `POST …/dnd/sheets {"title":"Aria"}` 200, `GET …/notebook` montre Aria avec `"entryOf":"dnd"` sous la racine ; `GET …/dnd/sheets/{id}` 200 fiche vide ; `PUT` Dex 31 → 400 `{"message":"Dextérité entre 1 et 30 : 31"}` ; `GET …/sheets/inconnue` → 404 « Fiche inconnue : inconnue » ; campagne créée avec `["oracle"]` : `POST …/dnd/sections` → 404 « Module inactif dans cette campagne : dnd » ; `modules/dnd.json` abîmé → 409 « Fiches de personnage illisibles : modules/dnd.json est abîmé », fichier intact, 200 après réparation ; `PUT` Roublard (Dex 14, Discrétion en expertise, Sag 12), arrêt de 8081, relance, `GET` → `"className":"Roublard"`, Discrétion 6, Perception passive 11.
