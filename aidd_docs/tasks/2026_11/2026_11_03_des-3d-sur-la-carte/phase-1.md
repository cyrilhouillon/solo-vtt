---
status: pending
---

# Instruction: Dés 3D lancés sur la carte à la place du roulement de la piste

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/
├── package.json                  ✏️
├── package-lock.json             ✏️
└── src/
    ├── dice/
    │   ├── shapes.ts             ✅
    │   └── DiceRoller.ts         ✅
    ├── components/
    │   ├── MapView.vue           ✏️
    │   └── DiceTray.vue          ✏️
    └── App.vue                   ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `npm run build` avant toute modification, puis serveurs de test 8081/5174 (`testing.md`), carte, Guerrière, Squelette et Gobelin posés | build vert ; piste vide, aucun canvas de dés visible | system, browser |
| Happy path   | Guerrière attaque le Squelette à l'Épée longue (touché) | un d20 et un d8 sont lancés depuis le bas de la carte, rebondissent, s'arrêtent face visible sur les valeurs des entrées `dice` de la réponse réseau, restent un instant puis s'effacent ; alors seulement verdict, dés posés et cible de la piste, jauge du Squelette et bulle « −N » apparaissent | browser |
| Edge case    | Raté | seul le d20 est lancé ; « Raté » à l'arrêt | browser |
| Edge case    | Arc court en portée longue (désavantage) | deux d20 lancés ; à l'arrêt, celui que le serveur écarte est atténué ; la piste le barre | browser |
| Edge case    | Coup critique | deux dés de dégâts lancés, valeurs du serveur | browser |
| Edge case    | Bouclier posé pendant l'attaque | rien ne roule pendant la question ; les dés sont lancés après la réponse, avec le d20 tiré avant | browser |
| Edge case    | Nouvelle attaque pendant un lancer | le lancer en cours s'arrête net, sa cible reçoit ses PV et sa bulle, puis le nouveau lancer part | browser |
| Edge case    | Clic, survol, glisser sur la carte pendant un lancer | la carte réagit comme sans les dés | browser |
| Edge case    | Rechargement de la page | aucun dé lancé ; la piste montre la dernière attaque posée | browser |
| Edge case    | Émulation `prefers-reduced-motion: reduce` | aucun dé 3D, module 3D non chargé (onglet réseau) ; tout posé d'emblée | browser |
| Edge case    | Fenêtre redimensionnée pendant un lancer | les dés restent dans la carte, la carte ne change pas de taille à cause d'eux | browser |
| Edge case    | Thème clair puis sombre | dés lisibles dans les deux | browser |
| Teardown     | Arrêter 8081 puis 5174 | ports libres, 8080/5173 intacts | system |

## Wireframe

```txt
┌─ carte (map-frame) ──────────────────────────────┐
│ (1) jetons Konva                                 │
│        ○ Squelette   (4) [ −11 ]                 │
│                                                  │
│            (2) ◆20   ◆8      ← dés 3D qui roulent│
│                                                  │
└──────────────────────────────────────────────────┘
┌─ (3) piste à dés (hauteur fixe) ─────────────────┐
│ verdict · calcul · annonce   [15] ╎ [6]   cible  │
└──────────────────────────────────────────────────┘
```

1. Carte Konva : inchangée, toujours interactive sous les dés.
2. Couche des dés : canvas WebGL transparent de la taille de la carte, sans pointeur ; dés lancés du bas, effacés après l'arrêt.
3. Piste : verdict, dés posés (valeurs) et cible apparaissent à l'arrêt des dés 3D ; plus de faces de passage.
4. Bulle d'issue et jauge de la cible : à l'arrêt, comme aujourd'hui.

## Tasks to do

### `1)` Dépendances

> Le client peut dessiner et simuler en 3D.

1. Ajouter `three` 0.186.1 et `cannon-es` 0.20.0 en dépendances, `@types/three` 0.186.0 en dépendance de développement, versions exactes.

### `2)` Formes des dés

> Chaque dé a une forme physique et des faces numérotées.

