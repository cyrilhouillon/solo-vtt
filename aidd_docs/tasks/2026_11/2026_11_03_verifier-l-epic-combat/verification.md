# Vérification de l'epic « jouer un combat D&D 2024 en local »

Parcours du 2026-11-03, serveur de test 8081 (sauvegarde dans le scratchpad), client 5174, interface seule : ni Swagger, ni fichier édité. Le serveur n'a été lu qu'en `GET` (`/api/journal`, `/api/scene`, `/api/combat`) pour comparer avant et après chaque redémarrage.

## Preuve 1 : combat de 2 personnages contre 3 adversaires, de bout en bout

Verdict : **tenue**.

- Carte 30 × 20 m ; Guerrière, Magicienne, Gobelin guerrier, Squelette, Loup posés au clic.
- Lancer le combat : initiative tirée, ordre « Magicienne (22), Squelette (15), Guerrière (14), Gobelin guerrier (8), Loup (6) ».
- Joué sur 2 rounds :
  - attaque à distance au-delà de la portée normale (Dague lancée à 8 m : désavantage, d20 3 gardé, 7 barré) ;
  - déplacement de la créature active limité (« Squelette se déplace de 6,7 m (reste 2,3 m) ») ;
  - touchés, coups critiques, ratés, style Duel (+2) ;
  - créature à 0 PV : marquée « À terre », boutons d'attaque fermés avec la raison, son tour s'ouvre quand même ;
  - créature posée en combat (Magicienne 2) : « entre dans le combat (initiative 14, jouera au round suivant) », puis ex æquo avec la Guerrière et « Qui joue ? » ;
  - Bouclier proposé sur un touché provisoire (13 contre CA 12), lancé : « 13 contre CA 17, RATÉ » ;
  - « Terminer le combat », confirmé : créatures et PV restent sur la carte.
- Journal : chaque action du moteur y figure (lancement, initiative, ordre, rounds, tours, déplacement, attaques, dés, réaction, entrée en combat, fin). Pose, retrait et déplacement libre n'y figurent pas, par conception ; la lecture du combat n'en souffre pas : conforme.
- Couvert aussi par `CombatDeBoutEnBoutTest` (phase 1).

## Preuve 2 : reprise à l'identique

Verdict : **tenue**.

| Moment du redémarrage de 8081 | Journal, scène, combat (`cmp`) | Interface après rechargement |
| --- | --- | --- |
| Pendant « Qui joue ? » | identiques | le dialogue revient ; choisir la Guerrière reprend le tour |
| Pendant Bouclier | identiques | le dialogue revient ; « Lancer Bouclier » fait rater l'attaque |
| Après la fin du combat | identiques | aucun combat, PV et « À terre » conservés |

`~/.solo-vtt/partie.json` inchangé (horodatage 02:10:18, 11 631 octets) ; 8080 et 5173 toujours à l'écoute.

## Écarts

Aucun écart bloquant ni gênant.

| Écart | Gravité | Preuve |
| --- | --- | --- |
| Pendant la décision Bouclier, la piste à dés montrait « Magicienne 2 attaque Squelette (Dague) » (l'attaque précédente) sans verdict ni dés, au lieu du d20 du Gobelin ; après réponse, la piste est juste (« 13 contre CA 17 », d20 9). Observé une fois, non reproduit. | cosmétique, à confirmer | capture du parcours, attaque n° 21 du journal |
| En combat, le tableau « Sur la carte » ne montre que 3 lignes sur 6 sans défiler. | cosmétique | capture du parcours |
| Après un rechargement, la piste montre la dernière attaque sans le nom ni la jauge de la cible. | cosmétique, voulu (`trayFromJournal()` sans cible) | capture après redémarrage |

## Hors périmètre de l'epic, relevé pour la suite

- Une créature à 0 PV reste « À terre » sans jet contre la mort ni stabilisation : règle non modélisée, et absente de l'epic.
- Attaque à distance au contact d'un ennemi sans désavantage : piège déjà connu (`architecture.md`).
