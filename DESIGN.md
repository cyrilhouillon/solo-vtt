---
name: Solo VTT
description: Une table de jeu en solo, posée sur un plateau de feutre bordé de bois.
colors:
  page: "#2a1910"
  wood: "#3b2418"
  wood-raised: "#4a2e1f"
  wood-sunken: "#2f1d13"
  rule: "#6b4a2c"
  control-border: "#a8865a"
  ink: "#efe6d2"
  ink-muted: "#cdbfa3"
  brass: "#dcb35c"
  on-brass: "#24170c"
  rubric: "#ef8070"
  felt: "#1f3a2e"
  felt-deep: "#183025"
  felt-rim: "#12241b"
  hp: "#8fbf7a"
  hp-low: "#e0a35a"
  token-character: "#4f84c4"
  token-monster: "#b5643a"
  token-rim: "#efe6d2"
  map-down: "#6f6a60"
  map-grid: "rgba(239, 230, 210, 0.25)"
  map-border: "#7f9a86"
  map-label-ground: "rgba(18, 36, 27, 0.88)"
  page-light: "#b98f5c"
  wood-light: "#dcbc8c"
  wood-raised-light: "#e6caa0"
  wood-sunken-light: "#cfab78"
  rule-light: "#9b7546"
  control-border-light: "#7a5530"
  ink-light: "#2a1a10"
  ink-muted-light: "#563c28"
  brass-light: "#7d520c"
  on-brass-light: "#fbf3e2"
  rubric-light: "#93231d"
  felt-light: "#93ab88"
  felt-deep-light: "#b4c6a7"
  felt-rim-light: "#6f8a64"
  hp-light: "#3f7a2e"
  hp-low-light: "#9a5a12"
  token-character-light: "#2f5f9a"
  token-monster-light: "#9a4a1c"
  token-rim-light: "#fbf3e2"
  map-down-light: "#8d887d"
  map-grid-light: "rgba(34, 52, 30, 0.41)"
  map-border-light: "#3f5537"
  map-label-ground-light: "rgba(226, 234, 218, 0.92)"
typography:
  display:
    fontFamily: "Alegreya SC, Georgia, serif"
    fontSize: "1.45rem"
    fontWeight: 700
    letterSpacing: "0.03em"
  headline:
    fontFamily: "Alegreya SC, Georgia, serif"
    fontSize: "1.6rem"
    fontWeight: 700
    lineHeight: 1.1
  verdict:
    fontFamily: "Alegreya SC, Georgia, serif"
    fontSize: "1.5rem"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "0.02em"
  title:
    fontFamily: "Alegreya SC, Georgia, serif"
    fontSize: "1.2rem"
    fontWeight: 700
    letterSpacing: "0.03em"
  tab:
    fontFamily: "Alegreya SC, Georgia, serif"
    fontSize: "1.05rem"
    fontWeight: 700
    letterSpacing: "0.04em"
  body:
    fontFamily: "Alegreya Sans, system-ui, sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.45
  label:
    fontFamily: "Alegreya SC, Georgia, serif"
    fontSize: "0.9rem"
    fontWeight: 500
    letterSpacing: "0.06em"
rounded:
  hairline: "2px"
  tag: "3px"
  control: "4px"
  tray: "8px"
  pill: "999px"
spacing:
  xs: "0.25rem"
  sm: "0.4rem"
  md: "0.6rem"
  lg: "1rem"
  xl: "1.5rem"
