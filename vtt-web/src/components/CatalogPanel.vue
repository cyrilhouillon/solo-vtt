<script setup lang="ts">
import { computed, onBeforeUnmount } from 'vue'
import type { CatalogEntry, CreatureKind } from '../types'

const props = defineProps<{ entries: CatalogEntry[]; armed: string | null; disabled: boolean }>()
const emit = defineEmits<{
  arm: [entryId: string | null]
  /** Une créature commence à être glissée. */
  dragStart: [entryId: string]
  /** Le pointeur (souris ou doigt) est en (clientX, clientY), dans la fenêtre. */
  dragMove: [entryId: string, clientX: number, clientY: number]
  /** Relâchée en (clientX, clientY) ; la carte dit si c'est sur elle. */
  drop: [entryId: string, clientX: number, clientY: number]
  dragCancel: []
}>()

const groups = computed(() =>
  (
    [
      ['CHARACTER', 'Personnages'],
      ['MONSTER', 'Monstres'],
    ] as [CreatureKind, string][]
  ).map(([kind, title]) => ({ kind, title, entries: props.entries.filter((e) => e.kind === kind) })),
)

function toggle(id: string) {
  // Le clic qui suit un glisser n'arme pas.
  if (justDragged) {
    justDragged = false
    return
  }
  emit('arm', props.armed === id ? null : id)
}

/** Distance en pixels au-delà de laquelle un appui devient un glisser. */
const DRAG_THRESHOLD = 6
/** Bande du bord de la fenêtre où le glisser fait défiler la page, et vitesse maximale (pixels par seconde). */
const EDGE = 56
const MAX_SCROLL = 1200

interface Drag {
  entryId: string
  pointerId: number
  element: HTMLElement
  startX: number
  startY: number
  x: number
  y: number
  active: boolean
}
let drag: Drag | null = null
let justDragged = false
let frame = 0
/** Instant de l'image précédente : la vitesse ne dépend pas de la cadence de l'écran. */
let lastFrame = 0

/**
 * Le glisser est en événements pointeur, souris comme doigt : le pointeur reste capturé par l'entrée,
 * et la carte vise depuis les coordonnées de la fenêtre.
 */
function onPointerDown(e: PointerEvent, entryId: string) {
  if (props.disabled || drag || !e.isPrimary || e.button !== 0) return
  const element = e.currentTarget as HTMLElement
  element.setPointerCapture(e.pointerId)
  drag = { entryId, pointerId: e.pointerId, element, startX: e.clientX, startY: e.clientY, x: e.clientX, y: e.clientY, active: false }
}

function onPointerMove(e: PointerEvent) {
  if (!drag || e.pointerId !== drag.pointerId) return
  drag.x = e.clientX
  drag.y = e.clientY
  if (!drag.active) {
    if (Math.hypot(drag.x - drag.startX, drag.y - drag.startY) < DRAG_THRESHOLD) return
    drag.active = true
    document.body.classList.add('dragging-creature')
    window.addEventListener('keydown', onEscape, true)
    emit('dragStart', drag.entryId)
    lastFrame = performance.now()
    frame = requestAnimationFrame(autoScroll)
  }
  emit('dragMove', drag.entryId, drag.x, drag.y)
}

function onPointerUp(e: PointerEvent) {
  if (!drag || e.pointerId !== drag.pointerId) return
  const { active, entryId, x, y } = drag
  finish()
  if (!active) return
  justDragged = true
  // Un toucher n'émet pas toujours de clic après un glisser : le drapeau ne doit pas avaler le clic suivant.
  setTimeout(() => (justDragged = false))
  emit('drop', entryId, x, y)
}

/** Le navigateur reprend le pointeur (geste système, perte de capture) : le glisser s'annule. */
function onPointerLost(e: PointerEvent) {
  if (!drag || e.pointerId !== drag.pointerId) return
  cancelDrag()
}

/** Échap annule le glisser, et ce seul glisser : l'écouteur de la page ne voit pas la touche. */
function onEscape(e: KeyboardEvent) {
  if (e.key !== 'Escape') return
  e.preventDefault()
  e.stopImmediatePropagation()
  cancelDrag()
}

/** Annule le glisser en cours, s'il y en a un ; le relâcher qui suit ne pose rien. */
function cancelDrag() {
  const active = drag?.active
  finish()
  if (active) emit('dragCancel')
}

