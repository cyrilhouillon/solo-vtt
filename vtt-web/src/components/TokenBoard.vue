<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { RangeVerdict } from '../map/range'
import { KIND_LABELS, type Combat, type TokenView } from '../types'
import AppIcon from './AppIcon.vue'

/**
 * Les créatures posées, en tableau comme un panneau des départs : état lisible d'un coup d'œil.
 * C'est le double accessible de la carte, et le seul moyen de les retirer au clavier.
 * `attacker` : l'attaquant d'un ciblage en cours ; choisir une ligne vise alors la créature.
 * `target` : la créature de l'onglet Cible, visée par la dernière attaque.
 * `ranges` : en ciblage, ce que la distance permet pour chaque créature (indicatif).
 * `combat` : en combat, les lignes suivent l'ordre d'initiative et la créature qui joue est marquée.
 */
const props = defineProps<{
  tokens: TokenView[]
  selected: string[]
  attacker: string | null
  target: string | null
  ranges: Record<string, RangeVerdict>
  combat: Combat | null
}>()

/** Ce que le combat dit d'une créature : son initiative, si elle joue, si elle a joué ce round. */
type TurnInfo = { initiative: number; active: boolean; played: boolean; candidate: boolean }

const turns = computed(() => {
  const info = new Map<string, TurnInfo>()
  const c = props.combat
  if (!c) return info
  for (const group of c.groups) {
    for (const m of group.members) {
      info.set(m.creatureId, {
        initiative: group.initiative,
        active: m.creatureId === c.activeId,
        played: m.played && m.creatureId !== c.activeId,
        candidate: c.activeId === null && c.candidates.includes(m.creatureId),
      })
    }
  }
  return info
})

/** En combat, l'ordre d'initiative (les créatures hors de l'ordre, en attente de pose, à la fin) ; sinon l'ordre de pose. */
const rows = computed(() => {
  const c = props.combat
  if (!c) return props.tokens
  const rank = new Map(c.groups.flatMap((g) => g.members.map((m) => m.creatureId)).map((id, i) => [id, i]))
  return [...props.tokens].sort((a, b) => (rank.get(a.creatureId) ?? Infinity) - (rank.get(b.creatureId) ?? Infinity))
})
const aiming = computed(() => props.attacker !== null)
const emit = defineEmits<{ select: [tokenId: string]; remove: [tokenIds: string[]] }>()

const title = ref<HTMLHeadingElement | null>(null)
/**
 * Le bouton de chaque ligne, par id de jeton. Un tableau de refs de `v-for` ne suit pas l'ordre des lignes
 * (il garde l'ordre de montage) : le tri d'initiative le décalerait.
 */
const entries = new Map<string, HTMLButtonElement>()
function entryRef(tokenId: string) {
  return (el: unknown) => {
    if (el instanceof HTMLButtonElement) entries.set(tokenId, el)
    else entries.delete(tokenId)
  }
}
function focusRow(token: TokenView | undefined) {
  ;(token ? entries.get(token.id) : title.value)?.focus()
}

/** Rang de la ligne retirée : le focus y reste, sur la créature suivante, une fois le retrait fait. */
let focusAfterRemoval: number | null = null

/**
 * En combat, un retrait ne se reprend pas (Ctrl+Z reposerait une créature avec une nouvelle initiative) :
 * il se fait en deux temps. `confirming` : les jetons dont le retrait attend sa confirmation, sur la ligne
 * du premier ; `returnFocus` : où le focus revient si le joueur annule.
 */
const confirming = ref<string[] | null>(null)
let returnFocus: HTMLElement | null = null
/** Ref de fonction : un `ref` nommé dans un `v-for` deviendrait un tableau. */
let confirmButton: HTMLButtonElement | null = null
function confirmRef(el: unknown) {
  confirmButton = el instanceof HTMLButtonElement ? el : null
}
const confirmRow = computed(() => {
  const ids = confirming.value
  return ids ? (rows.value.find((token) => ids.includes(token.id))?.id ?? null) : null
})
const confirmLabel = computed(() => {
  const ids = confirming.value ?? []
  return props.tokens.filter((t) => ids.includes(t.id)).map((t) => t.name).join(' et ')
})

function sameIds(a: string[], b: string[]): boolean {
  return a.length === b.length && a.every((id) => b.includes(id))
}

/**
 * Retirer : hors combat tout de suite (Ctrl+Z annule) ; en combat, la première demande ouvre la confirmation
 * sur la ligne, la seconde (mêmes créatures) retire.
 */
