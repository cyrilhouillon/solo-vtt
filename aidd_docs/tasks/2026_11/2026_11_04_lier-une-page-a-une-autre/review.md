# Review: lier une page du Carnet à une autre

- Rounds: 1
- Started: 2026-11-04

## Round 1 · r-7c3e

- Date: 2026-11-04
- By: Cyril Houillon
- Diff: `main...78b9d83`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 0/6 met, 0 unmet, 6 unverified at runtime, 0 out of the diff

### Criteria

- Unverified at runtime: Phase 1 · 1 — lien au titre actuel, suit renommage et déplacement, mort si cible absente, ouvert au clic et à Entrée verrouillé, survit au rechargement et au redémarrage — le code le porte (`pageLink.ts:14-26`, `PageLinkView.vue:15-27`, titre jamais stocké), mais l'activation native d'Entrée et la reprise ne se voient qu'au navigateur ; aucun parcours enregistré
- Unverified at runtime: Phase 1 · 2 — `[[` propose, Entrée insère, Échap rend le texte, Ctrl+Z défait — `Suggestion` (`pageLink.ts:80-96`, `insertContentAt` en une commande), `PageLinkList.vue:29-42` ; comportement du plugin à observer au navigateur
- Unverified at runtime: Phase 1 · 3 — mémoire et `DESIGN.md` décrivent nœud, format et dépendances (fait : `architecture.md`, `codebase-map.md`, `package.md`, `DESIGN.md`) ; `npm run build` sans exécution enregistrée sur ce head
- Unverified at runtime: Phase 2 · 1 — bulle sur lien sélectionné, texte choisi qui survit au renommage et au rechargement, vide rend le titre, Ctrl+Z — `PageLinkBubble.vue:18-57` ; affichage de la bulle et parcours clavier à voir au navigateur
- Unverified at runtime: Phase 2 · 2 — Ctrl+clic et « Ouvrir » ouvrent après l'enregistrement, lien mort inerte — `PageLinkView.vue:22-27`, `PageLinkBubble.vue:61-63`, `NotebookScreen.vue` `@open="open"` ; attente de l'enregistrement à observer
- Unverified at runtime: Phase 2 · 3 — mémoire, `DESIGN.md`, story `done`, plan `implemented` (faits) ; `npm run build` sans exécution enregistrée sur ce head

### Findings

- `vtt-web/src/notebook/pageLink.ts:65` : le repli `?? 'page introuvable'` est répété dans `renderHTML`, `renderText` (`:70`) et `PageLinkView.vue:20` → le porter dans `linkText` (ou une fonction `shownText`) qui rend toujours une chaîne — 🟢 minor, code-health
- `vtt-web/src/notebook/pageLink.ts:66` : `mergeAttributes(HTMLAttributes)` avec un seul argument ne fusionne rien → passer `HTMLAttributes` tel quel — 🟢 minor, code-health
- `vtt-web/src/components/PageLinkView.vue:35` : page verrouillée, le clic du milieu sur `href="#page-<id>"` déclenche `auxclick`, pas `click`, et ouvre un onglet sur un fragment que l'application ne lit pas → `@auxclick.prevent`, ou accepter et le noter — 🟢 minor, frontend
