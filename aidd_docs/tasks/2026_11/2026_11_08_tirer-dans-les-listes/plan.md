---
objective: "Un événement aléatoire dont le focus vise un PNJ ou un fil tire une ligne de la liste Personnages ou Fils du Carnet selon la règle de Mythic GME 2e (sections actives, d4 à d10 puis d10), et l'entrée tirée (lien `[[…]]` compris), « Choisir » sur une ligne vide, ou « Contexte actuel » pour une liste vide s'écrit avec l'événement dans la page ouverte."
status: in-progress
---

# Plan: laisser un événement aléatoire tirer dans ses listes

Source: aidd_docs/backlog/stories/laisser-un-evenement-aleatoire-tirer-dans-ses-listes.md (epic `aidd_docs/backlog/epics/jouer-sa-campagne-avec-mythic-gme-depuis-le-carnet.md`, order 4 ; dépend de « poser une question à l'oracle » et « tenir ses fils et personnages », toutes deux `done`).

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Le moteur Mythic tire une ligne dans une liste | [`phase-1.md`](./phase-1.md) | none |
| 2   | Le serveur Mythic lit les listes du Carnet et rend l'entrée tirée | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Le panneau écrit l'entrée tirée avec l'événement | [`phase-3.md`](./phase-3.md) | 2 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://wordmillgames.itch.io/mythic-game-master-emulator-second-edition (copie locale de l'utilisateur, `documents/`, hors dépôt) | p. 38-41 : Action d'un PNJ, PNJ négatif, PNJ positif tirent dans Personnages ; Vers un fil, Loin d'un fil, Clore un fil dans Fils ; PJ négatif / positif visent un PJ, Nouveau PNJ aucune liste. p. 44 « Empty List » : liste visée vide, le focus devient « Contexte actuel ». p. 44-46 : 25 lignes en 5 sections de 5, active dès qu'on y écrit ; 1-5 éléments sans dé de section, 6-10 d4, 11-15 d6, 16-20 d8, 21-25 d10, lus par paires, puis d10 par paires pour la ligne ; ligne vide : « Choisir » (élément le plus logique, ou relancer). p. 112-113 : une entrée répétée pèse plus, jusqu'à trois fois (tenue par le joueur) ; liste pleine : le joueur la nettoie. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Aucune dépendance nouvelle. `ask` lit en plus le contenu de deux pages du Carnet par question (port `NotebookPages`) ; `JsonFileNotebookStore.list()` relit déjà toutes les fiches à chaque recherche, coût jugé négligeable : non mesuré. Sérialisation par Quarkus REST d'un `JsonNode` Jackson dans un DTO (nœuds en ligne de l'entrée) : non vérifié dans ce dépôt, couvert par le test d'API de la phase 2. | 2 |
| Regression | Faible. `FateQuestion.ask` gagne un paramètre (listes) : 12 appels dans `FateQuestionTest`, 1 dans `MythicResource` (l. 146) (lus) ; ordre des dés inchangé quand le focus ne vise aucune liste, les dés de liste viennent après ceux du sens. `RandomEvent` gagne un champ (5 constructions dans `FateQuestionTest`, 1 dans `FateQuestion` l. 24, lues). `NotebookPages` gagne deux méthodes : implémentée par `NotebookPagesAdapter` et `PagesDeTest` (seuls implémenteurs, `grep "implements NotebookPages"`). `RandomEventDto` gagne un champ facultatif ; le client le lit dans `result.ts` et `MythicPanel.vue` (l. 204-209). Pas de phase filet : `FateQuestionTest`, `EventFocusTest`, `MythicResourceTest`, `NotebookApiTest` verrouillent l'existant. | 1, 2, 3 |
| Architecture | Départ : un module crée et liste les racines de sa section (`sectionRoots`, `createSectionRoot`), sans en lire le contenu. Cible : `NotebookPages.sectionRootContent(campaignId, moduleId, pageId)` rend le document d'une racine vivante de la section du module (vide sinon), et `pageTitle(campaignId, pageId)` le titre d'une page vivante, pour le texte d'un lien sans `label` (le nœud `pageLink` ne garde que `pageId` et `label`, `vtt-web/src/notebook/pageLink.ts` l. 12) ; Mythic lit lui-même les lignes de ses listes dans le document Tiptap. Le moteur ne connaît que des lignes écrites ou vides ; le serveur y joint le contenu. Le cœur ne nomme toujours pas Mythic. | 1, 2 |
| Documentation | `architecture.md` (lecture d'une racine de section, lignes d'une liste, tirage et ordre des dés, rendu de l'entrée), `codebase-map.md` (`AdventureList`, `ListDraw`, méthode du port), `project-brief.md` (un événement tire dans Fils / Personnages), `.gitignore` (`documents/`, livre commercial). Story `status: done` ; plan `status: implemented`. `DESIGN.md` inchangé : aucune forme nouvelle, une ligne de plus dans la citation et dans le dernier résultat du panneau. | 1, 2, 3 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Le livre de Mythic est commercial : jamais dans le dépôt ; les tests n'emploient que des mots inventés (`architecture.md`) | Le PDF est dans `documents/`, non ignoré par git (`git status` le montre `?? documents/`) : `.gitignore` le couvre dès la phase 1 ; libellés français écrits par nous. |
| Seuls les assemblages nomment un module (`architecture.md`) | La nouvelle méthode du port reste générique (id de module) ; `grep -ri mythic vtt-server/src vtt-web/src` reste vide. |
| Moteurs sans dépendance, hasard injecté (`Dice`), dés scriptés en test (`testing.md`) | Le tirage dans une liste vit dans `mythic-engine`, en Java pur. |
| Le serveur tire, le client écrit dans la page en nœuds standard (`architecture.md`, oracle et sens) | L'entrée tirée arrive en nœuds en ligne et s'écrit par `writeToOpenPage`. |
| Serveurs de test 8081 / 5174, chemins dans le scratchpad (`testing.md`) | Parcours navigateur de la phase 3. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Une ligne de liste = un élément de premier niveau d'une liste (`bulletList` ou `orderedList`) de premier niveau de la page, dans l'ordre du document ; son contenu = son premier paragraphe ; une sous-liste dans un élément est une note, pas une ligne ; un élément sans texte visible (ni texte non blanc, ni lien) est une ligne vide | « Une entrée est un élément de liste » (story 3) ; une ligne vide garde sa place, comme une ligne barrée de la feuille. |
| Lignes actives : jusqu'à la dernière ligne écrite parmi les 25 premières ; au-delà de 25 lignes, rien n'est tiré et l'écrit le signale (« liste pleine : à nettoyer ») | Feuille de 25 lignes du livre ; le nettoyage reste au joueur (p. 113). |
| Ligne vide : « Choisir » s'écrit (élément le plus logique, ou relancer) ; l'application ne relance pas d'elle-même | Le livre laisse le choix au joueur. |
| Liste vide ou absente (page de liste supprimée de la section ou jamais créée) : le focus écrit devient « Contexte actuel », avec le focus tiré et la raison | Règle « Empty List » ; une campagne jamais ouverte au Carnet n'a pas de listes (écart noté à la story 3). |
| L'entrée revient au client en nœuds en ligne (texte avec marques, liens `pageLink`) et en texte brut ; la page reçoit les nœuds, si bien qu'un personnage lié reste un lien vivant | Un personnage peut être un lien `[[…]]` (story 3). |
| Dés : question, focus, sens (si tables), puis section (si plus d'une section active) et ligne | Garde l'ordre des tests existants. |
| Le plafond de trois entrées par élément n'est pas imposé | Tenue de liste du joueur (p. 112) ; la story ne demande que le poids. |
