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
  danger: "#f39a52"
  felt: "#1f3a2e"
  felt-deep: "#183025"
  felt-rim: "#12241b"
  hp: "#8fbf7a"
  hp-low: "#e0a35a"
  token-character: "#4f84c4"
  token-monster: "#b5643a"
  token-rim: "#efe6d2"
  map-down: "#8a857a"
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
  danger-light: "#772803"
  felt-light: "#93ab88"
  felt-deep-light: "#b4c6a7"
  felt-rim-light: "#6f8a64"
  hp-light: "#3f7a2e"
  hp-low-light: "#8a4e0c"
  token-character-light: "#2f5f9a"
  token-monster-light: "#9a4a1c"
  token-rim-light: "#fbf3e2"
  map-down-light: "#67635a"
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

Quatre couleurs portent du sens, chacune pour un seul rôle : le laiton pour ce qui est vivant maintenant, la rubrique pour le texte de règle (bloc de statistiques et verdicts du moteur), le vermillon pour les erreurs et ce qui détruit, l'encre pour le reste.

**Key Characteristics:**
- Bois pour le cadre, feutre pour le jeu ; aucune surface neutre grise.
- Laiton réservé à l'état vivant, rubrique réservée au texte de règle, vermillon réservé aux erreurs.
- Le camp se lit par la forme (disque, carré), jamais par la seule couleur.
- Petites capitales Alegreya SC pour les titres, Alegreya Sans pour le texte.
- Un seul mouvement, l'attaque qui se tranche : les dés 3D roulent sur la carte (jet d'attaque, puis dégâts s'il touche), le verdict se pose sur la piste, puis l'issue s'élève au-dessus de la cible et s'efface.

## Colors

Une palette de matières (bois, feutre, encre) avec deux pigments de sens, déclinée en deux thèmes de mêmes rôles ; chaque clé `-light` est la valeur du thème clair.

### Primary
- **Laiton** (`brass`, `brass-light`) : le seul état vivant. Puce du mode en cours, entrée du catalogue armée (au clavier), attaque visée (ligne cerclée, bouton plein), jeton sélectionné, déplacement en cours (trait, cercle de vitesse restante, puce « Déplacement »), anneau de la cible survolée, cercles d'allonge et de portée, mesure. Sur la carte, il arrive sous l'alias `--map-live`, même valeur. Texte posé dessus : `on-brass`. C'est aussi la couleur du contour de focus et de la sélection de texte.

### Secondary
- **Rubrique** (`rubric`, `rubric-light`) : le texte de règle, comme l'encre rouge d'un bloc de statistiques. Verdict de la piste, issue d'une entrée du journal, mention « À terre », nom de la créature, intertitres et chiffres de la feuille, filet effilé.
- **Vermillon** (`danger`, `danger-light`) : les erreurs et les actions destructrices. Bandeau d'erreur, refus d'un formulaire, « Retirer ? » et son bouton, bouton « Remplacer la carte », trait et anneau d'un déplacement interdit sur la carte. Une teinte à part pour qu'une erreur ne se lise jamais comme un verdict ; il est proche de l'ambre des PV bas et de la rouille des monstres, si bien qu'une erreur se dit toujours aussi par du texte ou un trait pointillé, jamais par la seule couleur.

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

**The Rules Speak In Rubric Rule.** La rubrique est le texte de règle : le bloc de statistiques (nom, intertitres, chiffres) et ce que le moteur tranche (verdicts, issues, « À terre »). Aucune décoration, aucun appel à l'action ni aucune erreur n'est en rubrique : les erreurs sont en vermillon.

**The Two Lights Rule.** Toute nouvelle couleur se déclare dans les deux thèmes, sous le même nom, dans `style.css` ; la carte lit ces variables au rendu, jamais une valeur en dur.

## Typography

**Display Font:** Alegreya SC (with Georgia, serif)
**Body Font:** Alegreya Sans (with system-ui, sans-serif)

**Character:** Une famille humaniste d'éditeur de jeux : les petites capitales donnent la voix du livre de règles aux titres et au verdict, la sans-serif garde la lecture rapide des feuilles et du journal. Les deux sont auto-hébergées (`@fontsource`, graisses 400/500/700 pour la sans, 500/700 pour les petites capitales).

### Hierarchy
- **Display** (700, 1.45rem, interlettrage 0.03em) : le nom de l'application dans la barre.
- **Headline** (700, 1.6rem, 1.1) : le nom de la créature en tête de feuille, en rubrique.
- **Verdict** (700, 1.5rem, 1.2, 0.02em) : le verdict de la piste à dés, en rubrique ; un raté passe en 500, couleur de texte atténuée.
- **Title** (700, 1.2rem, 0.03em) : titres de panneau (Catalogue SRD, Nouvelle carte), suivis du filet effilé ; intertitres de feuille à 1.1rem, onglets à 1.05rem.
- **Body** (400, 16px, 1.45) : tout le texte courant ; les chiffres sont tabulaires (`font-variant-numeric: tabular-nums`) dans le tableau, la feuille, le journal et la barre d'état.
- **Label** (500, 0.85 à 0.95rem, 0.04 à 0.06em, petites capitales) : en-têtes du tableau, groupes du catalogue, étiquettes d'état, résumé « détails » du journal.

