# Run

## Phase 1 · PV perdus dans l'histoire de la fiche et port `sustained`

- Date: 2026-11-13
- Validated on: dc8a097

- [pass] Task 1 — Une histoire avec `HitPointsLost` se plie en PV baissés, jamais sous 0, construction et jets intacts — `SheetHistoryTest.desPvPerdus_baissentLesPvActuels_etGardentConstructionEtJets` (seuls les PV changent, construction et jets égaux), `desPvPerdus_seComposentAvecUneRetouche_etNeDescendentPasSous0` (retouche 10 puis −3 → 7 ; 7 − 20 → 0), `desPvPerdus_sontPositifs` (0 et −2 refusés) ; `mvn test` dnd-engine `Tests run: 138, Failures: 0`
- [pass] Task 2 — Un coup envoyé à `SheetCharacters` baisse la fiche lue par `GET` et par `take`, relue par une autre instance ; une page qui n'est pas une fiche rend `false` sans écrire ; un document abîmé lève 409 sans être écrasé — `SheetCharactersTest.unCoupRecu_…` (PV − 5, `dnd.json` contient `{"type":"lost","amount":5}`, nouvelles `SheetResource` et `SheetCharacters` : `read` et `take` à PV − 5), `unCoup_seComposeAvecUneRetouche_…` (10 → 7 → 0), `uneAutrePage_…` (fiche à la corbeille, id inconnu, page ordinaire : `false`, document inchangé), `sansDegats_…` (liste vide, autre événement, dégâts 0 : `true`, rien écrit), `unDocumentAbime_refuseLeCoupEn409_sansEtreEcrase`, `desPvPerdusSansMontant_…` (409) ; dnd-server `Tests run: 39, Failures: 0`. Injection de `SheetResource` dans `SheetCharacters` : le démarrage Quarkus des tests de vtt-server passe, `Tests run: 282, Failures: 0`, `BUILD SUCCESS` à la racine

## Phase 2 · La session envoie les coups d'un PJ, hors rejeu

- Date: 2026-11-13
- Validated on: 64ad55a

