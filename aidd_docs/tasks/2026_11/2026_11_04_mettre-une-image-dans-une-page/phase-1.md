---
status: done
---

# Instruction: insérer une image par le bouton de la page

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   ├── codebase-map.md ✏️
│   └── project-brief.md ✏️
├── vtt-server/src/
│   ├── main/java/solovtt/server/ImageResource.java ✏️
│   └── test/java/solovtt/server/NotebookApiTest.java ✏️
└── vtt-web/src/
    ├── api.ts ✏️
    ├── images.ts ✅
    ├── notebook/pageImage.ts ✅
    └── components/
        ├── AppIcon.vue ✏️
        ├── NewMapForm.vue ✏️
        ├── NotebookScreen.vue ✏️
        └── PageEditor.vue ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test 8081 / 5174, campagne neuve, page « Bertrand » avec deux paragraphes, déverrouillée | Page éditable, bouton Image visible | browser |
| Happy path   | Curseur entre les deux paragraphes ; Image, choisir un PNG | L'image s'affiche entre les deux paragraphes, à la largeur de la page au plus | browser |
| Happy path   | Même geste avec un JPEG puis un WebP | Chacune s'affiche à sa place | browser |
| Happy path   | Attendre l'enregistrement, redémarrer le serveur 8081, rouvrir la page | Les images sont là, à la même place | browser |
| Happy path   | Cliquer l'image, Retour arrière | L'image disparaît ; Ctrl+Z la remet à sa place, Ctrl+Y la retire | browser |
| Happy path   | Verrouiller la page | Bouton Image absent, image affichée ; aucune touche ne la retire | browser |
| Edge case    | Choisir un GIF ou un PDF | « Choisir une image PNG, JPEG ou WebP. » dans le bandeau, rien d'envoyé ni d'inséré | browser |
| Edge case    | Choisir un PNG de plus de 50 Mo | « Image trop lourde (50 Mo au plus). », rien d'envoyé | browser |
| Edge case    | Fichier renommé `.png` qui n'est pas une image (400 du serveur) | Message du serveur dans le bandeau, rien d'inséré | browser |
| Edge case    | Image effacée du dossier d'images, page rouverte | Le nom du fichier (`alt`) à la place de l'image, la page reste lisible et éditable | browser |
| Edge case    | Nouvelle carte avec un GIF dans l'écran de combat | Même refus qu'avant (« Choisir une image PNG, JPEG ou WebP. ») | browser |
| Teardown     | Arrêter 8081 puis 5174 | Ports libérés ; 8080 et 5173 intacts | system |

## Wireframe

```txt
┌────────────────────────────────────────────────────────────┐
│ (1) Barre : Accueil · Solo VTT · campagne · Combat · thème │
├──────────────┬─────────────────────────────────────────────┤
│ (2) Pages    │ (3) Titre de la page       [Image] [Verrou] │
│  Chercher    │ ─────────────────────────────────────────── │
│  arbre       │ paragraphe                                  │
│              │ ┌────────────────────────────┐              │
│  Nouvelle    │ │ (4) image                  │              │
│  page        │ └────────────────────────────┘              │
│              │ paragraphe                                  │
│              │ (5) Pages qui renvoient ici                 │
└──────────────┴─────────────────────────────────────────────┘
```

1. Barre : inchangée.
2. Panneau des pages : inchangé.
3. Tête de page : le bouton Image (icône, déverrouillée seulement) avant le verrou ; il ouvre le sélecteur de fichier.
4. Image : un bloc entre deux paragraphes, à sa taille, jamais plus large que la page.
5. Pages qui renvoient ici : inchangé.

## Tasks to do

### `1)` Partager les refus d'image

> Un seul endroit dit quelles images le serveur accepte, pour la carte et pour la page.

1. Créer `src/images.ts` : `IMAGE_TYPES`, `MAX_IMAGE_BYTES` (50 Mo, limite du serveur) et `imageRefusal(file): string | null` (« Choisir une image PNG, JPEG ou WebP. », « Image trop lourde (50 Mo au plus). »).
2. `NewMapForm.vue` : utiliser `imageRefusal` et `IMAGE_TYPES` (attribut `accept`) ; messages inchangés.
3. `api.ts` (commentaires de `imageUrl`, `uploadImage`) et `ImageResource.java` (Javadoc) : une image de carte ou de page.

### `2)` Le nœud image

> Une image dans le document d'une page ne garde que son empreinte.

1. Créer `src/notebook/pageImage.ts` : `Node.create` `pageImage`, bloc, atomique, sélectionnable, non glissable ; attributs `imageId` et `alt` ; `parseHTML` sur `img[data-image-id]` ; `renderHTML` en `img` avec `src` tiré de `imageUrl(imageId)`, `alt`, `data-image-id`.
2. Commande `insertPageImage({ imageId, alt }, position?)` : insère le nœud à la sélection, ou à la position donnée, en une transaction (un seul pas d'annulation).
3. `NotebookApiTest` : une page dont le contenu porte un nœud `pageImage` se relit à l'identique, et la recherche trouve le texte qui l'entoure.

### `3)` Insérer depuis la page

> Le bouton Image choisit un fichier, l'envoie, puis l'insère où était le curseur.

1. `PageEditor.vue` : enregistrer `PageImage` ; exposer `insertImage(file)` : `imageRefusal`, sinon `uploadImage`, puis la commande à la sélection courante (le nom du fichier pour `alt`) ; rendre une raison en cas de refus ou d'échec, sans rien insérer.
2. Styles : image en bloc, `max-width: 100%`, `height: auto` ; sélectionnée en écriture, cercle `brass` comme un lien sélectionné.
3. `NotebookScreen.vue` : bouton d'icône Image dans la tête de page, déverrouillée seulement, et un `<input type="file">` caché filtré par `IMAGE_TYPES` ; le choix appelle `insertImage` ; la raison va au bandeau d'erreur ; bouton éteint pendant l'envoi.
4. `AppIcon.vue` : icône image, même trait que les autres.

### `4)` Documenter

> La mémoire et le design disent où vivent les images d'une page.

1. `architecture.md` : nœud `pageImage { imageId, alt }`, images communes aux cartes et aux pages, refus client par `src/images.ts`, retirer ne supprime rien.
2. `codebase-map.md` : `src/images.ts`, `src/notebook/pageImage.ts`.
3. `project-brief.md` : insérer une image dans une page du Carnet.
4. `DESIGN.md` : bouton Image dans la tête de page, image dans le texte, image sélectionnée.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1    | La carte refuse un GIF et une image de plus de 50 Mo avec les mêmes messages qu'avant ; `npm run build` et `mvn -q compile` passent. |
| 2    | Le document enregistré contient `{ type: 'pageImage', attrs: { imageId, alt } }`, sans adresse ; rouvert, il affiche l'image ; `NotebookApiTest` passe (`mvn -q test` vert). |
| 3    | Un PNG, un JPEG et un WebP s'insèrent à la place du curseur et reviennent après redémarrage du serveur ; un autre type ou plus de 50 Mo est refusé avec la raison, sans envoi ; retirée, l'image revient par Ctrl+Z ; verrouillée, la page n'a pas de bouton Image et l'image ne se retire pas. |
| 4    | `architecture.md`, `codebase-map.md`, `project-brief.md` et `DESIGN.md` décrivent le nœud, le bouton et les refus. |
