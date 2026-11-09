# Run

## Phase 1 · Choisir les modules d'une campagne à sa création

- Date: 2026-11-09
- Validated on: 9f89d90

- [pass] Task 1 — `InstalledModules.ids()` rend `["mythic"]` sous `@QuarkusTest` ; `grep -rn mythic vtt-server/src/main` vide — `CampaignApiTest.sansListeDeModules_tousLesModulesInstallesSontActifs` : `GET …/modules` contient `mythic` ; `grep -rniE "mythic|['\"]oracle['\"]" vtt-server/src/main vtt-web/src` : aucune ligne.
- [pass] Task 2 — Une fiche sans `modules` se relit ; une fiche avec `modules` les relit après un nouveau `FileCampaignStore` — `FileCampaignStoreTest.lesModulesChoisis_seRelisent_uneFicheSansModules_seLitAvecTous` : `["mythic"]`, `[]` relus ; création sans liste : `campagne.json` sans `modules`, relue `null` ; fiche écrite à la main `{id, name}` relue `null`.
- [pass] Task 3 — Les lignes api du Test Scope passent dans `CampaignApiTest` ; `mvn -q test` vert — `CampaignApiTest` 9 tests : `modules: ["mythic"]` → `…/mythic/chaos` 200 ; `modules: []` → `…/mythic/chaos` et `POST …/mythic/lists` 404 « Module inactif dans cette campagne : mythic » (corps `ErrorDto` via `ErrorMapper`) ; `/api/mythic/meaning-tables` 200 ; `…/notebook` 200 ; `inconnu` 400 « Module inconnu : inconnu », `Mythic/..` 400, aucune campagne créée ; `/api/campaigns/inconnue/modules` 404. `mvn test` exit 0 : `vtt-engine` 27, `dnd-engine` 68, `mythic-engine` 65, `mythic-server` 43, `vtt-server` 237 tests, 0 échec.
- [pass] Task 4 — Sur 5174 : campagne sans Mythic sans panneau ni section ; avec Mythic, comme avant ; `npm run build` vert — Chrome, Vite 5174 → 8081 (chemins du scratchpad), 1568×778, thème clair : l'accueil montre « Modules » avec « Mythic » coché ; « Sans Mythic » créée Mythic décoché → `GET /api/campaigns` `modules: []` ; son Carnet : aucune colonne de modules, aucune section, après création de « Session 1 » arbre « Session 1 » seul, pas de `.with-modules`. « Avec Mythic » (coché) → `modules: ["mythic"]` ; son Carnet : panneau « Mythic », section Mythic avec Fils et Personnages. `npm run build` : `✓ built in 1.35s`. Écart au plan : `PageTree.vue` inchangé, le filtrage passe par `visiblePages` dans `NotebookScreen`.

## Phase 2 · Activer ou désactiver un module d'une campagne existante

- Date: 2026-11-09
- Validated on: 441093e

- [pass] Task 1 — Les lignes api du Test Scope passent ; les données d'un module désactivé puis réactivé sont identiques — `CampaignApiTest.desactiverPuisReactiverUnModule_gardeSesDonnees_sansToucherLesAutresCampagnes` : A et B avec Mythic, chaos de A à 7, Fils assuré ; `PUT A/modules []` → 200 `[]`, `A/mythic/chaos` 404, `B/mythic/chaos` 200, page Fils de A lisible (200) ; `PUT [mythic]` → chaos 7, `POST lists` rend le même Fils. `changerLesModules_refuse…` : module inconnu 400, `{}` 400, modules inchangés, campagne inconnue 404. `FileCampaignStoreTest.changerLesModules_reecritLaFiche_sansToucherLesDonnees` : fiche relue `[]`, `modules/mythic.json` intact, campagne inconnue `IllegalArgumentException`. `mvn test` exit 0 : `vtt-server` 240 tests, 0 échec, autres modules inchangés.
- [pass] Task 2 — Sur 5174 : désactiver puis réactiver Mythic suit les lignes browser du Test Scope ; `npm run build` vert — Chrome, Vite 5174 → 8081, 1568×778, campagne « Avec Mythic » (chaos 7, « Session 1 » avec un lien `[[Fils]]`) : Modules → décocher Mythic → Enregistrer : dialogue fermé, plus de colonne de modules, arbre « Session 1 » seul, lien Fils toujours `<a href=…/carnet/66208af8…>` ; clic → page « Fils » ouverte. Modules → recocher → Enregistrer : panneau Mythic, chaos 7, section Mythic Fils / Personnages, « Pages qui renvoient ici » de Fils : Session 1. Refus simulé (`fetch` du `PUT` remplacé par un 400 « Module inconnu : essai ») : dialogue ouvert avec le message, panneau Mythic toujours là. `npm run build` : `✓ built in 1.30s`.