function requestRemoval(tokenIds: string[]) {
  const ids = tokenIds.filter((id) => props.tokens.some((t) => t.id === id && !t.pending))
  if (!ids.length) return
  if (!props.combat || (confirming.value && sameIds(confirming.value, ids))) {
    confirmRemoval(ids)
    return
  }
  returnFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null
  confirming.value = ids
  void nextTick(() => confirmButton?.focus())
}

function confirmRemoval(ids = confirming.value) {
  if (!ids) return
  focusAfterRemoval = rows.value.findIndex((token) => ids.includes(token.id))
  confirming.value = null
  returnFocus = null
  emit('remove', ids)
}

/** Annule une confirmation ouverte ; rend vrai s'il y en avait une (Échap s'arrête là). */
function cancelRemoval(): boolean {
  if (!confirming.value) return false
  const row = confirmRow.value
  confirming.value = null
  // Le × qui a ouvert la confirmation a été remplacé par elle : le focus revient au × redessiné de la ligne.
  const back = returnFocus?.isConnected ? returnFocus : null
  returnFocus = null
  void nextTick(() => {
    const rowRemove = row ? entries.get(row)?.closest('tr')?.querySelector<HTMLButtonElement>('.remove') : null
    ;(back ?? rowRemove)?.focus()
  })
  return true
}

// La confirmation tombe si une de ses créatures quitte la carte, si le combat finit ou si un ciblage commence.
watch(
  () => [props.tokens.map((t) => t.id).join(), props.combat === null, props.attacker] as const,
  ([, noCombat, attacker]) => {
    const ids = confirming.value
    if (!ids) return
    if (noCombat || attacker !== null || !ids.every((id) => props.tokens.some((t) => t.id === id))) {
      confirming.value = null
      returnFocus = null
    }
  },
)

watch(
  () => props.tokens.length,
  async (length, before) => {
    if (focusAfterRemoval === null || length >= before) return
    const index = Math.min(focusAfterRemoval, length - 1)
    focusAfterRemoval = null
    await nextTick()
    focusRow(index >= 0 ? rows.value[index] : undefined)
  },
)

/**
 * En ciblage, le focus vient sur le tableau, où le clavier choisit la cible : sur la première créature permise,
 * sinon sur le titre (aucune à portée).
 */
function focusFirstTarget() {
  focusRow(rows.value.find((token) => token.id !== props.attacker && props.ranges[token.id]?.allowed))
}
defineExpose({ focusFirstTarget, requestRemoval, cancelRemoval })

/**
 * En combat hors ciblage, le tableau se resserre en bande d'initiative (la carte garde la place) ;
 * la ligne de la créature qui joue reste en vue.
 */
const compact = computed(() => props.combat !== null && !aiming.value)
watch(
  () => [props.combat?.activeId, compact.value] as const,
  async ([activeId]) => {
    if (!activeId) return
    await nextTick()
    const token = rows.value.find((t) => t.creatureId === activeId)
    if (token) entries.get(token.id)?.closest('tr')?.scrollIntoView({ block: 'nearest' })
  },
)

