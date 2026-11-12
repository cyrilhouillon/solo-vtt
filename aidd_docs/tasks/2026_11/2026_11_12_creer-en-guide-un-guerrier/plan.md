---
objective: "Sur la fiche d'un personnage joueur, le joueur choisit Guerrier puis ses caractéristiques (tableau standard, achat de points ou dés), deux compétences, un Style de combat, l'équipement A ou B et trois Bottes d'arme ; la fiche se remplit selon le SRD 5.2.1, se retouche à la main, et changer un choix recalcule ce qui en dérive, retouche comprise."
status: implemented
---

# Plan: créer en guidé un Guerrier de niveau 1

Source: aidd_docs/backlog/stories/creer-en-guide-un-guerrier-de-niveau-1.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Construction du Guerrier dans `dnd-engine` | [`phase-1.md`](./phase-1.md) | none |
| 2   | Histoire de la fiche et routes de construction | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Section « Création guidée » de la fiche | [`phase-3.md`](./phase-3.md) | 2 |
| 4   | Stories des capacités non jouées et mémoire | [`phase-4.md`](./phase-4.md) | 1, 2, 3 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf | Source de règles du projet. Lu dans sa traduction française (notes Obsidian `Compendio SRD/Français`, CC-BY 4.0) : Guerrier (d10, sauvegardes For et Con, 2 compétences parmi 9, armes courantes et de guerre, toutes armures et boucliers, équipement A / B / C ; niveau 1 : Bottes d'arme sur 3 types d'arme, Second souffle 2 utilisations 1d10 + niveau, Style de combat, Défense recommandé) ; les quatre dons de Style de combat (Archerie +2 à distance, Défense +1 CA en armure) ; valeurs standard 15, 14, 13, 12, 10, 8, dés 4d6 trois plus hauts six fois, achat 27 points de 8 à 15 aux coûts 0-1-2-3-4-5-7-9, répartition suggérée du Guerrier For 15 Dex 14 Con 13 Int 8 Sag 10 Cha 12 ; armes (dégâts, propriétés, portée, botte), Cotte de mailles CA 16, Cuir clouté 12 + Dex, Bouclier +2 |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| architecture | Aujourd'hui une fiche n'est que ses valeurs saisies (`SheetEvent.SheetCreated` / `SheetEdited`, `SheetHistory.replay` rend un `CharacterSheet`). Changement : elle garde aussi sa construction guidée (`Build`) et ses jets de caractéristiques ; deux événements de plus, `SheetBuilt(build, values)` et `AbilitiesRolled(rolls)`, et `SheetHistory.replay` rend un `SheetState` (valeurs, construction, jets). Le recalcul est un pur calcul du moteur ; les valeurs qui en sortent sont écrites dans l'événement, pour qu'un changement de données SRD ne réécrive pas une fiche d'avant. | phase 1, 2 |
| architecture | Les dés de la fiche viennent du bean `Dice` du serveur, injecté dans `SheetResource` comme dans `MythicResource` (`MythicResource.java:66`), et sont gardés dans l'histoire. | phase 2 |
| regression | Moyen : `SheetHistory.replay` change de type de retour, ce qui touche `SheetResource.sheet`, `create` (`SheetResource.java:73`), `edit` (`:103`) et `DndDtos.SheetDto.of` (`DndDtos.java:99`) ; le constructeur de `SheetResource` gagne `Dice` (`SheetResourceTest.java:127`) ; `SheetStore.EventFile` gagne deux types ; `CharacterSheetView.vue` gagne une section. `SheetResourceTest` et `CharacterSheetTest` couvrent création, saisie et refus, mais aucun test ne relit un `dnd.json` d'avant écrit à la main (`:239-248` ne couvre que des documents abîmés) : la phase 2 ajoute ce test, sans phase filet séparée. | phase 2 |
| runtime | Le client remplace ses valeurs par la réponse du serveur après un choix : la saisie en attente doit partir avant (`saved()`), et ce remplacement ne doit pas compter comme une frappe (`watch` de `CharacterSheetView.vue`). Non observé : à vérifier dans le navigateur sur 8081 / 5174. | phase 3 |
| documentation | `architecture.md` (construction guidée, événements, routes), `codebase-map.md` (`dnd.engine.sheet` / `build`, `GuidedCreation.vue`), `project-brief.md` (fonctionnalité), backlog (stories des capacités non jouées, tâche de la liste, story au statut `done`). | phase 4 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Les moteurs n'ont aucune dépendance runtime ; le hasard est injecté (`Dice`) | données SRD, construction et jets de 4d6 dans `dnd-engine`, sans Jackson ni `Random` |
| Un module n'est nommé que par les assemblages | tout reste dans `dnd-server` et `vtt-web/modules/dnd/` |
| Code et messages en français, identifiants en anglais ; tests aux noms français ; dés toujours scriptés | toutes les phases |
| `ErrorMapper` rend 400 pour `IllegalArgumentException`, 404 `NotFoundException`, 409 `ClientErrorException` | refus d'une construction, fiche inconnue, dés déjà lancés |
| Règles et fiches tirées du PDF SRD 5.2.1 ; le texte français vient des notes Obsidian (CC-BY 4.0) | données des armes, armures et du Guerrier |
| Tests navigateur sur 8081 / 5174 uniquement | phase 3 |
| Vérifications avant commit : `mvn -q compile`, `mvn -q test`, `npm run build` | chaque phase |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Les choix guidés vivent **sur la fiche**, dans une section « Création guidée » (classe « — » = saisie libre), pas dans un assistant à la création | un choix doit rester modifiable ensuite et recalculer ce qui en dérive ; une fiche libre devient guidée en choisissant Guerrier |
| **Recalcul par différence** : la construction dérive un ensemble de champs (par caractéristique, par sauvegarde, par compétence, puis CA, PV, attaques, textes…) ; changer un choix réécrit chaque champ dont la valeur dérivée change, et seulement lui | « changer d'armure recalcule la CA, même retouchée » sans écraser les retouches qui ne dépendent pas du choix ; aucune retouche n'a à être suivie |
| Choisir Guerrier part d'une construction complète : tableau standard selon la répartition suggérée du SRD, Défense (recommandé), équipement A et ses trois armes en Bottes ; les deux compétences restent à choisir | la fiche est jouable dès le premier choix ; achat de points part des mêmes valeurs (elles coûtent 27) |
| Équipement **A ou B** seulement, comme la story ; l'option C (155 po) est hors champ | un achat d'équipement libre est un autre chantier |
| Les dés de caractéristiques se lancent **une fois par fiche** (409 ensuite) ; la valeur reste retouchable à la main | le tirage garde son sens sans interdire au joueur solo de corriger |
| Les données SRD (armes, armures, Guerrier) sont des tables Java de `dnd-engine`, servies au client par `GET …/dnd/classes` | une seule source ; le client ne recopie que le coût de l'achat de points pour l'afficher à la frappe |
