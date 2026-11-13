# Review: voir sur la fiche les PV perdus en combat

- Rounds: 1
- Started: 2026-11-13

## Round 1 · r-3d44

- Date: 2026-11-13
- By: Cyril Houillon
- Diff: `main...9726382`
- Axes: code, functional, relevancy
- Verdict: approve
- Score: 7/7 met, 0 unmet, 0 unverified at runtime, 0 out of the diff

### Criteria

- [x] Phase 1 · 1 — une histoire avec `HitPointsLost` se plie en PV baissés, jamais sous 0, construction et jets intacts — `dnd-engine/src/main/java/dnd/engine/sheet/SheetHistory.java:31-33`, `SheetEvent.java:61-65` ; run Final sur 0ee4656 (seul du Markdown depuis) : dnd-engine `Tests run: 138, Failures: 0`
- [x] Phase 1 · 2 — un coup baisse la fiche lue par `GET`, `take` et une autre instance ; page non fiche `false` sans écrire ; document abîmé 409 non écrasé — `SheetCharacters.java:91-98`, `SheetResource.java:222-230`, `SheetStore.java:46-56,109-112` ; run Final : dnd-server `Tests run: 39, Failures: 0`
- [x] Phase 2 · 1 — un coup sur un PJ part une seule fois avec sa commande ; reprise, `replayed`, `resume`, pas à pas, nouvelle carte n'envoient rien ; un suivi qui lève laisse le coup gardé et avertit — `SceneSession.java:289-295,611-640`, garde `replaying` à `:626` ; run phase 2 (contre-épreuve garde retirée : 2 échecs) et Final : vtt-server `Tests run: 292, Failures: 0`
- [x] Phase 2 · 2 — de bout en bout une vraie fiche perd les PV du journal ; module inactif ou fiche inconnue refusés ; personnages de test sans avertissement — `CampaignCatalog.java:75-80`, `CampaignSessions.java:30-34`, `TestCharacters.java:57-61` ; run Final `BUILD SUCCESS`
- [x] Phase 3 · 1 — `npm run build` passe ; l'avertissement s'affiche au bandeau, l'action résolue — `GameScreen.vue:707-709,737-738`, `types.ts:277-278` ; run phase 3 sur 0ee4656 : bandeau « Fiche « Aria » non mise à jour : fiche introuvable ou module inactif », verdict « Touché · 5 dégâts tranchants »
- [x] Phase 3 · 2 — les quatre critères de la story s'observent dans le navigateur ; module désactivé avertit — run phase 3 sur 0ee4656 : fiche à 3 après le coup, après redémarrage de 8081, après rejeu « Pas 4 / 4 », pose suivante « Aria 3 / 11 »
- [x] Phase 4 · 1 — la mémoire nomme le port, l'événement et ses limites ; story `done` — `architecture.md:72,96`, `codebase-map.md`, `project-brief.md` ; `voir-sur-la-fiche-les-pv-perdus-en-combat.md:3` `status: done`

### Findings

- `aidd_docs/memory/architecture.md:72` : coquille « laisse l'ction faite » → « laisse l'action faite » — 🟢 minor, standards
- `aidd_docs/memory/architecture.md:96` : coquille « d'vant toute retouche » → « d'avant toute retouche » — 🟢 minor, standards
- `vtt-web/src/components/GameScreen.vue:736-738` : l'avertissement est reposé après `showSheet`, non attendu ; si ce rechargement échoue, `attempt` écrase l'avertissement par son erreur (et l'inverse si l'échec arrive plus tard) → tenir les avertissements dans un état à part du bandeau d'erreur, ou les reposer après l'`await` du rechargement — 🟢 minor, frontend
