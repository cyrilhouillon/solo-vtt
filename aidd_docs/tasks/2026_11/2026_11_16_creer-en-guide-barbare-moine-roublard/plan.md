---
objective: "En création guidée, le joueur choisit Barbare, Moine ou Roublard de niveau 1 : la fiche prend PV, sauvegardes, compétences (et l'Expertise du Roublard), maîtrises, outils (l'outil du Moine choisi dans la table des outils du SRD), équipement A ou tout en pièces d'or, CA (Défense sans armure si meilleure), attaques (Arts martiaux, Finesse, mains nues pour toute classe) et capacités de niveau 1 selon le SRD 5.2.1, et chaque capacité que le moteur ne joue pas a sa story."
status: implemented
---

# Plan: créer en guidé un Barbare, un Moine ou un Roublard

Source: aidd_docs/backlog/stories/creer-en-guide-un-barbare-un-moine-ou-un-roublard.md

## Phases

| #   | Phase        | File                         | Depends on |
| --- | ------------ | ---------------------------- | ---------- |
| 1   | Barbare, Moine, Roublard et outils dans `dnd-engine` | [`phase-1.md`](./phase-1.md) | none |
| 2   | Construction et options de classe au serveur | [`phase-2.md`](./phase-2.md) | 1 |
| 3   | Choix des trois classes sur la fiche | [`phase-3.md`](./phase-3.md) | 2 |
| 4   | Stories des capacités non jouées et mémoire | [`phase-4.md`](./phase-4.md) | 1, 2, 3 |

## External sources

