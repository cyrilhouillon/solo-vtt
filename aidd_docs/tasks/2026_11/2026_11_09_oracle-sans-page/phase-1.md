---
status: in-progress
---

# Instruction: Cœur et Oracle : écrire ou afficher et copier

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/src/
├── modules/notebookModule.ts        ✏️ ajoute writeResult(blocks) au NotebookContext
├── notebook/clipboard.ts            ✅ blocs → HTML + texte, copie au presse-papiers
└── components/
    ├── ResultDialog.vue             ✅ dialogue modal : résultat en lecture, Copier, Fermer
    └── NotebookScreen.vue           ✏️ implémente writeResult, monte ResultDialog
aidd_docs/memory/
├── architecture.md                  ✏️ contrat NotebookContext
└── codebase-map.md                  ✏️ ResultDialog, notebook/clipboard.ts
vtt-web/modules/oracle/
├── OraclePanel.vue                  ✏️ premier appelant de writeResult, plus de refus sans page
└── index.ts                         ✏️ commentaire « s'écrit dans la page ouverte »
```

## Test Scope

| Scenario | Action | Observable outcome | Actor |
| -------- | ------ | ------------------ | ----- |
| Setup | `mvn -q install -DskipTests`, serveur 8081 et Vite 5174 (`testing.md`), campagne neuve avec Oracle et Mythic | Carnet ouvert, panneaux visibles | system |
| Happy path | Page ouverte : Oui/non, d100, Tirer (Oracle) ; un geste Mythic | Les résultats s'écrivent comme avant (curseur, ou fin si verrouillée) ; aucun dialogue | browser |
| Happy path | Aucune page ouverte, Oui/non (Oracle) | Dialogue modal avec la citation rendue ; « Copié dans le presse-papiers. » ; Ctrl+V dans une page déverrouillée colle citation, gras et lien `[[…]]` vivant | browser |
| Edge case | Copie automatique refusée (presse-papiers indisponible, activation expirée) | Le dialogue dit « Copie automatique impossible » avec la raison ; « Copier » copie au clic | browser |
| Edge case | Clic sur un lien de page dans le dialogue | Le dialogue se ferme et la page cible s'ouvre | browser |
| Edge case | Échap ou « Fermer » | Le dialogue se ferme, le focus revient à l'élément qui l'avait | browser |
| Teardown | Tuer 8081 puis 5174 | Ports libérés, 8080/5173 intacts | system |

## Wireframe

```txt
┌──────────────────────────────────────────┐
│ (1) Résultat                              │
├──────────────────────────────────────────┤
│ (2) │ Oracle : Le pont tient-il ?         │
│     │ d100 : 37 → Oui                     │
├──────────────────────────────────────────┤
│ (3) Copié dans le presse-papiers.         │
├──────────────────────────────────────────┤
│                     (4) [Copier] [Fermer] │
└──────────────────────────────────────────┘
```

1. Titre : « Résultat » ; aucune page ouverte pour l'écrire.
2. Le résultat en lecture seule, rendu comme dans une page (citation, gras, liens vivants).
3. État de la copie : réussie, ou impossible avec la raison.
4. Copier relance la copie ; Fermer (focus par défaut) referme.

## Tasks to do

### `1)` Contrat

> Le Carnet offre une écriture qui ne refuse jamais.

1. Dans `notebookModule.ts`, ajouter `writeResult(blocks: JSONContent[]): void` : écrit dans la page ouverte comme `writeToOpenPage`, sinon affiche le résultat dans un dialogue et le copie au presse-papiers. Garder `hasOpenPage` et `writeToOpenPage` jusqu'à la phase 2.

### `2)` Copie

> Les blocs d'un module deviennent HTML et texte au presse-papiers.

1. Créer `src/notebook/clipboard.ts` : `copyBlocks(blocks, pages): Promise<string | null>` (raison de l'échec, `null` si copié).
2. Rendre `{ type: 'doc', content: blocks }` par `generateHTML` et `generateText` (`blockSeparator: '\n'`) avec `StarterKit.configure({ link: false })`, `PageLink.configure({ pages: () => pages })`, `PageImage`.
3. Écrire par `navigator.clipboard.write([new ClipboardItem({ 'text/html': …, 'text/plain': … })])` ; sans `ClipboardItem`, `writeText` du texte ; capter toute erreur en raison.

### `3)` Dialogue

> Montrer le résultat quand aucune page ne peut le recevoir.

1. Créer `ResultDialog.vue` (`<dialog>` modal, sur le modèle de `ShortcutsDialog` : `showModal`, Échap, focus rendu) : props `blocks`, `pages`, `pageHref`, `copyError` ; émet `copy`, `open(pageId)`, `close`.
2. Rendre les blocs par un `PageEditor` en lecture (`editable=false`, `content` = le document), hauteur minimale annulée.
3. Afficher l'état de la copie (3) et les boutons Copier / Fermer (4).

### `4)` Branchement

> `NotebookScreen` choisit la page ou le dialogue.

1. Implémenter `moduleContext.writeResult` : page et éditeur présents → `editor.insertBlocks` ; sinon garder les blocs, ouvrir `ResultDialog`, lancer `copyBlocks(blocks, notebook.pages)` et passer sa raison.
2. `copy` relance `copyBlocks` ; `open` ferme le dialogue puis `open(pageId)`.
3. Monter `ResultDialog` hors de la branche `.content`, comme `TrashDialog`.

### `5)` Oracle

> Premier appelant : l'oracle générique tire sans page.

1. Dans `OraclePanel.vue`, retirer le refus `hasOpenPage` de `roll` et remplacer `writeToOpenPage` + refus (et le type `Parameters<…['writeToOpenPage']>`) par `writeResult`.
2. Mettre à jour les commentaires du panneau et de `oracle/index.ts`.

### `6)` Mémoire

> La doc dit le nouveau contrat.

1. `architecture.md` : ligne `NotebookContext` (`writeResult`, dialogue et copie sans page) et panneau Oracle (plus de refus sans page).
2. `project-brief.md` : Oracle générique (« refusé sans page »).
3. `codebase-map.md` : `components/ResultDialog.vue`, `src/notebook/clipboard.ts`.

## Test acceptance criteria

| Task | Acceptance criteria |
| ---- | ------------------- |
| 1 | `npm run build` passe ; `NotebookContext` expose `writeResult` |
| 2 | Après `copyBlocks`, coller dans une page déverrouillée rend la citation et un lien `[[…]]` cliquable ; coller dans un éditeur texte rend « Oracle : … » puis « d100 : … » sur deux lignes |
| 3 | Le dialogue montre le résultat rendu, l'état de la copie, se ferme par Échap et Fermer, et un lien ouvre sa page |
| 4 | Page ouverte : écriture identique à avant, aucun dialogue ; sans page : dialogue ouvert et presse-papiers rempli |
| 5 | Sans page, Oui/non, d100 et Tirer ouvrent le dialogue et copient ; avec page, écriture inchangée ; une liste vide garde l'erreur dans le panneau, sans dialogue |
| 6 | `architecture.md`, `project-brief.md` et `codebase-map.md` nomment `writeResult`, `ResultDialog` et `clipboard.ts`, et ne disent plus l'Oracle refusé sans page |
