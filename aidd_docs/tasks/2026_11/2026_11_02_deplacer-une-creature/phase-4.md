---
status: done
---

# Instruction: Client : glisser ou déplacer au clavier, distance parcourue et restante

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/src/
├── api.ts                         ✏️ moveToken(tokenId, x, y)
├── types.ts                       ✏️ Combat.movementLeft
├── App.vue                        ✏️ déplacement, touche D, mode, consignes, refus côté client
├── components/
│   ├── MapView.vue                ✏️ événement tokenMove ; curseur de case en mode déplacement
│   └── CreatureSheet.vue          ✏️ vitesse restante de la créature active
└── map/
    └── SceneCanvas.ts             ✏️ jetons glissables, aperçu (trait, cercle, bulle), refus visibles
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | Serveurs 8081/5174 ; carte 30 × 20 m, deux personnages et un monstre posés | Scène prête | browser |
| Happy path   | Hors combat, glisser un jeton | Aperçu « {d} parcourus » ; au relâché, le jeton reste sous le pointeur, journal inchangé | browser |
| Happy path   | En combat, glisser l'active | Cercle laiton de la vitesse restante autour du départ ; bulle « 4,5 m · reste 4,5 m » ; au relâché, entrée au journal, feuille « Vitesse 9 m · reste 4,5 m » | browser |
| Happy path   | Maj tenue pendant le glisser | Le fantôme se cale sur la case | browser |
| Happy path   | Clavier : sélectionner l'active, D, flèches, Entrée | Curseur de case, même bulle ; Entrée déplace ; Échap annule | browser |
| Edge case    | Glisser l'active au-delà du cercle | Bulle et anneau `--danger` « au-delà de la vitesse restante » ; au relâché, retour au départ, message de refus, rien envoyé | browser |
| Edge case    | Relâcher à moins d'une case d'un autre jeton | Retour au départ, « Trop près de … » | browser |
| Edge case    | Glisser hors de la carte | Retour au départ | browser |
| Edge case    | En combat, glisser une créature non active, ou hors combat (`movementLeft` null = illimité) | Aucun cercle, bulle « {d} parcourus » sans reste, déplacement libre, journal inchangé | browser |
| Edge case    | Pose ou ciblage en cours, décision ouverte, jeton en attente | Aucun jeton ne se glisse | browser |
| Edge case    | Clic simple sur un jeton | Sélection, pas de déplacement | browser |
| Edge case    | Fin du tour puis retour de la créature | Vitesse restante pleine sur la feuille et dans le cercle | browser |
| Teardown     | Arrêter 8081 puis 5174 | Serveurs de l'utilisateur intacts | system |

## Tasks to do

### `1)` API et types

> Le client appelle la route de déplacement et lit la vitesse restante.

1. `api.moveToken` → `ActionResult` ; `Combat.movementLeft: number | null`, où `null` signifie « illimité » : le client ne suppose aucune règle D&D.

### `2)` Glisser sur la carte

> Un jeton se glisse quand aucun mode ne l'interdit ; l'aperçu dit la distance et ce qui reste.

1. `SceneCanvasState.moving` (jeton glissé et sa limite : vitesse restante, ou `null` pour illimité) et `movable` (faux en pose, ciblage, décision, jeton en attente).
2. Groupe du jeton `draggable` avec `dragDistance` de quelques pixels ; au `dragmove`, position libre ou calée (Maj), trait du départ au fantôme, bulle « {parcouru} · reste {r} » (« {parcouru} » seul sans limite) ou « au-delà de la vitesse restante », anneau `--danger` si trop loin (avec limite) ou trop près (`occupant` sans le jeton déplacé).
3. Au `dragend`, rendre `onTokenMove(tokenId, x, y)` ou rien (hors carte) ; le jeton revient au départ en attendant le serveur.
4. Avec une limite, cercle laiton de la vitesse restante autour du départ pendant le glisser ; sans limite, aucun cercle.

### `3)` Déplacer depuis l'application

> Le client refuse ce qui est évident, le serveur tranche le reste.

1. `App.moveToken` : refus client (trop loin seulement avec une limite, trop près) avec message ; sinon `api.moveToken`, position mise à jour, `settleAction` pour le journal et le combat.
2. Un refus serveur (400) affiche la raison, le jeton reste au départ ; un 409 (décision en attente) passe par `attempt`, qui recharge la scène, comme pour les autres actions.

### `4)` Clavier

> D déplace la créature sélectionnée avec le curseur de case.

1. `MapView` : prop `moving` (jeton) ; le curseur part de la case du jeton, les flèches le déplacent, Entrée émet `tokenMove`, Échap annule ; annonce « {d} parcourus, reste {r} » (sans « reste » si illimité).
2. `App` : D avec une seule créature sélectionnée ouvre le mode et donne le focus à la carte ; chip « Déplacement · {nom} » ; consignes de la barre d'état (combat : « D : déplacer »).

### `5)` Feuille

> La feuille de l'active dit ce qui lui reste.

1. `CreatureSheet` : nouvelle prop `movementLeft` (`number | null`), passée par `App.vue` à la feuille de l'onglet Feuille seulement ; « Vitesse 9 m · reste 4,5 m » quand elle n'est pas `null` ; sans limite, la vitesse seule.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1    | `npm run build` passe avec les nouveaux types. |
| 2    | Pendant le glisser, la distance parcourue et la restante s'affichent ; trop loin ou trop près se voit avant le relâché. |
| 3    | Un déplacement au-delà de la vitesse restante est refusé avec la raison ; dans la limite, le journal et la vitesse restante suivent. |
| 4    | Au clavier seul, l'active se déplace et le lecteur d'écran annonce distance et reste. |
| 5    | La feuille de l'active montre la vitesse restante, pleine au début de son tour. |
