---
status: pending
---

# Instruction: sorts sur la fiche et en création guidée

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/modules/dnd/
├── sheet.ts               ✏️ Spell ; SheetValues + spellcastingAbility, cantrips, spellbook, preparedSpells ; Derived + spellSaveDc, spellAttackBonus ; Build et ClassOptions : sorts
├── api.ts                 ✏️ spells()
├── SpellsPanel.vue        ✅ la section Sorts de la fiche : caractéristique, DD, attaque, sorts mineurs, grimoire, préparés, ajout et retrait
├── CharacterSheetView.vue ✏️ SpellsPanel au-dessus des emplacements ; emplacements inchangés
└── GuidedCreation.vue     ✏️ partie Sorts pour une classe qui en lance : sorts mineurs, grimoire, préparés parmi le grimoire
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081 / 5174, campagne neuve avec D&D, personnage créé | fiche vide ; section Sorts sans caractéristique, DD et attaque « — » | browser |
| Happy path   | création guidée : Magicien | partie Sorts : 3 sorts mineurs, 6 au grimoire, 4 préparés cochés (recommandés) ; fiche : Intelligence, DD 12, attaque +4, 2 emplacements | browser |
| Happy path   | remplacer Sommeil par Bouclier au grimoire, le préparer à la place de Feuille morte | fiche : Bouclier préparé ; Feuille morte au grimoire seulement ; rechargé : même état | browser |
| Happy path   | fiche libre : ajouter Trait de feu en sort mineur, retirer Lumière | gardés au rechargement ; Création guidée inchangée | browser |
| Happy path   | combat sur 8081 : poser le Magicien, un Gobelin le touche, « Lancer Bouclier » ; revenir à la fiche | emplacements niveau 1 : 1 dépensé | browser |
| Edge case    | décocher un sort du grimoire préparé | il quitte aussi les préparés avant l'envoi | browser |
| Edge case    | cocher un 5e préparé, un 7e sort de grimoire, un 4e sort mineur | case éteinte | browser |
| Edge case    | Magicien → Guerrier | partie Sorts disparue ; sorts et emplacements retirés de la fiche | browser |
| Teardown     | arrêter 8081 puis 5174 | ports libérés ; 8080 et 5173 jamais touchés | system |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────────┐
│ (1) Création guidée ▾ … Classe [Magicien ▾] …                │
│ (2) Sorts                                                    │
│     Sorts mineurs (3) ☑ Lumière ☑ Main du mage ☐ Trait de feu│
│     Grimoire (6)      ☑ Armure du mage ☑ Bouclier …          │
│     Préparés (4)      ☑ Armure du mage ☑ Bouclier ☐ …        │
├──────────────────────────────────────────────────────────────┤
│ (3) Combat …  Attaques …  Autres attaques …                  │
│ (4) Sorts : incantation [Intelligence ▾]  DD 12  Attaque +4  │
│     Sorts mineurs : Lumière ×  Main du mage ×  [Ajouter ▾]   │
│     Grimoire : Armure du mage ×  Bouclier × …  [Ajouter ▾]   │
│     Préparés : Armure du mage ×  Bouclier × …  [Ajouter ▾]   │
│ (5) Emplacements de sort  Niveau | Maximum | Dépensés        │
└──────────────────────────────────────────────────────────────┘
```

1. Création guidée : la classe en liste, comme aujourd'hui.
2. Partie Sorts, seulement pour une classe qui en lance : cases groupées par niveau, comptées ; un préparé seulement parmi le grimoire ; chaque geste émet la construction entière.
3. Combat : inchangé.
4. Section Sorts de la fiche : caractéristique d'incantation en liste (« — » sans), DD et attaque calculés par le serveur ; chaque liste se retouche (ajout par une liste des sorts du niveau, retrait par ×) et s'enregistre comme une frappe.
5. Emplacements : la table actuelle ; les dépensés suivent le combat.

## Tasks to do

### `1)` Types et appels

> Le client connaît sorts, DD et attaque de sort.

1. `sheet.ts` : types `Spell`, champs de fiche, de dérivés, de construction et d'options ; `toRequest` les envoie.
2. `api.ts` : `spells(campaignId)`.

### `2)` Section Sorts de la fiche

> Le joueur voit et retouche ses sorts.

1. `SpellsPanel.vue` : caractéristique en liste, DD et attaque depuis `derived`, trois listes avec ajout et retrait.
2. `CharacterSheetView.vue` : la monte avant les emplacements ; ses changements passent par l'enregistrement de la frappe.

### `3)` Partie Sorts de la création guidée

> Le joueur choisit ses sorts de Magicien.

1. `GuidedCreation.vue` : partie Sorts pour une classe à incantation, sorts au choix de `ClassOptions`, cases éteintes au nombre atteint ; décocher un sort du grimoire le retire des préparés.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe ; les champs de sort font l'aller-retour au rechargement |
| 2 | Fiche Magicien : Intelligence, DD 12, attaque +4 ; une retouche libre est gardée au rechargement ; sans caractéristique, DD et attaque « — » |
| 3 | Magicien guidé : sorts recommandés cochés ; Bouclier préparé passe à la fiche ; un préparé décoché du grimoire quitte les préparés ; Magicien → Guerrier retire la partie et les sorts ; Bouclier lancé en combat laisse 1 emplacement dépensé sur la fiche |
