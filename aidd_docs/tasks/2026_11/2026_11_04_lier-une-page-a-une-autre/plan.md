---
objective: "En écrivant une page du Carnet, le joueur tape [[ et choisit une page existante par son titre : un lien s'insère, affiche le titre actuel de sa cible (ou un texte choisi), ouvre la cible au clic et s'affiche mort si la cible n'existe plus."
status: implemented
---

# Plan: lier une page du Carnet à une autre

Source: aidd_docs/backlog/stories/lier-une-page-du-carnet-a-une-autre.md (epic `aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md`) ; choix du joueur : `[[` ouvre le choix de la page ; page déverrouillée, un clic sur un lien le sélectionne et montre une bulle (Ouvrir, « Texte affiché »), Ctrl+clic ouvre ; page verrouillée, un clic ouvre.

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Insérer un lien de page et le suivre | [`phase-1.md`](./phase-1.md) | none |
| 2   | Changer le texte d'un lien depuis sa bulle | [`phase-2.md`](./phase-2.md) | 1 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://tiptap.dev/docs/editor/api/utilities/suggestion | `@tiptap/suggestion` ouvre une liste sur un caractère déclencheur et rend la main par `items`, `command`, `render` (`onStart`, `onUpdate`, `onKeyDown`, `onExit`, `clientRect`). Vérifié dans le source 3.32.2 (`findSuggestionMatch.ts`) : `char` est échappé en expression régulière, donc `[[` (deux caractères) fonctionne ; `allowSpaces: true` garde la requête au-delà d'une espace (« Session 5 »). Pairs : `@tiptap/core` et `@tiptap/pm` 3.32.2, `@floating-ui/dom` ^1 (déjà installé, 1.8.0). |
| https://tiptap.dev/docs/editor/extensions/functionality/bubble-menu | `BubbleMenu` (`@tiptap/vue-3/menus`, extension `@tiptap/extension-bubble-menu` 3.32.2 déjà installée) s'affiche selon `shouldShow`, ici une `NodeSelection` sur un lien. |
| https://tiptap.dev/docs/editor/extensions/custom-extensions/node-views/vue | `VueNodeViewRenderer` rend un nœud par un composant Vue (`node`, `updateAttributes`, `selected`, `extension`) ; `stopEvent` laisse au composant les événements que ProseMirror ignorerait sinon. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Nouvelle dépendance `@tiptap/suggestion` 3.32.2 (même version que Tiptap) ; `@floating-ui/dom` 1.8.0, déjà tiré en pair de `@tiptap/vue-3`, est figé dans `package.json` puisque la bulle s'en sert. Aucune pour le serveur : le contenu d'une page reste un document JSON qu'il garde sans l'interpréter (`architecture.md`), un nœud `pageLink` y passe tel quel. | 1, 2 |
| Regression | Faible à moyen. `PageEditor.vue` (lu) gagne une extension et des props ; `NotebookScreen.vue` (lu) lui passe la liste des pages et reçoit `open`. Une page sans lien ne change pas : StarterKit et l'enregistrement automatique restent tels quels. Vérifié (`prosemirror-view` `dist/index.js:3167-3168`) : une vue non éditable n'exécute pas ses `editHandlers` (`keydown`, `paste`, `drop`…), donc `handleKeyDown` du verrou n'est jamais appelé et n'avale pas Entrée sur un lien ; `stopEvent` n'est pas nécessaire, et l'intercepter au clic empêcherait ProseMirror de sélectionner le nœud en écriture (bulle de la phase 2). Client sans tests automatisés : parcours navigateur sur 8081 / 5174 ; `mvn -q test` repasse sans changement serveur. Pas de phase filet. | 1, 2 |
| Architecture | Départ : une page est un document Tiptap opaque pour le serveur, sans lien vers une autre. Cible : un nœud en ligne `pageLink` (`pageId`, `label` : `null` pour afficher le titre de la cible) ; le titre n'est jamais stocké, il est lu à l'affichage dans la liste des pages du Carnet, d'où le suivi des renommages et déplacements sans réécrire aucune page. Les stories « voir les pages qui citent une page » et « corbeille » liront ces nœuds. | 1 |
| Documentation | `architecture.md` (nœud `pageLink`, `[[`, titre lu à l'affichage, lien mort, clic selon le verrou, bulle), `codebase-map.md` (`notebook/pageLink.ts`, `PageLinkView.vue`, `PageLinkList.vue`, `PageLinkBubble.vue`), `package.md` (`@tiptap/suggestion`, `@floating-ui/dom`), `project-brief.md` (fonctionnalité Carnet), `DESIGN.md` (lien, lien mort, liste `[[`, bulle). Story : `status: done`. Plan : `status: implemented`. | 1, 2 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Dépendances confinées à `vtt-web`, aux versions figées (`package.md`) | `@tiptap/suggestion` et `@floating-ui/dom` entrent sans `^`. |
| Le verrou est purement client ; verrouillée, l'éditeur avale touches, collage et dépôt ; même mise en page verrouillée ou non (`architecture.md`, `DESIGN.md`) | Le lien s'affiche pareil dans les deux états ; seul son effet au clic change. |
| Avant d'ouvrir une autre page, le Carnet attend l'envoi et reste sur place s'il échoue (`architecture.md`) | Suivre un lien passe par `open` de `NotebookScreen`, comme un clic dans l'arbre. |
| Toute action a son chemin clavier (`architecture.md`) | Liste `[[` aux flèches, Entrée, Échap ; lien atteignable au Tab une fois verrouillée ; bulle atteignable sans souris. |
| Client sans tests automatisés ; jamais de test navigateur sur 8080 / 5173, serveur de test avec `-Dsolovtt.campaigns.path=<scratchpad>/campagnes` (`testing.md`) | Les parcours créent des pages et des liens. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Le lien est un nœud atomique en ligne `pageLink { pageId, label }`, sans titre stocké ; son texte vient de la liste des pages du Carnet, passée à l'éditeur | Renommer ou déplacer la cible ne réécrit aucune page, et un lien vers une page absente se reconnaît sans autre donnée. Contrepartie : un lien mort sans `label` ne connaît plus le titre de sa cible (« page introuvable ») ; la corbeille pourra le rendre. |
| Ce format (`type: "pageLink"`, `attrs.pageId`) est le contrat que liront les liens entrants et la corbeille | Les stories suivantes en dépendent : le changer ensuite réécrirait les pages. |
| Le serveur ne valide pas les liens | Il garde le contenu sans l'interpréter ; une cible absente est le cas « lien mort », déjà prévu à l'affichage. |
| `@tiptap/suggestion` plutôt que `@tiptap/extension-mention` | Le nœud est propre au Carnet (titre vivant, texte choisi, lien mort) : `Mention` n'apporterait que son nœud, à surcharger entièrement. |
