---
status: pending
---

# Instruction: fiche, construction et options d'historique au serveur

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
dnd-server/src/main/java/dnd/server/
├── DndDtos.java          ✏️ SheetValuesDto + feats ; BuildDto + background (BackgroundChoiceDto) ; SpeciesChoiceDto + originFeat (OriginFeatChoiceDto) ; BackgroundOptionsDto, OriginFeatOptionsDto
├── SheetResource.java    ✏️ GET backgrounds, GET origin-feats
└── SheetStore.java       ✏️ storedBuild : historique gardé tronqué → 409 ; construction d'avant sans historique relue
dnd-server/src/test/java/dnd/server/
└── SheetResourceTest.java ✏️
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | campagne, fiche créée, `PUT build` Guerrier | 200, `build.background` `null`, `values.feats` `""` | api |
| Happy path   | `GET …/dnd/backgrounds` | 4 historiques : id, libellé, caractéristiques, don et son texte, compétences, outil, boîtes de jeux (Soldat), équipement A et B | api |
| Happy path   | `GET …/dnd/origin-feats` | 4 dons : id, libellé, texte ; listes de sorts et incantations d'Initié ; nombre de compétences de Doué | api |
| Happy path   | `PUT build` avec Soldat +2 For/+1 Con, A, dés | 200 ; `values.background` « Soldat », For 17, Athlétisme, attaques Arc court et Lance, `feats` Sauvagerie martiale ; `build.background` rendu tel quel | api |
| Happy path   | `PUT build` Humain, Polyvalent Vigilant | 200 ; initiative + 2 ; `build.species.originFeat` rendu | api |
| Happy path   | relire la fiche (`GET`) après un redémarrage simulé (nouveau `SheetResource` sur le même `ModuleData`) | même construction, mêmes valeurs | api |
| Edge case    | `modules/dnd.json` d'avant écrit à la main : construction sans `background`, valeurs sans `feats` | 200 ; `background` `null`, `feats` `""` | api |
| Edge case    | `built` gardé avec un `background` sans historique (`{"background": {}}`) | 409 | api |
| Edge case    | `PUT build` avec Guerrier Athlétisme + Soldat ; augmentation +3 ; Polyvalent pour un Nain ; retrait de l'historique | 400 avec la raison ; histoire inchangée | api |
| Edge case    | `GET backgrounds` sur une campagne inconnue | 404 | api |

## Tasks to do

### `1)` DTO de l'historique et du don

> La construction et la fiche passent l'historique et les dons, et une fiche d'avant se relit.

1. `OriginFeatChoiceDto` (don, liste, incantation, compétences dans l'ordre de `Skill`) ; `BackgroundChoiceDto` (historique, augmentations, équipement, boîte, incantation) ; `SpeciesChoiceDto.originFeat` ; `BuildDto.background`.
2. `SheetValuesDto.feats` : absent, `""`.
3. `SheetStore.storedBuild` : un historique gardé sans `background`, ou des augmentations absentes, rend la construction illisible (409).

### `2)` Options d'historique et de don

> Le client a de quoi proposer chaque choix.

1. `BackgroundOptionsDto` : id, libellé, caractéristiques, don (id, libellé, texte, liste fixée), compétences, outil, boîtes de jeux au choix (vide sauf Soldat), équipements A et B (id, libellé, texte).
2. `OriginFeatOptionsDto` : id, libellé, texte, listes de sorts et incantations (vides sauf Initié), nombre de compétences (3 pour Doué, 0 sinon).
3. `SheetResource` : `GET backgrounds`, `GET origin-feats`, 404 campagne inconnue, `@APIResponse` comme `species`.

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | Une construction avec historique et Polyvalent s'écrit, se relit et se rend à l'identique ; une fiche d'avant se relit sans historique, `feats` vide ; un historique tronqué rend 409 |
| 2 | `GET backgrounds` rend les 4 historiques et `GET origin-feats` les 4 dons, avec leurs sous-choix ; campagne inconnue 404 |
