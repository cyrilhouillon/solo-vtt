---
objective: "Quand une attaque est tranchée, ses dés 3D (d20 et dés bonus, dés de dégâts) sont lancés sur la carte, roulent physiquement et s'arrêtent sur les valeurs du serveur, puis s'effacent ; la piste, la bulle d'issue et les PV se posent à l'arrêt."
status: implemented
---

# Plan: dés 3D qui roulent sur la carte

Source: demande de l'utilisateur du 2026-11-03 (« des dés plus jolis, qui roulent vraiment ») ; options tranchées : three + cannon-es écrits chez nous, dés sur la carte. dice-box (Babylon) écarté : il lit la face par rayon (`getRollResult`, `Dice.js:33239`) et ne peut pas forcer la valeur du serveur.

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Dés 3D lancés sur la carte à la place du roulement de la piste | [`phase-1.md`](./phase-1.md) | none |
| 2   | Mettre à jour le design, la mémoire projet et la story | [`phase-2.md`](./phase-2.md) | 1 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://www.npmjs.com/package/three | Dernière 0.187.0 (2026-10-30, moins de deux semaines) : figer 0.186.1 (2026-09-24). `@types/three` : figer 0.186.0 (2026-09-11). |
| https://www.npmjs.com/package/cannon-es | 0.20.0, sans dépendance, types inclus (`dist/cannon-es.d.ts`), dernière publication 2022 : stable mais plus maintenu. |
| https://www.npmjs.com/package/@3d-dice/dice-box | 1.1.4 : résultat lu par la physique, aucune option de valeur imposée (observé dans `dist/Dice.js:33239` du paquet téléchargé par `npm pack` le 2026-11-03, hors dépôt). |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | Non vérifié : WebGL requis dans le navigateur du joueur. Sans contexte WebGL (ou échec du chargement du module), les dés se posent d'emblée comme sous `prefers-reduced-motion`. Poids : three + cannon-es chargés à la demande (`import()` dynamique) au premier lancer, hors du bundle initial (`index-*.js` : 316,47 Ko, 103,97 Ko gzip, mesuré par `npm run build` le 2026-11-03). | 1 |
| Regression | Moyen. Client seul, sans tests automatisés : `DiceTray.vue` perd son roulement (faces de passage, `ticker`) mais garde ses dés posés ; `App.vue` pilote désormais l'arrêt (`landed` → PV, bulle) ; le lancer n'est pas attendu dans `settleAction`, sinon `attacking` (`App.vue:484-497`), `answering` et le rafraîchissement des feuilles resteraient bloqués pendant le roulement. Le serveur, le journal et la sauvegarde ne changent pas. Filet : `npm run build` puis parcours navigateur sur 8081/5174 (attaque, raté, désavantage, Bouclier, critique, rechargement, `prefers-reduced-motion`, attaque pendant un lancer). | 1 |
| Architecture | Départ : la piste fait rouler des faces 2D et émet `landed` (`DiceTray.vue:93-118`). Cible : un module `vtt-web/src/dice/` sans Vue (comme `src/map/`) lance les dés dans un canvas WebGL transparent posé sur la carte et rend une promesse résolue à l'arrêt ; `App.vue` attend cette promesse avant de poser verdict, PV et bulle. La valeur affichée reste celle du serveur : la simulation choisit seulement le mouvement, puis les faces sont renumérotées. | 1 |
| Documentation | `DESIGN.md` : North Star « un seul mouvement », piste (roulement retiré), carte (dés 3D), Don't sur le mouvement. `architecture.md` : module `dice`, renumérotation des faces. `codebase-map.md` : `src/dice/`. `package.md` : three et cannon-es figés. `project-brief.md` : fonctionnalité. Story : critère « roulent sur la piste » ramené à « sur la carte ». | 2 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Les valeurs affichées sont celles du serveur, jamais un tirage du client (story, `architecture.md`) | La physique ne décide que du mouvement ; la face du dessus reçoit la valeur de l'entrée `dice`. |
| The Still Table Rule (`DESIGN.md`) | Le canvas des dés se superpose à la carte sans changer sa taille ni celle de la piste. |
| `prefers-reduced-motion` respecté (`DESIGN.md`, story) | Aucun dé 3D : valeurs, verdict, PV et bulle posés d'emblée. |
| Dépendances confinées à `vtt-web`, versions figées (`package.md`) | three 0.186.1 et cannon-es 0.20.0 exacts ; 0.187.0, publiée il y a moins de deux semaines, écartée par prudence. |
| Carte rendue sans dépendance à Vue (`architecture.md`) | Le moteur de dés vit dans `src/dice/`, en TypeScript seul, comme `src/map/`. |
| Ne jamais tester sur 8080/5173 (`testing.md`) | Parcours sur 8081/5174, sauvegarde dans le scratchpad. |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Simuler tout le lancer hors écran, lire la face du dessus, puis échanger les numéros de cette face et de celle qui porte la valeur du serveur, et rejouer les images enregistrées. | Le dé s'arrête naturellement, sans saut ni pivot final ; échanger deux numéros ne change pas la forme, contrairement à une rotation du modèle qui ferait traverser le sol. |
| three + cannon-es écrits chez nous, chargés à la demande. | Seule option qui garantit la valeur du serveur ; cannon-es n'est plus maintenu mais n'a aucune dépendance et la simulation est courte. |
| Les dés roulent sur la carte, la piste montre les valeurs posées après l'arrêt. | Choix de l'utilisateur : la carte offre la place de rouler sans agrandir la piste. |