| URL   | What it settled   |
| ----- | ----------------- |
| https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf | Lu dans sa traduction française (notes Obsidian `Compendio SRD/Français/Personnages/Classes/Barbare.md`, `Moine.md`, `Roublard.md`, `Guerrier.md`, `Règles/Création de votre personnage.md`, `Glossaire/Attaque à mains nues.md`, `Glossaire/Expertise.md`, `Équipement/Équipement.md`, `Équipement/Outils/*.md`, CC-BY 4.0). Barbare : d12 ; sauvegardes For, Con ; 2 parmi Athlétisme, Dressage, Intimidation, Nature, Perception, Survie ; armes courantes et de guerre ; armures légères, intermédiaires, boucliers ; A : hache à deux mains, 4 hachettes, paquetage d'explorateur, 15 po ; B : 75 po ; niveau 1 : Rage (2, +2 dégâts), Bottes d'arme (2 armes de corps à corps courantes ou de guerre), Défense sans armure (10 + Dex + Con, bouclier permis) ; valeurs suggérées 15/13/14/10/12/8. Moine : d8 ; For, Dex ; 2 parmi Acrobaties, Athlétisme, Discrétion, Histoire, Intuition, Religion ; armes courantes et armes de guerre Légères ; un type d'outils d'artisan ou d'instrument de musique au choix ; aucune armure ; A : lance, 5 dagues, l'outil ou l'instrument choisi, paquetage d'explorateur, 11 po ; B : 50 po ; niveau 1 : Arts martiaux (armes de Moine = courantes de corps à corps et de guerre de corps à corps Légères ; 1d6 à la place des dégâts des mains nues et des armes de Moine ; Dex au lieu de For au toucher et aux dégâts ; attaque à mains nues en action Bonus ; ni armure ni bouclier), Défense sans armure (10 + Dex + Sag) ; 12/15/13/10/14/8. Roublard : d8 ; Dex, Int ; 4 parmi Acrobaties, Athlétisme, Discrétion, Escamotage, Intimidation, Intuition, Investigation, Perception, Persuasion, Tromperie ; armes courantes et armes de guerre Finesse ou Légère ; outils de voleur ; armures légères ; A : armure de cuir, 2 dagues, épée courte, arc court, 20 flèches, carquois, outils de voleur, paquetage de cambrioleur, 8 po ; B : 100 po ; niveau 1 : Expertise (deux maîtrises de compétence au choix ; une compétence maîtrisée, une seule fois), Attaque sournoise (1d6, Finesse ou À distance, une fois par tour), Argot des voleurs (argot et une autre langue), Bottes d'arme (2 armes maîtrisées) ; 12/15/13/14/10/8. Guerrier : C : 155 po. Armure de cuir : 11 + Dex. Attaque à mains nues, pour toute créature : For + maîtrise au toucher, 1 + For contondants. Outils : 17 outils d'artisan (matériel d'alchimiste, de brasseur, de calligraphe, de peintre ; outils de bricoleur, de cartographe, de charpentier, de cordonnier, de forgeron, de joaillier, de maçon, de menuisier, de potier, de souffleur de verre, de tanneur, de tisserand ; ustensiles de cuisinier) ; autres : accessoires de déguisement, boîte de jeux (4 variantes), instrument de musique (10 variantes : chalemie, cor, cornemuse, flûte, flûte de pan, luth, lyre, tambour, tympanon, viole), instruments de navigateur, matériel de contrefaçon, d'empoisonneur, d'herboriste, outils de voleur. |

## Impact

| Category                                           | Verdict and evidence | Handled in         |
| -------------------------------------------------- | -------------------- | ------------------ |
| architecture | Aujourd'hui la création guidée suppose le Guerrier : `CharacterClass` n'a que `FIGHTER` (`CharacterClass.java:11`), `Build.suggested` un seul cas (`Build.java:61`), `Build.validate` exige un Style de combat (`Build.java:87`) et accepte toute arme en Botte (`Build.java:89-96`), `StartingEquipment` écarte l'option tout en pièces d'or (javadoc, `StartingEquipment.java:5`), `GuidedValues.armorClass` ne connaît qu'armure et bouclier (`GuidedValues.java:182`), `attack` ignore Arts martiaux et il n'y a pas d'attaque à mains nues, `features` écrit toujours le Style (`GuidedValues.java:211`), les outils ne sont qu'un texte de l'historique (`Background.java:29`, `GuidedValues.java:90-91`). Changement : `CharacterClass` porte ses données (Style ou non, armes de Botte permises, Expertises, outil fixe ou au choix, Défense sans armure, Arts martiaux) ; un enum `Tool` porte les outils du SRD, que `Background` et `CharacterClass` référencent ; `StartingEquipment` gagne l'option tout en po de chaque classe ; `Build` gagne `expertise` et `tool` ; `suggested` se dérive des données de classe ; `GuidedValues` calcule la CA sans armure, Arts martiaux, l'attaque à mains nues de toute classe, l'Expertise, les capacités par classe, et fusionne les outils sans doublon. | phase 1 |
| architecture | `BuildDto` gagne `expertise` et `tool` ; `fightingStyle` devient facultatif hors Guerrier ; `ClassOptionsDto` sert les Styles de la seule classe qui en a, les armes de Botte de la classe, `expertiseCount` et les outils au choix ; l'outil de l'historique est servi par son libellé. `SheetStore.storedBuild` n'exige le Style que si la classe en prend un. | phase 2 |
| regression | Moyen : le constructeur de `Build` gagne `expertise` et `tool` (`Build.java` ×3, `BackgroundChoiceTest`, `BuildTest` ×3, `GuidedValuesTest` ×2, `SheetHistoryTest` ×3, `DndDtos.BuildDto.toBuild`, `SheetResourceTest` ×5) ; celui de `CharacterClass` change ; `Background.tool` passe de `String` à `Tool` (`BackgroundChoice.toolText`, `DndDtos.java:285`) ; `BuildDto` change d'arité (`SheetResourceTest`) ; la signature de `GuidedValues.attack` gagne la classe (Arts martiaux), appelée par `CharacterProfileTest:89`. Toute fiche guidée gagne la ligne « Attaque à mains nues » au prochain choix qui réécrit ses attaques : les attentes d'attaques du Guerrier changent (`GuidedValuesTest`, `SheetResourceTest`). Couvert par ces tests. Une construction gardée d'avant, sans `expertise` ni `tool`, doit se relire : aucun test ne la couvre encore (phase 2 l'ajoute). Pas de phase filet : les tests du Guerrier existent. | phase 1, 2 |
| runtime | Aucune dépendance hors dépôt. Parcours client (changer de classe, Expertise, outil du Moine, option tout en po, absence de Style et de Bottes du Moine) non observé : à vérifier sur 8081 / 5174. | phase 3 |
| documentation | `architecture.md` (classes de la création guidée, outils, option tout en po, Expertise, Défense sans armure, Arts martiaux, mains nues), `codebase-map.md` (`Tool`, `CharacterClass`), `project-brief.md` (Barbare, Moine, Roublard), backlog (stories Rage, Attaque sournoise, attaque à mains nues en action Bonus ; liste de la tâche ; story `done`). | phase 4 |

