# Review: tenir l'inventaire du personnage

- Rounds: 4
- Started: 2026-11-16

## Round 1 · r-78cc

- Date: 2026-11-16
- By: Cyril Houillon
- Diff: `main...8d8cd9a`
- Axes: code, functional, relevancy
- Verdict: changes-requested
- Score: 9/9 met, 0 unmet, 0 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1 T1 — armures (CA, catégorie, Force, Discrétion) et maîtrise / formation des classes — `SrdArmor.java` (12 armures + bouclier conformes au tableau du plan), `CharacterClass.java:22-45, 213-224`, `InventoryTest.lesArmuresDuSrd_etLaFormationDesClasses` ; `mvn -q verify` exit 0 sur 333cb5e (run.md, Final ; depuis, Markdown seul)
- [x] P1 T2 — refus avec raison, inventaire de départ de chaque option — `Item.java:12-19`, `Inventory.java:17-20`, `StartingEquipment.java`, `Background.java`, `Build.startingInventory` ; `InventoryTest`, `GuidedValuesTest`
- [x] P1 T3 — CA et attaques de départ inchangées ; cas du Test Scope — `CombatStats.java`, `CharacterSheet.java:125-150` ; `CombatStatsTest`, `GuidedValuesTest`, `CharacterProfileTest`
- [x] P2 T1 — statuts, refus sans écriture, histoire sans CA ni attaques — `DndDtos.java` (`SheetValuesDto`, `DerivedDto`), `SheetResourceTest`, `SheetCharactersTest`, `PlayerCharacterApiTest`
- [x] P2 T2 — catalogue d'équipement complet — `SheetResource.equipment`, `DndDtos.EquipmentOptionsDto` ; `SheetResourceTest.leCatalogueDEquipement_listeArmesEtArmures`
- [x] P3 T1 — `npm run build` passe — run.md, Phase 3, sur 806b889 (depuis, Markdown seul)
- [x] P3 T2 — parcours du Test Scope sur 5174 — run.md, Phase 3, sur 806b889 : CA 17 → 19 → 20, refus « Une seule armure portée à la fois », Équipement C 155 po, fiche libre Roublard CA 11, rechargement
- [x] P4 T1 — chaque règle rencontrée vers une story existante — `lister-les-capacites-de-niveau-1-que-le-moteur-ne-joue-pas.md`, `subir-les-penalites-d-armure.md`, `manier-une-arme-a-une-ou-deux-mains.md`
- [x] P4 T2 — mémoire à jour, story `done` — `architecture.md`, `codebase-map.md`, `project-brief.md`, epic ; `tenir-l-inventaire-du-personnage.md:3`

### Findings

- `aidd_docs/backlog/stories/tenir-l-inventaire-du-personnage.md:23` : la story, passée `done`, garde « Une fiche d'avant […] se relit », que la décision du plan (« pas de compatibilité : 409 ») contredit → réécrire ce critère selon la décision (fiche d'avant illisible, 409, à recréer) — 🟡 major, fit
- `vtt-web/modules/dnd/CharacterSheetView.vue:238` : les attaques calculées sont tirées de `derived.attacks` en retirant les noms des autres attaques *tapées à l'écran* ; pendant la frappe, une autre attaque renommée s'affiche en double, et une autre attaque nommée « Dague » masque la Dague calculée jusqu'au refus → couper la liste selon la réponse (ses `otherAttacks` en fin, par leur nombre), ou faire rendre au serveur les attaques calculées à part — 🟢 minor, frontend
- `dnd-engine/src/main/java/dnd/engine/sheet/CharacterSheet.java:76` : un choix guidé dont l'inventaire de départ apporte une arme au nom d'une autre attaque saisie (autre attaque « Dague », puis Roublard A) est refusé en 400 avec « nom déjà pris par une arme tenue », sans dire que c'est l'autre attaque à renommer → nommer l'autre attaque en conflit dans le message, ou le dire dans la section guidée — 🟢 minor, error-handling
- `vtt-web/modules/dnd/CharacterSheetView.vue:77` : `api.classes()` est appelé ici et dans `GuidedCreation.vue:69`, deux requêtes identiques par ouverture de fiche → lire les classes une fois dans la fiche et les passer en prop à `GuidedCreation` — 🟢 minor, rot

## Round 2 · r-3346

- Date: 2026-11-17
- By: Cyril Houillon
- Diff: `main...06a7894`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 7/9 met, 0 unmet, 2 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1 T1 — armures et maîtrise / formation des classes — `SrdArmor.java`, `CharacterClass.java:213-224`, `InventoryTest.lesArmuresDuSrd_etLaFormationDesClasses`
- [x] P1 T2 — refus avec raison, inventaire de départ de chaque option — `Item.java`, `Inventory.java`, `StartingEquipment.java`, `Background.java`, `Build.startingInventory` ; `InventoryTest`, `GuidedValuesTest`
- [x] P1 T3 — CA et attaques de départ inchangées ; cas du Test Scope — `CombatStats.java` (`computedAttacks` puis autres attaques), `CharacterSheet.profile` ; `CombatStatsTest`, `CharacterSheetTest.uneAutreAttaqueAuNomDUneArmeTenue_seJoueAPart`
- [x] P2 T1 — statuts, refus sans écriture, histoire sans CA ni attaques — `DndDtos.java`, `SheetResourceTest.unInventaireImpossible_estRefuseEn400_sansRienEcrire`, `SheetCharactersTest`
- [x] P2 T2 — catalogue d'équipement complet — `SheetResource.equipment`, `SheetResourceTest.leCatalogueDEquipement_listeArmesEtArmures`
- Unverified at runtime: P3 T1 `npm run build` passe — run.md l'a sur 806b889 ; `CharacterSheetView.vue` et `GuidedCreation.vue` ont changé depuis (ec783ff) : un build sur la tête le montrerait
- Unverified at runtime: P3 T2 parcours du Test Scope sur 5174 — run.md l'a sur 806b889, code changé depuis ; un nouveau passage, consigné dans run.md, le montrerait
- [x] P4 T1 — chaque règle rencontrée vers une story existante — `lister-les-capacites-de-niveau-1-que-le-moteur-ne-joue-pas.md:84-85`, `attaquer-avec-une-seconde-arme-legere.md`
- [x] P4 T2 — mémoire à jour, story `done` — `architecture.md` (autres attaques identifiées par `autre:<nom>`, `Weapon.id`), `tenir-l-inventaire-du-personnage.md:3, 23`

