# Vérification de l'epic « jouer sa campagne avec Mythic GME depuis le Carnet »

Parcours du 2026-11-09, sur le commit `bd58ec8`. Serveur de test sur 8081, avec les campagnes, les images et les données de modules dans le scratchpad ; client sur 5174 ; Chrome en 1568 × 733, thème clair.

- Tout passe par l'interface, sans Swagger ni fichier édité.
- L'API n'a été lue qu'en `GET`, pour comparer avant et après le redémarrage.
- Le fichier de tables importé est généré, avec des mots inventés (`action1-001`…, format de `TablesDeTest`), hors du dépôt.

## Preuve 1 : les tables de mots importées une fois

Verdict : **tenue**.

- Avant l'import, la section Sens du panneau dit : « Tables de sens non importées : importer le fichier de vos tables. »
- Après l'import par « Importer », elle propose la table (Actions, Descriptions, Lieux), « Tirer » et « Remplacer les tables ».
- Après le redémarrage de 8081, `GET /api/mythic/meaning-tables` rend toujours `imported: true`, sans nouvel import.
- Les deux événements tirés plus bas ont chacun leurs deux mots d'action.

## Preuve 2 : une question écrite dans « Session 6 »

Verdict : **tenue**.

- Page créée : « Session 6 ». Au chaos 5, question « Le garde est-il corrompu ? », probabilité « Probable ».
- La page reçoit cette citation :
  - « Oracle : Le garde est-il corrompu ? (Probable, chaos 5) » ;
  - « d100 : 8 → Oui exceptionnel ».
- Le panneau rappelle le même résultat.

## Preuve 3 : un événement aléatoire tire un fil et deux mots

Verdict : **tenue**.

- La page Fils contient « Trouver l'évêque » et « Sauver Ireena ». La page Personnages contient un lien `[[Ismark]]`.
- Au chaos 9, il a fallu 18 questions, en appuyant sur « Demander » à 550 ms d'intervalle.
- Au 15e essai, un premier événement porte sur un fil : « Événement aléatoire : Loin d'un fil — action1-077 · action2-060 ».
  - Il tombe sur une ligne vide : « Fils : Choisir — ligne vide : l'entrée la plus logique, ou relancez (d10 : 10 → ligne 5) ».
  - C'est la règle du livre : une section de 5 lignes dont seules 2 sont écrites.
- Au 18e essai, l'événement tire une entrée :
  - « Événement aléatoire : Vers un fil — action1-040 · action2-037 » ;
  - « Fils : Sauver Ireena (d10 : 3 → ligne 2) ».
- Entre les deux, un événement « Action d'un PNJ » a tiré dans Personnages (« Choisir », ligne 5).
- Captures : `screenshot-1794240270367-0.jpg` dans `C:\Users\Cyril\AppData\Local\Temp\claude-chrome-screenshots-1VAmHu\`.

## Preuve 4 : la scène se clôt, le chaos s'ajuste, la scène suivante sort modifiée

Verdict : **tenue**.

- Au chaos 9, « Contrôle gardé » écrit « Fin de scène : contrôle gardé → chaos 9 → 8 ». Le panneau affiche ensuite 8.
- La scène attendue « On interroge le garde au poste » est testée au premier essai. Elle donne :
  - « Scène attendue : On interroge le garde au poste (chaos 8) » ;
  - « d10 : 7 → Scène modifiée » ;
  - « Ajustement : Intensifier une activité (d10 : 4) ».
- Le panneau rappelle « Scène modifiée · d10 7 · chaos 8 / Intensifier une activité ».

## Preuve 5 : les fils et personnages dans l'arbre, après un redémarrage

Verdict : **tenue**.

- À la première ouverture de « Barovie », l'arbre a une section « Mythic », sous les pages ordinaires, avec « Fils » et « Personnages ».
- Aller à l'accueil puis rouvrir « Barovie » donne Session 6, ses 4 onglets, le chaos 8 et la section Mythic.
- Relevé `GET` avant l'arrêt de 8081, puis après sa relance : `cmp` identique. Le relevé porte sur :
  - les campagnes et `…/notebook` ;
  - chaque page, avec son contenu ;
  - `…/mythic/chaos` (`{"chaos":8}`) ;
  - `/api/mythic/meaning-tables`.
- Le rechargement de l'onglet sur l'adresse de Fils donne Fils avec ses deux fils, les mêmes onglets et le chaos 8. Personnages s'ouvre avec le lien Ismark.
- La console n'affiche aucune erreur.
- Le `POST …/mythic/lists` refait à la réouverture ne crée pas de doublon : une seule page Fils et une seule Personnages.

## Preuve 6 : le code Mythic vit dans son module

Verdict : **tenue** (phase 1, `run.md`).

- `grep -rni mythic vtt-server/src vtt-web/src` ne trouve rien.
- Mythic n'est cité que par les assemblages : `pom.xml` (l. 17-18, 46-52), `vtt-server/pom.xml` (l. 45-46) et `vtt-web/modules/mythic/`.

## Écarts

Aucun bloquant ni gênant. Deux cosmétiques, laissés sans item :

- Après une réponse, le champ Question se vide (`MythicPanel.vue:203`). Redemander sans retaper écrit « question sans énoncé », ce qui est voulu.
- La probabilité revient à 50/50 quand le panneau est remonté (retour par l'accueil, rechargement) ; le chaos, lui, est gardé.
