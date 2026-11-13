---
status: done
---

# Instruction: avertissement à l'écran de combat et parcours navigateur

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/src/
├── types.ts                  ✏️ ActionResult.warnings: string[]
└── components/GameScreen.vue ✏️ settleAction : avertissements dans le bandeau d'erreur
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081 / 5174 (`testing.md`) ; campagne avec D&D ; personnage « Aria » créé en guidé, PV 12 | fiche à 12 PV | browser |
| Happy path   | poser Aria et un Gobelin, lancer le combat, attaquer Aria jusqu'à toucher | jauge de la carte baissée ; Carnet → fiche d'Aria : PV actuels baissés du même montant | browser |
| Happy path   | terminer le combat, redémarrer le serveur 8081, rouvrir la fiche | PV toujours baissés | browser |
| Happy path   | ouvrir le rejeu du combat terminé, le lire jusqu'au bout, rouvrir la fiche | PV inchangés | browser |
| Happy path   | « Nouvelle carte », reposer Aria | jeton aux PV de la fiche | browser |
| Edge case    | désactiver le module D&D (bouton Modules), revenir au combat, frapper Aria encore posée | attaque résolue ; bandeau : « Fiche d'Aria non mise à jour : … » | browser |
| Edge case    | démarrage du serveur 8081 | `SheetCharacters` reçoit `SheetResource` sans erreur CDI | system |
| Teardown     | arrêter 8081 et 5174 ; scratchpad jeté | 8080 et 5173 jamais touchés | system |

## Tasks to do

### `1)` Le joueur voit quand la fiche n'a pas suivi

> Un avertissement de l'action s'affiche sans bloquer le combat.

1. `types.ts` : `warnings: string[]` dans `ActionResult`.
2. `GameScreen.settleAction`, en tête, avant les sorties sur décision ou action sans cible : si `result.warnings.length`, `error.value = result.warnings.join(' ')`.

### `2)` Le parcours tient dans le navigateur

> La story se vérifie de bout en bout sur les serveurs de test.

1. Lancer 8081 / 5174 selon `testing.md`, dossiers du scratchpad.
2. Dérouler le Test Scope ; relever les PV de la fiche avant, après le coup, après redémarrage et après rejeu.
3. Si l'injection de `SheetResource` échoue au démarrage : verrou statique de `SheetStore`, partagé par `SheetResource` et `SheetCharacters`, puis reprendre.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe ; une action avec avertissement l'affiche dans le bandeau, l'action restant résolue |
| 2 | Les quatre critères de la story s'observent dans le navigateur ; le module désactivé donne l'avertissement |
