# dnd-engine : squelette de moteur de règles D&D 2024

Un petit noyau Java 21 (sans dépendance hors JUnit) pour illustrer une architecture de moteur de règles :
pipelines à phases, priorités au sein d'une phase, réactions qui suspendent le moteur, pile de résolutions
et journal d'événements rejouable. Le contenu de jeu est volontairement minimal : une attaque, deux conditions,
Bénédiction, Bouclier et le style de combat Duel.

## Lancer

```bash
mvn test                                              # 14 tests de règles
mvn -q compile
java -cp target/classes dnd.engine.demo.Main             # deux scénarios commentés
java -cp target/classes dnd.engine.demo.Main --interactif  # tu choisis pour la magicienne
java -cp target/classes dnd.engine.demo.Main --seed=42     # autre tirage
```

## Les idées, dans l'ordre où elles apparaissent dans le code

**1. Une action est un pipeline de phases** (`core/Pipeline`, `attack/AttackPhase`).
L'ordre de l'enum est l'ordre d'exécution. Quand les règles imposent un ordre (dégâts : ajustements →
résistance → vulnérabilité), ce sont des phases distinctes, pas des priorités.

**2. Tout est listener, y compris les règles de base** (`attack/CoreAttackRules`).
« Lancer le d20 » et « Bénédiction ajoute 1d4 » ont exactement le même statut. Ajouter un sort, un don ou
une capacité de classe = écrire un listener et l'enregistrer dans le `RuleBook`, sans toucher au moteur.

**3. Accumuler puis résoudre** (`AttackContext`, `RollAttack`).
Les sources d'avantage/désavantage et les dés bonus sont empilés pendant `MODIFY_ROLL`, puis résolus
une seule fois pendant `ROLL`. L'ordre des listeners n'a donc aucune importance dans cette phase.

**4. La priorité ne sert qu'à départager une même phase** (`Listener.priority()`).
Plus petit = plus tôt ; à égalité, ordre d'enregistrement (tri stable).

**5. Le moteur peut s'arrêter au milieu d'une action** (`Decision`, `Engine.answer`, `ShieldReaction`).
Un listener retourne une `Decision` ; le pipeline garde son curseur (phase + index) et le moteur rend la main
avec `StepResult.AwaitingDecision`. Pas de thread bloqué : l'UI, une IA ou un client réseau répond quand il veut.

**6. Une pile de résolutions** (`Engine.push`, `Resolution.Yielded`).
Un effet peut empiler sa propre résolution (Contresort, attaque d'opportunité…). Elle se résout entièrement,
puis la résolution du dessous reprend où elle en était.

**7. L'état ne change que par des événements** (`events/GameEvent`, `Engine.emit`).
Chaque événement sait s'appliquer à l'état. Les jets de dés sont eux-mêmes des événements. Rejouer le journal
sur l'état initial redonne l'état final : journal de combat, annulation, replay, synchro réseau, tests.

**8. Dés injectables** (`Dice.seeded`, `Dice.scripted`).
Les tests décrivent une situation de table exacte (« 9 au d20, puis 5 aux dégâts ») et vérifient le résultat.

## Ajouter une règle : exemple

```java
/** Invisible : l'attaquant a l'avantage (version simplifiée, sans la notion de « voir » la cible). */
public final class InvisibleAttacker implements Listener<AttackPhase, AttackContext> {
    public AttackPhase phase() { return AttackPhase.MODIFY_ROLL; }

    public boolean appliesTo(AttackContext ctx, GameState s) {
        return s.get(ctx.attackerId()).has(Condition.INVISIBLE);
    }

    public Optional<Decision> handle(AttackContext ctx, Engine e) {
        ctx.addAdvantage("attaquant invisible");
        return Optional.empty();
    }
}
// puis : StandardRules.create().register(new InvisibleAttacker());
```

Aucune autre modification n'est nécessaire : si la cible est aussi entravée et l'attaquant empoisonné,
`RollAttack` s'occupe toujours de l'annulation avantage/désavantage.

## Limites assumées et prochaines étapes

- **Structure de tour** : il manque `TurnStarted` / `TurnEnded` et des effets à durée. C'est ce qui permettra
  de retirer le +5 de Bouclier au début du tour suivant, de rendre la réaction, de gérer la concentration.
- **Décisions sérialisables** : le handler d'une `Decision` est une lambda. Pour sauvegarder une partie en
  attente (jeu en ligne), remplace-le par un type de décision + des paramètres.
- **Plusieurs déclencheurs simultanés** : quand deux réactions sont possibles au même moment, le moteur
  devrait poser une décision « dans quel ordre ? » au joueur concerné plutôt que d'utiliser la priorité.
- **Généralisation** : les sorts à jet de sauvegarde, les tests de caractéristique, les déplacements suivent
  le même modèle (un enum de phases + un contexte + des listeners).
- **Contenu** : brancher le chargement des données du SRD 5.2 (par exemple les JSON `srd-2024` d'Open5e)
  pour les créatures, armes et sorts, et garder en Java uniquement les comportements.

## Organisation

```
core/     moteur générique : Engine, Pipeline, Listener, RuleBook, Decision, Dice
state/    état du jeu : GameState, Creature, Weapon, Attack (allonge, portée, lancer), Condition, DamageType
events/   GameEvent (sealed) : tout ce qui se passe, et comment ça modifie l'état
attack/   l'action « attaque » : phases, contexte, règles de base 2024
rules/    règles optionnelles : conditions, Bénédiction, Bouclier, style Duel
demo/     Main : démonstration console
```

Les règles s'appuient sur le SRD 5.2 (CC-BY-4.0) de Wizards of the Coast.
