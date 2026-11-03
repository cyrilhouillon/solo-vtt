# Exécutions

## 2026-11-03 · head `e49b6b1`

Arbre de travail identique au head au moment des commandes.

| Commande | Résultat |
| --- | --- |
| `mvn -q test` (racine) | sortie 0 ; `vtt-engine` 25, `dnd-engine` 68, `vtt-server` 110 tests, 0 échec, 0 erreur (dont `MapApiTest`, `MapStoreTest`, `JsonFileMapStoreTest`, `ResumeGameTest`, `SceneWithoutMapTest`) |
| `npm run build` (`vtt-web/`) | sortie 0 ; `vue-tsc` sans erreur, build Vite fait (avertissement connu : chunk `DiceRoller` > 500 kB) |

## 2026-11-03 · head `dc6e017` · parcours navigateur de la phase 3

Code identique à `e49b6b1` (`dc6e017` ne touche que `aidd_docs/`). Serveurs de test 8081 / 5174, `solovtt.*.path` dans le scratchpad ; 8080 et 5173 jamais touchés.

| Scénario | Observé |
| --- | --- |
| Setup | image PNG, carte 37,5 × 24,9 m, grille masquée, Gobelin, Loup, Squelette (posés par l'API) |
| Aucune carte enregistrée | « Aucune carte enregistrée. » |
| Saisir « Crypte », Entrée | « Crypte · 37,5 m × 24,9 m · 3 créatures » listée ; annonce « Carte « Crypte » enregistrée. » ; champ vidé |
| Nom déjà pris | « Remplace la carte enregistrée « Crypte » » avant d'enregistrer |
| Combat lancé, Loup à 0 PV, puis Charger | confirmation « Remplace la carte actuelle, ses 3 créatures et son journal. », focus sur « Charger » ; Échap la ferme et rend le focus à « Charger Crypte » |
| Confirmer au clavier | même image, grille masquée, 3 créatures aux positions (12 ; 1,5), (20 ; 10), (30 ; 18), PV pleins ; `/api/combat` 204, journal vide, piste vide ; barre « Crypte · Carte 37,5 m × 24,9 m · 3 créatures » ; tiroir Catalogue ouvert ; annonce « Carte « Crypte » chargée : 3 créatures. » |
| Poser la Guerrière, lancer le combat | initiative tirée pour les 4 créatures (16, 13, 10, 6) |
| Redémarrer 8081 | « Crypte » toujours listée ; la partie (combat compris) reprise |
| Charger « Ruine » (entrée `dragon-rouge` ajoutée à la main dans `cartes.json`) | « Carte « Ruine » : créature inconnue au catalogue (dragon-rouge) » ; carte en cours et combat intacts |
| Échap dans le tiroir | tiroir fermé |
| Sans carte (partie du scratchpad effacée) | champ et Enregistrer désactivés, consigne affichée ; « Charger » charge d'emblée, sans confirmation |

Arrêt : 8081 puis 5174 ; 8080 et 5173 toujours à l'écoute.
