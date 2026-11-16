---
objective: "En création guidée, le joueur choisit un Magicien de niveau 1 : trois sorts mineurs, six sorts de grimoire et quatre préparés parmi eux, dans la liste du Magicien du SRD 5.2.1 ; la fiche porte sa caractéristique d'incantation, ses sorts (retouchables), son DD et son bonus d'attaque de sort, ses deux emplacements du 1er niveau ; un Bouclier lancé en combat dépense un emplacement qui baisse aussi sur la fiche, jamais au rejeu ; chaque sort ou règle que le moteur ne joue pas a sa story."
status: in-progress
---

# Plan: créer en guidé un Magicien

Source: aidd_docs/backlog/stories/creer-en-guide-un-magicien.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Magicien, sorts et emplacements dans `dnd-engine` | [`phase-1.md`](./phase-1.md) | none |
| 2   | Sorts au serveur et emplacements suivis par la fiche | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Sorts sur la fiche et en création guidée | [`phase-3.md`](./phase-3.md) | 2 |
| 4   | Stories des sorts non joués et mémoire | [`phase-4.md`](./phase-4.md) | 1, 2, 3 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf | Lu dans sa traduction française (notes Obsidian `Compendio SRD/Français/Personnages/Classes/Magicien.md`, `Sorts/*.md`, `Règles/Création de votre personnage.md`, CC-BY 4.0). Magicien : d6 ; PV 6 + Con ; sauvegardes Int, Sag ; 2 compétences parmi Arcanes, Histoire, Intuition, Investigation, Médecine, Nature, Religion ; armes courantes ; aucune armure ; A : 2 dagues, focaliseur arcanique (bâton de combat), robe, grimoire, paquetage d'érudit, 5 po ; B : 55 po ; valeurs suggérées For 8, Dex 12, Con 13, Int 15, Sag 14, Cha 10. Niveau 1 : Sorts (3 sorts mineurs, lumière, main du mage, rayon de givre recommandés ; grimoire de 6 sorts du 1er niveau, armure du mage, détection de la magie, feuille morte, projectile magique, sommeil, vague tonnante recommandés ; 4 sorts préparés choisis dans le grimoire ; 2 emplacements du 1er niveau ; Intelligence), Savoir rituel, Restauration magique (repos court). Liste : 15 sorts mineurs, 30 sorts du 1er niveau, école et mentions C (Concentration), R (rituel), M. Bouclier : réaction, +5 CA jusqu'au début du tour suivant. DD = 8 + modificateur + bonus de maîtrise ; attaque de sort = modificateur + bonus de maîtrise (règle connue, à recouper dans `Règles/Incantation.md` en phase 1). |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| architecture | Aujourd'hui aucune donnée de sort : `SpellList` ne sert qu'à Initié à la magie (`Background.java:14,22`), la fiche n'a que `spellSlots` saisis (`CharacterSheet.java`, `SpellSlots.java`), `GuidedValues` ne dérive pas les emplacements (javadoc), `CharacterSheet.profile` passe par `CreatureProfile.of`, sans sort ni emplacement (`CreatureProfile.java`), et `ShieldReaction` lit `knowsSpell("shield")` et `lowestAvailableSlot` (`ShieldReaction.java:31-32`). Changement : table `Spell` du moteur (sorts mineurs et du 1er niveau du Magicien, id moteur `shield` pour Bouclier) ; `CharacterClass.WIZARD` et ses traits d'incantation (caractéristique, nombres de sorts mineurs, de grimoire, préparés, emplacements) ; `Build` gagne `cantrips`, `spellbook`, `prepared` ; la fiche gagne `spellcastingAbility`, `cantrips`, `spellbook`, `preparedSpells`, DD et bonus d'attaque calculés ; `GuidedValues` dérive ces champs et les emplacements ; `profile` passe les sorts mineurs et préparés et les emplacements restants (max − dépensés) ; `SheetEvent.SpellSlotExpended(level)`. | phase 1 |
| architecture | Le suivi de la fiche ne passe que `DamageDealt` : `SceneSession.java:289-295` ne garde que lui, `SheetCharacters.sustained` filtre (`SheetCharacters.java:93-97`), `SheetResource.sustained` prend des montants (`SheetResource.java:233`). Changement : la session garde aussi `SpellSlotExpended` d'un personnage posé, et la fiche écrit un événement `expended` par emplacement, sous le même verrou et la même règle (après la garde, jamais au rejeu). | phase 2 |
| regression | Moyen : le constructeur de `Build` gagne trois listes (`Build.java` ×5, `BuildTest`, `GuidedValuesTest`, `SheetHistoryTest`, `BackgroundChoiceTest`, `CombatStatsTest.java:142`, `DndDtos.BuildDto.toBuild`, `SheetResourceTest`) ; celui de `CharacterSheet` gagne quatre champs (`blank`, `withHitPoints`, `GuidedValues.rebuild`, `DndDtos.SheetValuesDto.toSheet`, `CharacterSheetTest`, `CombatStatsTest`, `CharacterProfileTest`, `BackgroundChoiceTest.java:146,171`, `GuidedValuesTest.java:33,106,145,375`, `SheetHistoryTest.java:88`) ; `SheetResource.sustained` change de signature, appelé seulement par `SheetCharacters` (aucun test direct ; `SheetCharactersTest` passe par `PlayerCharacters.sustained`, inchangé). Le profil d'un personnage posé est gardé tel quel dans `SavedGame.Place` (record `CreatureProfile` sérialisé par Jackson, `SavedGame.java:40`) : sorts et emplacements suivent sans changement, à confirmer par un test de reprise (phase 2). Une fiche ou une construction d'avant, sans ces champs, doit se relire : test à ajouter (phase 2). `GuidedValues` réécrit désormais les emplacements à un changement de classe. Couvert par les tests existants ; pas de phase filet, la reprise et le suivi de PV sont déjà testés (`FicheSuitLeCombatTest`, `SheetCharactersTest`). | phase 1, 2 |
| runtime | Aucune dépendance hors dépôt. Parcours client (choix des sorts, grimoire puis préparés, retouche libre, emplacement baissé après Bouclier) non observé : à vérifier sur 8081 / 5174. | phase 3 |
| documentation | `architecture.md` (Magicien, sorts sur la fiche, emplacements suivis), `codebase-map.md` (`Spell`), `project-brief.md` (Magicien guidé, emplacements suivis), `testing.md` si `TestCharacters` change, backlog (stories des sorts non joués, liste de la tâche, story `done`). | phase 4 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Les moteurs n'ont aucune dépendance runtime | table des sorts et règles d'incantation dans `dnd-engine`, sans Jackson |
| Un module n'est nommé que par les assemblages | `SceneSession` ne connaît que `GameEvent`, jamais `dnd-server` |
| Code et messages en français, identifiants en anglais ; tests aux noms français | toutes les phases |
| `ErrorMapper` rend 400 pour `IllegalArgumentException` | refus de sorts hors liste, de nombre, de préparé hors grimoire |
| Règles tirées du SRD 5.2.1 ; texte français des notes Obsidian (CC-BY 4.0) | données du Magicien et des sorts |
| Une fiche d'avant se relit ; un document abîmé rend 409 et n'est jamais écrasé (`SheetStore`) | nouveaux champs de fiche et de construction |
| Le suivi de la fiche suit la garde de la partie, jamais au rejeu | emplacements dépensés, comme les PV |
| Tests navigateur sur 8081 / 5174 uniquement | phase 3 |
| Vérifications avant commit : `mvn -q compile`, `mvn -q test`, `npm run build` | chaque phase |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Grimoire de 6 sorts du 1er niveau, et 4 préparés choisis parmi eux | décision du joueur ; fidèle au SRD |
| Les sorts sont des champs structurés de la fiche (caractéristique d'incantation, sorts mineurs, grimoire, préparés), retouchables en saisie libre, choisis dans la table `Spell` du moteur | décision du joueur ; le profil de combat sait si le personnage prépare Bouclier, même sur une fiche libre |
| La fiche n'impose que niveau 0 pour un sort mineur, niveau 1+ pour grimoire et préparés, sans doublon ; « préparés ⊂ grimoire » n'est imposé que par la construction | une fiche libre d'une autre classe prépare sans grimoire |
| Le profil passe sorts mineurs et préparés (id moteur) et les emplacements restants (max − dépensés) | seuls les sorts préparés se lancent ; la pose part de la fiche comme pour les PV |
| `SheetEvent.SpellSlotExpended(level)` est un delta : dépensés + 1, jamais au-delà du maximum ; sans emplacement de ce niveau sur la fiche, ignoré | même logique que `HitPointsLost`, qui ne passe jamais sous 0 |
| `Spell` ne porte encore que la liste du Magicien ; les autres listes viendront avec « créer en guidé les autres lanceurs de sorts » | périmètre de la story |
| Les sorts non joués s'ouvrent en stories par comportement commun (attaque de sort, jet de sauvegarde, Projectile magique, Armure du mage, Concentration et effets à durée…), les sorts hors combat sans story | comme la tâche le permet (« ou par groupe de capacités au comportement commun ») |
