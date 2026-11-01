---
type: task
status: ready
parent: aidd_docs/backlog/epics/jouer-un-combat-dnd-2024-en-local.md
---

# Tâche : séparer le moteur générique des règles D&D en deux modules Maven

## Outcome

Un module Maven qui porte le moteur générique, sans aucune notion de D&D, et `dnd-engine` qui en dépend pour toutes ses règles.

## Scope

- **Inclut :**
  - séparer en deux modules ce qui est générique (pile de résolutions, pipeline à phases, listeners, décisions, dés, journal d'événements) et ce qui est propre à D&D (créatures, armes, conditions, dégâts, attaque, règles) ;
  - supprimer la dépendance actuelle du cœur vers `GameState` et `GameEvent` (dans `Engine` et `Listener`) ;
  - mettre à jour la mémoire projet (architecture, carte du code, package) pour décrire les deux modules.
- **Exclut :**
  - l'enchaînement des tours, qui relève de sa story ;
  - tout changement du comportement des règles, y compris le Bouclier dont le +5 n'est jamais retiré ;
  - la sauvegarde d'une décision en attente ;
  - toute interface ou carte.

## Done When

- Le module générique compile et passe ses tests sans aucune référence à un type D&D.
- `dnd-engine` dépend du module générique, et ses tests de règles passent sans modification de leurs assertions.
- Rejouer le journal redonne toujours la même `GameState.signature()`.
- Aucun des deux modules n'a de dépendance runtime.
- La démo console se comporte comme avant.

## Completion Evidence

- La sortie de `mvn -q verify` à la racine.
- Une recherche qui ne trouve aucun import `dnd.` dans le module générique.
