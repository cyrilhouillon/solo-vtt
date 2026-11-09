---
objective: "Les six preuves de succès de l'epic « jouer sa campagne avec Mythic GME depuis le Carnet » sont observées (import unique des tables, question écrite dans Session 6, événement qui tire un fil et deux mots, fin de scène puis scène modifiée, listes dans l'arbre après redémarrage, cœur sans Mythic), puis l'epic est close ou ses écarts deviennent des items du backlog."
status: in-progress
---

# Plan: vérifier l'epic « jouer sa campagne avec Mythic GME depuis le Carnet » de bout en bout

Source: aidd_docs/backlog/epics/jouer-sa-campagne-avec-mythic-gme-depuis-le-carnet.md (Success Evidence) ; choix de l'utilisateur : vérifier l'epic, ses cinq stories étant `done` et aucune story n'attendant au backlog

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Base verte et cœur sans Mythic | [`phase-1.md`](./phase-1.md) | none |
| 2   | Parcours navigateur de l'epic | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Clore l'epic ou reporter ses écarts | [`phase-3.md`](./phase-3.md) | 2 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | Vérification seulement : aucune bibliothèque ajoutée ni changée. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Regression | Aucune : aucun code modifié. Les briques sont couvertes une à une (`mythic-engine` : `FateQuestionTest`, `SceneCheckTest`… ; `mythic-server` : `MythicResourceTest`, `MeaningResourceTest`, `ListLinesTest` ; `vtt-server` : `FileCommonModuleDataTest`, `FileCampaignStoreTest`, `NotebookApiTest`). | 1 |
| Architecture | Pas de test de bout en bout serveur, contrairement à l'epic Carnet (`CarnetDeBoutEnBoutTest`) : il demanderait `MythicResource` et les adaptateurs fichier ensemble. Dans `vtt-server/src/test`, il ferait nommer Mythic au cœur (règle de l'epic, `architecture.md`) ; dans `mythic-server`, il dépendrait de `vtt-server`, qui dépend déjà de `mythic-server` (cycle Maven, `vtt-server/pom.xml:45-46`). La reprise est donc prouvée sur 8081 par relevés `GET` avant / après redémarrage. | 2 |
| Runtime | Non vérifié : écriture dans la page, événement tiré dans une liste, scène modifiée, arbre et reprise ne sont observables qu'au navigateur, sur 8081/5174. Les dés sont réels (`DiceProducer`, `SecureRandom`, sans réglage) : double et scène modifiée s'obtiennent en répétant au chaos 9, comme aux stories `tirer-dans-les-listes` et `ouvrir-et-clore-une-scene` (`run.md`). | 2 |
| Isolation du module | Tenue à la planification : `grep -rni mythic vtt-server/src vtt-web/src` vide ; Mythic n'est cité que par `pom.xml` (l. 17-18, 46-52), `vtt-server/pom.xml` (l. 45-46) et `vtt-web/modules/mythic/`. La phase 1 le refait sur le commit vérifié. | 1 |
| Documentation | Epic : `status: done` si les six preuves tiennent ; sinon un item par écart dans `aidd_docs/backlog/`, l'epic reste `ready`. `verification.md` créé en phase 2. Plan : `status: implemented`. | 2, 3 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Ne jamais tester sur 8080/5173 ; campagnes, images et données de modules dans le scratchpad (`testing.md`) | Le parcours écrit chaos, listes et tables : sans `solovtt.modules.path`, il écraserait les tables du joueur dans `~/.solo-vtt/modules/`. |
| Tables de sens : jamais de mot du livre, seulement des mots inventés (`architecture.md`) | Le fichier importé est généré dans le scratchpad, au format de `TablesDeTest` (`action1-001`…), hors du dépôt. |
| Le joueur ne passe que par l'interface (epic) | Ni Swagger ni fichier édité à la main ; l'API n'est lue qu'en `GET` pour comparer avant et après redémarrage. |
| Prouver `works` par des preuves (`CLAUDE.md`) | Chaque preuve de l'epic est consignée avec capture ou réponse `GET` dans `verification.md`. |
