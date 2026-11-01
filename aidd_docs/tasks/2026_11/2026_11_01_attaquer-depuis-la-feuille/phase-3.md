---
status: pending
---

# Instruction: client — attaque depuis la feuille, ciblage, journal

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── vtt-web/src/
    ├── types.ts                   ✏️ CreatureSheet.attacks, JournalEntry, AttackResult
    ├── api.ts                     ✏️ attack(attackerId, attackId, targetId), journal()
    ├── App.vue                    ✏️ mode ciblage (exclusif du placement), journal, rafraîchissement de la feuille
    ├── map/SceneCanvas.ts         ✏️ état `targeting` : cercles d'allonge ou de portée autour de l'attaquant, jetons cliquables
    └── components/
        ├── CreatureSheet.vue      ✏️ une ligne par attaque, avec son bouton « Attaquer » et son allonge ou sa portée
        ├── MapView.vue            ✏️ transmet le ciblage, curseur de visée
        ├── TokenList.vue          ✏️ en ciblage, choisir une ligne vise la créature (double clavier de la carte)
        └── JournalPanel.vue       ✅ entrées groupées par action, la plus récente visible, annoncée (aria-live)
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081/5174 (`testing.md`), carte 30 × 20 m, Guerrière, Magicienne, Gobelin, Squelette adjacents ou à distance | scène prête, journal vide | browser |
| Happy path   | feuille de la Guerrière → « Attaquer » sur l'Épée longue | barre d'état « Ciblage… », cercle de 1,5 m autour de la Guerrière | browser |
| Happy path   | clic sur le Squelette adjacent | le journal montre l'annonce, le d20, le résultat (touché, raté ou critique) et les dégâts ; les PV de la feuille du Squelette baissent | browser |
| Happy path   | feuille du Gobelin → « Arc court » → clic sur la Magicienne à 15 m | résolu ; cercles de 24 m et 96 m affichés pendant le ciblage | browser |
| Happy path   | Magicienne → « Dague (lancer) » → cible à 9 m | le journal indique « désavantage : portée longue » | browser |
| Edge case    | Épée longue → cible à 10 m | message d'erreur avec la raison du serveur, le ciblage reste actif | browser |
| Edge case    | Échap en ciblage | ciblage annulé, rien n'est envoyé | browser |
| Edge case    | choisir une créature à poser pendant le ciblage | le ciblage s'arrête, le placement commence | browser |
| Edge case    | ciblage au clavier depuis « Sur la carte » | attaque résolue comme au clic | browser |
| Edge case    | recharger la page | le journal est rechargé depuis le serveur | browser |
| Teardown     | arrêter 8081 puis 5174, jamais 8080 ni 5173 | serveurs de l'utilisateur intacts | system |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────────────────┐
│ (1) Barre : Nouvelle carte [largeur] [hauteur] [m|ft] [Créer]         │
├────────────────┬───────────────────────────────────┬─────────────────┤
│ (2) Catalogue  │ (4) Carte                          │ (6) Sur la carte│
│  Personnages   │  ┌─────────────────────────────┐   │  · Guerrière    │
│  Monstres      │  │      .-----.                │   │  · Squelette    │
│                │  │     ( (G)(S) )   (g)        │   │                 │
├────────────────┤  │      '-----'                │   ├─────────────────┤
│ (3) Journal    │  └─────────────────────────────┘   │ (7) Feuille     │
│  ▸ Guerrière   │ (5) Ciblage : Épée longue de la    │  CA · PV · Vit. │
│    attaque     │     Guerrière. Cliquer la cible ;  │  Attaques       │
│    d20 : 15    │     Échap pour annuler.            │  Épée longue    │
│    TOUCHÉ      │                                    │   +5 · 1d8+3    │
│    11 dégâts   │                                    │   allonge 1,5 m │
│                │                                    │   [Attaquer]    │
└────────────────┴───────────────────────────────────┴─────────────────┘
```

1. Barre : inchangée.
2. Catalogue : inchangé. Choisir une entrée met fin au ciblage.
3. Journal : les lignes de chaque action, groupées sous un titre, la plus récente en bas et défilée en vue.
4. Carte : en ciblage, un cercle d'allonge ou deux de portée (normale en trait plein, longue en tirets) autour de l'attaquant. Ils sont indicatifs.
5. Barre d'état : le mode ciblage, l'attaque, la marche à suivre ; sinon les messages actuels.
6. Sur la carte : en ciblage, une ligne choisie vise la créature.
7. Feuille : une ligne par attaque (libellé, bonus, dégâts, allonge ou portée) et son bouton « Attaquer ». Le bouton est actif pendant le ciblage de cette attaque.

## Tasks to do

### `1)` Attaques sur la feuille et appels

> La feuille propose chaque attaque, et le client sait l'envoyer.

1. `types.ts` et `api.ts` suivent les DTO de la phase 2.
2. `CreatureSheet` affiche les attaques et émet `attack(attackId)`.

### `2)` Mode ciblage

> Une attaque se lance en deux clics, ou au clavier.

1. `App` : état `targeting {attackerId, attack}`. Il est exclusif du placement et s'annule avec Échap. Le clic sur un jeton, ou sur une ligne de `TokenList`, appelle `api.attack`.
2. En cas de succès, les entrées sont ajoutées au journal et la feuille affichée est rechargée. En cas de refus, le message du serveur s'affiche et le ciblage reste actif.
3. `SceneCanvas` dessine les cercles à partir des longueurs de la feuille, sans règle (le client mesure, le serveur tranche).

### `3)` Journal

> Chaque action laisse une trace lisible.

1. `JournalPanel`, placé sous le catalogue, est chargé au démarrage par `api.journal()` et vidé à la création d'une carte.
2. La dernière action est annoncée aux lecteurs d'écran.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe. La feuille de la Magicienne montre deux attaques, avec allonge et portée. |
| 2 | Le parcours du Test Scope réussit dans le navigateur sur 5174 : attaque au clic et au clavier, refus de portée affiché, Échap. Les PV de la cible baissent sur sa feuille. |
| 3 | Le journal montre le jet, le résultat et les dégâts de chaque attaque, et survit au rechargement de la page. |
