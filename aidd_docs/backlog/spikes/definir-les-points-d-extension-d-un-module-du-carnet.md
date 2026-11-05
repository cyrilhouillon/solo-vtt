---
type: spike
status: resolved
parents:
  - aidd_docs/backlog/epics/jouer-sa-campagne-avec-mythic-gme-depuis-le-carnet.md
---

# Spike: définir les points d'extension d'un module du Carnet

## Question

Quel contrat le cœur doit-il offrir pour qu'un module (Mythic d'abord, puis Adventure Crafter et les feuilles de personnage) ajoute un panneau au Carnet, une section dans l'arborescence, garde ses données dans la campagne et écrive un résultat dans la page ouverte, sans code propre au module dans le cœur ? Corollaire : les listes de Mythic sont-elles des pages, ou des données propres affichées dans l'arbre ?

## Decision

Découpage et ordre des stories de l'epic Mythic : faut-il une story « socle de module » avant toute story Mythic ?

## Bounds

- Evidence needed : points d'accroche actuels lus dans le code (`CampaignStore`, `NotebookStore`, `NotebookResource`, `NotebookScreen`, `PageTree`, `PageEditor`) ; découverte par Quarkus des beans CDI et ressources REST d'un jar séparé (Jandex), d'après la doc ; enregistrement d'un module client dans `vtt-web` (liste statique à la construction, ou chargement dynamique) ; un essai jetable, hors dépôt, où un module factice voit sa ressource et son bean servis et son panneau affiché, sans import du module dans le code du cœur ; une règle explicite sur ce qui peut nommer le module (`pom.xml` d'assemblage, liste des modules du client).
- Stop when : un contrat esquissé pour les quatre points, confirmé par l'essai, et le stockage des listes tranché ; ou un point impossible sans toucher au cœur, écart nommé.

## Investigation

| Attempt | Evidence | Result |
| ------- | -------- | ------ |
| Lecture des points d'accroche (2026-11-05) | `CampaignStore`, `NotebookStore`, `CampaignResource` (localisateurs de sous-ressources), `NotebookScreen`, `PageEditor` (`insertImage`, `defineExpose`), `tsconfig.app.json`, `PUT …/notebook/tabs` | Onglets, adresses (`/carnet/:pageId`), liens `pageLink`, recherche, corbeille et pages citantes reposent tous sur la page ; une entrée qui n'est pas une page devrait être apprise par chacun. `PageEditor` n'émet `change` que si la page est éditable. |
| Essai serveur jetable (scratchpad, Quarkus 3.40.1.1) : `spi` (interfaces), `mythic` (bean `@ApplicationScoped`, ressource `@Path("/api/campaigns/{campaignId}/mythic")` qui injecte un port du cœur), `app` (Quarkus, `CampaignResource` à localisateur, `Instance<CampaignModule>`), dépendant de `mythic` par son seul `pom.xml` | 4 tests `@QuarkusTest` | Sans index, le module est ignoré (2 échecs, `Expected status code <200> but was <404>`). Avec un `META-INF/beans.xml` vide dans le jar du module : 4/4 verts ; bean découvert, ressource servie à côté du localisateur du cœur, port du cœur injecté dans le module ; aucune référence à `mythic` dans le code de `app`. |
| Essai client jetable (copie de `vtt-web` hors dépôt) : contrat `NotebookModule` et `NotebookContext` dans `src/modules/`, registre `import.meta.glob('../../modules/*/index.ts', { eager: true })`, panneau `modules/mythic/` qui écrit dans la page par `writeToOpenPage` (`insertContent` de Tiptap) | `npm run build` (`vue-tsc -b` + Vite) | Build vert, panneau dans le bundle ; dossier du module retiré : build vert, module absent du bundle ; `src/` ne cite pas Mythic. Panneau non vu dans le navigateur. |

## Outcome

- Result :
  - **Contrat serveur.** Un module Maven `vtt-module-api`, interfaces seules, sans dépendance runtime : le cœur implémente ses ports (données d'un module par campagne, lecture et création des pages du Carnet), le module les injecte. Un module serveur est un jar à `META-INF/beans.xml` vide, dont les ressources vivent sous `/api/campaigns/{id}/<module>/…`.
  - **Contrat client.** `vtt-web/modules/<id>/index.ts` exporte un `NotebookModule` (id, titre, panneau, section) ; le registre du cœur les découvre par `import.meta.glob`. Le Carnet donne au panneau un contexte (`campaignId`, `writeToOpenPage`), qui écrit des nœuds standard (citation, paragraphe) : aucune extension Tiptap n'est requise.
  - **Qui nomme un module.** Seuls les assemblages : `<module>` du `pom.xml` racine, la dépendance de `vtt-server/pom.xml`, le dossier `vtt-web/modules/`. Ni `vtt-server/src` ni `vtt-web/src`.
  - **Mythic.** Un moteur pur `mythic-engine` (dés injectés, comme `dnd-engine`), un jar serveur, un client dans `vtt-web/modules/mythic/`.
  - **Listes : des pages.** Une entrée est un élément de liste ordinaire, un personnage pouvant être un lien `[[…]]` vers sa page ; Mythic les lit par le port du Carnet. Onglets, adresses, liens, recherche, corbeille et pages citantes s'appliquent sans changement.
  - **Section à part (décision de l'utilisateur) : une vraie section.** Une page porte un marqueur générique de section (le module qui la range, aucun pour une page ordinaire) ; l'arbre montre les pages ordinaires, puis une section par module, sous un en-tête que le module titre. Le cœur ne connaît que le marqueur, jamais Mythic.
- Confidence : haute sur la découverte serveur et client (essais verts) ; moyenne sur l'écriture dans la page, typée et construite mais non jouée dans le navigateur.
- Remaining uncertainty :
  - un jet sur une page verrouillée : refusé ou déverrouille (décision de story) ;
  - les règles de la section : déplacer une page entre section et pages ordinaires, supprimer la page racine d'une section ;
  - les feuilles de personnage, données structurées : page à contenu propre au module, ou autre support ; le marqueur de section doit le permettre sans le trancher ici ;
  - le rechargement à chaud de `quarkus:dev` sur un module Maven voisin, non essayé.

## Follow-up

Epic Mythic : une story « socle de module » (`vtt-module-api`, découverte serveur et client, données de module, section du Carnet, écriture dans la page ouverte) avant toute story Mythic.
