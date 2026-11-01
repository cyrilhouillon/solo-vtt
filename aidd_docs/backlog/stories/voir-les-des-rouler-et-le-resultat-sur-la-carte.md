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
- La cible porte brièvement sur la carte l'issue de l'attaque (« −11 », « Raté »), là où le regard se pose après le clic.
- Chaque jeton montre ses PV restants en permanence (jauge ou anneau), lisibles sans ouvrir la feuille.
- Une créature à 0 PV est visiblement à terre sur la carte et dans la liste « Sur la carte » (forme et texte, pas seulement la couleur).
- Chaque entrée du journal annonce d'abord son issue (touché, raté, critique, dégâts) ; le détail des règles (`[Règles]`, `[Dégâts]`, `[Style Duel]`) est replié par défaut.

## Notes

- Le client a besoin des jets en données structurées (`AttackResultDto`), sans relire le texte du journal.
- Origine des quatre derniers critères : critique de design du 2026-11-01 (`.impeccable/critique/2026-11-01T17-31-51Z__vtt-web-src.md`, P1 n°1). La carte ne réagit pas à une attaque et une créature à 0 PV ressemble à une créature intacte (`SceneCanvas.ts` n'affiche aucun PV).
- Hors périmètre, à trancher : le serveur accepte aujourd'hui une attaque sur une créature à 0 PV. Le refuser ou demander confirmation est une règle, pas de l'affichage.
- L'animation ne sert pas de garde contre une double attaque : le client ignore déjà les clics tant que l'attaque n'a pas répondu (`App.vue`, `attacking`).
