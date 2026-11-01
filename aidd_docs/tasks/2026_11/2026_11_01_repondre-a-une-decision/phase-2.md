---
status: pending
---

# Instruction: client — dialogue modal de décision

## Architecture projection

> Tree of the final files. ✅ create · ✏️ modify · ❌ delete

```txt
.
└── vtt-web/src/
    ├── types.ts                       ✏️ Decision ; AttackResult.decision
    ├── api.ts                         ✏️ pendingDecision() (204 → null), answer(decisionId, option)
    ├── components/DecisionDialog.vue  ✅ <dialog> modal : question, options, réponse en cours
    └── App.vue                        ✏️ décision en attente ; suite de l'action après réponse ; raccourcis bloqués ; relecture au chargement et à la resynchronisation
```

## Test Scope

| Scenario     | Action                  | Observable outcome   | Actor                           |
| ------------ | ----------------------- | -------------------- | ------------------------------- |
| Setup        | serveurs de test 8081/5174 ; carte 30 × 20 m, Gobelin et Magicienne au contact | scène prête | browser |
| Happy path   | Gobelin, Cimeterre → Magicienne, jusqu'à un jet que Bouclier ferait rater | le dialogue s'ouvre avec la question et « Lancer Bouclier » / « Ne rien faire » ; le focus est sur la première option ; le journal montre le jet | browser |
| Happy path   | clic sur « Lancer Bouclier » | le dialogue se ferme ; journal : réaction, CA 17, attaque ratée ; la piste à dés montre l'issue ; PV de la Magicienne inchangés | browser |
| Happy path   | même situation, « Ne rien faire » | journal : dégâts ; PV de la Magicienne baissés dans le tableau, la feuille et l'onglet Cible | browser |
| Edge case    | pendant le dialogue : Échap deux fois de suite, Suppr, Ctrl+Z, clic sur la carte ou le rail | le dialogue reste ouvert ; aucune requête de pose, retrait ou attaque n'est envoyée | browser |
| Edge case    | rechargement de la page pendant le dialogue | le dialogue revient avec la même question ; la réponse reprend l'action | browser |
| Edge case    | clic rapide deux fois sur une option | une seule requête de réponse | browser |
| Edge case    | réponse refusée par le serveur (décision disparue : nouvelle carte depuis un autre onglet) | resynchronisation : le dialogue se ferme, le message d'erreur s'affiche | browser |
| Edge case    | thème clair et thème sombre | le dialogue reste lisible dans les deux | browser |
| Teardown     | arrêter 8081 puis 5174 | serveurs de l'utilisateur intacts | system |

## Wireframe

```txt
┌──────────────────────────────────────────────────────────┐
│ (1) fond assombri : carte, tableau et panneaux inertes    │
│      ┌────────────────────────────────────────────┐      │
│      │ (2) Réaction · Magicienne                   │      │
│      │ (3) Magicienne est touchée (14 contre CA    │      │
│      │     12). Lancer Bouclier (réaction,         │      │
│      │     emplacement de niveau 1) ?              │      │
│      │ (4) [ Lancer Bouclier ]  [ Ne rien faire ]  │      │
│      └────────────────────────────────────────────┘      │
└──────────────────────────────────────────────────────────┘
```

1. Fond : `::backdrop` du dialogue modal ; la page derrière est inerte.
2. Titre : nom de la créature qui choisit (`chooserName`), lié par `aria-labelledby`.
3. Question : `prompt` du moteur, tel quel, lié par `aria-describedby`.
4. Options : un bouton par option, dans l'ordre du moteur ; le premier a le focus ; tous désactivés pendant la requête.

## Tasks to do

### `1)` Contrat client

> Le client lit et envoie la décision du serveur.

1. `types.ts` : `Decision { id, chooserId, chooserName, prompt, options }` ; `AttackResult.decision: Decision | null`.
2. `api.ts` : `pendingDecision()` rend `Decision | null` ; `answer(decisionId, option)` rend un `AttackResult`.

### `2)` Dialogue

> La question s'impose tant qu'elle n'a pas de réponse.

1. `DecisionDialog.vue` : prop `decision`, émet `answer(option)` ; `showModal()` quand la décision arrive, `close()` quand elle part ; `cancel` annulé (`preventDefault`) ; sur `close`, rouvrir (`showModal()`) tant que `decision` n'est pas null (Chromium ferme sans `cancel` au second Échap sans activation utilisateur).
2. Boutons selon `DESIGN.md` ; le dialogue et son `::backdrop` n'y existent pas encore : les définir avec les tokens de `style.css` (surface, encre, ombre), thèmes sombre et clair.

### `3)` Reprise de l'action

> Après la réponse, l'action se termine comme une attaque sans décision.

1. `App.vue` : `decision` ref ; `attackTarget` termine le ciblage et ouvre le dialogue si `result.decision`, sinon finit l'action comme aujourd'hui.
2. Extraire la fin d'action (journal, PV du jeton, piste à dés, feuilles, onglet Cible) en une fonction partagée par l'attaque et la réponse.
3. Réponse : drapeau contre le double clic ; si la réponse rend une nouvelle décision, le dialogue la montre.
4. `onKey` ne fait rien tant qu'une décision attend (le dialogue modal ne bloque pas l'écouteur de `window`).
5. `onMounted` et `resync` relisent la décision en attente ; si `resync` en trouve une, il affiche « Une décision attend une réponse » au lieu de « La carte a été modifiée hors de cet onglet » (`attempt` renvoie tout 409 vers `resync`).

## Test acceptance criteria

| Task | Acceptance criteria              |
| ---- | -------------------------------- |
| 1 | `npm run build` passe. |
| 2 | Le dialogue s'ouvre sur la question et ses options, Échap ne le ferme pas, le focus reste dedans ; lisible en thème clair et sombre. |
| 3 | Parcours sur 5174 : Bouclier lancé fait rater, refusé fait perdre les PV ; Suppr, Ctrl+Z et clics n'envoient rien pendant l'attente ; un rechargement rouvre la même question. |
