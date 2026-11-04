---
objective: "Le joueur tape un ou plusieurs mots dans le Carnet et voit, sans casse ni accents, les pages de la campagne ouverte dont le titre ou le texte les contient, chacune avec un extrait autour du mot, qu'il ouvre au clic ; sans résultat, le Carnet le dit."
status: implemented
---

# Plan: chercher dans le Carnet

Source: aidd_docs/backlog/stories/retrouver-une-page-du-carnet-par-la-recherche.md (epic `aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md`, order 5)

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Le serveur cherche dans les pages | [`phase-1.md`](./phase-1.md) | none |
| 2   | Le Carnet montre les résultats | [`phase-2.md`](./phase-2.md) | 1 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/Normalizer.html | `Normalizer.normalize(s, Form.NFD)` sépare lettre et diacritique ; la classe `\p{M}` les retire (« Évêque » → « Eveque »). Le pliage reste dans l'implémentation serveur ; le client reçoit les morceaux surlignés et ne plie rien. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Aucune dépendance nouvelle : Jackson (déjà dans `vtt-server`, utilisé par `PageLinks`) lit le contenu ; `java.text.Normalizer` est du JDK ; le client n'ajoute qu'un appel `fetch`. | none |
| Regression | Faible. Serveur : une route et une classe ajoutées ; `NotebookResource` (lu) gagne une méthode ; `NotebookStore` (lu) gagne une méthode `default`, héritée telle quelle par `inMemory` et `JsonFileNotebookStore` ; `list()` inchangé. Client : `NotebookScreen.vue` (lu) gagne un champ et une liste qui remplace l'arbre tant qu'une recherche est en cours ; `open`, l'enregistrement, l'arbre et `CitingPages` ne changent pas. Couvert par `NotebookApiTest` et `mvn -q test` ; client par `npm run build` et parcours navigateur sur 8081 / 5174. Pas de phase filet : aucun comportement existant ne change. | 1, 2 |
| Architecture | Départ : le Carnet passe par le port `NotebookStore` (lu), le serveur ne lit le contenu que pour les nœuds `pageLink` (`PageLinks`). Cible : le port gagne `search(query)`, boîte noire comme la persistance ; son implémentation par défaut (`PageSearch`) lit les nœuds `text` et le texte affiché des liens, à chaque appel, sur `list()`, sans index. `NotebookResource` n'appelle que `notebook.search`. | 1 |
| Documentation | `architecture.md` (route `…/notebook/search`, texte tiré du document, pliage sans casse ni accents, extrait, pas d'index ; résultats à la place de l'arbre), `codebase-map.md` (`NotebookStore.search`, `PageSearch`, route, `SearchResults.vue`), `project-brief.md` (fonctionnalité Carnet), `DESIGN.md` (champ de recherche et résultats dans le panneau Pages). Story : `status: done`. Plan : `status: implemented`. | 1, 2 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Lire le Carnet ne rejoue pas la partie : `NotebookResource` est construit sur le `CampaignStore` seul (`architecture.md`) | La route vit dans `NotebookResource`, sous la campagne ouverte : la recherche ne voit que ses pages. |
| Une fiche illisible est ignorée, jamais écrasée (`architecture.md`) | Un contenu qui ne se lit pas est cherché sur son titre seul, sans faire échouer la recherche. |
| Le stockage passe par des ports, l'adaptateur porte le mécanisme (`architecture.md` : « Une base de données remplacera ce port le jour où l'application sera hébergée ») | La recherche suit le même chemin : contrat sur `NotebookStore`, mécanisme dans l'implémentation. |
| `ErrorMapper` rend 400 pour une `IllegalArgumentException` (`architecture.md`) | Une requête vide est refusée en 400. |
| Le lien affiche son `label`, sinon le titre actuel de sa cible (`architecture.md`, `links.ts:5`) | Le texte cherché d'une page est celui que lit le joueur, lien compris. |
| Toute nouvelle règle arrive avec son test ; tests d'API par campagne neuve (`testing.md`) | `PageSearchTest` et `NotebookApiTest`. |
| Client sans tests automatisés ; jamais de test navigateur sur 8080 / 5173 (`testing.md`) | Parcours sur les serveurs de test 8081 / 5174, campagnes dans le scratchpad. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| La recherche est une boîte noire derrière le port `NotebookStore` : `search(query)` → résultats (page, extrait découpé en morceaux trouvés ou non). La ressource et le client ne connaissent que ce contrat. L'implémentation par défaut de l'interface (`PageSearch`, partagée par `inMemory` et `JsonFileNotebookStore`) relit `list()` à chaque appel, sans index ; un moteur d'indexation ou une base la remplacera dans son adaptateur, sans toucher ni la ressource ni le client | Même principe que la persistance : l'adaptateur qui voit les écritures est le seul à pouvoir tenir un index à jour. Le port reste un seul objet par campagne : aucun câblage nouveau dans `CampaignStore`, `CampaignResource` ou `FileCampaignStore`. |
| L'extrait arrive au client déjà découpé (`[{ text, match }]`) : le serveur dit ce qui a été trouvé | Le client ne replie rien lui-même : un moteur qui trouverait « auberges » pour « auberge » surlignerait juste, sans changer le client. |
| Contrat du port : une page correspond si chaque mot de la requête apparaît dans son titre ou son texte (ET), sans casse ni accents. L'implémentation par défaut plie caractère par caractère (NFD, sans `\p{M}`, minuscule ; un caractère qui ne se plie pas en un seul reste tel quel) pour garder les positions de l'extrait | « Bertrand auberge » doit resserrer, pas élargir. Le pliage un pour un permet de découper l'extrait sans décalage. |
| Résultats : pages dont le titre contient un mot d'abord, puis les autres, chaque groupe trié par titre | Chercher un PNJ par son nom met sa page en tête, devant les sessions qui le citent. |
| Le texte d'une page est la suite de ses nœuds `text` et du texte affiché de ses liens, un espace entre deux blocs ; l'extrait (~120 caractères) part du premier mot trouvé dans ce texte, coupé aux espaces, « … » aux bords ; sans mot dans le texte, le début du texte | C'est ce que le joueur a lu ; un lien renommé se trouve sous son nouveau nom. |
| Les résultats remplacent l'arbre dans le panneau Pages tant que le champ n'est pas vide ; Échap ou « Effacer » rend l'arbre ; ouvrir un résultat garde la recherche | Le panneau est déjà la navigation du Carnet ; garder la liste permet d'essayer plusieurs résultats. |