components:
  button:
    backgroundColor: "{colors.wood-sunken}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "0.35rem 0.8rem"
  button-hover:
    backgroundColor: "{colors.wood-raised}"
  mode-chip:
    backgroundColor: "{colors.brass}"
    textColor: "{colors.on-brass}"
    rounded: "{rounded.pill}"
    padding: "0.25rem 0.85rem"
  tab:
    textColor: "{colors.ink-muted}"
    typography: "{typography.tab}"
    rounded: "{rounded.control}"
    padding: "0.4rem 0.9rem 0.35rem"
  tab-active:
    textColor: "{colors.ink}"
  rail-button-open:
    backgroundColor: "{colors.wood-sunken}"
    textColor: "{colors.ink}"
    width: "2.6rem"
    height: "2.6rem"
  catalog-entry-armed:
    backgroundColor: "{colors.wood-raised}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
  attack-button-aiming:
    backgroundColor: "{colors.brass}"
    textColor: "{colors.on-brass}"
    rounded: "{rounded.control}"
  dice-tray:
    backgroundColor: "{colors.felt-deep}"
    textColor: "{colors.rubric}"
    typography: "{typography.verdict}"
    rounded: "{rounded.tray}"
    padding: "0.6rem 1.25rem"
  tag:
    textColor: "{colors.ink-muted}"
    rounded: "{rounded.tag}"
    padding: "0 0.4rem"
  tag-down:
    textColor: "{colors.rubric}"
  input-number:
    backgroundColor: "{colors.wood-sunken}"
    textColor: "{colors.ink}"
    rounded: "{rounded.control}"
    padding: "0.3rem 0.5rem"
---

# Design System: Solo VTT

## Overview

**Creative North Star: "Le Plateau de dés"**

Une table de D&D posée sur un plateau de feutre bordé de bois : le moteur lance sur la piste, le joueur lit le verdict. Le cadre est en bois (barre, rail, tiroir, panneaux), le jeu se passe sur le feutre (carte, piste à dés). Deux heures d'éclairage : le soir, noyer et feutre vert bouteille avec un texte ivoire ; en journée, érable et feutre sauge avec une encre brune. Le thème suit le système, puis le choix du joueur (`data-theme` sur la racine).

La densité est celle d'un outil ouvert toute une session : panneaux serrés, chiffres tabulaires, peu de vide décoratif. Les panneaux se composent comme un bloc de statistiques : titres en petites capitales, filet effilé dessous, chiffres clés en rubrique. L'ornement est permis tant que la carte reste lisible ; un jeu vidéo (néons, effets) et un tableau de bord d'administration interchangeable sont les deux mondes refusés.

Trois couleurs portent du sens, chacune pour un seul rôle : le laiton pour ce qui est vivant maintenant, la rubrique pour ce que le moteur a tranché, l'encre pour le reste.

**Key Characteristics:**
- Bois pour le cadre, feutre pour le jeu ; aucune surface neutre grise.
- Laiton réservé à l'état vivant, rubrique réservée au verdict du moteur.
- Le camp se lit par la forme (disque, carré), jamais par la seule couleur.
- Petites capitales Alegreya SC pour les titres, Alegreya Sans pour le texte.
- Un seul mouvement : le verdict qui se pose sur la piste.

## Colors

Une palette de matières (bois, feutre, encre) avec deux pigments de sens, déclinée en deux thèmes de mêmes rôles ; chaque clé `-light` est la valeur du thème clair.

### Primary
- **Laiton** (`brass`, `brass-light`) : le seul état vivant. Puce du mode en cours, entrée du catalogue armée, attaque visée (ligne cerclée, bouton plein), jeton sélectionné, déplacement en cours (trait, cercle de vitesse restante, puce « Déplacement »), anneau de la cible survolée, cercles d'allonge et de portée, mesure. Sur la carte, il arrive sous l'alias `--map-live`, même valeur. Texte posé dessus : `on-brass`. C'est aussi la couleur du contour de focus et de la sélection de texte.

### Secondary
- **Rubrique** (`rubric`, `rubric-light`) : ce que le moteur tranche. Verdict de la piste, issue d'une entrée du journal, chiffres clés du bloc de statistiques (CA, PV), mention « À terre », nom de la créature et intertitres de la feuille, filet effilé. Les erreurs et actions destructrices (`--danger`) ont aujourd'hui la même valeur, sous un nom distinct.

### Tertiary
- **Bleu de personnage** (`token-character`) et **Rouille de monstre** (`token-monster`) : le remplissage des jetons et de leur pastille de camp, cerclés de `token-rim`.
- **Vert de vie** (`hp`) et **Ambre de vie basse** (`hp-low`) : les jauges de PV, seules.

