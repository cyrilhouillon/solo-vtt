<script setup lang="ts">
import { computed } from 'vue'
import type { AttackRoll, Damage } from '../types'

/**
 * La piste à dés, sous la carte : la dernière attaque tranchée par le moteur. Les tours de combat ne l'effacent pas
 * (la barre dit qui joue). Les dés eux-mêmes viendront avec les jets structurés (story « voir les dés rouler »).
 * Muette pour les lecteurs d'écran : l'issue leur est annoncée à part.
 */
export interface TrayResult {
  /** Numéro de l'action : chaque attaque tranchée en a un nouveau, et le verdict se pose à chacune. */
  action: number
  /** L'annonce de l'attaque (qui, avec quoi, sur qui). */
  title: string
  /** L'issue et son calcul, tels que le moteur les a tranchés. */
  roll: AttackRoll
  /** Les dégâts subis, dans l'ordre du journal. */
  damage: Damage[]
  /** Les réactions qui ont changé le coup, dans le texte du journal. */
  reactions: string[]
  /** Absent quand le résultat est relu du journal (rechargement) : la piste n'a alors pas la jauge. */
  target?: { name: string; hitPoints: number; maxHitPoints: number }
}

const props = defineProps<{ result: TrayResult | null }>()

/** L'issue de l'attaque, pas son intérêt pour le joueur : elle se lit à la forme (voir les styles). */
const LABELS: Record<AttackRoll['outcome'], string> = { CRITICAL: 'Coup critique', HIT: 'Touché', MISS: 'Raté' }

/** « Touché · 6 dégâts tranchants » : l'issue d'abord, ce qu'elle a coûté ensuite. */
const headline = computed(() => {
  if (!props.result) return ''
  const { roll, damage } = props.result
  if (!damage.length) return LABELS[roll.outcome]
  const total = damage.reduce((sum, d) => sum + d.amount, 0)
  const type = damage.length === 1 ? ` ${damage[0]!.type}` : ''
  return `${LABELS[roll.outcome]} · ${total} ${total > 1 ? 'dégâts' : 'dégât'}${type}`
})

/** Le calcul (« 14 contre CA 17 ») et la réaction qui l'a changé, lus juste après le verdict. */
const line = computed(() => {
  const r = props.result
  return r ? [`${r.roll.total} contre CA ${r.roll.armorClass}`, ...r.reactions].join(' · ') : ''
})
</script>

<template>
  <div class="rim" aria-hidden="true">
    <div class="tray">
      <template v-if="result">
        <p :key="result.action" class="verdict" :class="result.roll.outcome.toLowerCase()">{{ headline }}</p>
        <p class="line">{{ line }}</p>
        <p class="title">{{ result.title }}</p>
        <p v-if="result.target" class="target">
          <span class="who">{{ result.target.name }}</span>
          <span class="gauge"><i :style="{ width: `${Math.max(0, result.target.hitPoints / result.target.maxHitPoints) * 100}%` }" /></span>
          <span class="hp">{{ result.target.hitPoints === 0 ? 'à terre' : `${result.target.hitPoints} / ${result.target.maxHitPoints} PV` }}</span>
        </p>
      </template>
      <p v-else class="idle">Aucune attaque pour l'instant : le verdict du moteur s'affichera ici.</p>
    </div>
  </div>
</template>

<style scoped>
/* Bordure de bois autour d'un fond de feutre creusé. */
.rim {
  padding: 0.6rem 0.75rem 0.75rem;
  background: var(--wood);
  border-top: 1px solid var(--rule);
}
/*
 * Hauteur fixe de trois lignes (verdict, action, réaction ou dégâts) : la carte au-dessus ne se redimensionne pas
 * quand une réaction ajoute une ligne.
 */
.tray {
  height: calc(1.5rem * 1.2 + 2 * 0.95rem * 1.45 + 1.2rem);
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  grid-template-rows: auto auto auto;
  align-content: center;
  align-items: center;
  column-gap: 1.5rem;
  padding: 0.6rem 1.25rem;
  border-radius: 8px;
  background: var(--felt-deep);
  box-shadow:
    inset 0 3px 10px rgba(0, 0, 0, 0.45),
    inset 0 0 0 1px var(--felt-rim),
    0 0 0 3px var(--rule);
  color: var(--map-label);
}
p {
  margin: 0;
}
.title {
  grid-column: 1;
  font-size: 0.95rem;
  opacity: 0.85;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* Le calcul et la réaction : ce qui explique le verdict, lu juste après lui. */
.line {
  grid-column: 1;
  font-size: 0.95rem;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
/* L'issue se lit à la forme, pas à la teinte (le feutre est déjà vert) : touché en rubrique et en gras, coup critique
   en capitales pleines, raté en ivoire atténué et maigre. */
.verdict {
  grid-column: 1;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 1.5rem;
  letter-spacing: 0.02em;
  line-height: 1.2;
  color: var(--rubric);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  animation: land 320ms cubic-bezier(0.16, 1, 0.3, 1);
}
.verdict.critical {
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.verdict.miss {
  font-weight: 500;
  color: var(--map-label);
  opacity: 0.75;
}
/* Le verdict se pose sur le feutre, comme un dé qui s'arrête. */
@keyframes land {
  from {
    opacity: 0;
    transform: translateY(-0.4rem);
  }
}
@media (prefers-reduced-motion: reduce) {
  .verdict {
    animation: none;
  }
}
.target {
  grid-column: 2;
  grid-row: 1 / -1;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-variant-numeric: tabular-nums;
}
.who {
  font-weight: 700;
}
.gauge {
  width: 6rem;
  height: 0.5rem;
  border-radius: 3px;
  background: rgba(0, 0, 0, 0.3);
  overflow: hidden;
}
.gauge i {
  display: block;
  height: 100%;
  background: var(--hp);
}
.idle {
  grid-column: 1 / -1;
  font-style: italic;
  opacity: 0.75;
}
</style>
