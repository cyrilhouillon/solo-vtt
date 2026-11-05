# Vérification de l'epic « tenir le carnet de sa campagne »

Parcours du 2026-11-05. Serveur de test sur 8081 (campagnes et images dans le scratchpad), client sur 5174, Chrome en 1568 × 733, thème clair. Tout passe par l'interface, sans Swagger ni fichier édité. L'API n'a été lue qu'en `GET`, pour comparer avant et après le redémarrage.

## Preuve 1 : une session liée, puis l'aller-retour au combat

Verdict : **tenue**.

- Création de la campagne « Barovie », puis du Carnet vide qui invite à créer la première page.
- Pages créées depuis l'interface : « Sessions » et « PNJ » à la racine, « Ismark » et « Ireena » sous « PNJ » (menu « Nouvelle sous-page »), « Session 5 » sous « Sessions ».
- Session 5 s'ouvre déverrouillée. On y tape « À l'auberge du Sang de la Vigne, », `[[Ism`, Entrée, « nous présente », `[[Ire`, Entrée.
  - Le contenu enregistré porte deux nœuds `pageLink`, par `pageId`, avec `label: null`.
- Passage au combat :
  1. « Combat », puis carte 30 × 20 m ;
  2. Guerrière et Squelette glissés depuis le catalogue ;
  3. « Lancer le combat », « Fin du tour », « Terminer le combat » confirmé (journal : « Le combat se termine ») ;
  4. retour par le bouton Carnet.
- Au retour, Session 5 est ouverte, ses 5 onglets sont intacts, et son texte et ses liens sont présents.
- Couvert aussi par `CarnetDeBoutEnBoutTest` (phase 1).

## Preuve 2 : le renommage d'un PNJ et ses liens entrants

Verdict : **tenue**.

- Ismark renommé « Ismark le Petit » depuis le menu de sa ligne (« Renommer »).
- Le nouveau titre apparaît dans le lien de Session 5, dans l'onglet et dans l'arbre.
- Le lien de Session 5 ouvre la page d'Ismark, dont « Pages qui renvoient ici » liste « Session 5 ».

## Preuve 3 : la recherche

Verdict : **tenue**.

- Le mot « vigne » donne « 1 page », Session 5, avec l'extrait « …du Sang de la **Vigne**, Ismark le Petit nous présente Ireena ». Le texte du lien est lu sous le titre actuel.
- Un clic sur le résultat ouvre Session 5. Après « Effacer », l'arbre revient tel qu'il était déplié.

## Preuve 4 : la corbeille, liens compris

Verdict : **tenue**.

- Le menu de PNJ propose « Supprimer… ». La confirmation dit : « Supprimer « PNJ » et ses 2 sous-pages ? 1 page y renvoie : son lien s'affichera mort. »
- Après confirmation :
  - PNJ, Ismark et Ireena quittent l'arbre et leurs onglets se ferment ;
  - dans Session 5, les deux liens s'affichent barrés (« page introuvable »).
- La Corbeille montre « PNJ et 2 sous-pages ». Après « Restaurer » :
  - la corbeille est vide ;
  - l'arbre retrouve PNJ, Ireena et Ismark le Petit à leur place ;
  - les liens de Session 5 revivent sous leur titre actuel.

## Preuve 5 : la reprise après redémarrage

Verdict : **tenue**.

- Avant l'arrêt de 8081, relevé en `GET` de toutes les vues du Carnet :
  - liste des campagnes, `…/notebook` et `…/notebook/trash` ;
  - chaque page et ses pages qui la citent ;
  - `…/notebook/search?q=vigne`.
- Après l'arrêt de 8081 et sa relance sur le même dossier, le même relevé est identique octet pour octet (`cmp` ne trouve aucune différence, 2 212 octets).
- Interface :
  - le chargement direct de l'adresse d'Ismark le Petit rouvre cette page, qui liste Session 5 parmi les pages qui la citent ;
  - le rechargement de l'adresse de Session 5 rouvre Session 5 avec ses liens ;
  - Accueil, puis « Ouvrir Barovie », mène à Session 5, l'onglet actif ;
  - la console n'affiche aucune erreur.
- Le dossier `~/.solo-vtt/campagnes` n'a pas changé : même empreinte md5 de ses fichiers avant et après le parcours. 8080 et 5173 sont restés à l'écoute.

## Écarts

Aucun écart bloquant ni gênant.

| Écart | Gravité | Preuve |
| --- | --- | --- |
| Après un lien inséré par `[[`, taper une espace donne deux espaces dans le texte enregistré (« Ismark le Petit  nous présente »). | cosmétique, à confirmer : la suggestion semble ajouter son espace | contenu `GET …/pages/<Session 5>` : `"text":"  nous présente "` |
| Après une restauration, les onglets fermés par la suppression ne reviennent pas. | conforme : l'epic demande la restauration « liens compris », pas celle des onglets | parcours, preuve 4 |
