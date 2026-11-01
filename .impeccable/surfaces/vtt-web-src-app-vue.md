---
version: 1
slug: "vtt-web-src-app-vue"
primary_target: "vtt-web/src/App.vue"
related_targets: ["vtt-web/src"]
---

# Table de jeu (vtt-web)

- Mode : Operate. Le joueur solo, joueur et MJ à la fois, en longues sessions sur grand écran, le soir comme en journée (thèmes sombre et clair).
- Tâche : préparer la scène, poser les créatures, attaquer depuis la feuille, lire le verdict. Le moteur tranche, le client mesure.
- Intouchables : couche d'interaction (modes exclusifs, Échap, Ctrl+Z, pose au clavier), double accessible du canvas, aria-live, français.
- Anti-objectifs : jeu vidéo (néons, effets), outil d'admin interchangeable ; ornement permis tant que la carte reste lisible.
- Décisions ouvertes : colonne d'initiative absente tant que le moteur n'a pas de tours ; dés animés et verdict structuré dans la story « voir les dés rouler ».

## Direction contract

THESIS : une table de D&D posée sur un plateau de feutre bordé de bois ; le moteur lance sur la piste, le joueur lit le verdict. Refuse l'admin sombre à trois colonnes égales.

OWN-WORLD : feutre vert bouteille et noyer (sombre), lin avoine et érable (clair), texte ivoire ou encre brune ; laiton pour le seul état vivant (mode, cible cerclée) ; rouge rubrique pour ce que le moteur tranche. Titres en petites capitales Alegreya SC, composition « bloc de statistiques » : filets effilés, chiffres clés en rubrique. Ruban de camp par créature ; à 0 PV, gris et ✕. Portée en hachures.

STORY : le joueur voit où il est (mode), qui est sur la table et en quel état (tableau), et ce que le moteur vient de trancher (piste).

FIRST VIEWPORT : barre de bois en haut (nom, carte, mode en laiton, thème) ; rail d'icônes à gauche ouvrant Catalogue ou Nouvelle carte (catalogue ouvert sur carte vide) ; au centre, tableau « Sur la carte » façon tableau des départs, carte sur feutre, piste à dés en dessous ; à droite, onglets Feuille, Cible, Journal en blocs de statistiques.

FORM : Plateau de dés, candidat 5 de la liste ordonnée, plus rail et onglets du standard, tableau du challenger « Tableau des départs », panneaux du « Bloc de statistiques ». seed:1517dc06f45b6ba8

FINISH : unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
