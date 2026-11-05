# Review: retrouver un écran par son adresse

- Rounds: 1
- Started: 2026-11-05

## Round 1 · r-2d38

- Date: 2026-11-05
- By: Cyril Houillon
- Diff: `main...bce4028` plus the working tree
- Axes: code, functional, relevancy
- Verdict: changes-requested
- Score: 7/7 met, 0 unmet, 0 unverified at runtime, 0 out of the diff

### Criteria

- [x] Phase 1 · Task 1 — `npm run build` passe ; une adresse inconnue devient `/` ; une campagne inconnue mène à l'accueil — `run.md`, Phase 1 (validé sur f3016c0, seul du Markdown change depuis aecd618) ; `vtt-web/src/router.ts:35`, `vtt-web/src/router.ts:39-49`
- [x] Phase 1 · Task 2 — Accueil, Carnet et combat ont chacun leur adresse ; recharger rouvre le même écran — `run.md`, Phase 1 ; `vtt-web/src/router.ts:21-33`, `vtt-web/src/App.vue:9-15`
- [x] Phase 1 · Task 3 — Précédent / Suivant repassent par les pages visitées et attendent l'enregistrement ; page inconnue sans bandeau ; campagne sans page : onglet actif ; gestes existants — `run.md`, Phase 1 ; `vtt-web/src/components/NotebookScreen.vue:263-286`, `vtt-web/src/components/NotebookScreen.vue:370`
- [x] Phase 1 · Task 4 — Les fichiers de mémoire décrivent les routes telles que codées — `aidd_docs/memory/architecture.md` (adresses, `router.beforeEach`, `writeAddress`, gardes), `codebase-map.md`, `package.md`, `project-brief.md`
- [x] Phase 2 · Task 1 — Clic du milieu sur une ligne : nouvel onglet ; clic simple, clavier et glisser inchangés ; build — `run.md`, Phase 2 (validé sur aecd618) ; `vtt-web/src/components/PageBranch.vue:84-93`
- [x] Phase 2 · Task 2 — Clic du milieu sur un lien vivant, verrouillé ou non : nouvel onglet ; lien mort : rien — `run.md`, Phase 2 ; `vtt-web/src/components/PageLinkView.vue:29-35`, `vtt-web/src/components/PageLinkView.vue:40-50`
- [x] Phase 2 · Task 3 — Mémoire, story et plan à jour — story `status: done`, plan `status: implemented`, `architecture.md` (clic du milieu)

### Findings

- `vtt-web/src/components/NotebookScreen.vue:231` : `tryShow` avale toute erreur, pas seulement une page illisible ; or `open` (arbre, onglet, lien, recherche, citante) passe désormais par le `watch` de `pageId`, donc par `tryShow` : serveur injoignable ou 500, le clic ne fait rien et l'adresse revient en arrière, sans bandeau (avant : `call(() => show(...))` disait l'erreur). Même silence au montage (`:370`), qui retombe sur l'onglet actif → ne taire que `ApiError` au statut 404 et relancer le reste, que `call` écrit au bandeau — 🟡 major, error-handling
- `vtt-web/src/components/GameScreen.vue:43` : le commentaire « Retour à l'accueil, ou au Carnet de la campagne. » décrivait l'`emit` supprimé et coiffe maintenant `const router = useRouter()` → le retirer — 🟢 minor, standards
