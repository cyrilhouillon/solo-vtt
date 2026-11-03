---
objective: "Le joueur zoome sur la carte autour du pointeur et déplace la vue en glissant le fond, en combat comme en préparation et pendant le calage (où le bouton du milieu décale désormais la grille), sans rien changer aux positions où il pose, déplace, cible ou mesure."
status: in-progress
---

# Plan: zoomer et déplacer la vue de la carte

Source: aidd_docs/backlog/stories/zoomer-et-deplacer-la-vue-de-la-carte.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Zoomer la vue | [`phase-1.md`](./phase-1.md) | none |
| 2   | Déplacer la vue en glissant le fond | [`phase-2.md`](./phase-2.md) | 1 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | none |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Aucun : client seul, sans dépendance nouvelle (Konva 10.9.1 déjà là, `package.json`), ni serveur ni fichier de partie. Non vérifié, à observer au parcours : le coût d'un redessin complet (`SceneCanvas.redraw`, `SceneCanvas.ts:216`, qui détruit et recrée tous les nœuds) à chaque événement molette ou glisser, regroupé par `requestAnimationFrame` ; Konva ne pose aucun `touch-action` au conteneur (`Stage.js`, vérifié ; posé explicitement) ; `preventDefault` sur `mousedown` bouton du milieu coupe le défilement automatique de Chromium sous Windows. | 1, 2 |
| Regression | Moyen, client seul, sans test automatisé (`testing.md`) : parcours navigateur. Toute conversion passe déjà par `toPixelsX/Y` et `toUnits` (`SceneCanvas.ts:181-194`) : pose au clic, `dropTarget` du catalogue (`:169`), glisser d'un jeton (`dragBoundFunc`, `:592`), mesure, ciblage et curseur clavier suivent la vue si seuls `pixelsPerUnit` et `origin` changent (`:223-228`). Risques : un redessin entre l'appui et le relâchement perd le clic de pose (`MapView.vue:124-128`), d'où un seuil avant tout déplacement de vue ; Konva émet `click` si l'appui et le relâchement tombent sur la même forme (`clickStartShape`, `node_modules/konva/lib/Stage.js:432-441`, `:675-711`) ; le redessin pendant le glisser détruit le sol, mais à ×1 rien ne se redessine, d'où un drapeau qui ignore le clic après tout glisser au-delà du seuil ; sur mobile, la carte occupe jusqu'à 70svh d'une page qui défile (`App.vue:1543-1548`) : la vue ne se déplace donc qu'à deux doigts au toucher, et `touch-action: pan-x pan-y` (pas `none`) laisse un doigt faire défiler la page tout en coupant le zoom de page au pincement ; le glisser de calage (`SceneCanvas.ts:132-143`, `App.vue:184`) passe au bouton du milieu, ce qui change un geste livré. Pas de phase filet : rien à figer côté serveur, le parcours de chaque phase rejoue pose, déplacement, ciblage, mesure, catalogue et calage. | 1, 2 |
| Architecture | Départ : `SceneCanvas` ajuste la carte entière au cadre, centrée (`SceneCanvas.ts:222-228`) ; aucune vue. Cible : une vue client `{ zoom, centre en fraction de carte }` (`map/view.ts`, sans Vue ni Konva) dont `SceneCanvas` tire `pixelsPerUnit` et `origin` ; hors serveur, hors partie ; remise à la carte entière par l'application à la création et au chargement d'une carte (`App.vue:377`, `:431`), car `MapView` n'est pas remonté entre deux cartes (`App.vue:1172`, sans `:key`). L'overlay de dés (`MapView.vue:251-256`, `pointer-events: none`) laisse passer molette et glisser. | 1, 2 |
| Documentation | `architecture.md` (vue client non gardée ; geste de calage au bouton du milieu), `codebase-map.md` (`map/view.ts`), `project-brief.md` (fonctionnalité ; calage), `DESIGN.md` (contrôles de zoom sur la carte ; geste de calage, ligne 281). Story : `status: done`. Plan : `status: implemented`. | 1, 2 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| La carte est rendue par Konva dans `src/map/`, sans dépendance à Vue (`architecture.md`) | La géométrie de la vue vit dans `map/view.ts`, les gestes dans `SceneCanvas`. |
| Le client mesure, le serveur tranche (`architecture.md`) | Le zoom ne change que l'affichage : positions envoyées et distances restent dans l'unité de la carte. |
| Clic de souris ou toucher sur une entrée du catalogue ne pose rien ; glisser pose (`architecture.md`) | Le glisser du catalogue capture le pointeur : la carte ne doit pas le prendre pour un déplacement de vue. |
| Ne jamais tester sur 8080/5173 ni écrire les fichiers du joueur (`testing.md`) | Parcours sur 8081/5174 avec les chemins du scratchpad. |
| `vtt-web` sans tests automatisés ; `npm run build` vérifie les types (`testing.md`, `coding-assertions.md`) | Preuve par build et parcours navigateur. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Zoomer en recalculant `pixelsPerUnit` et `origin` puis en redessinant, pas en mettant à l'échelle le `Stage` Konva | Étiquettes, traits et jauges gardent leur taille en pixels et toutes les conversions existantes restent justes. Contrepartie : un redessin complet par image, à surveiller sur une grande carte. |
| La vue est gardée en zoom et centre en fraction de carte, jamais sauvée | Elle survit au calage validé (la carte change d'échelle, pas d'image) et au redimensionnement ; ni serveur ni partie ne la connaissent. Contrepartie : un rechargement de page montre la carte entière. |
| Glisser le fond à la souris déplace la vue partout ; le calage passe au bouton du milieu ; au toucher, la vue se déplace à deux doigts seulement (choix du joueur) | Un seul geste pour la vue, calage compris ; un doigt garde le défilement de la page sur mobile. Contrepartie : au doigt, le calage ne se fait plus qu'aux champs du tiroir. |
| Zoom borné entre la carte entière (×1) et ×8 ; la carte ne quitte pas le cadre | Rien à perdre hors champ, et « Ajuster » ramène toujours au départ. |