1. `src/dice/shapes.ts` : pour d4, d6, d8, d10, d12 et d20, les sommets et faces (d10 en trapézoèdre pentagonal), la normale de chaque face, et la valeur de chaque face (1 à n ; d4 : valeur lue au sommet du haut, numéros aux coins des faces).
2. Une géométrie three à un groupe par face et sa forme `ConvexPolyhedron` cannon-es, tirées des mêmes sommets.
3. Le numéro d'une face est une texture dessinée au canvas (Alegreya SC, 6 et 9 soulignés), changeable après coup.

### `3)` Lancer qui finit sur la valeur du serveur

> La physique choisit le mouvement, le serveur la valeur.

1. `src/dice/DiceRoller.ts` : un monde cannon-es (sol, quatre murs aux bords de la carte, gravité), les dés lancés du bas de la carte avec vitesse et rotation au hasard (affichage seulement).
2. Simuler hors écran pas à pas (1/60 s) en enregistrant position et orientation de chaque dé, jusqu'au repos de tous ou un plafond (~4 s).
3. Pour chaque dé, trouver la face du dessus (normale la plus alignée sur le haut, d4 : sommet le plus haut), puis échanger ses numéros avec la face qui porte la valeur du serveur.
4. Rejouer les images dans une scène three (canvas transparent, lumière chaude, ombres douces sur le sol invisible), tenir ~0,8 s, atténuer le d20 écarté (`kept`), effacer ~0,4 s, résoudre la promesse.
5. `roll(attackDice, damageDice)` rend une promesse ; un nouvel appel, `cancel()` ou `destroy()` arrête le lancer en cours et résout sa promesse tout de suite ; redimensionner suit la taille de la carte.
6. Corps des dés : ivoire et encre pour le jet d'attaque, rubrique pour les dégâts, couleurs lues dans les jetons du thème.

### `4)` Couche sur la carte

> Les dés roulent au-dessus des jetons sans rien bloquer.

1. `MapView.vue` : la racine devient un cadre `position: relative` qui contient le `.map` de Konva (inchangé, toujours conteneur du `Stage`) et, à côté, le canvas des dés en `position: absolute; inset: 0`, `pointer-events: none`, `aria-hidden`, redimensionné par le même `ResizeObserver`. Les murs du monde physique suivent les bords de ce canvas, donc du cadre de la carte (marges comprises), pas le rectangle de la scène.
2. `rollDice(attackDice, damageDice): Promise<void>` exposé : sous `prefers-reduced-motion`, ou si WebGL ou le chargement (`import()` dynamique de `DiceRoller`) échoue, résout tout de suite.

### `5)` Arrêt piloté par l'application

> Verdict, PV et bulle se posent quand les dés 3D s'arrêtent.

1. `App.vue` : pour un résultat `live`, `settleAction` lance le roulement sans l'attendre (`rolling` vrai, `mapView.rollDice(...).then(...)` : PV du jeton et bulle, contenu actuel de `showImpact`, puis `rolling` faux) ; `attacking`, `answering` et le rafraîchissement des feuilles se libèrent comme aujourd'hui. Une nouvelle attaque pendant le lancer l'annule : sa promesse se résout, la précédente se pose, puis le nouveau lancer part.
2. `DiceTray.vue` : retirer faces de passage, minuterie et émission `landed` ; une prop `rolling` cache verdict, calcul, dés posés et cible à leur place (`waiting`), sans changer la hauteur.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` vert ; le bundle initial ne contient ni three ni cannon-es (un chunk séparé apparaît). |
| 2 | Chaque type de dé lancé seul (d4, d6, d8, d10, d12, d20) repose à plat sur une face (d4 : une pointe en haut), ses numéros lisibles. |
| 3 | Sur dix attaques, la face du dessus de chaque dé à l'arrêt porte la valeur de son entrée `dice` dans la réponse réseau ; aucun saut ni pivot à l'arrêt. |
| 4 | Clics, survol et glisser sur la carte inchangés pendant un lancer ; hauteur de la carte et de la piste identiques avant, pendant et après. |
| 5 | Verdict, PV de la carte et bulle apparaissent après l'effacement des dés, jamais avant ; rien ne roule au rechargement ni sous `prefers-reduced-motion`. |