function finish() {
  if (!drag) return
  const { element, pointerId } = drag
  drag = null
  if (element.hasPointerCapture(pointerId)) element.releasePointerCapture(pointerId)
  cancelAnimationFrame(frame)
  document.body.classList.remove('dragging-creature')
  window.removeEventListener('keydown', onEscape, true)
}

/**
 * Près du haut ou du bas de la fenêtre, la page défile, plus vite vers le bord : la capture et `touch-action: none`
 * coupent le défilement natif. La carte bouge sous un pointeur immobile : la position est réémise.
 */
function autoScroll(now: number) {
  if (!drag?.active) return
  // Plafonné : après un onglet en arrière-plan, la page ne saute pas d'un coup.
  const elapsed = Math.min(0.1, (now - lastFrame) / 1000)
  lastFrame = now
  const height = window.innerHeight
  const depth = drag.y < EDGE ? drag.y - EDGE : drag.y > height - EDGE ? drag.y - (height - EDGE) : 0
  if (depth !== 0) {
    const before = window.scrollY
    window.scrollBy(0, Math.max(-1, Math.min(1, depth / EDGE)) * MAX_SCROLL * elapsed)
    if (window.scrollY !== before) emit('dragMove', drag.entryId, drag.x, drag.y)
  }
  frame = requestAnimationFrame(autoScroll)
}

onBeforeUnmount(cancelDrag)

defineExpose({ cancelDrag })
</script>

<template>
  <section class="catalog" aria-labelledby="catalog-title">
    <h2 id="catalog-title" class="panel-title">Catalogue SRD</h2>
    <hr class="tapered" />
    <p class="hint">
      {{ disabled ? 'Créer une carte pour poser des créatures.' : 'Choisir une créature, puis cliquer sur une case de la carte.' }}
    </p>
    <div v-for="group in groups" :key="group.kind" class="group">
      <h3>{{ group.title }}</h3>
      <ul>
        <li v-for="entry in group.entries" :key="entry.id">
          <button
            type="button"
            class="entry"
            :class="[group.kind.toLowerCase(), { armed: armed === entry.id }]"
            :aria-pressed="armed === entry.id"
            :disabled="disabled"
            @click="toggle(entry.id)"
            @pointerdown="onPointerDown($event, entry.id)"
            @pointermove="onPointerMove"
            @pointerup="onPointerUp"
            @pointercancel="onPointerLost"
            @lostpointercapture="onPointerLost"
          >
            <span class="swatch" aria-hidden="true" />
            {{ entry.name }}
          </button>
        </li>
      </ul>
    </div>
    <p class="attribution">Créatures tirées du <span class="nowrap">SRD 5.2.1 (CC-BY-4.0)</span>.</p>
  </section>
</template>

<style scoped>
.catalog {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  min-height: 100%;
}
.catalog .tapered {
  margin: -0.5rem 0 0;
}
h3 {
  font-family: var(--font-title);
  font-weight: 500;
  font-size: 0.95rem;
  letter-spacing: 0.05em;
  color: var(--ink-muted);
  margin: 0 0 0.3rem;
}
ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.hint,
.attribution {
  margin: 0;
  font-size: 0.9rem;
  color: var(--ink-muted);
  text-wrap: pretty;
}
.attribution {
  margin-top: auto;
}
.nowrap {
  white-space: nowrap;
}
.entry {
  width: 100%;
  /* Au doigt, le geste glisse la créature au lieu de faire défiler. */
  touch-action: none;
  cursor: grab;
  display: flex;
  align-items: center;
  gap: 0.6rem;
  text-align: left;
  background: transparent;
  border-color: transparent;
}
.entry:hover:not(:disabled) {
  background: var(--wood-raised);
  border-color: transparent;
}
/* Créature armée : l'état vivant, en laiton. */
.entry:disabled {
  cursor: default;
}
.entry.armed,
.entry.armed:hover:not(:disabled) {
  background: var(--wood-raised);
  border-color: var(--brass);
  font-weight: 700;
}
.swatch {
  width: 0.8rem;
  height: 0.8rem;
  flex: none;
  border: 1.5px solid var(--token-rim);
}
/* Même forme que sur la carte : disque pour un personnage, carré pour un monstre. */
.character .swatch {
  border-radius: 50%;
  background: var(--token-character);
}
.monster .swatch {
  border-radius: 2px;
  background: var(--token-monster);
}
/* Pendant le glisser, la main fermée partout : le pointeur est capturé par l'entrée. */
:global(body.dragging-creature),
:global(body.dragging-creature *) {
  cursor: grabbing !important;
}
</style>
