---
objective: "Un module Maven vtt-engine porte le moteur générique sans aucun type D&D, dnd-engine en dépend, et les tests et la démo se comportent comme avant."
status: implemented
---

# Plan: séparer le moteur générique des règles D&D

Source: aidd_docs/backlog/tasks/separer-le-moteur-generique-des-regles-dnd.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Rendre le cœur générique sur place | [`phase-1.md`](./phase-1.md) | none |
| 2   | Extraire le cœur dans le module vtt-engine | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Mettre à jour la mémoire projet | [`phase-3.md`](./phase-3.md) | 2 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| none | none |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Runtime | aucun : zéro dépendance runtime aujourd'hui (`dnd-engine/pom.xml`, JUnit en `test`, observé) ; pour vtt-engine, cible non vérifiée, contrôlée en phase 2 par `dependency:tree` | 2 |
| Regression | moyen : changement de signatures dans tous les fichiers de `attack/`, `rules/`, `demo/` et le test ; comportement inchangé. Filet : les 14 tests de `AttackRulesTest` (passent, `mvn -q test` observé) et la sortie de la démo capturée avant changement. Pas de phase de filet dédiée : les tests existants verrouillent déjà le comportement | 1, 2 |
| Architecture | départ : `core/Engine` importe `state.GameState` et `events.GameEvent`, `core/Listener` importe `state.GameState` ; la javadoc d'`Engine` cite `GameEvent` et `AttackAction`. Cible : cœur paramétré par l'état `S` et l'événement `E extends Event<S>`, dans un module séparé dont dnd-engine dépend | 1, 2 |
| Documentation | `architecture.md`, `codebase-map.md`, `package.md`, `coding-assertions.md`, `testing.md`, `project-brief.md` décrivent un seul module lancé depuis `dnd-engine/` ; `vcs.md` liste le scope de commit `core` ; javadoc de `demo/Main` donne `-cp target/classes` | 2, 3 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Aucune dépendance runtime (`package.md`) | vaut pour les deux modules |
| L'état ne change que via `Engine.emit` ; journal rejouable (`architecture.md`) | doit survivre à la généricité via `Event<S>.apply` |
| Dés toujours scriptés, noms de tests en français décrivant la règle (`testing.md`) | nouveaux tests du module générique |
| Code et commentaires en français, identifiants en anglais (`project-brief.md`) | tout fichier créé |
| Conventional Commits en français, commit après chaque phase (`vcs.md`) | fin de chaque phase |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Le cœur est paramétré par `S` (état) et `E extends Event<S>` (événement) : `Engine<S, E>`, `Resolution<S, E>`, `Listener<S, E, P, C>`, `Pipeline<S, E, P, C>`, `RuleBook<S, E>` | garde le typage fort côté règles (`emit`, `state()`) sans aucun type D&D dans le cœur |
| `Decision.DecisionHandler.onChoice(String option)` perd son paramètre `Engine` ; le handler capture le moteur | évite de rendre `Decision` et `StepResult` génériques ; seul `ShieldReaction` l'utilise |
| Alias par pipeline côté D&D : `AttackRule extends Listener<GameState, GameEvent, AttackPhase, AttackContext>` devient public | évite de répéter quatre paramètres dans chaque règle et test |
| Module générique `solovtt:vtt-engine`, package `solovtt.engine` ; pom agrégateur à la racine ; `dnd:dnd-engine` garde ses coordonnées | le package ne doit plus commencer par `dnd.` ; coordonnées de dnd-engine inchangées faute de consommateur |
