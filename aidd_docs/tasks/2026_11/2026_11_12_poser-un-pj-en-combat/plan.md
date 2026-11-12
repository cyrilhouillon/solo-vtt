---
objective: "En préparation de combat, le catalogue d'une campagne où D&D est actif liste ses PJ à la place de la Guerrière et de la Magicienne ; un PJ posé est une copie de sa fiche à la pose, gardée dans la partie et le combat, qui joue comme les autres créatures."
status: in-progress
---

# Plan: poser un PJ en combat depuis le catalogue

Source: aidd_docs/backlog/stories/poser-un-pj-en-combat-depuis-le-catalogue.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Moteur : attaques structurées et profil de créature | [`phase-1.md`](./phase-1.md) | none |
| 2   | Fiche : attaques structurées de bout en bout | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Catalogue de campagne et pose d'un PJ | [`phase-3.md`](./phase-3.md) | 1, 2 |
| 4   | Mémoire du projet et backlog | [`phase-4.md`](./phase-4.md) | 1, 2, 3 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf | Les 13 types de dégâts (« Damage Types ») ; Sarbacane : 1 dégât perforant fixe ; allonge 1,5 m, +1,5 m pour Allonge |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| Regression | Élevé. ~80 références à `guerriere` / `magicienne` dans 14 tests de `vtt-server` (`grep -rc`), dont `DecisionApiTest` (Bouclier), des fixtures de partie (`JsonFileGameStoreTest`, `ResumeGameTest`, `CombatsEnregistresTest:194`), cinq tests qui appellent `SceneSession.catalogEntry` hors CDI et `SceneApiTest:42` sur `/api/catalog` ; `SrdCatalogTest` dans `dnd-engine`. Filet : `TestCharacters` sert les deux prétirés sous les mêmes ids, en bean CDI pour les tests d'API et en statique pour les autres, si bien que les ids de jeton (`guerriere-1`) ne changent pas. Les tests de fiche (`CharacterSheetTest`, `GuidedValuesTest`, `SheetHistoryTest`, `SheetResourceTest`) changent avec le format des attaques. | 1, 2, 3 |
| Runtime | Les fiches déjà gardées (`modules/dnd.json`, attaques en texte) deviennent illisibles (409) : accepté par le joueur. Les parties, combats et cartes enregistrées qui citent `guerriere` / `magicienne` deviennent illisibles : accepté par la story. La réaction Bouclier n'est plus jouable dans l'application tant qu'aucun PJ ne connaît le sort (story Magicien). Non vérifié en exécution. | 2, 3 |
| Architecture | Aujourd'hui `CatalogResource` (`/api/catalog`, commun) et `SceneSession.catalogEntry` lisent `SrdCatalog` en statique ; `SavedCommand.Place` ne garde que `catalogId`. Après : port `PlayerCharacters` dans `dnd-engine`, implémenté par `dnd-server`, collecté par `vtt-server` (`CampaignCatalog`) sans nommer le module ; catalogue par campagne (`/api/campaigns/{id}/catalog`) ; `Place` garde le nom et le profil du PJ, le rejeu ne relit jamais la fiche. | 1, 3 |
| Documentation | `architecture.md` (catalogue par campagne, port, `Place`, attaques de fiche structurées), `codebase-map.md` (routes, nouveaux types), `project-brief.md` (fonctionnalités PJ), `testing.md` (source de PJ de test), statut de la story. | 4 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Un module se branche sans que le cœur le nomme (`architecture.md`) | `vtt-server/src` ne cite pas `dnd-server` : il collecte les implémentations du port |
| L'état ne change que par `Engine.emit` ; la partie se garde par ses commandes rejouées avec leurs dés | La copie du PJ doit vivre dans `Place`, pas être relue sur la fiche au rejeu |
| Moteurs sans dépendance runtime | Le port et le profil restent du Java pur dans `dnd-engine` |
| Dés toujours scriptés ; noms de test en français décrivant la règle (`testing.md`) | Nouveaux tests de profil, de catalogue et de rejeu |
| SRD 5.2.1 PDF officiel comme seule source de règle (`project-brief.md`) | Types de dégâts, allonge, Sarbacane |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Les attaques de fiche deviennent des données structurées (dés, bonus de dégâts, type parmi les 13 du SRD, allonge, portée normale et longue) ; les fiches d'avant sont illisibles | Demande du joueur : pas de texte libre à interpréter ; sans migration |
| Port `PlayerCharacters` dans `dnd-engine` (liste, puis copie à la pose), que `dnd-server` implémente et que le cœur collecte par CDI, filtré sur les modules actifs | Le cœur dépend déjà de `dnd-engine` et ne nomme aucun module ; `vtt-module-api` ne peut pas porter `Creature` |
| `SavedCommand.Place` garde `name` et `profile` (`CreatureProfile`, bloc de stats sérialisable) pour un PJ, `null` pour un monstre | Retoucher, renommer ou supprimer la fiche ne change ni la reprise ni le rejeu |
| L'id d'entrée d'un PJ est l'id de sa page ; une carte enregistrée le garde et la recharger reprend une copie fraîche de la fiche (409 si elle n'existe plus ou si D&D est inactif) | Une carte enregistrée est un modèle de départ, comme pour les monstres |
