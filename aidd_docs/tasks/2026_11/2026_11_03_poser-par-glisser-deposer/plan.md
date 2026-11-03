---
objective: "Le joueur pose une créature en la glissant du catalogue sur la carte, à la souris comme au doigt ; un clic ou un toucher sur le catalogue ne fait plus rien, et la pose armée ne reste qu'au clavier."
status: in-progress
---

# Plan: poser une créature par glisser-déposer

Source: aidd_docs/backlog/stories/poser-une-creature-en-la-glissant-du-catalogue-sur-la-carte.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Glisser une créature du catalogue et la déposer sur la carte | [`phase-1.md`](./phase-1.md) | none |
| 2   | Réserver la pose armée au clavier, textes et mémoire | [`phase-2.md`](./phase-2.md) | 1 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://w3c.github.io/pointerevents/#implicit-pointer-capture | Au toucher, le pointeur est capturé par l'élément de départ : les `pointermove` arrivent au bouton du catalogue, pas à la carte. La position se lit donc dans `clientX/clientY`, pas dans le pointeur de Konva. |
| https://developer.mozilla.org/en-US/docs/Web/CSS/touch-action | Sans `touch-action: none` sur l'entrée, le navigateur prend le geste au doigt pour défiler et émet `pointercancel`. |
| https://developer.mozilla.org/en-US/docs/Web/API/UIEvent/detail | Un `click` venu du clavier (Entrée, Espace) ou d'un lecteur d'écran a `detail === 0` ; un clic de souris ou un toucher a `detail >= 1`. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Non vérifié : le geste au doigt n'est vérifiable qu'en émulation (événements pointeur synthétiques, qui ignorent `touch-action`). Le joueur le valide sur tablette. Sous 900 px (`App.vue:1245`), la carte passe sous le catalogue : le glisser fait défiler la page près du bord de la fenêtre, à la souris comme au doigt (le défilement natif est coupé par la capture et `touch-action: none`). | 1 |
| Regression | Moyenne. La pose au clic (`App.vue:347` `onBackgroundClick`) devient une fonction partagée par le clic armé et le dépôt : jeton en attente, refus « Trop près », historique Ctrl+Z, `afterRosterChange` en combat. L'Échap du glisser ne doit pas aussi vider la sélection (`App.vue:781`). Le client n'a pas de tests automatisés (`testing.md`) : pas de phase filet, la pose au clavier et au clic armé se rejoue dans le navigateur à chaque phase. Serveur inchangé : même `api.placeToken`. | 1, 2 |
| Architecture | Départ : la pose passe par un mode (`armed`) puis un clic sur le sol Konva (`SceneCanvas.ts:220`). Cible : un glisser en événements pointeur partant du catalogue ; la carte convertit une position d'écran en position de carte (même aimant, même Maj) et dessine le fantôme ; `App` pose. Le mode armé reste, réservé au clavier. | 1, 2 |
| Documentation | `architecture.md` (pose et déplacement, aimant), `project-brief.md` (« posées au clic »), `DESIGN.md` (entrée armée `:176`, curseur de glisser `:293`) ; textes du catalogue, de la barre d'état et du tableau vide (`TokenBoard.vue:205`). Aucun fichier créé : `codebase-map.md` inchangé. Story : `status: done`. Plan : `status: implemented`. | 2 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Seul le client refuse une position à moins d'une case ; aimant au contact ; Maj cale sur la case (`architecture.md`) | Le dépôt suit les mêmes règles que la pose au clic. |
| La carte est rendue par Konva dans `src/map/`, sans dépendance à Vue (`architecture.md`) | Conversion écran → carte et fantôme dans `SceneCanvas`, pas dans un composant. |
| Ne jamais tester sur 8080/5173 (`testing.md`) | Parcours navigateur sur 8081/5174, sauvegarde dans le scratchpad. |
| Une créature posée en combat tire son initiative (`architecture.md`) | Le dépôt passe par le même chemin que la pose (`afterRosterChange`). |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Glisser en événements pointeur, pas en glisser-déposer HTML natif | Le natif ne marche pas au doigt sur la plupart des navigateurs mobiles ; les événements pointeur couvrent souris et doigt avec le même code. |
| `touch-action: none` sur les entrées du catalogue | Sinon le doigt fait défiler au lieu de glisser. Contrepartie : on ne fait plus défiler le catalogue en touchant une entrée (titres et marges défilent encore). |
