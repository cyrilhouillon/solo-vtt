# Run: tenir l'inventaire du personnage

## Phase 1 · Inventaire, CA et attaques calculées dans `dnd-engine`

- Date: 2026-11-16
- Validated on: 780c8dc

- [pass] Task 1 — Chaque armure a la CA, la catégorie, la Force et la Discrétion du tableau ; chaque classe maîtrise et est formée comme sa note SRD — `InventoryTest.lesArmuresDuSrd_etLaFormationDesClasses` (13 entrées, chemise de mailles 13 + Dex max 2, harnois 18 For 15, cotte de mailles For 13, matelassée Discrétion ; formation Guerrier / Barbare / Moine / Roublard, bouclier compris ; maîtrise d'arme distincte des Bottes) ; `mvn test -pl dnd-engine` exit 0, `Tests run: 174, Failures: 0, Errors: 0`
- [pass] Task 2 — Chaque refus du Test Scope a sa raison ; l'inventaire de départ de chaque option liste ses objets et ses po — `InventoryTest` (deux armures, deux boucliers, texte équipé, quantité 0, po négatives, nom vide ou de 81 caractères, rien ou deux sortes ; Guerrier A + Soldat A : 18 po, cotte de mailles, Javeline ×8, boîte de jeux (dés) ; textes des options FIGHTER_A, MONK_A, FIGHTER_C, Soldat A et B), `GuidedValuesTest` (Moine A avec « luth », Moine sans outil « à choisir », Barbare A, 100 po, 155 po)
- [pass] Task 3 — CA et attaques de départ inchangées pour les quatre classes ; chaque cas du Test Scope donne la CA et les attaques attendues — tous les tests d'avant de `GuidedValuesTest`, `BackgroundChoiceTest`, `SheetHistoryTest` passent avec leurs CA et attaques inchangées ; `CombatStatsTest` : Guerrier 17 → 19 au bouclier, Barbare 13 / chemise de mailles 14 / + bouclier 16 / bouclier seul 15, Moine en cuir (CA 13, Lance +3 1d6+1, mains nues 1 + For) et au bouclier (CA 12), Épée longue non maîtrisée +1, Roublard au bouclier 13 et sans armure 12, arme retirée et doublon, ajustement +3 et « Trait de feu » jusqu'au profil, Dex retouchée suivie, option A → B remplace l'inventaire ; `GuidedValuesTest.unChoixSansRapportAvecLEquipement_gardeLInventaireRetouche` ; `CharacterSheetTest` (Style refusé au Roublard, autre attaque « Dague » refusée), `CharacterProfileTest` (sans classe : ni CA ni attaques, pose refusée)

## Phase 2 · Fiche calculée au serveur

- Date: 2026-11-16
- Validated on: 780c8dc

- [pass] Task 1 — Chaque cas du Test Scope rend le statut et la fiche attendus ; un refus n'écrit rien ; l'histoire ne garde ni CA ni attaques calculées — `SheetResourceTest` : Guerrier A (`derived.armorClass` 17, 4 attaques, inventaire de départ), bouclier en saisie 19 et relu à l'identique, fichier sans `"armorClass"`, Fléau rangé + « Trait de feu » + 30 po, fiche libre Roublard 13, vierge `null`, refus 400 (deux armures, quantité 0, nom d'attaque pris, objet inconnu) sans écriture, document d'avant 409 sans écrasement ; `SheetCharactersTest` : profil à la CA calculée, fiche sans classe refusée (« Aucune classe : la choisir avant de poser la fiche ») ; `PlayerCharacterApiTest` (API réelle) : pose, retouche par `armorBonus`, reprise et rejeu inchangés ; `mvn test` à la racine exit 0, `BUILD SUCCESS`, `dnd-server` 50 tests, `vtt-server` 292 tests, 0 échec
- [pass] Task 2 — Le catalogue liste toutes les armes et armures du moteur — `SheetResourceTest.leCatalogueDEquipement_listeArmesEtArmures` : 38 armes, 13 armures dont le bouclier, cotte de mailles en « Armures lourdes », 404 campagne inconnue

## Phase 3 · Inventaire, classe et valeurs calculées sur la fiche

- Date: 2026-11-16
- Validated on: 806b889

- [pass] Task 1 — `npm run build` passe — `npm run build` dans `vtt-web/` exit 0, `✓ built in 891ms` (vue-tsc compris)
- [pass] Task 2 — Chaque ligne du Test Scope observée sur 5174 — serveur 8081 (`quarkus:dev`, campagnes, images et modules dans le scratchpad) et Vite 5174, Chrome, campagne « Test inventaire » (module D&D), Guerrier guidé A sans historique « Bran » : Setup CA 17, cotte de mailles cochée, Épée à deux mains / Fléau / Javeline ×8 cochées, paquetage, 4 po, quatre attaques calculées ; Bouclier ajouté puis coché → CA 19, « Enregistrée » ; Fléau décoché → attaques Épée à deux mains, Javeline, mains nues ; autre attaque « Trait de feu » gardée dans « Autres attaques » ; ajustement +1 → CA 20 ; « Corde de chanvre » ajoutée, quantité 2, po 30 → « Enregistrée », rechargé : Corde ×2, 30 po, CA gardée ; Armure de cuir cochée en plus de la cotte → « Non enregistrée : Une seule armure portée à la fois », puis retirée → « Enregistrée » ; cotte retirée → CA 15 (10 + Dex 2 + bouclier 2 + ajustement 1) ; Langues tapées puis Javeline décochée aussitôt → les deux relues après rechargement ; Création guidée « Équipement C » → aucun objet, 155 po, CA 13, « Trait de feu » gardée ; fiche libre « Zed » : CA « — » et « Choisir une classe pour calculer la CA et les attaques. », Roublard + armure de cuir cochée → CA 11 ; fiche libre « Kael » : Guerrier montre « Style de combat », Défense → CA 12, retour à Roublard retire le Style → CA 11, « Enregistrée ». Premier passage (160022b) : quantité et po partaient à 0 (« Corde de chanvre : quantité d’au moins 1 ») ; corrigé en 806b889 (`valueAsNumber`), parcours rejoué en entier. Teardown : 8081 et 5174 arrêtés, 8080 et 5173 jamais touchés
