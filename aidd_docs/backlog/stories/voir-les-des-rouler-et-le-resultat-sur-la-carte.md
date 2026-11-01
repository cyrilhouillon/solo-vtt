---
type: story
status: draft
parent: aidd_docs/backlog/epics/jouer-un-combat-dnd-2024-en-local.md
depends_on:
  - aidd_docs/backlog/stories/attaquer-depuis-la-feuille-en-ciblant-sur-la-carte.md
order: 8
---

# Story : voir les dés rouler et le résultat sur la carte

**En tant que** joueur solo
**Je veux** voir les dés de mon attaque rouler puis s'arrêter sur leur valeur, et lire ses effets en surimpression sur la carte
**Afin de** sentir le jet comme à la table, sans quitter la carte des yeux

## Acceptance

- Dès qu'une attaque part, des dés roulent sur la carte. Ils s'arrêtent sur les valeurs tirées par le serveur : un d20, ou deux avec avantage ou désavantage (le dé gardé est distingué), puis les dés de dégâts.
- Les valeurs affichées sont celles du serveur, jamais un tirage du client.
- Les effets (touché, raté, critique, dégâts, PV restants) s'affichent en surimpression au milieu de la carte, puis disparaissent seuls.
- La surimpression ne capte pas les clics et ne bloque pas la carte une fois disparue.
- Les lecteurs d'écran entendent le résultat une seule fois.
- Si l'utilisateur demande moins d'animations (`prefers-reduced-motion`), le résultat s'affiche sans dés qui roulent.
- Le journal reste la trace complète de l'attaque.

## Notes

- Le client a besoin des jets en données structurées (`AttackResultDto`), sans relire le texte du journal.
- L'animation ne sert pas de garde contre une double attaque : le client ignore déjà les clics tant que l'attaque n'a pas répondu (`App.vue`, `attacking`).
