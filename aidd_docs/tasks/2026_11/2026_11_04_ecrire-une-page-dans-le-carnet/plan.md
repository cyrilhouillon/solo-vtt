---
objective: "Ouvrir une campagne mène à son Carnet, où le joueur crée une page titrée et l'écrit en texte riche, verrouillée ou non, enregistrée toute seule ; il passe au combat et revient sur la même page, retrouvée aussi après un redémarrage du serveur."
status: implemented
---

# Plan: écrire une page dans le Carnet

Source: aidd_docs/backlog/stories/ecrire-une-page-dans-le-carnet.md (epic `aidd_docs/backlog/epics/tenir-le-carnet-de-sa-campagne.md`, spike `aidd_docs/backlog/spikes/choisir-l-editeur-du-journal-de-campagne.md`) ; consigne du joueur : stockage sur disque, en local, une base de données viendra plus tard.

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Garder les pages du Carnet sur le serveur | [`phase-1.md`](./phase-1.md) | none |
| 2   | Arriver sur le Carnet et passer au combat | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Écrire une page au fil du texte | [`phase-3.md`](./phase-3.md) | 2 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://www.npmjs.com/package/@tiptap/vue-3 | `npm view` (2026-11-04) : `@tiptap/vue-3` et `@tiptap/starter-kit` 3.32.2, MIT ; `@tiptap/vue-3` a pour pairs `vue ^3`, `@tiptap/core`, `@tiptap/pm` 3.32.2 et `@floating-ui/dom ^1` (installé d'office par npm, inutilisé ici). StarterKit embarque titres, gras, italique, listes, citation, annuler/rétablir. |
| https://github.com/ueberdosis/tiptap/blob/v3.32.2/packages/extensions/src/undo-redo/undo-redo.ts | Lu dans le paquet `@tiptap/extensions` 3.32.2 : `Mod-z` annule, `Mod-y` et `Shift-Mod-z` rétablissent ; Ctrl+Y est donc fourni sans code. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Nouveau sous-dossier `carnet/` dans `~/.solo-vtt/campagnes/<id>/` (une fiche par page, `<pageId>.json`, et `carnet.json` pour la dernière page consultée), sous `solovtt.campaigns.path` existant ; mémoire en profil de test. Nouvelles dépendances `@tiptap/vue-3`, `@tiptap/starter-kit`, `@tiptap/pm` 3.32.2 (versions observées par `npm view`). Poids : 147 Ko gzip mesurés par le spike pour core + StarterKit + Markdown + Image ; sans Markdown ni Image, non mesuré. | 1, 3 |
| Regression | Faible côté serveur : `CampaignStore` gagne `notebook(id)`, implémenté par `inMemory` et `FileCampaignStore` ; `CampaignStoreTest`, `FileCampaignStoreTest` et `CampaignApiTest` couvrent l'existant et doivent repasser ; partie et cartes ne changent pas. Moyen côté client : `App.vue` ouvre le Carnet au lieu de `GameScreen` (changement voulu par l'epic), `GameScreen.vue` gagne un bouton Carnet dans sa barre (`GameScreen.vue:1082`) ; pas de tests automatisés côté client, parcours dans le navigateur. Pas de phase filet : les tests serveur existants sont le filet. | 1, 2 |
| Architecture | Départ : une campagne = une partie (`GameStore`) et des cartes (`MapStore`), servies sous `/api/campaigns/{id}/…` ; le client n'a que deux écrans, accueil et jeu. Cible : un troisième port par campagne, `NotebookStore` (pages : id, titre, contenu ; dernière page consultée), adaptateur `persistence.JsonFileNotebookStore`, servi par `NotebookResource` sous `/api/campaigns/{id}/notebook` ; le client a trois écrans, accueil → Carnet ↔ combat. Le contenu d'une page est le document JSON de Tiptap, que le serveur garde sans l'interpréter. | 1, 2, 3 |
| Documentation | `architecture.md` (port `NotebookStore`, dossier `carnet/`, contenu JSON Tiptap opaque, enregistrement automatique, Carnet écran d'ouverture), `codebase-map.md` (`NotebookStore`, `JsonFileNotebookStore`, `NotebookResource`, routes `notebook`, `NotebookScreen`, `PageEditor`), `package.md` (Tiptap parmi les dépendances confinées à `vtt-web`), `project-brief.md` (fonctionnalité Carnet), `DESIGN.md` (écran Carnet, verrou, bouton Carnet / Combat). Story : `status: done`. Plan : `status: implemented`. | 1, 2, 3 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Port et adaptateur dans `persistence/`, mémoire en test, écriture atomique, fichier illisible jamais écrasé (`architecture.md`) | `NotebookStore` suit `GameStore` et `MapStore` ; c'est aussi ce qui permettra de passer à une base de données sans toucher aux ressources. |
| Toute route d'une partie est sous `/api/campaigns/{id}/…`, localisateur de sous-ressource dans `CampaignResource` (`architecture.md`) | `NotebookResource` s'y branche ; le 404 de campagne ou de page inconnue est levé par le localisateur et la ressource (`NotFoundException`, comme `MapResource`), puisque `ErrorMapper.java:13` rend 400 pour une `IllegalArgumentException` ; `CampaignNotFoundFilter` le documente. |
| Les moteurs n'ont aucune dépendance runtime ; les dépendances restent confinées à `vtt-server` et `vtt-web`, versions figées (`package.md`) | Tiptap entre dans `vtt-web` seul, en version exacte. |
| Ne jamais tester sur 8080/5173 ; serveur de test avec `-Dsolovtt.campaigns.path=<scratchpad>/campagnes` (`testing.md`) | Le parcours navigateur écrit des pages. |
| Tests `@QuarkusTest` + RestAssured, chaque test dans une campagne neuve (`Campagnes.creerUneNeuve()`), noms en français (`testing.md`) | Nouveaux tests d'API et d'adaptateur. |
| Code et messages en français, identifiants en anglais (`project-brief.md`) | Nommage `Notebook`, `Page` ; textes « Carnet », « Nouvelle page ». |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Le contenu d'une page est gardé en JSON Tiptap (document ProseMirror), opaque pour le serveur, qui vérifie seulement `type: "doc"` | Sans perte et sans l'API Markdown de Tiptap, marquée bêta (spike). Contrepartie : le format suit le schéma de l'éditeur ; les stories suivantes (liens entrants, recherche) parcourront ce JSON côté serveur pour en tirer liens et texte. |
| Une fiche par page (`carnet/<pageId>.json`), plus `carnet/carnet.json` pour la dernière page consultée | L'enregistrement automatique réécrit souvent une page : il ne réécrit qu'elle, et une fiche abîmée n'emporte pas le Carnet. Le port isole ce choix : une base de données le remplacera sans toucher aux routes. |
| La dernière page consultée est gardée par le serveur (`PUT …/notebook/last-page`), pas par le navigateur | La story l'exige après redémarrage du serveur ; le serveur fait déjà foi pour toute la campagne, et l'hébergement web viendra. |
| Carnet et combat sont deux écrans montés tour à tour (`v-if`), chacun relisant le serveur au montage ; le Carnet enregistre la page avant de céder la place | Aucun état à cacher en mémoire du client, et le combat repart tel que le serveur le garde ; la vue de la carte (zoom) n'est déjà pas gardée. |
| Pas de barre d'outils : mise en forme au clavier (Ctrl+B, Ctrl+I) et par la frappe (`# `, `- `, `> `, `**…**`) | Une barre qui n'apparaîtrait que déverrouillé changerait la mise en page, contraire à la story (« seuls l'icône du verrou et le curseur changent »). |
| Une liste plate des pages (triées par titre) à côté de la page ouverte, que la story de l'arbre remplacera | Sans elle, une deuxième page créée rendrait la première inaccessible, et « une page existante s'ouvre verrouillée » ne se vérifierait qu'au rechargement. |