### Neutral
- **Noyer / Érable** (`page`, `wood`, `wood-raised`, `wood-sunken`) : le cadre. `page` sépare les panneaux (filets de 2px), `wood` est le fond des panneaux, `wood-raised` le survol et la ligne sélectionnée, `wood-sunken` le fond des contrôles et la latte paire du tableau.
- **Filet** (`rule`) : séparateurs de 1px, bord des étiquettes, barre de défilement.
- **Bord de contrôle** (`control-border`) : contour des boutons et champs, au-dessus de 3:1 sur le bois.
- **Ivoire / Encre brune** (`ink`, `ink-muted`) : texte principal et secondaire.
- **Feutre vert bouteille / Feutre sauge** (`felt`, `felt-deep`, `felt-rim`) : la table et la piste. `felt-deep` est aussi le sol de la carte (`--map-ground`).
- **Gris à terre** (`map-down`) : jeton et pastille d'une créature à 0 PV.
- **Grille** (`map-grid`) et **fond d'étiquette** (`map-label-ground`) : traits de la grille (2:1 sur le sol, assumé sous le seuil WCAG pour ne pas couvrir les jetons) et pavé sous les noms sur la carte.
- **Lisière** (`map-border`) : le bord de la carte, un feutre surpiqué au-dessus de 3:1 contre la table, pour voir où l'on peut poser.

### Named Rules
**The Brass Is Alive Rule.** Le laiton ne marque que ce qui est vivant à cet instant : un mode, une créature armée, une sélection, une cible, une attaque visée. Un état d'interface (tiroir ouvert, onglet actif) n'est pas un état vivant et reste en bois et encre.

**The Engine Speaks In Rubric Rule.** La rubrique est la voix du moteur : verdicts, issues, chiffres tranchés, « À terre », et les intertitres du bloc de statistiques. Aucune décoration ni appel à l'action n'est en rubrique.

**The Two Lights Rule.** Toute nouvelle couleur se déclare dans les deux thèmes, sous le même nom, dans `style.css` ; la carte lit ces variables au rendu, jamais une valeur en dur.

## Typography

**Display Font:** Alegreya SC (with Georgia, serif)
**Body Font:** Alegreya Sans (with system-ui, sans-serif)

**Character:** Une famille humaniste d'éditeur de jeux : les petites capitales donnent la voix du livre de règles aux titres et au verdict, la sans-serif garde la lecture rapide des feuilles et du journal. Les deux sont auto-hébergées (`@fontsource`, graisses 400/500/700 pour la sans, 500/700 pour les petites capitales).

### Hierarchy
- **Display** (700, 1.45rem, interlettrage 0.03em) : le nom de l'application dans la barre.
- **Headline** (700, 1.6rem, 1.1) : le nom de la créature en tête de feuille, en rubrique.
- **Verdict** (700, 1.5rem, 1.2, 0.02em) : le verdict de la piste à dés, en rubrique.
- **Title** (700, 1.2rem, 0.03em) : titres de panneau (Catalogue SRD, Nouvelle carte), suivis du filet effilé ; intertitres de feuille à 1.1rem, onglets à 1.05rem.
- **Body** (400, 16px, 1.45) : tout le texte courant ; les chiffres sont tabulaires (`font-variant-numeric: tabular-nums`) dans le tableau, la feuille, le journal et la barre d'état.
- **Label** (500, 0.85 à 0.95rem, 0.04 à 0.06em, petites capitales) : en-têtes du tableau, groupes du catalogue, étiquettes d'état, résumé « détails » du journal.

### Named Rules
**The Small Caps Are Headings Rule.** Alegreya SC sert aux titres, en-têtes, onglets, étiquettes et au verdict ; jamais à un paragraphe ni à un texte de plus d'une ligne.

## Layout

Grille d'application pleine hauteur : barre en haut, puis rail d'icônes (3.5rem), table au centre (fluide), panneau latéral à onglets (24rem). Un tiroir de 17rem s'insère entre le rail et la table quand Catalogue ou Nouvelle carte est ouvert. La table empile, de haut en bas : le tableau « Sur la carte » (cinq lignes visibles, 14rem, défile au-delà ; en combat hors ciblage, une bande d'initiative de trois lignes, 8.25rem, qui garde la créature qui joue en vue), la carte encadrée de 6px de bois à gauche et à droite, la piste à dés, la barre d'état.

