---
status: done
---

# Instruction: écrire une page au fil du texte

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/
│   ├── backlog/stories/ecrire-une-page-dans-le-carnet.md ✏️
│   ├── memory/
│   │   ├── architecture.md ✏️
│   │   ├── codebase-map.md ✏️
│   │   └── package.md ✏️
│   └── tasks/2026_11/2026_11_04_ecrire-une-page-dans-le-carnet/plan.md ✏️
└── vtt-web/
    ├── package.json ✏️
    ├── package-lock.json ✏️
    └── src/components/
        ├── AppIcon.vue ✏️
        ├── NotebookScreen.vue ✅
        └── PageEditor.vue ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs de test de la phase 2 ; une campagne neuve | Carnet vide sur 5174 | system |
| Happy path   | Créer « Session 1 », taper `# Arrivée`, Entrée, `> La pluie tombe`, Entrée deux fois, `- un`, Ctrl+B sur un mot, Ctrl+I sur un autre | Titre, citation, liste, gras et italique rendus au fil du texte, sans zone de texte brute ni balise visible | browser |
| Happy path   | Attendre une seconde sans taper, puis recharger l'onglet | Le texte revient mis en forme, sans bouton « Enregistrer » | browser |
| Happy path   | Ctrl+Z puis Ctrl+Y | La dernière frappe est défaite puis refaite | browser |
| Happy path   | Ouvrir une autre page puis revenir à « Session 1 » | Elle s'ouvre verrouillée : la frappe n'y change rien, le curseur n'est plus celui du texte | browser |
| Happy path   | Cliquer le verrou | Déverrouillée, elle s'édite au même endroit ; seuls l'icône du verrou et le curseur ont changé (mêmes positions du texte) | browser |
| Happy path   | Taper puis cliquer aussitôt Combat (moins d'une seconde) ; revenir au Carnet | La frappe est là : l'enregistrement part avant de quitter | browser |
| Happy path   | Taper, attendre, arrêter et relancer le serveur 8081, rouvrir la campagne | Même page, même texte | browser |
| Edge case    | Taper puis fermer l'onglet aussitôt ; rouvrir | La frappe est gardée (envoi à `visibilitychange`, et `keepalive` à `pagehide` sous 60 Ko) ; refaire avec une page de plus de 64 Ko : non vérifié, à observer | browser |
| Edge case    | Serveur arrêté pendant la frappe | Bandeau d'erreur ; le texte reste dans l'éditeur et s'enregistre à la frappe suivante, serveur relancé | browser |
| Edge case    | Page verrouillée : Ctrl+Z, Ctrl+B, coller | Rien ne change, aucun enregistrement envoyé | browser |
| Edge case    | Clavier seul : Tab jusqu'au verrou, Entrée | Le verrou bascule et annonce son état (`aria-pressed`, libellé) | browser |
| Teardown     | Arrêter 8081 puis 5174, effacer `<scratchpad>/campagnes` | Rien du joueur touché | system |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────┐
│ (1) Page ouverte                                         │
│  Session 1                                   (2) [🔒]    │
│  ────────────                                            │
│  (3) Arrivée                                             │
│      ▌La pluie tombe                                     │
│      • un                                                │
│      texte **gras** et *italique*                        │
└──────────────────────────────────────────────────────────┘
```

1. Page ouverte : titre et verrou en tête, corps dessous (région 4 de la phase 2).
2. Verrou : bouton d'icône à droite du titre, cadenas fermé (verrouillée) ou ouvert ; seul élément qui change avec l'état.
3. Corps : l'éditeur Tiptap, même typographie verrouillé ou non ; seul le curseur change.

## Tasks to do

### `1)` L'éditeur

> La page s'écrit en texte riche, avec annuler et rétablir.

1. `vtt-web` : `@tiptap/vue-3`, `@tiptap/starter-kit`, `@tiptap/pm` en 3.32.2 exactes.
2. `PageEditor.vue` (props `page`, `editable` ; émet `change` avec le JSON du document) : `useEditor` + StarterKit, contenu `page.content` ou document vide ; StarterKit sans `link` (fourni par StarterKit 3.32.2, observé dans ses dépendances) : aucun lien dans cette story, les liens vers une page, ouvrables même verrouillée, viennent avec la story « lier une page » ; `setEditable` suit la prop ; un éditeur par page (`:key` = id), pour que l'historique d'annulation ne passe pas d'une page à l'autre.
3. Styles des titres, listes, citations, gras et italique depuis les jetons de `style.css`, identiques verrouillé ou non ; verrouillé, curseur par défaut et pas de caret.

### `2)` Le verrou

> Une page existante s'ouvre verrouillée ; celle qu'on vient de créer reste déverrouillée jusqu'à ce qu'on la quitte.

1. `NotebookScreen.vue` : état `unlocked` par page ouverte, vrai seulement juste après `createPage`, remis à faux en ouvrant une autre page ou en quittant l'écran.
2. Bouton verrou (`aria-pressed`, « Déverrouiller la page » / « Verrouiller la page »), à droite du titre ; `AppIcon.vue` : icônes `lock` et `unlock`.

### `3)` L'enregistrement automatique

> Ce qui est écrit s'enregistre tout seul et n'est jamais perdu en quittant.

1. `NotebookScreen.vue` : à chaque `change`, envoi différé d'une seconde (`savePageContent`) ; un seul envoi à la fois, le dernier contenu part après celui en cours.
2. Avant d'ouvrir une autre page, d'émettre `combat` ou `home` : attendre l'envoi en attente.
3. `visibilitychange` vers `hidden` : envoyer le contenu en attente (envoi ordinaire) ; `pagehide` : le renvoyer avec `keepalive` s'il fait moins de 60 Ko, plafond des corps `keepalive` (environ 64 Kio, fait navigateur non vérifié) ; au-delà, seul l'envoi de `visibilitychange` compte.
4. Échec : bandeau d'erreur, contenu gardé en attente, renvoyé au changement suivant.

### `4)` Clôture

> La doc décrit l'éditeur et la story est livrée.

1. `DESIGN.md` : corps de page, verrou, curseur verrouillé.
2. `architecture.md` : un éditeur par page, envoi différé d'une seconde et à `pagehide`, verrou purement client ; `codebase-map.md` : `PageEditor` ; `package.md` : Tiptap parmi les dépendances de `vtt-web`.
3. Story `status: done` ; `plan.md` `status: implemented`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Titres, gras, italique, listes, citations se mettent en forme à la frappe ; Ctrl+Z / Ctrl+Y défont et refont ; `npm run build` passe. |
| 2 | Une page rouverte est verrouillée et rien ne s'y modifie ; déverrouillée, elle s'édite sans que le texte bouge ; une page créée s'édite d'emblée. |
| 3 | Le texte survit au rechargement, au passage immédiat au combat, à la fermeture de l'onglet et au redémarrage du serveur 8081, sans bouton. |
| 4 | Doc à jour ; story `done` ; plan `implemented`. |
