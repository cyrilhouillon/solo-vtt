# Review: créer en guidé un Guerrier de niveau 1

- Rounds: 1
- Started: 2026-11-12

## Round 1 · r-7c3a

- Date: 2026-11-12
- By: Cyril Houillon
- Diff: `main...2c33cc0` plus the working tree (`vtt-web/vite.config.ts` modifié, `.mcp.json` non suivi)
- Axes: code, functional, relevancy
- Verdict: changes-requested
- Score: 8/11 met, 0 unmet, 3 unverified at runtime, 0 out of the diff

### Criteria

- [x] P1.1 Chaque arme et armure du Guerrier a les valeurs de sa fiche SRD — `SrdWeapon.java:30-67` (38 armes : dégâts, propriétés, portée en m, botte conformes au SRD 5.2.1), `SrdArmor.java:5-7`
- [x] P1.2 Construction impossible refusée avec sa raison ; `Build.fighter()` valide — `Build.java:45-66`, `AbilityMethod.java:43-71`
- [x] P1.3 Guerrier A Défense : CA 17, PV 11, trois attaques ; passer à B recalcule CA et attaques et garde les retouches indépendantes — `GuidedValues.java:44-65` (16 + 1 ; 10 + 1 ; +4 For), `GuidedValues.java:73-112` (recalcul par différence)
- [x] P2.1 Rejouer créée, construite, saisie redonne valeurs, construction et jets — `SheetHistory.java:18-39`
- [x] P2.2 Construction valide remplit la fiche, invalide 400 sans écrire ; dés une seule fois — `SheetResource.java:139-150` (validate avant emit), `SheetResource.java:159-166` (409)
- [x] P2.3 Fiche construite relue à l'identique ; `dnd.json` d'avant relu — `SheetStore.java` `EventFile` (`NON_NULL`, champs nouveaux facultatifs), test `unDocumentDAvantLaConstruction_seRelitSansConstruction`
- Unverified at runtime: P3.1 `npm run build` passe ; types suivent les DTO — types conformes à `BuildDto`, `ClassOptionsDto`, `SheetDto` à la lecture ; le build demande un passage (run.md sur d798345, arbre de travail modifié depuis : `vite.config.ts`)
- Unverified at runtime: P3.2 Choisir Guerrier puis chaque choix remplit la fiche ; achat > 27 refusé avant l'envoi — garde `applyPointBuy` présente (`GuidedCreation.vue:146`) ; le remplissage se voit dans le navigateur
- Unverified at runtime: P3.3 Frappe puis choix : deux envois ordonnés, aucun troisième ; rechargement identique — `choose` attend `saved()` puis chaîne `saving` (`CharacterSheetView.vue:114-130`) ; l'ordre réseau se voit à l'exécution
- [x] P4.1 Chaque capacité de niveau 1 jouée, portée par la fiche ou renvoyée à une story — `lister-les-capacites-…md`, tableau de 6 lignes, trois stories créées
- [x] P4.2 La mémoire nomme chaque nouveau type, route et événement — `architecture.md`, `codebase-map.md` (tous les types, `SheetBuilt`, `AbilitiesRolled`, `classes`, `build`, `ability-rolls`, `GuidedCreation`)

### Findings

- `dnd-engine/src/main/java/dnd/engine/sheet/GuidedValues.java:46` : CA, attaques, initiative et PV sont calculés depuis les valeurs de la construction, jamais depuis celles de la fiche ; une caractéristique retouchée (cas de tout personnage 2024 : +2/+1 de l'historique, non modélisé) est ignorée au choix suivant. Ex. Dex retouchée à 16, puis A → B : CA réécrite à 15 au lieu de 16, attaques Finesse et Arc long fausses de 1 → dériver les modificateurs des caractéristiques de la fiche au `rebuild`, ou porter les hausses d'historique dans `Build` (et le dire dans la story) — 🟡 major, fit
- `vtt-web/vite.config.ts:8` : `host: '0.0.0.0'` (arbre de travail) expose sur le réseau local Vite et son proxy `/api`, sans authentification, qui écrit les campagnes ; sans rapport avec la story → ne pas le committer sur cette branche (garder `127.0.0.1`, ou `--host` en ligne de commande au besoin) — 🟡 major, security
- `vtt-web/modules/dnd/GuidedCreation.vue:92` : « — (saisie libre) » n'est que local ; la construction reste au serveur et Guerrier revient au rechargement → l'assumer dans le libellé/la doc, ou l'enregistrer (route qui retire la construction) — 🟢 minor, fit
- `vtt-web/modules/dnd/GuidedCreation.vue:105` : `chooseMethod` change `method` avant l'envoi ; refusé, la liste montre une méthode que le serveur n'a pas (seule une nouvelle `props.build` remet les brouillons) → remettre `classId`, `method`, `scores` depuis `props.build` quand `error` arrive — 🟢 minor, frontend
- `dnd-server/src/main/java/dnd/server/SheetResource.java:147` : une construction gardée sans une des six valeurs (fichier édité à la main) passe `toBuild` puis fait `NullPointerException` dans `GuidedValues.of` → 500 au lieu de 409 → valider la construction relue dans `EventFile.event()` (valeur manquante : `IllegalStateException`, donc 409) — 🟢 minor, error-handling
- `dnd-server/src/main/java/dnd/server/SheetResource.java:21` : `jakarta.ws.rs.core.Response` et `SchemaType` importés au milieu des imports OpenAPI → les ranger avec leurs paquets — 🟢 minor, standards
