---
status: done
---

# Instruction: inventaire, classe et valeurs calculées sur la fiche

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
vtt-web/modules/dnd/
├── sheet.ts               ✏️ Item, Inventory ; SheetValues suit le serveur ; Derived + armorClass, attacks ; EquipmentOptions
├── api.ts                 ✏️ equipment()
├── InventoryPanel.vue     ✅ la table des objets, les po, l'ajout ; émet chaque nouvel inventaire
└── CharacterSheetView.vue ✏️ Classe et Style en listes ; CA et attaques calculées en lecture, ajustement de CA et autres attaques en saisie ; InventoryPanel ; un geste d'inventaire part sans délai ; dérivés tirés de la réponse
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081 / 5174, campagne neuve avec D&D, Guerrier guidé A sans historique | Inventaire : cotte de mailles « Portée », trois armes « Tenue », paquetage, 4 po ; CA 17 ; trois attaques et mains nues en lecture | browser |
| Happy path   | ajouter Bouclier, le cocher | CA 19 après la réponse ; rechargé : même état | browser |
| Happy path   | décocher Fléau d'armes ; ajouter l'autre attaque « Trait de feu » | Fléau retiré des attaques calculées ; « Trait de feu » en fin | browser |
| Happy path   | ajustement de CA +1 | CA 20 | browser |
| Happy path   | ajouter « Corde de chanvre » ×2, changer les po | gardés au rechargement | browser |
| Happy path   | changer d'équipement A → C en création guidée | inventaire « 155 po », sans objet ; CA 10 + Dex | browser |
| Happy path   | fiche libre : choisir Roublard, cocher une armure de cuir | CA 11 + Dex | browser |
| Edge case    | cocher une seconde armure | refus affiché sur la fiche ; repart au changement suivant | browser |
| Edge case    | fiche sans classe | CA et attaques « — », invitation à choisir une classe | browser |
| Teardown     | arrêter 8081 puis 5174 | ports libérés ; 8080 et 5173 jamais touchés | system |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────────┐
│ (1) Classe [Guerrier ▾]  Style [Défense ▾]  Niveau …         │
├──────────────────────────────────────────────────────────────┤
│ (2) Combat : CA 19 (ajustement [+0])  Initiative …  PV …     │
│ (3) Attaques (calculées)                                     │
│     Épée à deux mains  +5  2d6 + 3 tranchants  1,5 m         │
│     Attaque à mains nues +5  4 contondants                   │
│ (4) Autres attaques [saisie, comme aujourd'hui] [Ajouter]    │
├──────────────────────────────────────────────────────────────┤
│ (5) Inventaire                                               │
│ ┌──────────────────────────────┬──────┬──────────┬───┐       │
│ │ Cotte de mailles (armure)    │  1   │ ☑ Portée │ × │       │
│ │ Épée à deux mains            │  1   │ ☑ Tenue  │ × │       │
│ │ Paquetage d'exploration…     │  1   │          │ × │       │
│ └──────────────────────────────┴──────┴──────────┴───┘       │
│ (6) Ajouter : [ Arme ▾ | Armure ▾ | texte ____ ] [Ajouter]   │
│ (7) Pièces d'or : [  4 ] po                                  │
└──────────────────────────────────────────────────────────────┘
```

1. Identité : la classe et le Style deviennent des listes des choix du moteur (`GET …/classes`) ; Style seulement pour une classe qui en prend un.
2. CA calculée en lecture, ajustement saisi à côté.
3. Attaques calculées, en lecture, résumées comme aujourd'hui.
4. Autres attaques : la table de saisie actuelle.
5. Une ligne par objet : nom (modifiable pour un texte), quantité, case Portée (armure) ou Tenue (arme, bouclier), retirer.
6. Ajout : arme ou armure du SRD (listes groupées par catégorie, `GET …/equipment`) ou objet en texte.
7. Les po.

## Tasks to do

### `1)` Types et appels

> Le client parle la fiche calculée du serveur.

1. `sheet.ts` : `Item`, `Inventory`, `SheetValues`, `Derived` ; `toRequest` assure quantités, po, ajustement, autres attaques nommées.
2. `api.ts` : `equipment`.

### `2)` Fiche

> Le joueur choisit sa classe, tient son inventaire et lit CA et attaques calculées.

1. `InventoryPanel.vue` : lignes, ajout, po ; quantité, nom et po émis au `change` ; libellés accessibles par ligne.
2. `CharacterSheetView` : Classe et Style ; CA et attaques depuis `derived` de la dernière réponse (lecture et chaque enregistrement) ; un geste d'inventaire enregistre aussitôt, sans le délai d'une seconde.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe |
| 2 | Chaque ligne du Test Scope observée sur 5174, consignée dans `run.md` |
