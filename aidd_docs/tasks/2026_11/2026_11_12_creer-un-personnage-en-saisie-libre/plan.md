---
objective: "Dans une campagne où le module D&D est actif, le joueur crée une fiche de personnage en saisie libre, rangée dans la section « Personnages joueurs » du Carnet, qu'il modifie et retrouve comme une page, même après un redémarrage."
status: implemented
---

# Plan: créer un personnage en saisie libre et le retrouver dans « Personnages joueurs »

Source: aidd_docs/backlog/stories/creer-un-personnage-en-saisie-libre-dans-personnages-joueurs.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Entrées de module dans le Carnet | [`phase-1.md`](./phase-1.md) | none |
| 2   | Fiche de personnage à événements dans `dnd-engine` | [`phase-2.md`](./phase-2.md) | none |
| 3   | Module serveur `dnd-server` | [`phase-3.md`](./phase-3.md) | 1, 2 |
| 4   | Module client `vtt-web/modules/dnd` | [`phase-4.md`](./phase-4.md) | 1, 3 |
| 5   | Mémoire du projet | [`phase-5.md`](./phase-5.md) | 1, 2, 3, 4 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | none |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| architecture | Le Carnet ne connaît qu'une sorte de page sans texte, l'entrée de combat, propre au cœur (`Page.combatId`, `NotebookStore.checkParent` / `checkContent`, `CombatEntryView` dans `NotebookScreen.vue:885`). Un module n'a que des racines de section (`NotebookPages`). Changement : une page peut être l'entrée d'un module (`Page.entryOf`, l'id du module), sans texte ni sous-page ; le module la crée par `NotebookPages.createEntry` et la montre par un `entryView` déclaré dans son `NotebookModule`. | phase 1 |
| architecture | Nouveau module D&D (`dnd-server`, `vtt-web/modules/dnd/`), branché comme Oracle et Mythic : seuls `pom.xml` racine, `vtt-server/pom.xml` et le dossier client le nomment. | phase 3, 4 |
| regression | Moyen : on touche `Page`, `NotebookStore`, `JsonFileNotebookStore`, `Dtos`, `NotebookResource`, `tree.ts`, `PageBranch.vue`, `NotebookScreen.vue`. Couverts par `NotebookStoreTest`, `NotebookApiTest`, `NotebookResourceTest`, `CombatsEnregistresTest`, `persistence/*` (serveur) ; aucun test client. Pas de phase filet : les tests des entrées de combat verrouillent déjà leur comportement, message de refus compris (`NotebookStoreTest.java:247`), que la phase 1 garde ; `combatId` n'est pas modifié. | phase 1 |
| runtime | Un module installé plus tard n'est actif nulle part (`InstalledModules`, `CampaignModulesAtStartup`) : les campagnes existantes n'ont D&D qu'après activation par « Modules » ; les nouvelles l'ont coché par défaut. Observé : `CampaignModulesAtStartup.java:11`, `InstalledModules.java:62`, coché par défaut `HomeScreen.vue:20` ; non rejoué. | phase 3, 4 |
| documentation | `architecture.md` (entrée de module, module D&D, fiche à événements), `codebase-map.md` (`dnd-server`, `vtt-web/modules/dnd`, `dnd.engine.sheet`), `package.md` (`dnd-server` et ses dépendances), `project-brief.md` (fonctionnalité), `testing.md` (`dnd-server/src/test`). | phase 5 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Un module n'est nommé que par les assemblages (`pom.xml` racine, `vtt-server/pom.xml`, `vtt-web/modules/`) | `dnd-server` et `vtt-web/modules/dnd` ne doivent apparaître ni dans `vtt-server/src` ni dans `vtt-web/src` |
| Les moteurs n'ont aucune dépendance runtime | la fiche vit dans `dnd-engine` sans Jackson ; la sérialisation reste dans `dnd-server` |
| Code et messages en français, identifiants en anglais ; tests aux noms français qui décrivent la règle | toutes les phases |
| `ErrorMapper` rend 400 pour `IllegalArgumentException` ; page inconnue : `NotFoundException` (404) | refus de la fiche et des entrées |
| Tests navigateur sur 8081 / 5174 uniquement, jamais 8080 / 5173 | phase 4 |
| Vérifications avant commit : `mvn -q compile`, `mvn -q test`, `npm run build` | chaque phase |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Une fiche est une **entrée de module** : une page sans texte ni sous-page qui porte `entryOf` (l'id du module), dont l'id est celui de la fiche ; le module la montre par son `entryView` | elle « passe pour une page » (onglet, adresse, liens `[[…]]`, recherche par titre, renommage, corbeille) sans que le cœur connaisse D&D ; `combatId` reste tel quel, pour ne pas migrer les fiches de combat |
| Le nom du personnage est le **titre de sa page** | un seul nom, renommé depuis l'arbre ; liens, onglets et recherche le suivent sans rien apprendre |
| La fiche est un **agrégat à événements** dans `dnd-engine` (`dnd.engine.sheet`) : `SheetCreated`, puis `SheetEdited` qui porte toutes les valeurs saisies ; l'état est le pli des événements | décision de l'epic ; la story 5 y ajoutera les PV perdus en combat. Le mécanisme du combat vers la fiche reste au spike |
| Les événements de toutes les fiches d'une campagne sont gardés dans le document du module (`ModuleData`, `<campagne>/modules/dnd.json`) | port existant, écriture atomique, sans nouveau port ; volume d'un joueur solo |
| Les dérivés évidents sont calculés, pas saisis : modificateur = ⌊(valeur − 10) / 2⌋, bonus de sauvegarde et de compétence = modificateur + maîtrise (×2 en expertise), Perception passive = 10 + Perception | ce sont des formules du SRD, pas des choix ; tout ce qu'une fiche papier fait écrire (CA, PV, vitesse, initiative, attaques…) est saisi |
