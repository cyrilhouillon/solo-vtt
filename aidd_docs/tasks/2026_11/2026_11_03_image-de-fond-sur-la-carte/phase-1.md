---
status: done
---

# Instruction: garder et servir les images de carte

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-server/src/
├── main/
│   ├── java/solovtt/server/
│   │   ├── ImageStore.java                 ✅ port : garder une image, la relire par son empreinte
│   │   ├── ImageResource.java              ✅ POST /api/images, GET /api/images/{id}
│   │   ├── Dtos.java                       ✏️ ImageDto (id)
│   │   └── persistence/
│   │       ├── FileImageStore.java         ✅ un fichier par image, nommé par son empreinte
│   │       └── ImageStoreProducer.java     ✅ fichier si solovtt.images.path, sinon mémoire
│   └── resources/application.properties    ✏️ solovtt.images.path, limite de corps 50M
└── test/java/solovtt/server/
    ├── ImageApiTest.java                   ✅ envoi, relecture, refus
    └── persistence/FileImageStoreTest.java ✅ adaptateur fichier sur @TempDir
aidd_docs/memory/
├── architecture.md                         ✏️ images de carte, limite de corps
├── codebase-map.md                         ✏️ ImageStore, ImageResource, /api/images
└── testing.md                              ✏️ -Dsolovtt.images.path du serveur de test
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Profil de test : `solovtt.images.path` vide, store en mémoire | Aucune image | system |
| Happy path   | `POST /api/images` d'une PNG (octets bruts, `image/png`) | 201, `{ id }` = SHA-256 hexadécimal des octets | api |
| Happy path   | `GET /api/images/{id}` | 200, mêmes octets, `Content-Type: image/png`, `Cache-Control` immuable | api |
| Edge case    | Envoyer deux fois la même image | Même `id`, un seul fichier (`FileImageStoreTest`) | api, system |
| Edge case    | Envoyer une JPEG puis une WebP | 201, relues avec `image/jpeg` et `image/webp` | api |
| Edge case    | `Content-Type: image/gif` ou `text/plain` | 415 | api |
| Edge case    | `image/png` dont les octets ne sont pas une PNG, ou corps vide | 400 « Image PNG, JPEG ou WebP attendue » | api |
| Edge case    | `GET /api/images/inconnue` | 404 | api |
| Edge case    | `FileImageStore` relancé sur le même dossier | L'image reste lisible | system |
| Teardown     | `@TempDir` effacé | Aucun fichier hors du dossier de test | system |

## Tasks to do

### `1)` Port et adaptateurs

> Garder une image par son empreinte, sur disque en vrai, en mémoire en test.

1. `ImageStore` : `String put(byte[] bytes, String mediaType)` rend l'empreinte SHA-256 (hexadécimal minuscule) ; `Optional<StoredImage> find(String id)` avec `record StoredImage(byte[] bytes, String mediaType)` ; `static ImageStore inMemory()`.
2. `FileImageStore(Path dir)` : écrit `<empreinte>.<png|jpg|webp>` par fichier voisin puis déplacement atomique, ne réécrit pas un fichier existant ; `find` refuse un id qui n'est pas 64 caractères hexadécimaux (pas de chemin forgé) et retrouve l'extension, donc le type.
3. `ImageStoreProducer` : `@Produces @Singleton`, `FileImageStore` si `solovtt.images.path` n'est pas vide, sinon `ImageStore.inMemory()` (même forme que `GameStoreProducer`).
4. `application.properties` : `solovtt.images.path=${user.home}/.solo-vtt/images`, `%test.solovtt.images.path=`, `quarkus.http.limits.max-body-size=50M` avec la raison en commentaire.

### `2)` Ressource REST

> Envoyer une image et la relire par son URL.

1. `ImageResource` sous `/api/images` : `POST` `@Consumes({"image/png","image/jpeg","image/webp"})`, corps `byte[]`, vérifie la signature (PNG `89 50 4E 47`, JPEG `FF D8 FF`, WebP `RIFF....WEBP`) et qu'elle correspond au type, sinon `IllegalArgumentException` (400) ; rend `Dtos.ImageDto(id)` en 201.
2. `GET /api/images/{id}` : les octets avec leur type et `Cache-Control: public, max-age=31536000, immutable` ; inconnu → `NotFoundException` (404).
3. `@APIResponse` sur chaque code (201, 400, 404, 413, 415), comme les autres ressources.

### `3)` Tests et mémoire

> Verrouiller l'envoi, la relecture et les refus ; documenter.

1. `ImageApiTest` (`@QuarkusTest`, RestAssured) : un cas par ligne du Test Scope, avec de petites images construites dans le test (en-têtes valides).
2. `FileImageStoreTest` sur `@TempDir` : écriture, déduplication, relecture après une nouvelle instance, id forgé (`../x`) refusé.
3. `architecture.md` : images gardées hors de la partie par empreinte, port `ImageStore`, limite de corps 50 Mo ; `codebase-map.md` : fichiers et route ; `testing.md` : ajouter `-Dsolovtt.images.path=<scratchpad>/images` à la commande du serveur de test.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Une image gardée par `FileImageStore` se relit après une nouvelle instance ; la même image envoyée deux fois ne fait qu'un fichier ; un id forgé ne sort jamais du dossier. |
| 2 | `POST` d'une PNG, JPEG ou WebP rend 201 et son empreinte ; `GET` rend les mêmes octets et le bon type ; un autre type rend 415, une signature fausse 400, un id inconnu 404. |
| 3 | `mvn -q test` passe ; la mémoire décrit le stockage des images et l'option du serveur de test. |
