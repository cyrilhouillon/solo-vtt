# Run

## Phase 1 · Scénario de l'epic en test du Carnet

- Date: 2026-11-05
- Validated on: df698cf

- [pass] Task 1 — `mvn -q test` passe avant l'ajout — `mvn -q test` à la planification : exit 0, 314 tests, 0 échec.
- [pass] Task 2 — `CarnetDeBoutEnBoutTest` passe ; il échoue si un renommage réécrit une page citante, si la restauration perd un lien entrant ou si la relecture diffère — `mvn -q test` : exit 0, 316 tests, 0 échec ; `Tests run: 2, Failures: 0, Errors: 0 -- in solovtt.server.CarnetDeBoutEnBoutTest`. Le test compare le contenu de Session 5 avant et après renommage, `PageLinks.citing` après restauration, puis `list`, `tabs`, `trash`, recherche et `NotebookResource.get()` relus par un nouveau `FileCampaignStore` ; un lot laissé à la corbeille survit au redémarrage, `tabs()` garde l'id d'Ismark et `get()` l'omet.
