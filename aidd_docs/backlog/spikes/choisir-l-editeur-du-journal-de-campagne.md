---
type: spike
status: resolved
source: aidd_docs/tasks/2026_11/2026_11_04_journal-de-campagne/brainstorm.md
parents:
  - aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md
---

# Spike: choisir l'éditeur du journal de campagne

## Question

Quelle bibliothèque d'édition (ou petit assemblage), compatible Vue 3, couvre l'édition riche au fil du texte, la sauvegarde en Markdown augmenté de nos balises, les liens vers une page par son identifiant (extensibles en `[[…]]` créés à la volée), les images, l'enregistrement automatique et l'historique des versions ou une corbeille ?

## Decision

Conception de l'epic « Journal de campagne » : choix de l'éditeur, format stocké, et historique ou corbeille en v1, qui fixe la règle de suppression d'une page à sous-pages (confirmer puis tout supprimer, ou refuser tant qu'elle a des enfants).

## Bounds

- Evidence needed: comparaison de 3 à 4 candidats (Tiptap, Milkdown, BlockNote, Lexical…) sur ces besoins, d'après leur documentation et leur dépôt (licence, maintenance, poids, intégration Vue) ; un essai jetable, hors dépôt, sur le candidat retenu, qui vérifie l'aller-retour Markdown d'un lien par identifiant.
- Stop when: un candidat couvre la v1 avec l'aller-retour Markdown vérifié, ou aucun ne le fait et l'écart est chiffré. Pour l'historique : savoir s'il vient de la bibliothèque ou est à notre charge côté serveur, sans le concevoir.

## Investigation

| Attempt | Evidence | Result |
| ------- | -------- | ------ |
| Métadonnées npm des candidats (2026-11-04) | `npm view` : `@tiptap/core`, `@tiptap/vue-3`, `@tiptap/markdown` 3.32.2 MIT ; `@milkdown/kit`, `@milkdown/vue` 7.23.0 MIT ; `@blocknote/core` 0.57.0 MPL-2.0 (pairs Yjs) ; `lexical` 0.53.0 MIT. Tous publiés en octobre ou novembre 2026. | Les quatre sont maintenus. Tiptap et Milkdown ont une liaison Vue 3 officielle. |
| Documentation BlockNote | [docs](https://www.blocknotejs.org/docs), [export Markdown](https://www.blocknotejs.org/docs/features/export/markdown), [issue #1938](https://github.com/TypeCellOS/BlockNote/issues/1938) | Écarté : éditeur React (ou JS nu), pas de Vue ; Markdown « lossy » à l'export comme à l'import, le format sans perte est son JSON. |
| Liaisons Vue de Lexical | [wobsoriano/lexical-vue](https://github.com/wobsoriano/lexical-vue), [gridigor/vue-lexical](https://github.com/gridigor/vue-lexical) | Écarté : seules des liaisons Vue communautaires, l'officielle est React. |
| Documentation Markdown de Tiptap | [intégrer le Markdown dans une extension](https://tiptap.dev/docs/editor/markdown/guides/integrate-markdown-in-your-extension.md), [tokenizer sur mesure](https://tiptap.dev/docs/editor/markdown/advanced-usage/custom-tokenizer.md) | Un nœud déclare `markdownTokenizer`, `parseMarkdown`, `renderMarkdown` ; syntaxe libre (MarkedJS). La sérialisation sur mesure est marquée bêta. |
| Documentation Milkdown | [vue3-milkdown-sample](https://github.com/yumamama00/vue3-milkdown-sample), [milkdown-mentions-plugin](https://github.com/zgrybus/milkdown-mentions-plugin) | Viable (Markdown natif via remark, nœud = `$node` + `$remark`), mais aucun plugin de lien wiki trouvé et un écosystème plus petit. Non essayé. |
| Historique des versions chez Tiptap | [Snapshot](https://tiptap.dev/docs/collaboration/documents/snapshot.md), [Integrate snapshots](https://tiptap.dev/api/extensions/collaboration-history) | Extension Pro (registre privé), liée à Yjs et au service de collaboration Tiptap ; l'extension MIT `history` n'est que l'annuler/rétablir. Aucun candidat libre ne fournit un historique ou une corbeille persistants. |
| Essai jetable Tiptap 3.32.2 + `@tiptap/markdown` + StarterKit + Image, sous jsdom (scratchpad, hors dépôt) | Nœud inline atomique `pageLink` (`[[<id>]]`, `[[<id>\|alias]]`), titre affiché lu dans un index des pages ; source avec titre, gras, italique, citation, liste, image et trois liens | Aller-retour Markdown identique (`true`). Changer le titre dans l'index renomme tous les liens sans toucher au texte. Les ids des liens sortants se lisent en parcourant le document. |
| Poids (esbuild, minifié) | core + StarterKit + Markdown + Image | 467 Ko, 147 Ko gzip ; chargeable à part, comme les dés. `@tiptap/suggestion` (pour `[[` à la volée) : MIT, 3.32.2. |

## Outcome

- Result: Tiptap 3 (MIT, `@tiptap/vue-3`) couvre la v1 : édition riche, liens par identifiant qui suivent le renommage, liens sortants lisibles pour tirer les liens entrants, images, `[[` à la volée possible ensuite (`@tiptap/suggestion`). L'enregistrement automatique (mise à jour de l'éditeur, envoi différé) et la corbeille sont à notre charge, côté client et serveur. Le Markdown n'est pas exigé comme format stocké : le JSON de Tiptap, sans perte, convient aussi et évite l'API Markdown bêta, au prix d'un format lié au schéma de l'éditeur. Décision produit prise à la conclusion : l'annuler/rétablir de l'éditeur suffit contre une modification malheureuse ; une corbeille côté serveur protège la suppression, donc supprimer une page à sous-pages se confirme (« et ses 2 sous-pages ») puis envoie le tout à la corbeille. Pas d'historique des versions en v1.
- Confidence: haute sur l'éditeur et le lien par identifiant (essai vérifié, aller-retour identique) ; moyenne sur la stabilité de la sérialisation Markdown sur mesure, marquée bêta.
- Remaining uncertainty: format stocké (Markdown augmenté ou JSON Tiptap), à trancher à la conception ; Milkdown non essayé.

## Follow-up

Rédiger l'epic « Journal de campagne » (`/aidd-pm:07-epic`) à partir du brainstorm `aidd_docs/tasks/2026_11/2026_11_04_journal-de-campagne/brainstorm.md` et de ce spike : éditeur Tiptap 3, corbeille en v1, suppression confirmée qui envoie la page et ses sous-pages à la corbeille.
