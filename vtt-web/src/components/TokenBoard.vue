<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { KIND_LABELS, type TokenView } from '../types'
import AppIcon from './AppIcon.vue'

/**
 * Les créatures posées, en tableau comme un panneau des départs : état lisible d'un coup d'œil.
 * C'est le double accessible de la carte, et le seul moyen de les retirer au clavier.
 * `aiming` : un ciblage est en cours ; choisir une ligne vise la créature.
 * `shown` et `target` : créatures des onglets Feuille et Cible.
 * `ranges` : en ciblage, ce que la distance permet pour chaque créature (indicatif).
 */
const props = defineProps<{
  tokens: TokenView[]
  selected: string[]
  shown: string | null
  target: string | null
  aiming: boolean
  ranges: Record<string, string>
}>()
const emit = defineEmits<{ select: [tokenId: string]; remove: [tokenId: string] }>()

const title = ref<HTMLHeadingElement | null>(null)
const entries = ref<HTMLButtonElement[]>([])
/** Rang de la ligne retirée : le focus y reste, sur la créature suivante, une fois le retrait fait. */
let focusAfterRemoval: number | null = null

function remove(tokenId: string, index: number) {
  focusAfterRemoval = index
  emit('remove', tokenId)
}

watch(
  () => props.tokens.length,
  async (length, before) => {
    if (focusAfterRemoval === null || length >= before) return
    const index = Math.min(focusAfterRemoval, length - 1)
    focusAfterRemoval = null
    await nextTick()
    ;(index >= 0 ? entries.value[index] : title.value)?.focus()
  },
)

/** En ciblage, le focus vient sur le tableau : c'est là que le clavier choisit la cible. */
function focusTitle() {
  title.value?.focus()
}
defineExpose({ focusTitle })

/** En ciblage, la ligne vise : son nom dit ce que la distance permet, et elle n'est plus un interrupteur. */
function entryLabel(token: TokenView): string | undefined {
  if (!props.aiming) return undefined
  const range = props.ranges[token.id]
  return range ? `Viser ${token.name}, ${range}` : `Viser ${token.name}`
}

function isDown(token: TokenView): boolean {
  return token.hitPoints === 0
}

/** Part des PV restants, pour la jauge. */
function health(token: TokenView): number {
  if (token.hitPoints === undefined || !token.maxHitPoints) return 0
  return Math.max(0, Math.min(1, token.hitPoints / token.maxHitPoints))
}
</script>