/** En ciblage, la ligne vise : son nom dit ce que la distance permet, et elle n'est plus un interrupteur. */
function entryLabel(token: TokenView): string | undefined {
  if (!aiming.value) return undefined
  const range = props.ranges[token.id]
  return range ? `Viser ${token.name}, ${range.label}` : `Viser ${token.name}`
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
  <section class="board" :class="{ compact: combat }" aria-labelledby="board-title">

    <p v-if="!tokens.length" class="empty">Aucune créature posée : choisir une créature du catalogue, puis une case.</p>
    <div v-else class="scroll">
      <table aria-labelledby="board-title">
        <thead>
          <tr>
            <th v-if="combat" scope="col" class="num init-col"><abbr title="Initiative">Init.</abbr></th>
            <th scope="col"><h2 id="board-title" ref="title" tabindex="-1" class="board-title">Sur la carte</h2></th>
            <th scope="col" class="side">Camp</th>
            <th scope="col" class="num">PV</th>
            <th scope="col" class="num">CA</th>
            <th scope="col" class="state-col">État</th>
            <th scope="col"><span class="sr-only">Retirer</span></th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="token in rows"
            :key="token.id"
            :class="[token.kind.toLowerCase(), { selected: selected.includes(token.id), down: isDown(token), active: turns.get(token.creatureId)?.active }]"
            :aria-current="turns.get(token.creatureId)?.active ? 'true' : undefined"
          >
            <td v-if="combat" class="num figure init-col">{{ turns.get(token.creatureId)?.initiative ?? '…' }}</td>
            <td class="who">
              <button
                :ref="entryRef(token.id)"
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
                <span class="hp-value">{{ token.hitPoints }} / {{ token.maxHitPoints }}</span>
              </span>
              <template v-else>…</template>
            </td>
            <td class="num figure">{{ token.armorClass ?? '…' }}</td>
            <td class="state-col">
              <span class="state">
              <span v-if="token.pending" class="tag">Pose en cours…</span>
              <span v-else-if="isDown(token)" class="tag down-tag">À terre</span>
              <span v-if="turns.get(token.creatureId)?.active" class="tag turn-tag">À son tour</span>
              <span v-else-if="turns.get(token.creatureId)?.candidate" class="tag">Au choix</span>
              <span v-else-if="turns.get(token.creatureId)?.played" class="tag">Joué</span>
              <span v-if="token.id === attacker" class="tag">Attaquant</span>
              <span v-else-if="!aiming && token.id === target" class="tag">Cible</span>
              <span v-if="ranges[token.id]" class="tag" :class="{ allowed: ranges[token.id].allowed }">{{ ranges[token.id].label }}</span>
              </span>
            </td>
            <td class="act">
              <span
                v-if="confirmRow === token.id"
                role="group"
                class="confirm"
                :aria-label="`Retirer ${confirmLabel} du combat ? Le retrait ne s'annule pas.`"
              >
                <span class="ask" aria-hidden="true">Retirer ?</span>
                <button
                  :ref="confirmRef"
                  type="button"
                  class="danger"
                  :aria-label="`Confirmer le retrait de ${confirmLabel}`"
                  @click="confirmRemoval()"
                >
                  Confirmer
                </button>
                <button type="button" @click="cancelRemoval">Annuler</button>
              </span>
              <button
                v-else
                type="button"
                class="remove"
                :disabled="token.pending"
                :aria-label="`Retirer ${token.name}`"
                :title="combat ? 'Retirer du combat (à confirmer)' : 'Retirer de la carte'"
                @click="requestRemoval([token.id])"
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
/*
 * En combat : trois lignes, la créature qui joue et les suivantes, ciblage compris. La bande garde sa hauteur
 * quand on vise : la carte ne bouge pas sous le pointeur ; les autres cibles défilent dans la bande.
 */
.compact .scroll {
  max-height: 8.25rem;
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
td.side {
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
/* Largeur de « 99 / 99 » : les jauges s'alignent quels que soient les PV. */
.hp-value {
  min-width: 5ch;
  text-align: right;
}
/* Largeur de la plus longue étiquette : les colonnes ne bougent pas quand l'état change. */
.state-col {
  width: 15rem;
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
/* Les verdicts de portée sont écrits pour une phrase ; en étiquette, ils prennent la capitale. */
.tag::first-letter {
  text-transform: uppercase;
}
/* Cible permise : cerclée de laiton, comme la bulle sur la carte. */
.tag.allowed {
  color: var(--ink);
  border-color: var(--brass);
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
/* La créature qui joue : un état vivant, repère et contour d'étiquette en laiton (seul le mode est une puce pleine). */
tbody tr.active td:first-child {
  box-shadow: inset 4px 0 0 var(--brass);
}
.tag.turn-tag {
  color: var(--ink);
  border-color: var(--brass);
  font-weight: 700;
}
.init-col {
  width: 1%;
  padding-right: 0.25rem;
}
abbr {
  text-decoration: none;
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
/* Retrait en combat à confirmer : la question et ses deux réponses prennent la place du ×. */
.confirm {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}
.ask {
  font-weight: 700;
  color: var(--danger);
}
.confirm button {
  padding: 0.1rem 0.5rem;
}
.confirm .danger {
  border-color: var(--danger);
  color: var(--danger);
}
h2:focus {
  outline: none;
}
/*
 * Quand la table se serre entre les panneaux, la forme du ruban dit déjà le camp et le chiffre dit
 * déjà les PV : la colonne État garde la place de son étiquette la plus longue.
 */
@media (max-width: 1280px) {
  th.side,
  td.side {
    display: none;
  }
}
@media (min-width: 901px) and (max-width: 1100px), (max-width: 700px) {
  .gauge {
    display: none;
  }
  td,
  th {
    padding-inline: 0.5rem;
  }
}
@media (max-width: 600px) {
  .state-col {
    display: none;
  }
  td,
  th {
    padding-inline: 0.4rem;
  }
}
</style>
