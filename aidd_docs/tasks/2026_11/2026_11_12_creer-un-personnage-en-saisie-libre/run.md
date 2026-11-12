# Run

## Phase 1 · Entrées de module dans le Carnet

- Date: 2026-11-12
- Validated on: deb0bfb (la vue d'une entrée ne pouvait se voir qu'une fois le module D&D livré : phase 1 validée avec la phase 4, son code inchangé depuis 9bc010b)

- [pass] Task 1 — Une entrée de module se crée sous sa section, se renomme, se déplace, va à la corbeille et en revient, se trouve par son titre ; sous-page, contenu et parent hors section refusés en 400 ; elle survit à une relecture du disque — `NotebookStoreTest.enMemoire_lesEntreesDeModule_suiventLesReglesDuPort`, `JsonFileNotebookStoreTest.lesEntreesDeModule_suiventLesReglesDuPort_etSeRelisent`, `NotebookApiTest.uneEntreeDeModule_creeeParLePort_seMontreSeCherche_maisNAniTexteNiSousPage` (contenu 400 « Une entrée de module n'a pas de texte », sous-page 400 « Une entrée de module n'a pas de sous-page », déplacement sous l'entrée 400, renommage 204, corbeille puis restauration 204) ; `entreesDeCombat` inchangé et vert (message « Une entrée de combat n'a pas de texte ») ; `mvn -q verify` run 500, fail 0.
- [pass] Task 2 — Le build passe ; une entrée de module s'ouvre dans la vue de son module, ou dit que le module est inactif ; elle n'offre pas « Nouvelle sous-page » et ne reçoit pas de page — `npm run build` ✓ built ; navigateur 5174 → 8081 (Chrome, 1568×778, thème clair) : la fiche Aria s'ouvre dans sa vue sous son titre ; menu de la ligne : « Renommer », « Déplacer sous… », « Supprimer… » seulement ; dialogue « Déplacer Session 1 » : « Aria la Rouge » éteint, « Personnages joueurs » possible ; D&D désactivé (`PUT …/modules` [mythic, oracle]) puis rechargé : « Module dnd inactif dans cette campagne. », pages citantes toujours là.

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

## Phase 4 · Module client `vtt-web/modules/dnd`

- Date: 2026-11-12
- Validated on: deb0bfb

- [pass] Task 1 — Dans une campagne à D&D actif, la section « Personnages joueurs » apparaît, et « Nouveau personnage » crée une fiche ouverte en onglet ; aucun `modules/dnd` dans `vtt-web/src` — accueil 5174 : « Personnages joueurs » coché par défaut ; campagne « Validation fiche » créée : arbre avec la section et sa racine, panneau « Personnages joueurs » ; nom « Aria » + Entrée : entrée « Aria » sous la racine, onglet « Aria » actif, adresse `/carnet/<id de la fiche>`, fiche vide (niveau 1, valeurs 10, maîtrise 2) ; `grep -rln "dnd.server|modules/dnd" vtt-server/src vtt-web/src` : aucun résultat.
- [pass] Task 2 — Chaque rubrique se saisit et se garde après rechargement et redémarrage ; les dérivés suivent la saisie ; un refus s'affiche sans perte de saisie ; la fiche se lie, se cherche et se renomme comme une page ; désactiver puis réactiver D&D la rend intacte — Dex 14 : modificateur +2, Acrobaties +2 à l'écran ; Discrétion en expertise : +6 ; CA 15, attaque « Rapière » +4 1d8+2, Capacités « Attaque sournoise » : « Enregistrée », `GET …/dnd/sheets/{id}` rend ces valeurs et Discrétion 6 ; Dex 31 : « Non enregistrée : Dextérité entre 1 et 30 : 31 », 31 reste à l'écran, serveur à 14 ; Dex 16 : « Enregistrée » ; rechargement : Dex 16, Rapière, Attaque sournoise, Discrétion +7 ; arrêt et relance de 8081 puis rechargement : mêmes valeurs ; « aria » dans Chercher : 1 page, Aria ; `[[Aria` + Entrée dans « Session 1 » : lien, Ctrl+clic ouvre la fiche, « Pages qui renvoient ici : Session 1 » ; Renommer « Aria la Rouge » depuis l'arbre : titre, onglet et ligne suivent ; D&D désactivé : section et panneau disparus ; réactivé : section, fiche (Dex 16, Rapière) et une seule racine « Personnages joueurs ». Défaut vu et corrigé pendant la validation : champs des attaques trop étroits (b53c3bc, deb0bfb), revus à l'écran après correction.