- Sous 1280px : tiroir et panneau latéral ne partagent plus l'écran. Le tiroir ouvert replie le latéral en une bande de 3rem d'onglets verticaux (l'actif souligné d'un trait d'encre côté table) ; choisir un onglet ferme le tiroir et rouvre le panneau.
- Sous 1100px : latéral à 20rem.
- Sous 900px : une colonne ; la page défile, le cadre de la carte prend la forme de la carte (14rem minimum, 70svh maximum), le panneau latéral passe dessous (24rem minimum), le rail se couche à l'horizontale quand le tiroir est fermé.
- Le tableau tient sans défilement horizontal : sous 1280px, il masque le camp en toutes lettres (la forme de la pastille le dit déjà) ; entre 901 et 1100px et sous 700px, la jauge (le chiffre reste) ; sous 600px, l'état.

Le rythme est en rem : 0.25 entre les entrées d'une liste, 0.4 à 0.6 à l'intérieur d'une ligne, 0.75 à 1 entre blocs, 1 à 1.1 en marge de panneau. Les erreurs se posent par-dessus la carte sans la redimensionner.

## Elevation & Depth

Le système est plat et matériel : la profondeur vient des matières (bois plus clair au survol, plus sombre en creux, feutre en retrait) et des filets, pas d'ombres portées. Deux exceptions fonctionnelles seulement.

### Shadow Vocabulary
- **Feutre creusé** (`box-shadow: inset 0 3px 10px rgba(0, 0, 0, 0.45), inset 0 0 0 1px var(--felt-rim), 0 0 0 3px var(--rule)`) : la piste à dés, une cuvette de feutre dans son cadre de bois.
- **Bandeau flottant** (`box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3)`) : la bannière d'erreur posée sur la carte.
- **Soulignement d'onglet** (`box-shadow: inset 0 -3px 0 var(--ink)`) : l'onglet actif ; un trait, pas une élévation.

### Named Rules
**The Matter Not Shadow Rule.** Un élément monte en passant à `wood-raised`, descend en passant à `wood-sunken` ; aucune ombre portée sur les panneaux, boutons ou lignes.

## Shapes

Des angles à peine adoucis, comme du bois tourné : 4px pour les contrôles et les onglets (coins hauts seulement), 3px pour les étiquettes, 2px pour les jauges et la pastille de monstre, 8px pour la piste, une pilule pour la puce de mode. Les séparations sont des filets droits : 2px de `page` entre panneaux, 1px de `rule` dans les panneaux, 2px de `felt-rim` sous le tableau.

Deux formes portent le camp partout : le **disque** pour un personnage, le **carré aux coins arrondis** pour un monstre (20% du côté sur la carte, 2px en pastille de 0.8rem). La pastille du catalogue et du tableau répète la forme et la couleur du jeton, cerclée de 1.5px de `token-rim`.

Le **filet effilé** (2px, dégradé de la rubrique vers le transparent) suit chaque titre de panneau et sépare les blocs de la feuille.

## Components

### Buttons
Du bois en creux, francs et discrets.
- **Shape:** coins adoucis (4px), contour 1px `control-border`.
- **Default:** fond `wood-sunken`, encre, graisse 500.
- **Hover / Focus:** fond `wood-raised`, contour `ink-muted` ; focus visible par un contour laiton de 2px décalé de 2px. Désactivé : opacité 0.5.
- **Danger:** contour et texte `--danger`, fond inchangé (Remplacer la carte, retirer un jeton au survol).
- **Ghost:** entrées de liste, boutons du rail et de retrait : fond et contour transparents, fond `wood-raised` au survol.

### Chips
- **Mode en cours:** pilule laiton pleine, texte `on-brass` en 700, dans la barre. C'est la seule puce pleine du système.
- **Étiquettes d'état:** petites capitales 0.85rem, première lettre capitale, contour 1px `rule`, 3px ; « À terre » passe en rubrique (texte et contour) ; une portée qui permet l'attaque se cercle de laiton ; « À son tour » aussi, en encre grasse.
- **Contrôle de combat:** dans la barre, avant la pilule du mode. Hors combat, un bouton « Lancer le combat », désactivé sans créature (la raison au survol) ; en combat, « Round N · **Tour de X** » en `ink-muted` puis encre, et le bouton « Fin du tour ». Pendant « Qui joue ? », « Round N · **Qui joue ?** ».

### Navigation
- **Rail:** colonne de boutons d'icônes de 2.6rem sur `wood`, icônes SVG dessinées (trait 1.75, grille 24). Le bouton du tiroir ouvert reste en bois : fond `wood-sunken`, contour `rule`, encre. Jamais en laiton.
- **Onglets (Feuille, Cible, Journal):** petites capitales 1.05rem en `ink-muted` ; l'onglet actif passe à l'encre avec un soulignement de 3px d'encre.

### Inputs / Fields
- **Style:** champ numérique en creux (`wood-sunken`), contour 1px `control-border`, 4px, chiffres tabulaires, unité accolée à droite.
- **Focus:** le contour laiton commun. Radios et cases suivent `accent-color: brass`.

### Tableau « Sur la carte »
Un tableau des départs : en-têtes collants en petites capitales, lattes alternées (`wood-sunken` une ligne sur deux), ligne sélectionnée en `wood-raised` avec le nom en gras et la pastille cerclée de laiton, comme le jeton sur la carte. Chaque ligne porte la pastille de camp, le nom, le camp, la jauge de PV (4.5rem, `hp`, `hp-low` sous le seuil), les chiffres tabulaires (jauges alignées quel que soit le nombre de PV) et l'état. La colonne État a la largeur de sa plus longue étiquette, pour que les colonnes ne bougent pas : elle dit l'état de jeu de la créature, jamais celui de l'interface (la ligne sélectionnée et la feuille disent déjà quelle créature est ouverte). Hors ciblage : « Pose en cours… », « À terre », « Cible » (visée par la dernière attaque). En combat, les lignes suivent l'ordre d'initiative, une colonne « Init. » (abréviation, chiffre gras) ouvre la ligne, et la créature qui joue porte un filet laiton de 4px sur son bord gauche et l'étiquette « À son tour » ; celles qui ont joué ce round portent « Joué », les candidats de « Qui joue ? » « Au choix ». En combat, le tableau se resserre en bande de trois lignes (la créature qui joue défile en vue) ; le ciblage le rouvre en entier, le focus sur la première cible permise. En ciblage : « Attaquant » et, pour les autres, le verdict de portée. Une créature à terre : pastille grise, nom barré en `ink-muted`, étiquette « À terre » en rubrique.

### Bloc de statistiques (Feuille, Cible)
Nom en rubrique (headline), type en italique `ink-muted`, filet effilé, lignes libellé gras puis chiffre en rubrique (1.15rem). Intertitres en petites capitales rubrique soulignés d'un filet rubrique. Une attaque visée : la ligne se cercle de laiton sur `wood-raised`, son bouton passe en laiton plein. Quand les règles ferment les attaques (à terre, pas son tour), la raison du moteur s'écrit sous l'intertitre, en italique `ink-muted`, et les boutons sont désactivés. Les touches 1 à 9 lancent les attaques de la feuille ; F termine le tour.

### Piste à dés
Une cuvette de feutre `felt-deep` (8px) dans un cadre de bois, sous la carte. Elle montre la dernière attaque, qu'un changement de tour n'efface pas (la barre dit qui joue) : le verdict en rubrique, l'action en dessous à 0.95rem, puis la réaction et les dégâts en 500, et à droite la cible avec sa jauge. Le verdict se pose : 320ms, `cubic-bezier(0.16, 1, 0.3, 1)`, de 0.4rem plus haut et transparent à sa place ; aucun mouvement sous `prefers-reduced-motion`. C'est le seul mouvement du système. La piste est muette pour les lecteurs d'écran : la région `aria-live` annonce l'issue, la réaction et les dégâts ; la barre d'état ne répète pas le verdict.

### Journal
Une entrée par action : titre en gras, issue en rubrique gras, détail des jets replié sous un résumé en petites capitales. Un nouveau round coupe l'action en cours : « Round N » s'affiche en intertitre de petites capitales `ink-muted`, à sa place.

### Dialogue de décision
Un panneau de bois modal sur un voile sombre, que seule une option ferme. Titre en rubrique : « Réaction · *créature* » pour une réaction, « Combat » pour « Qui joue ? ». La question dessous, puis une option par bouton.

### Carte (canvas)
Sol `felt-deep`, grille `map-grid` de 1px, bord `map-border`. Jetons pleins en couleur de camp, cerclés de 2px `token-rim`, initiales en Alegreya Sans 700 ; noms sur un pavé `map-label-ground`, montrés pour le jeton sélectionné, survolé, et celui qui joue. Sélection : contour laiton de 3.5px. Cible survolée : anneau laiton de 3px à 1.35 fois le rayon. Allonge (à la limite des règles, allonge + demi-case) et portée normale : cercle laiton plein de 2px ; portée longue : cercle en tirets (8/6) et bande hachurée laiton (traits obliques à 45% d'opacité) entre les deux portées. Hors de portée : jeton à 40% d'opacité. Au survol en ciblage, une bulle au-dessus du jeton donne la distance et ce qu'elle permet (« portée longue : désavantage », « hors d'allonge »…), cerclée de laiton si la cible est permise, et son nom sous le jeton s'allonge de sa CA et de ses PV ; le tableau répète ce verdict dans sa colonne État. Le curseur dit l'issue du clic : viseur sur une cible permise, interdit hors de portée et sur l'attaquant, flèche ailleurs. Indicatif : le serveur tranche. À terre : jeton gris (`map-down`) barré d'une croix dessinée de 3px, sans initiales. Position trop proche d'un autre jeton (pose ou déplacement) : anneau `--danger`. À son tour (combat) : anneau laiton en tirets (7/5) de 3px à 1.55 fois le rayon, plus large que l'anneau de cible et distinct du contour de sélection. Déplacement : le curseur `grab` sur un jeton glissable, `grabbing` pendant le geste ; le jeton suit le pointeur (calé sur la case avec Maj, sinon aimanté au contact des créatures proches), un trait laiton en tirets (8/6) de 2px part du départ, et une bulle au-dessus de l'arrivée dit « 4,5 m parcourus · reste 4,5 m » (sans « reste » quand rien ne limite). Pour une créature limitée, un cercle laiton plein de 2px autour du départ montre ce qui reste. Trop loin ou trop près : trait, contour de bulle et anneau en `--danger`. Au clavier, le même aperçu suit le curseur de case, avec un fantôme à 50% d'opacité. La feuille de la créature active ajoute « · reste 4,5 m » à sa vitesse.

## Do's and Don'ts

### Do:
- **Do** réserver le laiton à l'état vivant : mode, entrée armée, sélection, cible survolée, attaque visée.
- **Do** mettre en rubrique ce que le moteur tranche (verdict, issue, chiffres clés, « À terre ») et les intertitres du bloc de statistiques.
- **Do** dire le camp par la forme : disque pour un personnage, carré pour un monstre, répété dans une pastille cerclée de `token-rim` partout où la créature est nommée.
- **Do** marquer une créature à 0 PV par trois signes : jeton gris, croix dessinée, nom barré.
- **Do** dessiner la portée longue en hachures, pas seulement en couleur.
- **Do** suivre chaque titre de panneau d'un filet effilé rubrique.
- **Do** déclarer toute couleur dans les deux thèmes et la lire depuis les variables, carte comprise.

### Don't:
- **Don't** mettre en laiton un état d'interface : l'onglet actif se souligne d'encre, le bouton de tiroir ouvert reste en `wood-sunken` et encre.
- **Don't** marquer le camp par une bande latérale colorée : la pastille de forme la remplace.
- **Don't** ajouter un mouvement autre que la pose du verdict, ni l'animer sous `prefers-reduced-motion`.
- **Don't** poser d'ombre portée sur un panneau, une ligne ou un bouton ; la profondeur passe par le bois et le feutre.
- **Don't** introduire de surface grise neutre ni de néon : tout fond est bois ou feutre.
- **Don't** employer Alegreya SC pour un texte courant.