## Phase 3 · L'oracle générique répond oui/non et lance un d100

- Date: 2026-11-09
- Validated on: b2b135c

- [pass] Task 1 — `mvn -q verify` construit `oracle-server` ; `InstalledModules.ids()` rend `["mythic", "oracle"]` — `mvn test` exit 0, réacteur 8 modules dont « Module Oracle générique du serveur » (4 tests) ; `CampaignApiTest.sansListeDeModules_tousLesModulesInstallesSontActifs` : `modules` = `["mythic", "oracle"]`.
- [pass] Task 2 — Les lignes system et api du Test Scope passent — `OracleResourceTest` (4 tests) : d100 50 → Oui, question nettoyée ; 51 → Non, question `null` ; corps absent → `null` ; `d100` 100 et 37 ; 501 caractères → `IllegalArgumentException` (400) sans dé, 500 acceptés ; campagne inconnue → `NotFoundException`. `CampaignApiTest` : `POST …/oracle/yes-no` (dé scripté 37) → 200 `yes: true` ; Oracle inactif (`modules: []` ou `["mythic"]`) → `yes-no` et `d100` 404. `vtt-server` 240 tests, 0 échec.
- [pass] Task 3 — Sur 5174 : les lignes browser du Test Scope ; `npm run build` vert — Chrome, Vite 5174 → 8081, 1568×733 : l'accueil propose « Mythic » et « Oracle » cochés ; « Oracle seul » (Mythic décoché) → `modules: ["oracle"]`. Carnet vide : pas de colonne de modules ; « Session 1 » créée : panneau « Oracle » seul. « Le pont tient ? » + Oui/non → citation « Oracle : Le pont tient ? / d100 : 72 → Non » au curseur ; d100 → « d100 : Le pont tient ? / d100 : 16 », rappel « d100 : 16 · Le pont tient ? ». Page verrouillée, question vidée, d100 → « d100 : 45 » en fin de page, `contenteditable=false`. Onglet fermé (aucune page) puis Oui/non → « Aucune page ouverte : ouvrir une page pour y écrire le jet. », aucun appel écrit. `npm run build` : `✓ built in 1.02s` ; `grep -rniE "mythic|['\"]oracle['\"]" vtt-server/src/main vtt-web/src` vide.

## Phase 4 · L'oracle générique tire une ligne dans une liste du Carnet

- Date: 2026-11-09
- Validated on: c091f84

- [pass] Task 1 — `NotebookPagesAdapterTest` vert ; Mythic inchangé (`MythicResourceTest` vert) — écart au plan : le test du port est `NotebookApiTest.lePortDesSections_litLesSousPagesEtLeContenuDeToutePageDeSaSection`, à côté des tests du port existants (même `@QuarkusTest`, port injecté) : sous-pages de Listes triées sans casse (`humeurs`, `Lieux`), sous-page de sous-page (`Jetée`), page ordinaire, autre module, page à la corbeille, autre racine → aucune ; contenu de Lieux et de la racine rendu, sans contenu / page ordinaire / corbeille / autre module → vide ; campagne inconnue et id invalide → `IllegalArgumentException`. `NotebookApiTest` 30 tests ; `MythicResourceTest` 32 tests, 0 échec.
- [pass] Task 2 — Les lignes system du Test Scope passent ; `mvn -q verify` vert — `OracleListsTest` (6 tests) : `POST lists` deux fois → une racine, même id, liste à puces vide ; renommée → même id ; données perdues → reprise par le titre. Sous-pages `[humeurs, Lieux]`, sous-sous-page ignorée ; autre campagne : aucune liste, tirage 404. « Tour », vide, « Port », « Forêt » : d3 2 → « Port », ligne 3, `sides` 3 ; 1 → Tour ; 3 → ligne 4. Lien → nœud `pageLink`, texte « Taverne du Gué ». Liste sans ligne écrite ou sans contenu → 400 « La liste Vide est vide : y écrire des lignes ». Sous-sous-page, page ordinaire, corbeille, Listes elle-même, campagne inconnue → 404. `mvn verify` exit 0 : 27 + 68 + 65 + 43 + 10 + 241 tests, 0 échec.
- [pass] Task 3 — Sur 5174 : les lignes browser du Test Scope ; `npm run build` vert — Chrome, Vite 5174 → 8081, 1568×733, campagne « Oracle seul » : à l'ouverture, section « Oracle » avec « Listes » ; panneau : « Aucune liste : ajouter une sous-page à « Listes »… ». Lieux créée sous Listes (Tour, ligne vide, [[Session 1]]), « Relire les listes » → choix « Lieux ». Tirer ×4, page verrouillée : « Lieux : Session 1 (d2 : 2 → ligne 3) » avec un `a.page-link`, puis « Lieux : Tour (d2 : 1 → ligne 1) » ×3 ; rappel « Lieux : Tour · d2 1, ligne 1 ». « Humeurs » créée pendant que le panneau est ouvert : proposée dès l'ouverture du choix (`Humeurs`, `Lieux`) ; Tirer sur Humeurs → « Jet non tiré : La liste Humeurs est vide : y écrire des lignes », aucune citation de plus (7). Onglet fermé puis Tirer → « Aucune page ouverte : ouvrir une page pour y écrire le jet. ». `npm run build` : `✓ built in 940ms` ; `grep -rniE "mythic|['\"]oracle['\"]" vtt-server/src/main vtt-web/src` vide.

## Revue · corrections

- Date: 2026-11-09
- Validated on: c1c59ce

- [pass] Majeur 1 — une moitié de module absente n'empêche pas de créer une campagne — `GET /api/modules` → `["mythic","oracle"]` (`CampaignApiTest.lesModulesInstalles_seListent`). Chrome, 5174 → 8081 : `/api/modules` simulé à `["mythic"]`, l'accueil ne propose que « Mythic » (coché) ; « Moitie absente » créée sans erreur, `modules: ["mythic"]`.
- [pass] Décision utilisateur — un module installé plus tard n'est jamais actif d'office — `CampaignModulesAtStartupTest` : fiche sans `modules` → `[mythic, oracle]`, fiche choisie inchangée ; second démarrage avec un module « nouveau » : rien ne change. Serveur 8081 démarré sur une fiche `{"id":"0000aaaa-legacy","name":"Ancienne"}` : réécrite `"modules" : [ "mythic", "oracle" ]`. `POST /api/campaigns` sans `modules` écrit la liste installée.
- [pass] Mineur 2 — l'erreur d'enregistrement des modules ne se perd pas — `PUT …/modules` simulé à 1,5 s puis 400 « Refus simulé » : pendant l'envoi, Annuler désactivé, Échap et `cancel` sans effet (dialogue ouvert) ; après, dialogue ouvert avec « Refus simulé ». Le dialogue propose Mythic et Oracle.
- [pass] Mineur 4 — le filtre ne relit plus toutes les fiches — `FileCampaignStore.find` lit `<id>/campagne.json` seul (id `[A-Za-z0-9-]+`) ; `FileCampaignStoreTest` (inconnue, `../id` refusé, fiche relue) vert.
- [pass] Mineur 5 — « Listes illisibles » s'efface après une relecture réussie — `GET …/oracle/lists` simulé à 500 : « Listes illisibles : panne » ; relecture réelle : message effacé, choix `Humeurs`, `Lieux`.
- [pass] Mineur 6 — documentation — `architecture.md` (routes `modules`, liste figée, démarrage, `find`, `availableModules`, dialogue), `codebase-map.md` (routes, `ModuleResource`, `CampaignModulesAtStartup`, `CampaignModulesDialog`, `visiblePages`, coquille), `project-brief.md`.
- [pass] Régression Mythic sur HEAD — « Avec Mythic », « Session 1 » : panneau Mythic seul, section Fils / Personnages ; Demander → « Oracle : question sans énoncé (50/50, chaos 7) / d100 : 16 → Oui ». `mvn verify` exit 0 : 27 + 68 + 65 + 43 + 10 + 243 tests, 0 échec ; `npm run build` `✓ built in 915ms` ; `grep -rniE "mythic|['\"]oracle['\"]" vtt-server/src/main vtt-web/src` vide.

## Final

- Date: 2026-11-09
- Validated on: c1c59ce

- [pass] `mvn verify` — exit 0 ; `vtt-engine` 27, `dnd-engine` 68, `mythic-engine` 65, `mythic-server` 43, `oracle-server` 10, `vtt-server` 243 tests, 0 échec, 0 erreur.
- [pass] `npm run build` dans `vtt-web/` — vue-tsc puis Vite : `✓ built in 915ms`.
