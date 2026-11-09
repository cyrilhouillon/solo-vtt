---
objective: "Sans page ouverte, tout tirage d'un module du Carnet (Mythic, Oracle) s'affiche dans un dialogue et son contenu est copié dans le presse-papiers, au lieu d'être refusé."
status: implemented
---

# Plan: Oracle sans page ouverte

Source: texte — « quand aucune page n'est ouverte, on doit pouvoir quand même utiliser un oracle, de mythic ou autre, à ce moment-là le résultat doit s'afficher dans une pop up, et le contenu qui aurait dû être affiché est copié dans le presse papier »

## Phases

| #   | Phase | File | Depends on |
| --- | ----- | ---- | ---------- |
| 1   | Cœur et Oracle : écrire ou afficher et copier | [`phase-1.md`](./phase-1.md) | none |
| 2   | Mythic : tirer sans page | [`phase-2.md`](./phase-2.md) | 1 |

## External sources

| URL | What it settled |
| --- | --------------- |
| https://tiptap.dev/docs/editor/api/utilities/html | `generateHTML(doc, extensions)` (et `generateText`) rendent un document JSON sans éditeur ; vérifié exportés par `@tiptap/core` 3.32.2 (`node_modules/@tiptap/core/dist/index.d.ts`) : aucune dépendance de plus |
| https://developer.mozilla.org/docs/Web/API/Clipboard/write | `navigator.clipboard.write([new ClipboardItem(...)])` pose `text/html` et `text/plain` ; contexte sûr requis (localhost l'est) |

## Impact

| Category | Verdict and evidence | Handled in |
| -------- | -------------------- | ---------- |
| Runtime | Unverified : l'écriture au presse-papiers arrive après l'appel serveur (`await`), donc hors du geste du clic ; Firefox et Safari exigent une activation récente, Chrome le focus du document. Repli : bouton « Copier » dans le dialogue, et message si la copie automatique échoue | 1 |
| Regression | Medium : `vtt-web` n'a pas de test automatisé (`testing.md`) ; l'écriture dans une page ouverte (curseur, fin de page verrouillée, regroupement d'annulation) doit rester identique, vérifiée au navigateur sur 5174/8081 ; pas de phase filet (rien à verrouiller par test) | 1, 2 |
| Architecture | Le contrat `NotebookContext` passe de `hasOpenPage` + `writeToOpenPage` (refus sans page) à `writeResult(blocks)` : le cœur choisit la page ou le dialogue ; les modules ne testent plus la page | 1, 2 |
| Documentation | `architecture.md` (contrat `NotebookContext`, panneaux Mythic et Oracle « refusé sans page »), `project-brief.md` (Oracle Mythic et générique « refusé sans page »), `codebase-map.md` (`ResultDialog`, `notebook/clipboard.ts`) | 1, 2 |

## Rules

| Rule | Why it applies |
| ---- | -------------- |
| Le cœur n'importe jamais un module (`architecture.md`) | Le dialogue et la copie vivent dans le cœur, derrière `NotebookContext` ; les modules ne font qu'envoyer des blocs |
| Un module n'écrit que des nœuds standard (`notebookModule.ts`) | Ces mêmes nœuds se rendent en HTML pour le presse-papiers et dans le dialogue |
| Tests navigateur sur 8081/5174 uniquement (`testing.md`) | Ne jamais toucher 8080/5173 du joueur |
| Code et messages en français, identifiants en anglais (`project-brief.md`) | Nouveaux textes du dialogue |

## Decisions

| Decision | Why |
| -------- | --- |
| Le repli (dialogue + copie) est dans le cœur, pas dans chaque module | Un seul comportement pour tout module présent et futur ; les modules perdent leurs cinq refus « Aucune page ouverte » |
| Le presse-papiers reçoit `text/html` (rendu Tiptap) et `text/plain` | Coller dans une page du Carnet rend citation, gras et liens `[[…]]` (`parseHTML` de `pageLink` lit `a[data-page-link]`) ; ailleurs, du texte lisible |
| Carnet vide (aucune page) hors périmètre | Il n'a pas de colonne de modules ; tout module actif crée ses pages au `prepare`, donc un Carnet avec module n'est vide que si `prepare` échoue |
