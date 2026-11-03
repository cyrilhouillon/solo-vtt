---
status: done
---

# Instruction: scénario de l'epic en test de session

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── vtt-server/src/test/java/solovtt/server/
    └── CombatDeBoutEnBoutTest.java ✅
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | `mvn -q test` à la racine avant tout ajout | tous les tests passent | cli |
| Happy path   | Carte 30 × 20 m ; Guerrière et Magicienne contre Gobelin, Squelette et Loup ; lancer le combat, déplacer la créature active, attaquer (touché, raté, Bouclier avec réponse), finir les tours, mettre un adversaire à 0 PV, terminer le combat | chaque action du moteur ajoute au moins une entrée au journal (lancement, déplacement de la créature active, attaque, réponse, fin de tour, fin du combat) ; pose, retrait et déplacement libre restent hors journal par conception (`architecture.md`) ; PV finaux attendus | system |
| Edge case    | L'adversaire à 0 PV, dont le tour s'ouvre quand même (`architecture.md`), tente d'attaquer | `attackRefusal` présent, `attack` lève `IllegalArgumentException` (`AttackAction.of`), journal inchangé | system |
| Edge case    | Session neuve sur le même `InMemoryGameStore` après `endCombat` | même scène, même journal, mêmes signatures de créatures, aucun combat | system |

## Tasks to do

### `1)` Filet de sécurité

> Partir d'une base verte.

1. Lancer `mvn -q test` à la racine ; si rouge, s'arrêter et le signaler, sans toucher la production.

### `2)` Écrire le scénario complet

> Jouer la preuve de l'epic sur `SceneSession`, dés scriptés.

1. Créer `CombatDeBoutEnBoutTest` sur le modèle de `ResumeGameTest` (`SceneSession` + `InMemoryGameStore` + `Dice.scripted`).
2. Poser les 5 créatures du catalogue, jouer au moins un round complet puis le combat jusqu'à `endCombat`.
3. Vérifier après chaque action du moteur qu'une entrée du journal la décrit, et que pose, retrait et déplacement libre n'en ajoutent pas.
4. Rouvrir une session sur le même stockage et comparer scène, journal et signatures.
5. Lancer `mvn -q test` : vert.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `mvn -q test` passe avant l'ajout. |
| 2 | `CombatDeBoutEnBoutTest` passe ; il échoue si une action du moteur n'apparaît pas au journal ou si la reprise diffère. |