## Rules

| Rule   | Why it applies |
| ------ | -------------- |
| Les moteurs n'ont aucune dépendance runtime | données de classe et d'outils dans `dnd-engine`, sans Jackson |
| Un module n'est nommé que par les assemblages | tout reste dans `dnd-server` et `vtt-web/modules/dnd/` |
| Code et messages en français, identifiants en anglais ; tests aux noms français | toutes les phases |
| `ErrorMapper` rend 400 pour `IllegalArgumentException` | refus d'Expertise, d'outil, de Botte ou de Style |
| Règles tirées du SRD 5.2.1 ; texte français des notes Obsidian (CC-BY 4.0) | données et textes des classes et des outils |
| Une fiche d'avant se relit ; un document abîmé rend 409 et n'est jamais écrasé (`SheetStore`) | `expertise` et `tool` absents des constructions gardées |
| Tests navigateur sur 8081 / 5174 uniquement | phase 3 |
| Vérifications avant commit : `mvn -q compile`, `mvn -q test`, `npm run build` | chaque phase |

## Decisions

| Decision   | Why   |
| ---------- | ----- |
| Chaque classe propose aussi son option tout en pièces d'or (Barbare B 75 po, Moine B 50 po, Roublard B 100 po, Guerrier C 155 po) : ni armure ni arme, « N po » dans Équipement | décision du joueur : l'Équipement reste un texte qu'il retouche à la main (objets, po) |
| Les outils sont une table du moteur (`Tool`) : 17 outils d'artisan, 10 instruments de musique, et les autres outils du SRD ; la boîte de jeux reste `GamingSet` | décision du joueur ; un outil choisi survit au recalcul de « Maîtrises », qu'un texte saisi à la main ne survivrait pas |
| Le Moine choisit un outil d'artisan ou un instrument (`Build.tool`), « à choisir » tant qu'il ne l'a pas fait ; son équipement A le contient | SRD ; même règle que la boîte de jeux du Soldat |
| Doué reste trois compétences, sans outil | hors du périmètre de la story ; à rouvrir maintenant que la table existe |
| Toute classe reçoit « Attaque à mains nues » : For + maîtrise, 1 + For contondants ; le Moine : meilleure de For et Dex au toucher et aux dégâts, 1d6 | SRD, Glossaire et Arts martiaux ; l'action Bonus du Moine a sa story |
| Arts martiaux s'applique aussi aux armes de Moine des attaques : meilleure de For et Dex, dé de l'arme ou 1d6 s'il est plus fort | équipement guidé du Moine sans armure ni bouclier |
| L'Expertise est un choix de la construction, parmi toutes les compétences maîtrisées (classe, espèce, historique, Doué), deux au plus, distinctes ; une compétence qui perd sa maîtrise est refusée (400), le client la retire avant l'envoi | SRD, « deux de vos maîtrises de compétence » et Glossaire |
| Argot des voleurs s'écrit dans Capacités ; Langues reste au joueur | `GuidedValues` ne dérive pas les langues |
| CA et attaques dérivent de ce qui est porté (armure, bouclier, armes) et de la classe, pas de `StartingEquipment` ; l'inventaire viendra ensuite, en story à part (`order` 8) | l'inventaire n'aura qu'à changer la source, sans réécrire les règles de classe |
| Style de combat requis pour le Guerrier, refusé pour les autres classes | seul le Guerrier en prend un au niveau 1 |