### Named Rules
**The Small Caps Are Headings Rule.** Alegreya SC sert aux titres, en-têtes, onglets, étiquettes et au verdict ; jamais à un paragraphe ni à un texte de plus d'une ligne.

## Layout

L'application s'ouvre sur l'accueil : la même barre (nom de l'application, thème), puis, centrés dans une colonne de 34rem au plus (16px de marge sur un téléphone), deux panneaux de bois cerclés de `page` : « Campagnes » (une ligne par campagne, son nom et un bouton « Ouvrir », ou « Aucune campagne. » en `ink-muted`) et « Nouvelle campagne » (champ Nom et « Créer », éteint tant que le nom est vide ; Entrée crée). Titres en petites capitales et filet effilé, comme les tiroirs. Une erreur se pose sous la barre en vermillon, avec « Fermer ».

Ouvrir une campagne mène à son Carnet : la même barre (Accueil, nom de l'application, nom de la campagne, puis à droite un bouton « Combat » à icône d'épées croisées et le thème). Dessous, sur 1.25rem de marge, un panneau de bois de 17rem cerclé de `page` (« Pages » en petites capitales et filet effilé, puis l'arbre des pages ; sous un filet, « Nouvelle page » : champ Titre et « Créer », qui crée à la racine), collant en haut de l'écran et dont l'arbre défile au-delà de la hauteur de la fenêtre, et la page ouverte, posée sur le fond sans panneau, 46rem au plus : son titre en Alegreya SC 700 à 1.8rem (déverrouillée, un champ de même police, même taille, même place, sans fond ni contour, creusé en `wood-sunken` cerclé de `control-border` au survol et au focus ; Entrée ou sortie du champ renomme, Échap rend le titre d'avant), à sa droite le verrou (bouton d'icône, cadenas fermé ou ouvert, « Page verrouillée » en `aria-pressed`), un filet effilé, puis le corps : texte à 1.1rem, interligne 1.6, titres en Alegreya SC, citations en italique `ink-muted` derrière un trait de 3px `rule`. Verrouillée ou non, la page garde exactement la même mise en page : seuls l'icône du verrou et le curseur (flèche, ou curseur de texte) changent ; aucune barre d'outils, la mise en forme se fait à la frappe (`# `, `- `, `> `, `**…**`) ou au clavier (Ctrl+B, Ctrl+I). Un Carnet vide montre à la place, centré dans une colonne de 34rem comme l'accueil, un panneau « Carnet » : « Aucune page. Créer la première page du Carnet. », champ Titre (focus d'entrée) et « Créer ». Un module du Carnet qui a un panneau (Mythic) ajoute une troisième colonne de 18rem, contre la page (alors limitée à 46rem par la grille) : un panneau de bois cerclé de `page` par module, son titre en petites capitales et filet effilé, collant en haut de l'écran ; sans module, la colonne n'existe pas. Le panneau Mythic : « Chaos » en `ink-muted`, puis − et + (boutons de 2rem, éteints aux bornes 1 et 9) autour du chaos en Alegreya SC 700 à 1.4rem ; sous un filet `rule`, les champs « Question » (facultative) et « Probabilité » (liste d'« Impossible » à « Certain », 50/50 d'abord), puis « Demander » (gras, `wood-raised`). Dessous, la dernière réponse en gras, suivie en `ink-muted` du jet, de la probabilité et du chaos, et de l'événement aléatoire s'il y en a un ; un refus ou une erreur en `danger` à sa place. Sous un second filet, la section « Sens » : sans tables, « Tables de sens non importées : importer le fichier de vos tables. » en `ink-muted` et « Importer » (gras, `wood-raised`) ; avec, la liste des tables (Actions, Descriptions, puis un groupe « Éléments »), « Tirer » (même style que « Demander ») et « Remplacer les tables », lien souligné `ink-muted` à 0.9rem. Le dernier sens se rappelle comme la dernière réponse : les deux mots en gras, la table et les jets en `ink-muted`. Dans la page, la réponse est une citation : « Oracle : », « Événement aléatoire : » et « Sens (table) : » en gras, la réponse en gras. Sous 900px, panneau des pages, page et panneaux des modules s'empilent en une colonne, plus aucun n'est collant.

L'écran de jeu est une grille d'application pleine hauteur : barre en haut, puis rail d'icônes (3.5rem), table au centre (fluide), panneau latéral à onglets (24rem). Un tiroir de 17rem s'insère entre le rail et la table quand Catalogue, Nouvelle carte ou Cartes enregistrées est ouvert. La table empile, de haut en bas : le tableau « Sur la carte » (cinq lignes entières visibles, défile au-delà ; en combat, ciblage compris, une bande d'initiative de trois lignes, qui garde la créature qui joue en vue ; en-tête de 2rem et lignes de 2.2rem fixes, pour que la fenêtre tombe sur une ligne entière), la carte encadrée de 6px de bois à gauche et à droite, la barre d'état (deux lignes réservées), la piste à dés (trois lignes, hauteur fixe).

**The Still Table Rule.** La carte ne change jamais de taille sous le pointeur : ni en entrant en ciblage, ni quand la piste ou la barre d'état changent de contenu. Tout ce qui l'entoure a une hauteur réservée ; ce qui déborde défile ou se coupe, jamais ne pousse.

- Sous 1280px : tiroir et panneau latéral ne partagent plus l'écran. Le tiroir ouvert replie le latéral en une bande de 3rem d'onglets verticaux (l'actif souligné d'un trait d'encre côté table) ; choisir un onglet ferme le tiroir et rouvre le panneau.
- Sous 1100px : latéral à 20rem.
- Sous 900px : une colonne ; la page défile, le cadre de la carte prend la forme de la carte (14rem minimum, 70svh maximum), le panneau latéral passe dessous (24rem minimum), le rail se couche à l'horizontale quand le tiroir est fermé.
- Sous 1000px : la barre passe sur deux lignes, le contrôle de combat seul sur la seconde, sur toute la largeur (en combat, une ligne déborderait sous 745px environ, et sous 1000px pendant la confirmation de fin de combat) ; la confirmation peut passer à la ligne sur un téléphone.
- Sous 600px : l'info de carte et le bouton Raccourcis se retirent de la barre (le tableau compte les créatures ; « ? » ouvre toujours la légende).
- Le tableau tient sans défilement horizontal : sous 1280px, il masque le camp en toutes lettres (la forme de la pastille le dit déjà) ; entre 901 et 1100px et sous 700px, la jauge (le chiffre reste) ; sous 600px, l'état.

Le rythme est en rem : 0.25 entre les entrées d'une liste, 0.4 à 0.6 à l'intérieur d'une ligne, 0.75 à 1 entre blocs, 1 à 1.1 en marge de panneau. Une erreur prend la place de la barre d'état sous la carte, à sa hauteur réservée : elle ne couvre ni le tableau ni les boutons de zoom, et la carte ne bouge pas. Sans carte, elle ouvre la colonne.

## Elevation & Depth

Le système est plat et matériel : la profondeur vient des matières (bois plus clair au survol, plus sombre en creux, feutre en retrait) et des filets, pas d'ombres portées. Deux exceptions fonctionnelles seulement.

### Shadow Vocabulary
- **Feutre creusé** (`box-shadow: inset 0 3px 10px rgba(0, 0, 0, 0.45), inset 0 0 0 1px var(--felt-rim), 0 0 0 3px var(--rule)`) : la piste à dés, une cuvette de feutre dans son cadre de bois.
- **Bandeau flottant** (`box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3)`) : le dialogue de décision posé sur la table.
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
- **Mode en cours:** pilule laiton pleine, texte `on-brass` en 700, dans la barre, juste après les infos de carte et avant l'espace libre : elle apparaît sans déplacer les boutons ancrés à droite. C'est la seule puce pleine du système.
- **Étiquettes d'état:** petites capitales 0.85rem, première lettre capitale, contour 1px `rule`, 3px ; « À terre » passe en rubrique (texte et contour) ; une portée qui permet l'attaque se cercle de laiton ; « À son tour » aussi, en encre grasse.
- **Contrôle de combat:** dans la barre, ancré à droite avant le bouton de thème. Hors combat, un bouton « Lancer le combat », désactivé sans créature (la raison au survol) ; en combat, « Round N · **Tour de X** » en `ink-muted` puis encre, le bouton « Fin du tour », puis le bouton ghost « Terminer le combat » (`ink-muted`, contour et texte `--danger` au survol). Il se confirme sur place : « **Terminer le combat ?** » en `--danger` gras, « Confirmer » (contour `--danger`, focus) et « Annuler » ; Échap et Annuler rendent le focus au bouton, un changement de tour ferme la confirmation, et après la fin le focus passe à « Lancer le combat ». Pendant « Qui joue ? », « Round N · **Qui joue ?** ». Au lancement, la piste se vide (l'attaque d'avant n'est pas de ce combat) et la barre d'état dit l'initiative tirée, dans l'ordre de jeu : « Combat lancé. Initiative : Guerrière 22 · Magicienne 7 · Loup 6 ».
- **Retrait en combat:** le × de la ligne du tableau ne retire pas tout de suite : la cellule devient « **Retirer ?** » en `--danger` gras, « Confirmer » (contour `--danger`, focus) et « Annuler » ; Échap annule. Hors combat, le retrait est immédiat (Ctrl+Z annule).

### Navigation
- **Rail:** colonne de boutons d'icônes de 2.6rem sur `wood`, icônes SVG dessinées (trait 1.75, grille 24) : Catalogue (livre ouvert), Nouvelle carte (grille encadrée), Cartes enregistrées (signet). Le bouton du tiroir ouvert reste en bois : fond `wood-sunken`, contour `rule`, encre. Jamais en laiton.
- **Bouton Accueil:** premier de la barre, icône de maison (même trait), « Accueil : changer de campagne » au survol ; il ramène à l'accueil. Dans l'écran de jeu, le suit le bouton Carnet (icône de carnet, même trait, « Carnet de la campagne » au survol), qui ramène au Carnet sur la page qu'on y avait laissée. Après le nom de l'application, le nom de la campagne en Alegreya SC 500 à 1.1rem, à l'encre, coupé d'une ellipse au-delà de 16rem.
- **Barre:** la ligne d'information de la carte commence par le nom de la carte enregistrée d'où elle vient (« Crypte · Carte 30 m × 20 m · 3 créatures »).
- **Bouton Raccourcis:** dernier de la barre, icône de clavier (même trait), « Raccourcis (?) » au survol ; la touche « ? » l'ouvre aussi. Il ouvre la légende : un panneau de bois modal comme le dialogue de décision, titre en petites capitales et filet effilé, trois groupes (Carte, Créatures, Combat) d'au plus quatre lignes, chaque touche en pavé `wood-sunken` cerclé de `control-border` (bord bas de 2px), les gestes (« Glisser », « Deux clics ») en italique `ink-muted`. Échap, un clic sur le voile ou « Fermer » (qui prend le focus) la referment ; tant qu'elle est ouverte, les touches du jeu se taisent.
- **Bouton grille:** dans la barre, icône de grille (trait 1.75, grille 24) entre le contrôle de combat et le bouton de thème, présent dès qu'une carte existe. Bascule (`aria-pressed`) : grille affichée, icône à l'encre ; masquée, en `ink-muted`. Le survol dit l'action (« Masquer la grille », « Afficher la grille »).
- **Onglets (Feuille, Cible, Journal):** petites capitales 1.05rem en `ink-muted` ; l'onglet actif passe à l'encre avec un soulignement de 3px d'encre.

### Inputs / Fields
- **Style:** champ numérique en creux (`wood-sunken`), contour 1px `control-border`, 4px, chiffres tabulaires, unité accolée à droite.
- **Focus:** le contour laiton commun. Radios et cases suivent `accent-color: brass`.
- **Image de fond (tiroir Nouvelle carte):** après l'unité, un choix de fichier dont le bouton natif prend l'allure d'un bouton standard, sans le texte natif (« Aucun fichier choisi » se coupait dans le tiroir) ; une fois l'image lue, son nom et sa taille en pixels, puis « Retirer l'image ». Avec image, « Cases en largeur » et « Case … px » remplacent Largeur et Hauteur : saisir l'un calcule l'autre (au centième), et une ligne donne les cases et les dimensions de la carte. Refus (type, image illisible, plus de 50 Mo) en `--danger` sous le choix.

- **Cartes enregistrées (tiroir):** titre et filet effilé comme Nouvelle carte. Un champ texte « Nom » (en creux, pleine largeur, 80 caractères) et « Enregistrer » ; un nom déjà pris annonce « Remplace la carte enregistrée « … » » en `--danger` ; sans carte, champ et bouton éteints avec une consigne en `ink-muted`. Sous un second filet, une entrée par carte, triée par nom : nom en gras, « 30 m × 20 m · 3 créatures » en `ink-muted`, bouton « Charger ». Avec une carte en cours, Charger ouvre sous l'entrée la même confirmation que Nouvelle carte (« Remplace la carte actuelle… » en `--danger`, « Charger » `danger`, « Annuler »), qui prend le focus ; Annuler ou Échap le rend au bouton. Chargée, la carte ouvre le Catalogue pour poser les personnages.
- **Caler la grille (tiroir Nouvelle carte, sous le formulaire):** sur une carte avec image seulement, après un filet effilé, le titre « Caler la grille », une consigne en `ink-muted` et le bouton « Caler la grille » ; en combat, la raison en `ink-muted` et le bouton éteint. Le bouton ouvre trois champs numériques en pixels de l'image : « Décalage horizontal », « Décalage vertical » (pas de 1) et « Taille de case » (pas de 0,1), le premier pris au focus. Une ligne donne les cases et les dimensions qui en résultent. « Valider » `primary` et « Annuler » ; Échap annule et rend le focus au bouton, le tiroir reste ouvert. Pendant le calage, la grille d'aperçu s'affiche même masquée, un glisser au bouton du milieu sur la carte, jetons compris, décale la grille au pixel entier de l'image, comme les champs de décalage, et le glisser ordinaire déplace la vue ; les jetons ne réagissent plus, pose, déplacement, ciblage et lancement du combat sont suspendus. Validé, la barre d'état annonce « Grille calée. » et la grille reprend le choix d'affichage d'avant.


### Arbre du Carnet
Une ligne par page, triée par titre parmi ses sœurs (« Session 2 » avant « Session 10 »), retrait de 1.1rem par niveau. En tête de ligne, un chevron (trait 1.75, 18px, `ink-muted`) déplie ou replie une page à sous-pages (`aria-expanded`, « Déplier … » / « Replier … ») ; sans sous-pages, la même place reste vide pour que les titres s'alignent. Le titre est un bouton sans fond en `ink-muted` ; la page ouverte passe en `wood-sunken` cerclée de `rule` à l'encre. En bout de ligne, un bouton ⋯ (« Actions de … ») qui n'apparaît qu'au survol de la ligne, au focus et menu ouvert, toujours au toucher ; il ouvre sous lui, ou au-dessus quand le bas de la fenêtre manque de place, un menu de bois clair (`wood-raised`, contour `rule`) posé par-dessus le panneau, jamais coupé par l'arbre qui défile : « Nouvelle sous-page », « Renommer », « Déplacer sous… », « Supprimer… » (en `danger`), le premier pris au focus ; Échap, un clic ailleurs, un défilement ou un redimensionnement le ferment. Sous les pages ordinaires, chaque section d'un module (Mythic : « Fils », « Personnages ») vient à 1rem, sous son titre en Alegreya SC 700 à 1rem et un filet effilé, ses racines au premier niveau ; le menu d'une racine de section n'a que « Nouvelle sous-page » et « Renommer », et elle ne se glisse pas (un appui reste un clic qui l'ouvre) ; ses sous-pages ont le menu complet. Renommer change le titre de la ligne en champ creusé prérempli, Nouvelle sous-page ouvre un champ « Titre » sous la ligne, au niveau du dessous, et déplie la branche ; Entrée ou sortie du champ valide (vide ou inchangé : rien), Échap annule, et le focus revient au titre de la ligne.

**Glisser une page.** À la souris ou au stylet, un titre tenu puis tiré de plus de 6px se glisse ; un clic plus court ouvre la page. Pendant le geste, le texte ne se sélectionne pas et le curseur est `grabbing` ; la ligne glissée s'estompe (opacité 0.5) et un fantôme de bois clair (`wood-raised`, contour `brass`, titre sur une ligne) suit le pointeur juste à sa droite. La ligne visée s'éclaire (`wood-raised`, contour `brass`, titre à l'encre) si elle accepte la page, sinon son contour passe en `danger`, pointillé. Une zone « Racine du Carnet » (pointillés `rule`, italique `ink-muted`) recouvre le titre « Pages » le temps du glisser, sans décaler l'arbre, et s'éclaire ou se refuse de même. Une branche repliée survolée 600ms s'ouvre ; près du haut ou du bas de l'arbre, il défile, plus vite vers le bord. Échap ou un relâcher hors cible n'envoie rien, et le clic qui suit le relâcher n'ouvre aucune page. Au toucher, rien ne se glisse : le doigt fait défiler, le menu « Déplacer sous… » déplace.

**Dialogue Déplacer.** Un panneau de bois modal comme le dialogue de décision : « Déplacer « … » » en petites capitales et filet effilé, « Ses sous-pages la suivent. » en `ink-muted`, puis, entre deux filets et défilant au-delà de 20rem, « Racine du Carnet » et toutes les pages en arbre indenté, chacune avec un bouton radio laiton ; la page, ses sous-pages et sa place actuelle sont éteintes en `ink-muted`. « Déplacer » (gras, `wood-raised`, éteint sans choix) et « Annuler » ; Échap ferme. Le focus prend la première destination permise, et revient au titre de la page dans l'arbre, dépliée jusqu'à elle.

**Dialogue Supprimer.** Même panneau modal : « Supprimer « … » » en petites capitales et filet effilé, puis la question (« Supprimer « Bertrand » et sa sous-page ? »), « N pages y renvoient : leurs liens s'afficheront morts. » quand il y en a, et en `ink-muted` « Elle ira à la corbeille, d'où elle se restaure. » ; « Supprimer » (gras, trait et texte `danger`) et « Annuler », qui prend le focus.

**Corbeille.** Au pied du panneau Pages, sous « Nouvelle page », un bouton sans cadre en `ink-muted` (encre au survol) : icône de corbeille de 18px et « Corbeille » ; le Carnet vide l'offre aussi, sous « Créer ». Il ouvre un panneau modal de 30rem au plus : « Corbeille » et filet effilé, puis une ligne par suppression, la plus récente d'abord, entre filets `rule` et défilant au-delà de 20rem : titre en gras, « et N sous-pages » en `ink-muted`, dessous la date courte (0.9rem, `ink-muted`), « Restaurer » à droite. Vide : « La corbeille est vide. » en `ink-muted`. En bas, « Vider la corbeille » (trait et texte `danger`, éteint si vide), qui laisse place sur la même ligne à « Tout effacer pour de bon ? » avec « Vider » et « Annuler », et « Fermer » à droite ; Échap annule la confirmation, puis ferme.

**Image de page.** Déverrouillée, la tête de page montre avant le verrou un bouton d'icône Image (cadre, soleil et collines, même trait ; « Insérer une image » ; éteint pendant l'envoi), absent verrouillée. Dans le texte, une image tient sa ligne entre deux paragraphes, à sa taille, jamais plus large que la page, coins arrondis de 2px, 0.75rem dessous ; en écriture, une image sélectionnée (clic ou flèches) est cerclée de 2px `brass` décalé de 2px. En écriture, une image déposée sur le texte entre au point de dépôt, collée au curseur ; ailleurs (marge, arbre, page verrouillée), le curseur de dépôt dit qu'on ne dépose rien. Une image qui ne se lit plus montre son nom de fichier.

**Lien de page.** Dans le texte, un lien garde la police et la couleur du texte (`ink`), souligné d'un trait `brass` de 2px décalé sous les lettres, qui passe à l'encre au survol ; il affiche le titre actuel de sa cible. Verrouillée, la page montre le curseur main sur un lien, et le focus clavier le cercle de `control-border` ; en écriture, le curseur reste la flèche, et un lien sélectionné passe en `wood-sunken` cerclé de `brass`. Un lien mort est barré d'un trait fin en `ink-muted`, curseur interdit, « page introuvable » s'il n'a pas de texte choisi. Taper `[[` ouvre sous le curseur (au-dessus si la place manque) une liste de bois clair comme le menu de l'arbre (`wood-raised`, contour `rule`) : huit pages au plus, titre à l'encre et page parente en `ink-muted` à droite, option active en `wood-sunken` cerclée de `brass` ; « Aucune page » en italique `ink-muted` quand rien ne correspond. En écriture, un lien sélectionné (clic ou flèches) montre sous lui une bulle de même bois : « Ouvrir » (absent pour un lien mort, remplacé par « Page introuvable » en italique `ink-muted`), puis « Texte affiché » en `ink-muted` et son champ de 12rem, vide quand le lien suit le titre, qu'il montre en indication ; Entrée ou sortie du champ pose le texte, Échap rend l'ancien, et le focus revient au texte. Ctrl+clic ouvre la cible sans passer par la bulle.

**Pages qui renvoient ici.** Sous le texte de la page, à 2.5rem, un filet effilé puis « Pages qui renvoient ici » en petites capitales comme « Pages », et dessous les titres des pages qui la citent, un par ligne, triés comme l'arbre, à l'allure d'un lien de page (encre, trait `brass` de 2px qui passe à l'encre au survol, cercle `control-border` au focus) ; un clic ou Entrée ouvre la page. Rien ne la cite : « Aucune page ne renvoie ici. » en italique `ink-muted`. Même place et même effet, verrouillée ou non ; sans panneau, sur le fond de la page.

**Recherche du Carnet.** Sous « Pages » et son filet, un champ « Chercher » de toute la largeur du panneau ; non vide, un bouton d'icône (croix, « Effacer la recherche ») le suit, et Échap le vide aussi. Tant qu'il n'est pas vide, les résultats prennent la place de l'arbre, sans panneau de plus : « N pages » en `ink-muted`, puis chaque page en bouton de toute la largeur (fond `wood-raised` au survol, cercle `control-border` au focus) : son titre à l'allure d'un lien de page (encre, trait `brass` de 2px), dessous l'extrait à 0.9rem en `ink-muted`, les mots trouvés en `on-brass` sur fond `brass`. Un clic ou Entrée ouvre la page et garde la liste. Rien de trouvé : « Aucune page ne contient « … ». » en italique `ink-muted`. Vider le champ rend l'arbre tel qu'il était déplié.

**Onglets du Carnet.** Au-dessus de la page ouverte, dans sa colonne, une barre des pages ouvertes sur un filet de 1px `rule` : chaque onglet porte le titre de sa page en Alegreya SC 700 à 1.05rem (`ink-muted`, à l'encre au survol et pour l'actif ; fond `wood-raised` au survol ; 14rem au plus, coupé d'une ellipse, titre entier au survol), suivi d'une petite croix (bouton de 1.75rem, icône de 1rem, « Fermer « … » », creusée en `wood-sunken` au survol). L'actif se souligne de 3px d'encre, comme les onglets du combat, jamais de laiton. Au-delà de la largeur de la page, la barre défile horizontalement sans faire défiler l'écran. Sans onglet ouvert : « Aucune page ouverte. Choisir une page dans l'arbre. » en `ink-muted`.
### Tableau « Sur la carte »
Un tableau des départs : en-têtes collants en petites capitales, lattes alternées (`wood-sunken` une ligne sur deux), ligne sélectionnée en `wood-raised` avec le nom en gras et la pastille cerclée de laiton, comme le jeton sur la carte. Chaque ligne porte la pastille de camp, le nom, le camp, la jauge de PV (4.5rem, `hp`, `hp-low` sous le seuil), les chiffres tabulaires (jauges alignées quel que soit le nombre de PV) et l'état. La colonne État a la largeur de sa plus longue étiquette, pour que les colonnes ne bougent pas : elle dit l'état de jeu de la créature, jamais celui de l'interface (la ligne sélectionnée et la feuille disent déjà quelle créature est ouverte). Hors ciblage : « Pose en cours… », « À terre », « Cible » (visée par la dernière attaque). En combat, les lignes suivent l'ordre d'initiative, une colonne « Init. » (abréviation, chiffre gras) ouvre la ligne, et la créature qui joue porte un filet laiton de 4px sur son bord gauche et l'étiquette « À son tour » ; celles qui ont joué ce round portent « Joué », les candidats de « Qui joue ? » « Au choix ». En combat, le tableau se resserre en bande de trois lignes (la créature qui joue défile en vue) ; le ciblage garde la même hauteur et pose le focus sur la première cible permise, les autres défilent dans la bande. En ciblage : « Attaquant » et, pour les autres, le verdict de portée. Une créature à terre : pastille grise, nom barré en `ink-muted`, étiquette « À terre » en rubrique.

### Bloc de statistiques (Feuille, Cible)
Nom en rubrique (headline), type en italique `ink-muted`, filet effilé, lignes libellé gras puis chiffre en rubrique (1.15rem). Intertitres en petites capitales rubrique soulignés d'un filet rubrique. Une attaque visée : la ligne se cercle de laiton sur `wood-raised`, son bouton passe en laiton plein. Quand les règles ferment les attaques (à terre, pas son tour), la raison du moteur s'écrit sous l'intertitre, en italique `ink-muted`, et les boutons sont désactivés. Les touches 1 à 9 lancent les attaques de la feuille, et chaque bouton « Attaquer » montre la sienne dans un petit pavé cerclé de `rule` en `ink-muted` (pas sur « Annuler ») ; Maj+F termine le tour (pas F seul : un tour fini ne se reprend pas).

### Piste à dés
Une cuvette de feutre `felt-deep` (8px) dans un cadre de bois, sous la carte, de hauteur fixe : trois lignes, chacune coupée d'une ellipse plutôt que de passer à la ligne. Elle montre la dernière attaque, qu'un changement de tour n'efface pas (la barre dit qui joue) : d'abord le verdict, l'issue puis ce qu'elle a coûté (« Touché · 6 dégâts tranchants », le type seulement s'il est unique) ; dessous, à 0.95rem en 500, le calcul, à vérifier contre les dés posés, la réaction qui l'a changé et les dégâts (« 10 + 2 = 12 contre CA 17 · Magicienne utilise sa réaction : Bouclier », « 10 + 5 = 15 contre CA 15 · dégâts 5 + 5 = 10 ») : chaque dé qui compte (le d20 gardé, les dés bonus, les dés de dégâts), puis la somme des modificateurs, que le client tire du total moins les dés sans connaître les règles ; leur détail par source reste dans le journal ; enfin l'annonce de l'action, à 0.95rem atténuée ; au milieu, les dés de l'attaque, puis la cible avec sa jauge. L'issue se lit à la forme, pas à la seule teinte : touché en rubrique grasse, raté en ivoire (encre en clair) atténué et en 500, coup critique en capitales pleines. Les dés sont des pavés de feutre `felt` (2.4 × 2.6rem, 6px, cerclés de `felt-rim`) : la valeur en Alegreya SC 700 à 1.25rem, les faces dessous (« d20 ») à 0.7rem atténuées ; d'abord le jet d'attaque (d20, puis dés bonus), un filet `felt-rim`, puis les dés de dégâts, absents sur un raté. Le d20 écarté par l'avantage ou le désavantage est à 45% d'opacité et barré : la forme le dit autant que la teinte. Les dés ne poussent jamais la piste : ils se coupent. Pendant que les dés 3D roulent sur la carte, verdict, calcul, dés posés et cible attendent à leur place, invisibles ; à leur arrêt, le verdict se pose : 320ms, `cubic-bezier(0.16, 1, 0.3, 1)`, de 0.4rem plus haut et transparent à sa place. Relue au rechargement, l'attaque est posée d'emblée. Sous `prefers-reduced-motion`, rien ne roule ni ne glisse : dés et verdict sont posés d'emblée. La piste est muette pour les lecteurs d'écran : la région `aria-live` annonce l'issue, la réaction et les dégâts ; la barre d'état ne répète pas le verdict.

### Journal
Une entrée par action : titre en gras ; dessous, ce que le moteur a tranché, dans l'ordre (issue de l'attaque, réaction, dégâts), en rubrique gras, un raté en encre atténuée et en 500 comme sur la piste ; une action sans issue marquée montre sa dernière ligne ; détail des jets replié sous un résumé en petites capitales. L'onglet Cible ouvre sur une ligne en italique atténuée, « Visée par la dernière attaque tranchée », pour ne pas la confondre avec la visée en cours. Un nouveau round coupe l'action en cours : « Round N » s'affiche en intertitre de petites capitales `ink-muted`, à sa place.

### Dialogue de décision
Un panneau de bois modal sur un voile sombre, que seule une option ferme. Titre en rubrique : « Réaction · *créature* » pour une réaction, « Combat » pour « Qui joue ? ». La question dessous (pour une réaction, le jet et la CA : « Coup au but sur X (15 contre CA 12). »), puis une option par bouton. Le focus va sur l'option qui ne coûte rien : « Ne rien faire » pour une réaction, la première créature pour « Qui joue ? ».

### Carte (canvas)
Sol `felt-deep`, grille `map-grid` de 1px, bord `map-border`. Une image de fond couvre toute la carte, au-dessus du sol et sous la grille ; la grille, masquable, garde le même trait sur l'image (lisible dans les deux thèmes). Jetons pleins en couleur de camp, cerclés de 2px `token-rim`, initiales en Alegreya Sans 700 ; noms sur un pavé `map-label-ground`, montrés pour le jeton sélectionné, survolé, et celui qui joue. Sélection : contour laiton de 3.5px. Cible survolée : anneau laiton de 3px à 1.35 fois le rayon. Allonge (à la limite des règles, allonge + demi-case) et portée normale : cercle laiton plein de 2px ; portée longue : cercle en tirets (8/6) et bande hachurée laiton (traits obliques à 45% d'opacité) entre les deux portées. Hors de portée : jeton à 40% d'opacité. Au survol en ciblage, une bulle au-dessus du jeton donne la distance et ce qu'elle permet (« portée longue : désavantage », « hors d'allonge »…), cerclée de laiton si la cible est permise, et son nom sous le jeton s'allonge de sa CA et de ses PV ; le tableau répète ce verdict dans sa colonne État. Le curseur dit l'issue du clic : viseur sur une cible permise, interdit hors de portée et sur l'attaquant, flèche ailleurs. Indicatif : le serveur tranche. Sous chaque jeton aux PV connus, une jauge de PV de sa largeur (4px, 3px sous le jeton, `hp`, `hp-low` à la moitié ou moins, sur un fond noir à 45%) ; le nom se place sous la jauge. À terre : jeton gris (`map-down`) barré d'une croix dessinée de 3px, sans initiales ni jauge. Dés 3D : une couche WebGL transparente sur toute la carte, qui ne capte pas le pointeur et que les lecteurs d'écran ignorent. Les dés partent du bas de la carte, rebondissent sur ses bords sous une lumière chaude, avec une ombre douce sur le feutre, et s'arrêtent sur les valeurs du serveur. Dès qu'ils sont arrêtés à l'œil, la suite part (dégâts, question de réaction, verdict) ; eux restent posés 1,2s puis s'estompent en 1,6s, et les dés du lancer suivant rebondissent dessus. Deux lancers, comme à la table : le jet d'attaque (d20, dés bonus), puis les dégâts s'il touche ; une réaction (Bouclier) se décide entre les deux, sur le d20 arrêté. Jet d'attaque en os (`#efe6d2`) à chiffres noyer (`#2a1a10`), dégâts en rubrique (`#93231d`) à chiffres os, dans les deux thèmes : ce sont des objets. Le d20 écarté s'atténue à 45% à l'arrêt. Chiffres en Alegreya SC 700 ; 6 et 9 soulignés sur les dés de 10 faces et plus ; le d4 porte ses numéros aux coins et se lit à la pointe. Sous `prefers-reduced-motion`, sans WebGL, aucun dé ne roule. Quand les dés s'arrêtent, l'issue se pose au-dessus de la cible dans une bulle `map-label-ground` (« −11 », « Raté », 20px gras) qui ne capte pas le pointeur : 1,2s, puis elle s'élève de 18px en s'effaçant (600ms) ; sans s'élever sous `prefers-reduced-motion`. Pose : une entrée du catalogue montre le curseur `grab` ; pendant le glisser, `grabbing` partout, et sur la carte un fantôme de la créature à 50% d'opacité, cerclé de laiton, suit le pointeur (aimanté, ou calé avec Maj). Position trop proche d'un autre jeton (pose ou déplacement) : anneau `--danger`. À son tour (combat) : anneau laiton en tirets (7/5) de 3px à 1.55 fois le rayon, plus large que l'anneau de cible et distinct du contour de sélection. Déplacement : le curseur `grab` sur un jeton glissable, `grabbing` pendant le geste ; le jeton suit le pointeur (calé sur la case avec Maj, sinon aimanté au contact des créatures proches, ou au bord du cercle de ce qui reste), un trait laiton en tirets (8/6) de 2px part du départ, et une bulle au-dessus de l'arrivée dit « 4,5 m parcourus · reste 4,5 m » (sans « reste » quand rien ne limite). Pour une créature limitée, un cercle laiton plein de 2px autour du départ montre ce qui reste. Trop loin ou trop près : trait, contour de bulle et anneau en `--danger`. Au clavier, le même aperçu suit le curseur de case, avec un fantôme à 50% d'opacité. La feuille de la créature active ajoute « · reste 4,5 m » à sa vitesse.

Zoom : trois boutons de 2rem empilés dans le coin haut droit de la carte, au-dessus des dés (« + » zoom avant, « − » zoom arrière, « Ajuster » : quatre coins tracés, carte entière), nommés et atteignables au Tab, éteints à leur borne. La molette, le pincement et les touches + − 0 font de même ; la carte zoome de la carte entière à ×8 autour du pointeur (du centre pour les boutons et le clavier) et ne quitte jamais le cadre. Étiquettes, traits et jauges gardent leur taille en pixels : seuls la carte, ses jetons et ses cercles grossissent. Le curseur clavier de case reste en vue. Glisser le fond (curseur `grab`, `grabbing` pendant le geste ; en pose et en ciblage, le curseur garde l'issue du clic) déplace la vue ; un clic sans glisser garde son effet. Au doigt, un doigt fait défiler la page et deux doigts déplacent et zooment la vue.

## Do's and Don'ts

### Do:
- **Do** réserver le laiton à l'état vivant : mode, entrée armée, sélection, cible survolée, attaque visée.
- **Do** mettre en rubrique le texte de règle : ce que le moteur tranche (verdict, issue, « À terre ») et le bloc de statistiques (nom, intertitres, chiffres).
- **Do** mettre en vermillon (`--danger`) les erreurs et les actions destructrices, jamais en rubrique.
- **Do** dire le camp par la forme : disque pour un personnage, carré pour un monstre, répété dans une pastille cerclée de `token-rim` partout où la créature est nommée.
- **Do** marquer une créature à 0 PV par trois signes : jeton gris, croix dessinée, nom barré.
- **Do** dessiner la portée longue en hachures, pas seulement en couleur.
- **Do** suivre chaque titre de panneau d'un filet effilé rubrique.
- **Do** déclarer toute couleur dans les deux thèmes et la lire depuis les variables, carte comprise.

### Don't:
- **Don't** mettre en laiton un état d'interface : l'onglet actif se souligne d'encre, le bouton de tiroir ouvert reste en `wood-sunken` et encre.
- **Don't** marquer le camp par une bande latérale colorée : la pastille de forme la remplace.
- **Don't** ajouter un mouvement hors de la séquence de l'attaque (dés 3D, verdict, bulle d'issue), ni l'animer sous `prefers-reduced-motion`.
- **Don't** poser d'ombre portée sur un panneau, une ligne ou un bouton ; la profondeur passe par le bois et le feutre.
- **Don't** introduire de surface grise neutre ni de néon : tout fond est bois ou feutre.
- **Don't** employer Alegreya SC pour un texte courant.
