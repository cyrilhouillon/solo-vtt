<script setup lang="ts">
import { computed } from 'vue'
import type { AttackRoll, RolledDice, TrayResult } from '../types'

/**
 * La piste à dés, sous la carte : la dernière attaque tranchée par le moteur. Les tours de combat ne l'effacent pas
 * (la barre dit qui joue). Ses dés y sont posés, valeurs du serveur, une fois que les dés 3D se sont arrêtés sur la
 * carte ; le verdict se pose alors. Muette pour les lecteurs d'écran : l'issue leur est annoncée à part.
 *
 * `rolling` : les dés 3D roulent sur la carte ; verdict, dés posés et cible attendent leur arrêt, à leur place.
 */
const props = defineProps<{ result: TrayResult | null; rolling: boolean }>()

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

/**
 * « 10 + 5 = 15 » : chaque dé, puis ce qui s'y ajoute. Le serveur ne détaille pas les modificateurs : leur somme est le
 * total moins les dés, si bien que le calcul tombe juste sans connaître les règles. Sans modificateur ni second dé,
 * le total seul.
 */
function sum(dice: number[], total: number): string {
  const extra = total - dice.reduce((a, b) => a + b, 0)
  const terms = dice.map(String).join(' + ')
  if (extra === 0) return dice.length > 1 ? `${terms} = ${total}` : String(total)
  return `${terms} ${extra > 0 ? '+' : '−'} ${Math.abs(extra)} = ${total}`
}

/** Les dés qui comptent : le d20 gardé (pas celui écarté), puis les dés bonus ou de dégâts. */
function counted(rolls: RolledDice[]): number[] {
  return rolls.flatMap((r) => (r.kept === null ? r.values : [r.kept]))
}

/**
 * Le calcul, à vérifier contre les dés posés : « 10 + 5 = 15 contre CA 15 », la réaction qui l'a changé, puis
 * « dégâts 5 + 5 = 10 ». Relue d'un journal sans dés, l'attaque garde son seul total.
 */
const line = computed(() => {
  const r = props.result
  if (!r) return ''
  const attack = r.attackDice.length ? sum(counted(r.attackDice), r.roll.total) : String(r.roll.total)
  const parts = [`${attack} contre CA ${r.roll.armorClass}`, ...r.reactions]
  const dealt = r.damage.reduce((total, d) => total + d.amount, 0)
  if (r.damageDice.length && r.damage.length) parts.push(`dégâts ${sum(counted(r.damageDice), dealt)}`)
  return parts.join(' · ')
})

/** Un dé posé sur la piste ; `dropped` : le d20 écarté par l'avantage ou le désavantage. */
interface Die {
  sides: number
  value: number
  dropped: boolean
}

function diceOf(rolls: RolledDice[]): Die[] {
  return rolls.flatMap((r) => {
    // Le d20 gardé est le premier qui porte la valeur gardée ; les autres sont écartés.
    const keptAt = r.kept === null ? -1 : r.values.indexOf(r.kept)
    return r.values.map((value, i) => ({ sides: r.sides, value, dropped: r.kept !== null && i !== keptAt }))
  })
}

const attackDice = computed(() => diceOf(props.result?.attackDice ?? []))
const damageDice = computed(() => diceOf(props.result?.damageDice ?? []))

</script>

<template>
  <div class="rim" aria-hidden="true">
    <div class="tray">
      <template v-if="result">
        <p :key="`${result.action}-${rolling}`" class="verdict" :class="[result.roll.outcome.toLowerCase(), { waiting: rolling }]">
          {{ headline }}
        </p>
        <p class="line" :class="{ waiting: rolling }">{{ line }}</p>
        <p class="title">{{ result.title }}</p>
        <div class="dice" :class="{ waiting: rolling }">
          <span v-for="(die, i) in attackDice" :key="`a${result.action}-${i}`" class="die" :class="{ dropped: die.dropped }">
            <b>{{ die.value }}</b><small>d{{ die.sides }}</small>
          </span>
          <i v-if="damageDice.length" class="split" />
          <span v-for="(die, i) in damageDice" :key="`d${result.action}-${i}`" class="die">
            <b>{{ die.value }}</b><small>d{{ die.sides }}</small>
          </span>
        </div>
        <p v-if="result.target" class="target" :class="{ waiting: rolling }">
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
  grid-template-columns: minmax(0, 1fr) auto auto;
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
/* Le calcul, la réaction et les dégâts : ce qui explique le verdict, lu juste après lui. */
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
/* Le verdict et le calcul attendent l'arrêt des dés, à leur place : la piste ne bouge pas. */
.waiting {
  visibility: hidden;
}
/* Les dés : le jet d'attaque, un filet, puis les dégâts. Ils se coupent plutôt que de pousser la piste. */
.dice {
  grid-column: 2;
  grid-row: 1 / -1;
  display: flex;
  align-items: center;
  gap: 0.4rem;
  min-width: 0;
  overflow: hidden;
}
.die {
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 2.4rem;
  height: 2.6rem;
  border-radius: 6px;
  background: var(--felt);
  box-shadow:
    inset 0 0 0 1px var(--felt-rim),
    0 2px 0 rgba(0, 0, 0, 0.35);
  font-variant-numeric: tabular-nums;
  line-height: 1;
}
.die b {
  font-family: var(--font-title);
  font-size: 1.25rem;
  font-weight: 700;
}
.die small {
  margin-top: 0.15rem;
  font-size: 0.7rem;
  opacity: 0.75;
}
/* Le d20 écarté par l'avantage ou le désavantage : atténué et barré, la forme le dit autant que la teinte. */
.die.dropped {
  opacity: 0.45;
}
.die.dropped b {
  text-decoration: line-through;
  text-decoration-thickness: 2px;
}
.split {
  flex: none;
  align-self: stretch;
  width: 1px;
  margin: 0.4rem 0.2rem;
  background: var(--felt-rim);
}
.target {
  grid-column: 3;
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
