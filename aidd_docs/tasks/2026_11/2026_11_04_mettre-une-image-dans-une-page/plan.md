---
objective: "Dans une page déverrouillée du Carnet, le joueur insère une image PNG, JPEG ou WebP depuis son disque, qui s'affiche à sa place dans le texte et survit au redémarrage ; un autre type ou un fichier de plus de 50 Mo est refusé avec la raison ; il retire l'image, et Ctrl+Z la remet."
status: implemented
---

# Plan: mettre une image dans une page du Carnet

Source: aidd_docs/backlog/stories/mettre-une-image-dans-une-page-du-carnet.md (epic `aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md`, order 6)

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Insérer une image par le bouton de la page | [`phase-1.md`](./phase-1.md) | none |
| 2   | Déposer ou coller une image dans le texte | [`phase-2.md`](./phase-2.md) | 1 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | Le stockage existe : `POST /api/images` (`ImageResource.java`) vérifie la signature PNG / JPEG / WebP (415 pour un autre type), garde chaque image sous son empreinte SHA-256 (`FileImageStore`, dossier `solovtt.images.path`, jamais supprimée) et `GET /api/images/{id}` la relit ; le client la refuse déjà au-delà de 50 Mo (`NewMapForm.vue:46`). |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Aucune dépendance nouvelle : le nœud image est écrit avec `@tiptap/core` (déjà là), comme `pageLink` ; `@tiptap/extension-image` n'est pas installé (`node_modules/@tiptap`) et n'est pas nécessaire. Le serveur sert déjà les images, en cache immuable. | 1 |
| Regression | Faible. Serveur inchangé hormis un commentaire. Client : `PageEditor.vue` gagne une extension et une méthode ; `NewMapForm.vue` lit ses refus d'image d'un module partagé, mêmes messages. La recherche (`PageSearch.append`) rend un espace pour un nœud de type inconnu sans enfant, donc une image ne change pas le texte cherché ; `PageLinks` ne lit que `pageLink`. Couvert par `mvn -q test` ; `NotebookApiTest` ne garde aujourd'hui qu'un document à `heading`, d'où un test ajouté en phase 1 pour un nœud `pageImage` relu à l'identique ; le client par `vue-tsc` et parcours navigateur sur 8081 / 5174. Pas de phase filet : aucun comportement existant ne change. | 1, 2 |
| Architecture | Départ : les images sont communes, réservées au fond des cartes ; le contenu d'une page est un document Tiptap que le serveur garde sans le valider, et ne lit que pour les liens entrants (`pageLink`) et la recherche (`text`, `hardBreak`, `pageLink`). Cible : un nœud bloc `pageImage { imageId, alt }` référence une image du même `ImageStore` par son empreinte, jamais par son adresse ; ni les liens entrants ni la recherche ne le lisent. | 1 |
| Documentation | `architecture.md` (nœud `pageImage`, images communes aux cartes et aux pages, refus côté client par `src/images.ts`, dépôt et collage), `codebase-map.md` (`src/notebook/pageImage.ts`, `src/images.ts`), `project-brief.md` (fonctionnalité Carnet : images), `DESIGN.md` (bouton Image dans la tête de page, image dans le texte, image sélectionnée). Story : `status: done`. Plan : `status: implemented`. | 1, 2 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Le client refuse une image de plus de 50 Mo avant l'envoi : au-delà, le serveur coupe la connexion (`architecture.md`) | Même limite et même message que la carte, depuis un seul module. |
| Aucune image n'est jamais supprimée (`architecture.md`) | Retirer l'image de la page ne touche pas le serveur ; Ctrl+Z la remet sans nouvel envoi. |
| Les options d'une extension Tiptap passent par des fonctions, jamais une `Ref` (`architecture.md`) | Le nœud image n'a besoin d'aucune option réactive ; l'envoi passe par une méthode de `PageEditor`. |
| Verrouillée, l'éditeur n'est pas éditable ; `handlePaste` et `handleDrop` ne sont qu'une garde (`architecture.md`) | Le bouton n'apparaît que déverrouillée ; dépôt et collage n'insèrent qu'en écriture. |
| Client sans tests automatisés ; jamais de test navigateur sur 8080 / 5173 (`testing.md`) | Parcours sur les serveurs de test 8081 / 5174, images et campagnes dans le scratchpad. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| L'image d'une page est un nœud bloc maison `pageImage { imageId, alt }`, rendu en `<img src="/api/images/{imageId}">`, plutôt que `@tiptap/extension-image` (attribut `src`) | Le document garde l'empreinte, comme `pageLink` garde l'id : l'adresse peut changer (hébergement) sans réécrire de page. Aucune dépendance à ajouter pour un nœud d'une trentaine de lignes. Bloc : l'image tient sa ligne entre deux paragraphes, sans habillage. |
| Les images des pages vont dans le même `ImageStore` que les fonds de carte | Il existe, vérifie la signature, déduplique et survit au redémarrage ; l'empreinte ne dépend pas de la campagne. Revers : une image retirée n'est jamais effacée du disque, comme pour les cartes. |
| Insertion par un bouton « Image » dans la tête de page, visible déverrouillée seulement, puis dépôt et collage en phase 2 | La story demande « depuis mon disque » : le sélecteur de fichier suffit et reste atteignable au clavier. Un fichier déposé sur le texte doit de toute façon être pris en main, sinon le navigateur pourrait l'ouvrir à la place de l'application. C'est le premier bouton de mise en forme d'une page jusqu'ici sans barre d'outils (`DESIGN.md`). |