### Findings

- `vtt-web/modules/dnd/CharacterSheetView.vue:80` : classes et équipement sont lus par un seul `Promise.all` ; depuis que `GuidedCreation` reçoit les classes en prop, un échec de `equipment()` laisse aussi la création guidée sans classe (avant : `allSettled`, chacun pour soi) → `Promise.allSettled`, une erreur par lecture — 🟢 minor, error-handling
- `vtt-web/modules/dnd/CharacterSheetView.vue:246` : deux lignes vides laissées par le retrait de `computedAttacks` → en retirer une — 🟢 minor, standards

## Round 3 · r-d60b

- Date: 2026-11-17
- By: Cyril Houillon
- Diff: `main...f35964b`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 8/9 met, 0 unmet, 1 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1 T1 — armures et maîtrise / formation des classes — `SrdArmor.java`, `CharacterClass.java:213-224`, `InventoryTest.lesArmuresDuSrd_etLaFormationDesClasses`
- [x] P1 T2 — refus avec raison, inventaire de départ de chaque option — `Item.java`, `Inventory.java`, `StartingEquipment.java`, `Background.java`, `Build.startingInventory` ; `InventoryTest`, `GuidedValuesTest`
- [x] P1 T3 — CA et attaques de départ inchangées ; cas du Test Scope — `CombatStats.java`, `CharacterSheet.profile` ; `CombatStatsTest`, `CharacterSheetTest.uneAutreAttaqueAuNomDUneArmeTenue_seJoueAPart`
- [x] P2 T1 — statuts, refus sans écriture, histoire sans CA ni attaques — `DndDtos.java`, `SheetResourceTest`, `SheetCharactersTest`
- [x] P2 T2 — catalogue d'équipement complet — `SheetResource.equipment`, `SheetResourceTest.leCatalogueDEquipement_listeArmesEtArmures`
- [x] P3 T1 — `npm run build` passe — run.md, « Revue · corrections », sur f7d7717 (depuis, Markdown seul) : exit 0, `✓ built in 915ms`
- Unverified at runtime: P3 T2 chaque ligne du Test Scope sur 5174 — run.md sur f7d7717 ne rejoue que les parties touchées (autre attaque homonyme, classes, Dague décochée, rechargement) ; les autres lignes (bouclier CA 19, ajustement +1, Corde ×2, Équipement C, seconde armure refusée, fiche sans classe) datent de 806b889 : un passage complet sur la tête les montrerait
- [x] P4 T1 — chaque règle rencontrée vers une story existante — `lister-les-capacites-de-niveau-1-que-le-moteur-ne-joue-pas.md:84-85`, `attaquer-avec-une-seconde-arme-legere.md`
- [x] P4 T2 — mémoire à jour, story `done` — `architecture.md`, `tenir-l-inventaire-du-personnage.md:3, 23`

## Round 4 · r-fa41

- Date: 2026-11-17
- By: Cyril Houillon
- Diff: `main...6118746`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 9/9 met, 0 unmet, 0 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1 T1 — armures et maîtrise / formation des classes — `SrdArmor.java`, `CharacterClass.java:213-224`, `InventoryTest.lesArmuresDuSrd_etLaFormationDesClasses`
- [x] P1 T2 — refus avec raison, inventaire de départ de chaque option — `Item.java`, `Inventory.java`, `StartingEquipment.java`, `Background.java`, `Build.startingInventory` ; `InventoryTest`, `GuidedValuesTest`
- [x] P1 T3 — CA et attaques de départ inchangées ; cas du Test Scope — `CombatStats.java`, `CharacterSheet.profile` ; `CombatStatsTest`, `CharacterSheetTest.uneAutreAttaqueAuNomDUneArmeTenue_seJoueAPart`
- [x] P2 T1 — statuts, refus sans écriture, histoire sans CA ni attaques — `DndDtos.java`, `SheetResourceTest`, `SheetCharactersTest`
- [x] P2 T2 — catalogue d'équipement complet — `SheetResource.equipment`, `SheetResourceTest.leCatalogueDEquipement_listeArmesEtArmures`
- [x] P3 T1 — `npm run build` passe — run.md, « Revue · corrections », sur f7d7717 (depuis, Markdown seul) : exit 0, `✓ built in 915ms`
- [x] P3 T2 — chaque ligne du Test Scope sur 5174 — run.md, « Phase 3 · parcours complet, rejoué après la revue » (code inchangé depuis f7d7717) : CA 17 → 19 → 20, Fléau retiré, « Trait de feu » en fin, rechargement identique, seconde armure refusée puis « Enregistrée », Équipement C 155 po CA 13, fiche sans classe « — » et invitation, Roublard + cuir CA 11
- [x] P4 T1 — chaque règle rencontrée vers une story existante — `lister-les-capacites-de-niveau-1-que-le-moteur-ne-joue-pas.md:84-85`, `attaquer-avec-une-seconde-arme-legere.md`
- [x] P4 T2 — mémoire à jour, story `done` — `architecture.md`, `tenir-l-inventaire-du-personnage.md:3, 23`