<template>
  <section class="board" aria-labelledby="board-title">

    <p v-if="!tokens.length" class="empty">Aucune créature posée : choisir une créature du catalogue, puis une case.</p>
    <div v-else class="scroll">
      <table aria-labelledby="board-title">
        <thead>
          <tr>
            <th scope="col"><h2 id="board-title" ref="title" tabindex="-1" class="board-title">Sur la carte</h2></th>
            <th scope="col">Camp</th>
            <th scope="col" class="num">PV</th>
            <th scope="col" class="num">CA</th>
            <th scope="col" class="state-col">État</th>
            <th scope="col"><span class="sr-only">Retirer</span></th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(token, index) in tokens"
            :key="token.id"
            :class="[token.kind.toLowerCase(), { selected: selected.includes(token.id), down: isDown(token) }]"
          >
            <td class="who">
              <button
                ref="entries"
                type="button"
                class="entry"
                :aria-pressed="aiming ? undefined : selected.includes(token.id)"
                :aria-label="entryLabel(token)"
                :disabled="token.pending"
                @click="emit('select', token.id)"
              >
                <span class="ribbon" aria-hidden="true" />
                <span class="name">{{ token.name }}</span>
              </button>
            </td>
            <td class="side">{{ KIND_LABELS[token.kind] }}</td>
            <td class="num figure">
              <span v-if="token.hitPoints !== undefined" class="hp">
                <span class="gauge" aria-hidden="true"><i :style="{ width: `${health(token) * 100}%` }" :class="{ low: health(token) <= 0.5 }" /></span>
                {{ token.hitPoints }} / {{ token.maxHitPoints }}
              </span>
              <template v-else>…</template>
            </td>
            <td class="num figure">{{ token.armorClass ?? '…' }}</td>
            <td class="state-col">
              <span class="state">
              <span v-if="token.pending" class="tag">Pose en cours…</span>
              <span v-else-if="isDown(token)" class="tag down-tag">À terre</span>
              <span v-if="token.id === shown" class="tag">Feuille</span>
              <span v-if="token.id === target" class="tag">Cible</span>
              <span v-if="aiming && ranges[token.id]" class="tag">{{ ranges[token.id] }}</span>
              </span>
            </td>
            <td class="act">
              <button
                type="button"
                class="remove"
                :disabled="token.pending"
                :aria-label="`Retirer ${token.name}`"
                title="Retirer de la carte"
                @click="remove(token.id, index)"
              >
                <AppIcon name="remove" />
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.board {
  background: var(--wood);
  border-bottom: 2px solid var(--felt-rim);
}
.empty {
  margin: 0;
  padding: 0.75rem 1rem;
  color: var(--ink-muted);
}
/* Cinq lignes visibles ; au-delà, le tableau défile sans pousser la carte. */
.scroll {
  max-height: 14rem;
  overflow-y: auto;
}
table {
  width: 100%;
  border-collapse: collapse;
  font-variant-numeric: tabular-nums;
}
th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: var(--wood);
  text-align: left;
  font-family: var(--font-title);
  font-weight: 500;
  font-size: 0.9rem;
  letter-spacing: 0.06em;
  color: var(--ink-muted);
  padding: 0.35rem 0.75rem 0.25rem;
  border-bottom: 1px solid var(--rule);
}
td {
  padding: 0 0.75rem;
  white-space: nowrap;
}
/* Lattes alternées, comme les rangées d'un tableau d'affichage. */
tbody tr:nth-child(even) td {
  background: var(--wood-sunken);
}
.figure {
  font-size: 1.1rem;
  font-weight: 700;
}
.board-title {
  margin: 0;
  font: inherit;
  font-weight: 700;
  color: var(--ink);
}
.num {
  text-align: right;
}
th.num {
  text-align: right;
}
tbody tr.selected td {
  background: var(--wood-raised);
}
tr.selected .name {
  font-weight: 700;
}
.who {
  width: 40%;
  padding-left: 0.4rem;
}
.entry {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  text-align: left;
  border-color: transparent;
  background: transparent;
  padding: 0.2rem 0.4rem;
}
.entry:hover:not(:disabled) {
  background: var(--wood-raised);
  border-color: transparent;
}
.name {
  overflow: hidden;
  text-overflow: ellipsis;
}
/* Ruban de camp : forme et couleur du jeton ; sélectionné, il se cercle de laiton comme le jeton. */
.ribbon {
  width: 0.8rem;
  height: 0.8rem;
  flex: none;
  border: 1.5px solid var(--token-rim);
}
tr.selected .ribbon {
  outline: 2px solid var(--brass);
  outline-offset: 1px;
}
.character .ribbon {
  border-radius: 50%;
  background: var(--token-character);
}
.monster .ribbon {
  border-radius: 2px;
  background: var(--token-monster);
}
.side {
  color: var(--ink-muted);
}
.hp {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
}
.gauge {
  width: 4.5rem;
  height: 0.4rem;
  border-radius: 2px;
  background: var(--wood-sunken);
  overflow: hidden;
}
.gauge i {
  display: block;
  height: 100%;
  background: var(--hp);
}
.gauge i.low {
  background: var(--hp-low);
}
.state {
  display: inline-flex;
  gap: 0.4rem;
}
.tag {
  font-family: var(--font-title);
  font-size: 0.85rem;
  letter-spacing: 0.04em;
  color: var(--ink-muted);
  padding: 0 0.4rem;
  border: 1px solid var(--rule);
  border-radius: 3px;
}
.down-tag {
  color: var(--rubric);
  border-color: var(--rubric);
}
/* À terre : imprimée en gris, ruban retiré, nom barré. */
tr.down .ribbon {
  background: var(--map-down);
}
tr.down .name {
  text-decoration: line-through;
  color: var(--ink-muted);
}
.act {
  width: 1%;
  text-align: right;
}
.remove {
  padding: 0.25rem;
  border-color: transparent;
  background: transparent;
  color: var(--ink-muted);
}
.remove:hover:not(:disabled) {
  color: var(--danger);
  border-color: var(--danger);
  background: transparent;
}
h2:focus {
  outline: none;
}
/* Sur un écran étroit, la forme du ruban dit déjà le camp. */
@media (max-width: 600px) {
  th:nth-child(2),
  td:nth-child(2),
  .state-col,
  .gauge {
    display: none;
  }
  td,
  th {
    padding-inline: 0.4rem;
  }
}
</style>
