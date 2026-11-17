---
status: done
---

# Instruction: stories des sorts non joués et mémoire

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
aidd_docs/
├── backlog/
│   ├── stories/
│   │   ├── creer-en-guide-un-magicien.md                       ✏️ status done
│   │   ├── lancer-un-sort-d-attaque.md                         ✅ jet d'attaque de sort (Trait de feu, Rayon de givre, Poigne électrique…)
│   │   ├── lancer-un-sort-a-jet-de-sauvegarde.md               ✅ DD de sort (Aspersion acide, Mains brûlantes, Vague tonnante…)
│   │   ├── lancer-projectile-magique.md                        ✅ dégâts sans jet, Bouclier qui l'annule
│   │   ├── tenir-les-effets-a-duree-et-la-concentration.md     ✅ Bouclier jusqu'au tour suivant, Armure du mage, Concentration
│   │   └── se-reposer-court-et-long.md                         ✏️ Restauration magique, emplacements
│   └── tasks/
│       └── lister-les-capacites-de-niveau-1-que-le-moteur-ne-joue-pas.md ✏️ lignes du Magicien et de ses sorts
└── memory/
    ├── architecture.md      ✏️ Magicien, sorts de la fiche, DD et attaque, emplacements suivis
    ├── codebase-map.md      ✏️ Spell ; CharacterClass (cinq classes)
    ├── project-brief.md     ✏️ Magicien guidé ; emplacements dépensés suivis par la fiche
    └── testing.md           ✏️ seulement si `TestCharacters` change (Magicienne de test)
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | relire la liste de la tâche et les 45 sorts de `Spell` | chaque sort et règle du Magicien repéré | system |
| Happy path   | ajouter les lignes du Magicien à la liste | chaque ligne a « Jouée » et une story, ou « hors combat » / « portée par la fiche » | system |
| Happy path   | ouvrir les stories par comportement commun | chaque story a son epic, son `order` et son acceptance qui rappelle la liste | system |
| Edge case    | sort qui relève de deux groupes (Couteau de glace : attaque puis sauvegarde) | cité dans les deux stories | system |
| Edge case    | groupe déjà couvert par une story existante (repos) | story existante complétée, pas de doublon | system |

## Tasks to do

### `1)` Liste et stories

> Chaque sort ou règle du Magicien que le moteur ne joue pas a sa story.

1. Liste de la tâche : Sorts (préparation, grimoire), Savoir rituel et rituels, Restauration magique, Concentration, chaque groupe de sorts ; Bouclier « joué » (sauf la durée) ; sorts utilitaires « hors combat ».
2. Stories créées (`type: story`, `status: proposed`, epic de combat ou des personnages selon le comportement), ordonnées après les stories existantes ; `se-reposer-court-et-long` complétée.
3. Story du Magicien `done`.

### `2)` Mémoire

> La mémoire décrit ce que le code fait désormais.

1. `architecture.md` : Magicien, table `Spell`, champs de sort de la fiche, DD et attaque, profil (sorts, emplacements restants), `SpellSlotExpended` suivi comme les PV, `GET …/dnd/spells` ; piège Magicienne mis à jour (Bouclier rejouable par une fiche).
2. `codebase-map.md`, `project-brief.md` ; `testing.md` si `TestCharacters` a changé.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Chaque sort de `Spell` et chaque règle du Magicien est dans la liste ; chaque ligne non jouée renvoie à une story existante du backlog |
| 2 | La mémoire nomme `Spell`, les champs de sort, `GET …/dnd/spells` et le suivi des emplacements ; le piège « aucune créature ne connaît Bouclier » est corrigé |
