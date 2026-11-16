---
status: pending
---

# Instruction: stories des capacités non jouées et mémoire

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
aidd_docs/
├── backlog/
│   ├── stories/
│   │   ├── creer-en-guide-un-barbare-un-moine-ou-un-roublard.md        ✏️ status done
│   │   ├── entrer-en-rage-en-combat.md                                 ✅
│   │   ├── porter-une-attaque-sournoise.md                             ✅
│   │   ├── frapper-a-mains-nues-en-action-bonus-avec-arts-martiaux.md  ✅
│   │   ├── choisir-des-outils-avec-doue.md                              ✅
│   │   ├── tenir-l-inventaire-du-personnage.md                          ✅ order 8, avant les lanceurs de sorts
│   │   └── appliquer-les-bottes-d-arme-en-combat.md                    ✏️ Bottes du Barbare et du Roublard
│   └── tasks/
│       └── lister-les-capacites-de-niveau-1-que-le-moteur-ne-joue-pas.md ✏️ lignes du Barbare, du Moine, du Roublard
└── memory/
    ├── architecture.md      ✏️ classes de la création guidée, Expertise, Défense sans armure, Arts martiaux, expertiseCount
    ├── codebase-map.md      ✏️ Tool ; CharacterClass (les quatre classes)
    └── project-brief.md     ✏️ Barbare, Moine, Roublard en création guidée
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | phases 1 à 3 livrées | fiches Barbare, Moine, Roublard construites | system |
| Happy path   | relire la liste de la tâche | Rage, Attaque sournoise, attaque à mains nues en action Bonus renvoient chacune à une story ; Défense sans armure, Arts martiaux (dé, Dex), Expertise « portées par la fiche » ; Argot des voleurs hors combat ; Bottes du Barbare et du Roublard à la story des Bottes | system |
| Happy path   | relire la mémoire | `architecture.md`, `codebase-map.md`, `project-brief.md` décrivent les trois classes telles que livrées | system |
| Edge case    | une capacité affichée sur l'une des trois fiches absente de la liste | ajoutée à la liste | system |

## Tasks to do

### `1)` Stories des capacités non jouées

> Chaque capacité de niveau 1 que le moteur ne joue pas a sa story.

1. Story « entrer en Rage en combat » (action Bonus, 2 par jour, Résistance contondants/perforants/tranchants, +2 aux dégâts de Force, durée et fin), parent l'epic, `proposed`, ordre après les stories existantes.
2. Story « porter une Attaque sournoise » (1d6 une fois par tour, Finesse ou À distance, Avantage ou allié à 1,50 m).
3. Story « frapper à mains nues en action Bonus avec Arts martiaux ».
4. Story « choisir des outils avec Doué » (trois maîtrises de compétence ou d'outil, dans la table `Tool`), parent l'epic, `proposed`.
5. Story « tenir l'inventaire du personnage » : la fiche porte des objets (armes, armures, bouclier, quantités, po) et ce qui est porté ou en main ; l'équipement de départ les y range ; ajouter, retirer, équiper à la main recalcule CA et attaques par les dérivations de la phase 1 ; parent l'epic, `ready`, `order` 8, les stories suivantes de l'epic décalées d'un rang.
6. `appliquer-les-bottes-d-arme-en-combat.md` : Barbare et Roublard en profitent aussi.
7. Tâche de liste : lignes Rage, Défense sans armure (Barbare, Moine), Bottes (Barbare, Roublard), Arts martiaux (dé et Dex ; action Bonus), Expertise, Attaque sournoise, Argot des voleurs.

### `2)` Mémoire et backlog

> La mémoire décrit ce qui est livré.

1. `architecture.md` : la création guidée couvre Guerrier, Barbare, Moine, Roublard ; Style selon la classe ; armes de Botte par classe ; Expertise et outil dans la construction ; table `Tool` ; CA sans armure ; Arts martiaux ; attaque à mains nues pour toute classe ; option tout en po ; `expertiseCount`, `tools`.
2. `codebase-map.md` : `Tool` ; `CharacterClass` porte les quatre classes de la création guidée.
3. `project-brief.md` : création guidée des quatre classes.
4. Story `done`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque capacité de niveau 1 du Barbare, du Moine et du Roublard figure dans la liste ; chaque « non » renvoie à une story existante |
| 2 | La mémoire ne décrit plus la création guidée comme Guerrier seul ; la story est `done` |
