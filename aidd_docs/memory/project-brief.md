# Brief du projet

## Ce que c'est

- `solo-vtt` : un Virtual TableTop web modulaire, conçu spécialement pour le jeu de rôle en solo.
- Aujourd'hui, le dépôt contient `vtt-engine` (moteur de règles et scène génériques), `dnd-engine` (squelette de règles D&D 2024 qui s'appuie dessus), `vtt-server` (serveur Quarkus qui les expose) et `vtt-web` (client Vue).

## Pourquoi il existe

- Les VTT existants sont pensés pour une table multijoueur avec un MJ humain. `solo-vtt` vise le joueur seul : le moteur arbitre les règles, et le joueur ou une IA répond aux décisions.
- `dnd-engine` valide l'architecture du moteur avant d'y brancher du contenu et une interface.

## Vocabulaire du domaine

Le code, les commentaires et les messages sont en français ; les identifiants sont en anglais.

| Terme | Sens |
| ----- | ---- |
| SRD 5.2 | Référence de règles D&D 2024 sous licence CC-BY-4.0, base des règles implémentées. Source à consulter pour toute règle ou fiche : le PDF officiel 5.2.1, https://media.dndbeyond.com/compendium-images/srd/5.2/SRD_CC_v5.2.1.pdf (pas de wiki tiers) |
| Phase | Étape ordonnée d'une action (`AttackPhase`) ; l'ordre de l'enum est l'ordre d'exécution |
| Listener | Une règle qui agit dans une phase ; règles de base et exceptions ont le même statut |
| RuleBook | Registre des listeners actifs ; activer un module = enregistrer ses listeners |
| Résolution | Ce qui est en cours sur la pile du moteur (une attaque, un sort…) |
| Décision | Question posée à un joueur ou une IA ; suspend le moteur jusqu'à `Engine.answer` |
| Réaction | Capacité déclenchée pendant l'action d'un autre (Bouclier), via une décision |
| Événement | `GameEvent` : seul moyen de modifier l'état ; le journal est rejouable |
| CA | Classe d'armure |
| Scène | Une carte (dimensions, unité) et les jetons posés dessus ; générique, sans règle |
| Jeton | Pion d'une scène, qui référence une entité du système par son id |
| Avantage / désavantage | Lancer 2d20 et garder le meilleur / le pire ; les deux s'annulent |

## Fonctionnalités clés