- [pass] Task 1 — Un coup sur un PJ, en combat ou non, part une seule fois avec la commande qui le porte ; reprise, `replayed`, `resume`, pas à pas et nouvelle carte n'envoient rien ; un suivi qui lève laisse le coup gardé et rend l'avertissement — `FicheSuitLeCombatTest` : `unCoupEnCombat_…` (rien à l'attaque suspendue par Bouclier, un envoi `magicienne` `[DamageDealt magicienne-1 5]` à la réponse, dernière commande gardée `Answer`), `unCoupHorsCombat_…` (un envoi `guerriere`, montant du journal), `unCoupRate_unCoupSurUnMonstre_ouUneAttaqueRefusee_…` (aucun envoi), `rouvrirLaPartie_rejouerLeCombat_ouLeLirePasAPas_…` (session rouverte, `replayed`, `CombatReplay.of` : aucun envoi), `nouvelleCarte_puisReprendreLeCombat_…` (aucun envoi à la nouvelle carte ni au `resume`, un seul pour le coup suivant), `uneFicheQuiNeSuitPas_…` (avertissement « Fiche « Magicienne » non mise à jour : … », 5 dégâts, `Answer` gardé, PV 2). Contre-épreuve : garde `replaying` retirée → 2 échecs (`expected: <true> but was: <false>` à la reprise et au `resume`), garde remise. Écart au plan : l'avertissement cite le nom entre guillemets (« Fiche « Aria » … ») au lieu de « Fiche d'Aria », pour éviter l'élision ; `CharacterSheetFollower.sustained` prend `(characterId, events)`, le nom ne servant qu'à la session
- [pass] Task 2 — De bout en bout, une vraie fiche perd les PV du journal ; un module inactif ou une fiche inconnue rend un refus ; les personnages de test n'avertissent pas — `PlayerCharacterApiTest.unCoupRecuParLePJ_…` (POST `/attacks` 200, `warnings` `[]`, `GET /dnd/sheets/{id}` PV − dégâts du journal, inchangés après une `SceneSession` rouverte, Aria reposée aux PV de la fiche), `sansLeModuleDnd_…` (`warnings` « Fiche « Aria » non mise à jour : fiche introuvable ou module inactif », fiche intacte après réactivation), `unCoupSurUnPersonnageDeTest_nAvertitPas` ; `FicheSuitLeCombatTest.leCatalogue_…` (source active reçoit, id inconnu et module inactif : « fiche introuvable ou module inactif ») ; `mvn test` racine `BUILD SUCCESS`, vtt-server `Tests run: 292, Failures: 0`

## Phase 3 · Avertissement à l'écran de combat et parcours navigateur

- Date: 2026-11-13
- Validated on: 0ee4656

- [pass] Task 1 — `npm run build` passe ; une action avec avertissement l'affiche dans le bandeau, l'action restant résolue — `npm run build` : `✓ built in 1.28s`, aucune erreur. Premier passage (0b1eea2) en échec : le serveur rendait l'avertissement (journal 15:55:11, « fiche introuvable ou module inactif »), mais le bandeau restait vide, car `settleAction` relance `showSheet`, dont `attempt` efface `error`. Corrigé en 0ee4656 (l'avertissement est reposé après `showSheet`), puis tout le parcours a été repris sur une campagne neuve. Chrome, 5174 → 8081, module D&D désactivé : le Gobelin attaque Aria hors combat ; deux ratés sans bandeau ; touché à 3 dégâts puis à 5 dégâts : le bandeau dit « Fiche « Aria » non mise à jour : fiche introuvable ou module inactif », avec « Fermer » ; le verdict « Touché · 5 dégâts tranchants » s'affiche et Aria passe à 0/11, « À terre »
- [pass] Task 2 — Les quatre critères de la story s'observent dans le navigateur ; le module désactivé donne l'avertissement — campagne neuve « Test PV fiche 2 », Aria (Guerrier par défaut) à 11/11 ; « Lancer le combat » dans l'écran, fin du tour d'Aria, Gobelin → Cimeterre → Aria : « Touché · 8 dégâts tranchants », 3/11 sur la carte, sans bandeau. (1) Carnet, fiche d'Aria : « PV actuels = 3 ». (2) Combat terminé, serveur 8081 arrêté puis relancé (« Listening on: http://localhost:8081 ») : fiche toujours à 3. (3) Rejeu du combat lu de « Début » au « Pas 4 / 4 » : fiche à 3 (`GET …/dnd/sheets/{id}`). (4) Nouvelle carte, Aria reposée : tableau « Aria 3 / 11 ». Module désactivé : voir la tâche 1 ; la fiche reste à 3 après réactivation. Démarrage CDI : `SheetCharacters` reçoit `SheetResource` sans erreur (trois démarrages de 8081). Serveurs 8081 et 5174 arrêtés ; 8080 et 5173 jamais touchés. Mise en place (campagne, fiche, carte, poses, fin du combat, modules) par l'API 8081 ; coups et rejeu par l'écran

## Phase 4 · Mémoire et backlog

- Date: 2026-11-13
- Validated on: 34f2c40

- [pass] Task 1 — La mémoire nomme le port, l'événement et ses limites ; la story est `done` — `architecture.md` : puce du catalogue de préparation complétée (envoi après la garde, hors rejeu, `CharacterSheetFollower`, `CampaignCatalog.sustained`, `PlayerCharacters.sustained`, `HitPointsLost` écrit `lost`, verrou de `SheetResource`, `ActionResultDto.warnings`, tout coup compté) et nouveau piège (coup perdu si arrêt entre les deux écritures, saisie périmée, PJ posé deux fois, combat repris avec la copie d'avant). `codebase-map.md` : `PlayerCharacters.sustained`, `CharacterSheetFollower`. `project-brief.md` : les coups baissent la fiche, la phrase sur la retouche est gardée. Chaque nom cité se trouve dans le code (`grep` : `PlayerCharacters.java`, `SheetCharacters.java`, `SheetEvent.java`, `SheetStore.java`, `CharacterSheetFollower.java`, `Dtos.java`, `SceneSession.java`, `SheetResource.java`). Story `voir-sur-la-fiche-les-pv-perdus-en-combat.md` : `status: done`

## Final

- Date: 2026-11-13
- Validated on: 0ee4656 (dernier commit de code ; seuls des fichiers Markdown ont changé depuis)

- [pass] `mvn -q compile` — exit 0, aucune erreur
- [pass] `mvn verify` — `BUILD SUCCESS` ; dnd-engine `Tests run: 138, Failures: 0`, dnd-server `Tests run: 39, Failures: 0`, vtt-server `Tests run: 292, Failures: 0`, autres modules 27, 65, 43 et 10, sans échec
- [pass] `npm run build` dans `vtt-web/` — vue-tsc et vite : `✓ built in 932ms`, aucune erreur
