---
status: done
---

# Instruction: scénario de l'epic en test du Carnet

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── vtt-server/src/test/java/solovtt/server/
    └── CarnetDeBoutEnBoutTest.java ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `mvn -q test` à la racine avant tout ajout | tous les tests passent | cli |
| Happy path   | `FileCampaignStore` sur `@TempDir` ; campagne « Barovie » ; « Sessions », « PNJ », puis « Ismark » et « Ireena » sous « PNJ » ; « Session 5 » sous « Sessions », contenu Tiptap qui lie Ismark et Ireena et contient « auberge » ; onglets [Session 5, Ismark], actif Session 5 | arbre, contenu et onglets relus tels quels | system |
| Happy path   | Renommer « Ismark » en « Ismark le Petit » | contenu de Session 5 inchangé ; `PageLinks.citing` d'Ismark rend Session 5 | system |
| Happy path   | `search("auberge")` puis `search("ismark petit")` | Session 5 trouvée par son texte ; Ismark trouvée par son titre, en premier | system |
| Happy path   | `delete(PNJ)` puis `restore(PNJ)` | PNJ, Ismark et Ireena absents de `list`, un seul lot dans `trash` avec 2 sous-pages ; après restauration, arbre d'origine et Session 5 de nouveau citante | system |
| Edge case    | Nouveau `FileCampaignStore` sur le même dossier (redémarrage) | campagne, pages, parents, contenus, onglets, corbeille vide, liens entrants et recherche identiques | system |
| Edge case    | `delete(PNJ)` sans restaurer, puis redémarrage | la corbeille garde le lot ; Ismark introuvable (`find` vide) ; `tabs()` garde l'id brut d'Ismark, que `new NotebookResource(notebook).get()` omet (`NotebookResource.java:45-51`) | system |

## Tasks to do

### `1)` Filet de sécurité

> Partir d'une base verte.

1. Lancer `mvn -q test` à la racine ; si rouge, s'arrêter et le signaler, sans toucher la production.

### `2)` Écrire le scénario complet

> Jouer les preuves serveur de l'epic sur le vrai stockage.

1. Créer `CarnetDeBoutEnBoutTest` sur `FileCampaignStore` + `@TempDir`, modèle `JsonFileNotebookStoreTest`.
2. Enchaîner création, liens, onglets, renommage, liens entrants, recherche, suppression et restauration.
3. Relire tout par un nouveau `FileCampaignStore` et comparer ; même chose avec un lot laissé à la corbeille, onglets lus par `NotebookResource.get()`.
4. Lancer `mvn -q test` : vert.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `mvn -q test` passe avant l'ajout. |
| 2 | `CarnetDeBoutEnBoutTest` passe ; il échoue si un renommage réécrit une page citante, si la restauration perd un lien entrant ou si la relecture diffère. |
