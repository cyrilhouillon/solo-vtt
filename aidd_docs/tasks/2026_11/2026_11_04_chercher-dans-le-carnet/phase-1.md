---
status: done
---

# Instruction: le serveur cherche dans les pages

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   └── codebase-map.md ✏️
└── vtt-server/src/
    ├── main/java/solovtt/server/
    │   ├── Dtos.java ✏️
    │   ├── NotebookResource.java ✏️
    │   ├── NotebookStore.java ✏️
    │   └── PageSearch.java ✅
    └── test/java/solovtt/server/
        ├── NotebookApiTest.java ✏️
        └── PageSearchTest.java ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Campagne neuve ; pages « Évêque Aldric » (texte « Il garde la crypte »), « Session 1 » (« Nous rencontrons l'évêque à l'auberge du Gué » dans une liste imbriquée), « Lore » (sans contenu) | Carnet de trois pages | api |
| Happy path   | `GET …/notebook/search?q=eveque` | 200 : Évêque Aldric (titre) puis Session 1, chacune avec id, titre, parent et extrait en morceaux, « évêque » marqué trouvé | api |
| Happy path   | `q=EVEQUE auberge` | Session 1 seule (les deux mots) ; « évêque » et « auberge » marqués | api |
| Happy path   | `q=crypte` | Évêque Aldric, extrait « Il garde la » puis « crypte » marqué | api |
| Happy path   | Session 1 contient un `pageLink` sans `label` vers Évêque Aldric ; `q=aldric` | Session 1 trouvée par le titre affiché du lien | api |
| Edge case    | Texte long, mot au milieu | Extrait coupé aux espaces, « … » au début et à la fin, ~120 caractères | system |
| Edge case    | Mot trouvé dans le titre seulement | Extrait = début du texte, sans morceau marqué ; aucun morceau pour une page sans contenu | system |
| Edge case    | `q=dragon` | 200, `[]` | api |
| Edge case    | `q` absent ou blanc | 400 | api |
| Edge case    | Contenu non JSON (test unitaire) | Page cherchée sur son titre seul, les autres listées | system |
| Edge case    | Deux campagnes, le mot dans l'autre | Absent des résultats | api |

## Tasks to do

### `1)` Le contrat du port

> La recherche est une boîte noire de `NotebookStore`, comme la persistance.

1. `NotebookStore` : records `SearchHit(Page page, List<Fragment> excerpt)` et `Fragment(String text, boolean match)` ; méthode `default List<SearchHit> search(String query)` qui délègue à `PageSearch.search(list(), query)`. Javadoc du contrat : chaque mot dans le titre ou le texte, sans casse ni accents ; pages au titre trouvé d'abord ; extrait autour du premier mot trouvé dans le texte ; un adaptateur peut la remplacer (index, base).
2. `inMemory` et `JsonFileNotebookStore` héritent de la méthode, sans changement.

### `2)` L'implémentation par défaut

> `PageSearch`, pure, relit tout à chaque appel.

1. Créer `PageSearch` : `fold(String)` caractère par caractère (NFD, retrait de `\p{M}`, minuscule `Locale.ROOT` ; garde le caractère d'origine en minuscule si le pli n'en donne pas un seul).
2. Texte d'une page : parcourt le document comme `PageLinks.contains`, concatène les nœuds `text` et, pour un `pageLink`, son `label` sinon le titre de la cible lu dans la liste, un espace entre deux nœuds de bloc ; contenu `null` → `""` ; illisible → `""` et avertissement journalisé.
3. `search(List<Page> pages, String query)` : mots = requête pliée coupée aux blancs ; garde les pages où chaque mot est dans `fold(titre)` ou `fold(texte)` ; tri : titre contenant un mot d'abord, puis l'ordre reçu (`list()` trie déjà par titre).
4. Extrait : depuis la première position, dans le texte plié, d'un des mots ; ~40 caractères avant, ~80 après, élargis jusqu'à l'espace, « … » si coupé ; sans mot dans le texte, les 120 premiers caractères. Découpé en `Fragment` : chaque occurrence d'un mot dans l'extrait est un morceau `match`.
5. `PageSearchTest` : pliage (« Évêque » → « eveque », « Œ » reste un caractère), ET des mots, ordre titre d'abord, extrait coupé et « … », morceaux marqués, lien avec et sans `label`, liste imbriquée, contenu illisible.

### `3)` Exposer la route

> `GET /api/campaigns/{id}/notebook/search?q=` rend les résultats, sans rien savoir du mécanisme.

1. `Dtos.SearchResultDto(id, title, parentId, List<FragmentDto> excerpt)`, `FragmentDto(text, match)`.
2. `NotebookResource.search(@QueryParam("q") String q)` : `q` nul ou blanc → `IllegalArgumentException` (400) ; sinon `notebook.search(q)` mappé en DTO. Aucune référence à `PageSearch`.
3. `@APIResponse` 200 (tableau de `SearchResultDto`) et 400.
4. `NotebookApiTest` : scénarios API ci-dessus.

### `4)` Documenter

> La mémoire dit comment le serveur cherche.

1. `architecture.md` : `NotebookStore.search`, boîte noire remplaçable par un adaptateur ; route `…/notebook/search` ; implémentation par défaut (texte des nœuds `text` et des liens, pliage, ET, titre d'abord, extrait en morceaux, sans index).
2. `codebase-map.md` : `NotebookStore.search`, `PageSearch`, route `notebook/search`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1    | `NotebookResource` n'appelle que `notebook.search` ; `inMemory` et `JsonFileNotebookStore` cherchent sans code propre. |
| 2    | `PageSearchTest` passe : « eveque » trouve « Évêque », tous les mots sont exigés, les titres viennent d'abord, l'extrait entoure le mot, porte « … » quand il est coupé et marque les mots trouvés, un contenu illisible ne fait rien échouer. |
| 3    | `NotebookApiTest` passe : 200 et les résultats attendus, `[]` sans résultat, 400 pour une requête vide, rien d'une autre campagne ; `mvn -q test` vert. |
| 4    | `architecture.md` et `codebase-map.md` décrivent le contrat du port et `PageSearch`. |