- Attaque complète en 12 phases, du jet de d20 aux dégâts (résistance puis vulnérabilité).
- Conditions, Bénédiction, réaction Bouclier, style de combat Duel.
- Moteur suspendable sur décision, pile de résolutions, journal d'événements rejouable.
- Démo console (`dnd.engine.demo.Main`), avec un mode interactif.
- Préparation de scène dans le navigateur : carte vide en mètres ou en pieds, ou tirée d'une image de battlemap (PNG, JPEG, WebP) dont on donne le nombre de cases en largeur ou la taille d'une case en pixels, grille affichable ou masquable, créatures du catalogue SRD glissées sur la carte, à la souris ou au doigt (au clavier : Entrée sur la créature, puis flèches et Entrée), distance entre deux créatures, feuille de chaque créature.
- Attaque depuis la feuille : ciblage sur la carte, allonge, portée normale et longue, lancer ; résultat dans le journal et PV mis à jour.
- Dés à la table : les dés 3D de l'attaque roulent sur la carte (jet d'attaque, réaction éventuelle, puis dégâts) et s'arrêtent sur les valeurs du serveur ; la piste les pose (d20 écarté barré), le verdict se pose, et l'issue (« −11 », « Raté ») s'affiche brièvement sur la cible. Chaque jeton montre ses PV par une jauge.
- Décision du moteur dans le navigateur : la réaction Bouclier arrête l'attaque, le joueur choisit dans un dialogue, puis l'attaque reprend.
- Déplacement : glisser un jeton, ou D puis flèches au clavier ; hors grille par défaut, Maj cale sur la case. En combat, la créature active est limitée par sa vitesse restante (affichée pendant le geste, refusée au-delà, pleine à son tour suivant) ; les autres se repositionnent librement.
- Combat au tour par tour : lancer le combat tire l'initiative, l'ordre de jeu et la créature active s'affichent, « Fin du tour » passe à la suivante et ouvre un nouveau round après la dernière ; les ex æquo font choisir qui joue. Seule la créature active attaque ; une créature posée ou retirée en combat entre dans l'ordre ou en sort. « Terminer le combat », à confirmer, y met fin à tout moment (sauf pendant une décision) : les créatures restent sur la carte avec leurs PV.
- Zoom et vue : molette, pincement, boutons de la carte ou + − 0, de la carte entière à ×8 ; glisser le fond déplace la vue (à deux doigts au toucher), en combat, en préparation et pendant le calage. La vue n'est pas gardée : une nouvelle carte s'affiche entière.
- Caler la grille sur l'image : décalage horizontal et vertical, taille de case au pixel de l'image, en glissant la carte au bouton du milieu ou dans des champs, avec la grille d'aperçu qui bouge ; valider met la carte à la nouvelle échelle (dimensions, distances, Maj), les créatures restent au même endroit de l'image ; annuler rend le calage d'avant. Impossible en combat. Le calage se garde avec la partie et les cartes enregistrées.
- Campagnes : l'application s'ouvre sur un accueil qui liste les campagnes et en crée une sous un nom ; chacune garde sa partie et ses cartes enregistrées dans `~/.solo-vtt/campagnes/<id>/`. Le bouton Accueil de la barre permet d'en changer ; y revenir la retrouve à l'identique.
- Adresses : chaque écran a la sienne (accueil, Carnet d'une campagne sur une page, combat) ; recharger l'onglet ou ouvrir un favori rouvre le même écran sur la même page, Précédent et Suivant repassent par les pages et les écrans visités, après l'enregistrement de la page en cours (refusé s'il échoue). Une adresse vers une campagne inconnue mène à l'accueil ; vers une page inconnue, au Carnet comme sans page.
- Carnet de campagne : ouvrir une campagne mène à son Carnet, sur ses onglets (même après un redémarrage du serveur) : chaque page ouverte, depuis l'arbre, un lien, la recherche ou les pages qui la citent, entre dans un onglet ou active le sien ; on passe de l'un à l'autre, on les ferme, et chacun suit le renommage de sa page ; on y crée des pages titrées, rangées dans un arbre qu'on construit soi-même (créer sous une page, renommer depuis l'arbre ou le titre de la page, déplacer une page avec ses sous-pages, jamais sous elle-même ; branches qu'on ouvre et replie, sœurs triées par titre, « Session 2 » avant « Session 10 »), et on les écrit en texte riche au fil du texte (titres, gras, italique, listes, citations, annuler et rétablir), enregistré tout seul. Déverrouillée, une page reçoit des images PNG, JPEG ou WebP du disque (bouton Image, dépôt ou collage), posées au curseur ou au point de dépôt ; un autre type ou plus de 50 Mo est refusé avec la raison ; une image se retire comme du texte, et Ctrl+Z la remet. Taper `[[` lie une page existante choisie par son titre : le lien affiche le titre actuel de sa cible, même renommée ou déplacée, ou un texte choisi (« l'aubergiste »), ouvre la cible au clic (Ctrl+clic ou sa bulle en écriture) et s'affiche barré si la cible n'existe plus. Sous son texte, chaque page liste les pages qui la citent, par leur titre actuel, et les ouvre au clic (« Aucune page ne renvoie ici. » sinon). Un champ « Chercher » au-dessus de l'arbre retrouve les pages dont le titre ou le texte contient tous les mots tapés, sans casse ni accents (« eveque » trouve « Évêque »), les pages au titre trouvé d'abord, chacune avec un extrait où les mots sont surlignés ; un clic l'ouvre. Une page s'ouvre verrouillée, sauf celle qu'on vient de créer ; un clic sur le verrou la rend éditable, au même endroit. « Combat » ouvre l'écran de jeu, d'où le bouton Carnet ramène à la même page. Une page se supprime depuis son menu, après une confirmation qui dit ses sous-pages et les pages qui y renvoient : elle part à la corbeille avec ses sous-pages, ses onglets se ferment et les liens vers elle s'affichent barrés ; la corbeille la restaure à sa place (à la racine si sa place n'existe plus), liens compris, et se vide après confirmation. Les pages sont gardées dans `~/.solo-vtt/campagnes/<id>/carnet/`.
- Oracle Mythic GME 2e dans le Carnet : un panneau à droite de la page tient le facteur de chaos de la campagne (5 au départ, de 1 à 9, gardé après un redémarrage, propre à chaque campagne) et pose une question oui/non avec une probabilité, d'« Impossible » à « Certain » : la table de destin répond oui, non, oui exceptionnel ou non exceptionnel, et un double dont le chiffre ne dépasse pas le chaos (ou 100) annonce un événement aléatoire et son focus. Question, probabilité, chaos, jet, réponse et événement s'écrivent dans la page ouverte, au curseur, ou à la fin si elle est verrouillée ; sans page, la question est refusée. Mythic est un module : sans lui, le Carnet marche comme avant.
- Cartes enregistrées : la carte en cours (image, échelle, grille et son calage, créatures posées et positions) s'enregistre sous un nom, dans sa campagne ; la charger remplace la carte comme « Nouvelle carte », avec des créatures neuves, sans combat. La partie copie la carte, ne la référence pas, et en affiche le nom.
- Reprise à l'identique : la partie est gardée après chaque action dans le dossier de sa campagne ; relancer le serveur puis rouvrir la campagne la retrouve, décision en attente comprise.
