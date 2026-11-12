---
status: done
---

# Instruction: fiche, construction et options d'espèce au serveur

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-server/src/main/java/dnd/server/
├── DndDtos.java         ✏️ SheetValuesDto + size, speciesTraits ; BuildDto + species (SpeciesChoiceDto) ; SpeciesOptionsDto
└── SheetResource.java   ✏️ GET …/dnd/species
dnd-server/src/test/java/dnd/server/
└── SheetResourceTest.java ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | campagne en mémoire, fiche « Aria » construite en Guerrier | fiche sans espèce | api |
| Happy path   | `GET …/dnd/species` | 9 espèces, chacune avec tailles, vitesse, options (id, libellé, texte), compétences au choix, incantation au choix | api |
| Happy path   | `PUT …/sheets/{id}/build` avec Nain | réponse : espèce, taille, vitesse, traits, PV 12 ; construction rendue avec son espèce | api |
| Happy path   | relire la fiche | mêmes valeurs et même construction | api |
| Edge case    | `dnd.json` d'avant écrit à la main : `built` sans `species`, `edited` sans `size` ni `speciesTraits` | fiche lue, champs vides ; une construction avec espèce s'applique | api |
| Edge case    | option d'une autre espèce | 400 avec la raison, histoire inchangée | api |

## Tasks to do

### `1)` DTO

> La fiche et la construction portent l'espèce, et un fichier d'avant se relit.

1. `SheetValuesDto` : + `size`, `speciesTraits` ; `of` et `toSheet`.
2. `SpeciesChoiceDto(species, size, option, skill, spellcastingAbility)` dans `BuildDto` (`null` : pas d'espèce) ; `of` et `toBuild`.
3. `SpeciesOptionsDto` : id, libellé, tailles, vitesse, libellé et liste des options (`DescribedChoiceDto`), compétences au choix (`ChoiceDto`), incantation au choix, traits.

### `2)` Route des espèces

> Le client lit les espèces au serveur.

1. `SheetResource.species` : `GET …/dnd/species`, 404 campagne inconnue, `@APIResponse`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Une construction avec espèce s'écrit et se relit à l'identique ; un `dnd.json` d'avant se lit sans 409 |
| 2 | `GET …/dnd/species` rend les 9 espèces et leurs options ; campagne inconnue 404 |
