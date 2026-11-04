---
status: done
---

# Instruction: afficher et ranger l'arbre dans le Carnet

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
├── DESIGN.md ✏️
├── aidd_docs/memory/
│   ├── architecture.md ✏️
│   ├── codebase-map.md ✏️
│   └── project-brief.md ✏️
└── vtt-web/src/
    ├── api.ts ✏️
    ├── types.ts ✏️
    ├── notebook/
    │   └── tree.ts ✅
    └── components/
        ├── AppIcon.vue ✏️
        ├── MovePageDialog.vue ✅
        ├── NotebookScreen.vue ✏️
        └── PageTree.vue ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveur de test sur 8081 (campagnes et images dans le scratchpad), Vite sur 5174 ; une campagne neuve | Carnet vide | browser |
| Happy path   | Créer « Lore » à la racine, puis par le menu de sa ligne « Nouvelle sous-page » « Villes », puis sous Villes « Barovie » | Chaque page créée s'ouvre déverrouillée ; ses ancêtres sont dépliés et elle est marquée ouverte dans l'arbre | browser |
| Happy path   | Créer « Session 10 », « Session 2 », « session 1 » à la racine | Ordre : session 1, Session 2, Session 10 | browser |
| Happy path   | Replier puis rouvrir Lore | Ses sous-pages disparaissent puis reviennent ; le bouton dit `aria-expanded` | browser |
| Happy path   | Cliquer Barovie dans l'arbre | La page s'ouvre verrouillée, comme avant | browser |
| Happy path   | Menu de ligne « Renommer » sur Villes, saisir « Cités », Entrée | Ligne et, si Villes est ouverte, titre de la page à jour | browser |
| Happy path   | Déverrouiller une page, modifier le titre en tête, Entrée (ou quitter le champ) | Titre enregistré, arbre à jour ; la mise en page ne bouge pas | browser |
| Happy path   | Menu de ligne « Déplacer sous… » sur Cités, choisir « Racine », puis la redéplacer sous Session 2 | Cités et Barovie changent de place ensemble ; l'arbre se déplie jusqu'à elles | browser |
| Happy path   | Recharger, puis redémarrer le serveur de test et rouvrir la campagne | Même arbre ; seule la branche de la page ouverte est dépliée | browser |
| Edge case    | « Déplacer sous… » sur Lore | Le dialogue ne propose ni Lore ni ses sous-pages, ni son parent actuel | browser |
| Edge case    | Renommer avec un titre vide, ou Échap | Rien n'est envoyé ; l'ancien titre revient | browser |
| Edge case    | Erreur serveur au renommage ou au déplacement (serveur de test arrêté) | Bandeau d'erreur ; l'arbre reste celui du serveur | browser |
| Edge case    | Tout au clavier : Tab jusqu'au dépli, au titre, au menu ; Entrée ; Échap ferme menu et dialogue et rend le focus | Chaque geste se fait sans souris | browser |
| Edge case    | Largeur < 900px | Arbre au-dessus de la page, menus utilisables au doigt | browser |
| Teardown     | Arrêter 8081 puis 5174 ; effacer le dossier scratchpad | 8080 et 5173 intacts | system |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────────────┐
│ (1) Accueil · Solo VTT · Campagne               Combat · Thème    │
├────────────────────────┬─────────────────────────────────────────┤
│ (2) PAGES              │ (5) Titre de la page (champ si ouverte)  🔒│
│ ───────────            │ ─────────────────                        │
│ ▾ Lore            ⋯    │ (6) Corps de la page                     │
│    ▾ Cités        ⋯    │                                          │
│        Barovie ●  ⋯    │                                          │
│ ▸ PNJ             ⋯    │                                          │
│   session 1       ⋯    │                                          │
│ ┌(3)───────────────┐   │                                          │
│ │ Nouvelle sous-page│   │                                          │
│ │ Renommer          │   │                                          │
│ │ Déplacer sous…    │   │                                          │
│ └───────────────────┘   │                                          │
│ ─────────────          │                                          │
│ (4) Nouvelle page      │                                          │
│ [Titre        ] Créer  │                                          │
└────────────────────────┴─────────────────────────────────────────┘
┌(7) Déplacer « Cités » ──────────┐
│ ○ Racine                        │
│ ○ Lore                          │
│ ○   PNJ …                       │
│            Déplacer   Annuler   │
└─────────────────────────────────┘
```

1. Barre : inchangée.
2. Arbre : listes imbriquées, retrait par niveau, bouton de dépli (▾ ▸) pour une page à sous-pages, ligne de la page ouverte marquée ; défile dans le panneau au-delà de la hauteur de la fenêtre.
3. Menu de ligne (bouton ⋯, visible au survol, au focus et toujours au toucher) : Nouvelle sous-page et Renommer ouvrent un champ dans l'arbre, Déplacer sous… ouvre le dialogue.
4. Création à la racine : inchangée.
5. Titre : texte verrouillée, champ de même allure déverrouillée.
6. Corps : inchangé.
7. Dialogue modal : destinations permises en arbre indenté, Racine en tête.

## Tasks to do

### `1)` L'arbre construit depuis la liste

> Le client tire un arbre trié de la liste plate du serveur.

1. `types.ts` : `PageSummary.parentId: string | null`.
2. `api.ts` : `createPage(title, parentId = null)`, `renamePage(pageId, title)`, `movePage(pageId, parentId)`.
3. `notebook/tree.ts` (sans Vue) : `buildTree(pages)` (nœuds `{ page, children }`, sœurs triées par `Intl.Collator('fr', { numeric: true, sensitivity: 'base' })`) ; `ancestors(pages, id)` ; `descendants(pages, id)` ; `moveTargets(pages, id)` (toutes sauf elle, ses descendants et son parent actuel, racine comprise si elle n'y est pas).

### `2)` Afficher, ouvrir et replier

> L'arbre remplace la liste plate, à côté de la page ouverte.

1. `PageTree.vue` : rendu récursif de `buildTree` en `<ul>` imbriquées ; bouton de dépli `aria-expanded` pour une page à sous-pages ; bouton de page `aria-current` ; émet `open(id)`.
2. `NotebookScreen.vue` : remplace la liste par `PageTree` ; garde l'ensemble des branches ouvertes ; ouvre les ancêtres de la page ouverte (au montage, à l'ouverture, à la création, au déplacement).
3. Panneau collant (`position: sticky`) dont l'arbre défile au-delà de la hauteur de la fenêtre ; sous 900px, comportement actuel.

### `3)` Créer, renommer, déplacer

> Chaque page se range depuis sa ligne ; le titre se renomme aussi en tête de page.

1. `PageTree.vue` : bouton ⋯ par ligne (« Actions de <titre> »), menu Nouvelle sous-page / Renommer / Déplacer sous… ; Échap ou clic hors du menu le ferme et rend le focus au bouton.
2. Nouvelle sous-page : champ sous la ligne, Entrée crée (`createPage(title, parentId)`) et ouvre la page déverrouillée, Échap annule ; passe par `saved()` avant de quitter la page ouverte.
3. Renommer : la ligne devient un champ prérempli (120 caractères) ; Entrée ou sortie envoie si le titre nettoyé change et n'est pas vide ; Échap annule.
4. `NotebookScreen.vue` : titre en tête rendu en `<input>` de même police et taille quand la page est déverrouillée ; même règle d'envoi ; `aria-label` « Titre de la page ».
5. `MovePageDialog.vue` : `<dialog>` modal, `moveTargets` en arbre indenté (radios), Racine en tête, « Déplacer » et « Annuler » ; envoie `movePage`, recharge le Carnet, ouvre les ancêtres de la page déplacée.
6. Après chaque geste, relire `api.notebook()` ; une erreur passe par le bandeau existant ; `AppIcon.vue` : créer les icônes `more` (⋯), `chevron-right` et `chevron-down`, absentes aujourd'hui.

### `4)` Mémoire et design

> La documentation décrit l'arbre côté client.

1. `DESIGN.md` : panneau Pages en arbre (retrait, dépli, menu de ligne, champ de renommage), dialogue Déplacer, titre éditable déverrouillé.
2. `architecture.md` : la liste plate devient l'arbre, branches ouvertes en mémoire du client, sœurs triées en ordre numérique côté client.
3. `codebase-map.md` : `notebook/tree.ts`, `PageTree.vue`, `MovePageDialog.vue`. `project-brief.md` : le Carnet range ses pages en arbre.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe ; « Session 2 » précède « Session 10 » ; une page n'est jamais proposée sous elle-même ou ses descendants. |
| 2 | L'arbre s'affiche à côté de la page, un clic ouvre la page, une branche se replie et se rouvre, la branche de la page ouverte est dépliée au montage. |
| 3 | Créer sous une page, renommer depuis l'arbre et depuis le titre déverrouillé, déplacer à la racine ou sous une autre page avec ses sous-pages : tout est relu identique après redémarrage du serveur de test ; tout se fait au clavier. |
| 4 | `DESIGN.md` et la mémoire décrivent l'arbre, son menu, le dialogue et le titre éditable. |
