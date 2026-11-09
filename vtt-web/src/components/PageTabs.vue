<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'
import type { PageSummary } from '../types'

/**
 * La barre des pages ouvertes, dans l'ordre. Le titre de chaque onglet vient de `pages`, que le Carnet relit à chaque
 * geste : un renommage s'y voit aussitôt.
 */
const props = defineProps<{
  tabIds: readonly string[]
  activeId: string | null
  pages: readonly PageSummary[]
  busy: boolean
}>()
const emit = defineEmits<{ activate: [pageId: string]; close: [pageId: string]; reorder: [tabIds: string[]] }>()

/** L'ordre montré pendant un glisser, avant d'être envoyé ; `null` hors glisser. */
const preview = ref<string[] | null>(null)

const tabs = computed(() => {
  const titles = new Map(props.pages.map((p) => [p.id, p.title]))
  return (preview.value ?? props.tabIds).map((id) => ({ id, title: titles.get(id) ?? 'Page introuvable' }))
})

const bar = ref<HTMLElement | null>(null)

/** Vrai quand le focus est dans la barre : fermer un onglet au clavier le rend ensuite à l'onglet actif. */
function hasFocus() {
  return !!bar.value?.contains(document.activeElement)
}

/** Donne le focus à l'onglet actif, sinon au dernier. */
function focusActive() {
  const tabs = bar.value?.querySelectorAll<HTMLButtonElement>('.tab')
  if (!tabs?.length) return
  const index = props.activeId ? props.tabIds.indexOf(props.activeId) : -1
  tabs[index >= 0 ? index : tabs.length - 1]?.focus()
}

/** La ligne des signets, qui défile quand ils ne tiennent pas tous ; des flèches le disent et y mènent. */
const strip = ref<HTMLElement | null>(null)
const canScrollLeft = ref(false)
const canScrollRight = ref(false)

function measure() {
  const el = strip.value
  if (!el) return
  canScrollLeft.value = el.scrollLeft > 1
  canScrollRight.value = el.scrollLeft + el.clientWidth < el.scrollWidth - 1
}

/** Défile d'environ deux signets. */
function scrollBy(direction: -1 | 1) {
  strip.value?.scrollBy({ left: direction * strip.value.clientWidth * 0.6, behavior: 'smooth' })
}

/** La molette, verticale sur une souris ordinaire, fait défiler les signets à l'horizontale. */
function onWheel(e: WheelEvent) {
  const el = strip.value
  if (!el || el.scrollWidth <= el.clientWidth || Math.abs(e.deltaX) > Math.abs(e.deltaY)) return
  e.preventDefault()
  el.scrollLeft += e.deltaY
}

/** Le signet de la page lue reste en vue : ouvrir une page loin dans la ligne y mène. */
async function revealActive() {
  await nextTick()
  strip.value?.querySelector('li.active')?.scrollIntoView({ block: 'nearest', inline: 'nearest' })
  measure()
}

let resize: ResizeObserver | undefined
onMounted(() => {
  resize = new ResizeObserver(measure)
  if (strip.value) {
    resize.observe(strip.value)
    if (strip.value.firstElementChild) resize.observe(strip.value.firstElementChild)
  }
  void revealActive()
})
onBeforeUnmount(() => resize?.disconnect())
// Les valeurs elles-mêmes, pas un tableau neuf à chaque lecture : réordonner ne ramène pas la ligne au signet lu.
watch([() => props.activeId, () => props.tabIds.length], revealActive)

/*
 * Réordonner : à la souris ou au stylet, un signet tenu puis tiré de plus de 6px se glisse ; les autres s'écartent à
 * mesure qu'il passe leur milieu, et la ligne défile d'elle-même près de ses bords, plus vite tout contre. Relâcher
 * envoie le nouvel ordre ; Échap, ou une interruption, rend l'ancien. Au toucher, rien ne se glisse : le doigt fait
 * défiler. Au clavier, Ctrl+Maj+← et → déplacent le signet qui a le focus.
 */
const DRAG_THRESHOLD = 6
/** Distance du bord où la ligne commence à défiler, et vitesse maximale, en pixels par image. */
const EDGE = 56
const MAX_SPEED = 14

let press: { id: string; x: number; pointerId: number } | null = null
const dragging = ref<string | null>(null)
let pointerX = 0
let frame = 0
/** Le clic qui suit le relâcher d'un glisser n'ouvre pas la page. */
let swallowClick = false

function onPointerDown(e: PointerEvent, id: string) {
  if (e.button !== 0 || e.pointerType === 'touch' || props.busy) return
  press = { id, x: e.clientX, pointerId: e.pointerId }
}

function onPointerMove(e: PointerEvent) {
  if (!press || e.pointerId !== press.pointerId) return
  pointerX = e.clientX
  if (!dragging.value) {
    if (Math.abs(e.clientX - press.x) < DRAG_THRESHOLD) return
    dragging.value = press.id
    preview.value = [...props.tabIds]
    try {
      strip.value?.setPointerCapture(e.pointerId)
    } catch {
      // Pointeur déjà relâché : le relâcher qui suit finit le glisser.
    }
    document.body.classList.add('dragging-tab')
    window.addEventListener('keydown', onDragKey, true)
    frame = requestAnimationFrame(autoScroll)
  }
  place()
}

/** Le signet glissé prend la place que désigne le pointeur : après chaque autre signet dont il a passé le milieu. */
function place() {
  const id = dragging.value
  const order = preview.value
  if (!id || !order || !strip.value) return
  const others = [...strip.value.querySelectorAll<HTMLElement>('li[data-tab-id]')].filter((li) => li.dataset.tabId !== id)
  const index = others.filter((li) => {
    const r = li.getBoundingClientRect()
    return r.left + r.width / 2 < pointerX
  }).length
  const next = order.filter((t) => t !== id)
  next.splice(index, 0, id)
  if (next.some((t, i) => t !== order[i])) preview.value = next
}

function autoScroll() {
  const el = strip.value
  if (!dragging.value || !el) return
  const r = el.getBoundingClientRect()
  const left = pointerX - r.left
  const right = r.right - pointerX
  let speed = 0
  if (left < EDGE) speed = -MAX_SPEED * Math.min(1, (EDGE - left) / EDGE)
  else if (right < EDGE) speed = MAX_SPEED * Math.min(1, (EDGE - right) / EDGE)
  if (speed) {
    const before = el.scrollLeft
    el.scrollLeft += speed
    if (el.scrollLeft !== before) place()
  }
  frame = requestAnimationFrame(autoScroll)
}

function endDrag(commit: boolean) {
  cancelAnimationFrame(frame)
  window.removeEventListener('keydown', onDragKey, true)
  document.body.classList.remove('dragging-tab')
  const order = preview.value
  const wasDragging = !!dragging.value
  dragging.value = null
  press = null
  preview.value = null
  if (wasDragging) swallowClick = true
  if (commit && order && order.some((t, i) => t !== props.tabIds[i])) emit('reorder', order)
}

function onPointerUp(e: PointerEvent) {
  if (press && e.pointerId === press.pointerId) endDrag(true)
}

/** Une capture perdue sans relâcher (fenêtre quittée…) rend l'ancien ordre. */
function onLostCapture() {
  if (dragging.value) endDrag(false)
}

function onDragKey(e: KeyboardEvent) {
  if (e.key !== 'Escape') return
  e.preventDefault()
  e.stopPropagation()
  endDrag(false)
}

function onTabClick(id: string) {
  if (swallowClick) {
    swallowClick = false
    return
  }
  emit('activate', id)
}

/** Ctrl+Maj+← / → : le signet qui a le focus change de place, et le garde. */
async function onTabKey(e: KeyboardEvent, id: string) {
  if (!e.ctrlKey || !e.shiftKey || (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') || props.busy) return
  e.preventDefault()
  const from = props.tabIds.indexOf(id)
  const to = from + (e.key === 'ArrowLeft' ? -1 : 1)
  if (from < 0 || to < 0 || to >= props.tabIds.length) return
  const order = [...props.tabIds]
  order.splice(from, 1)
  order.splice(to, 0, id)
  emit('reorder', order)
  await nextTick()
  strip.value?.querySelector<HTMLButtonElement>(`li[data-tab-id="${id}"] .tab`)?.focus()
}

onBeforeUnmount(() => {
  if (dragging.value) endDrag(false)
})

defineExpose({ hasFocus, focusActive })
</script>

<template>
  <nav ref="bar" class="page-tabs" aria-label="Pages ouvertes">
    <button
      v-if="canScrollLeft"
      type="button"
      class="scroll left"
      tabindex="-1"
      aria-hidden="true"
      title="Signets précédents"
      @click="scrollBy(-1)"
    >
      <AppIcon name="chevron-left" />
    </button>
    <!-- La ligne tient le pointeur pendant un glisser : les signets, eux, changent de place dans le DOM. -->
    <ul
      ref="strip"
      @scroll.passive="measure"
      @wheel="onWheel"
      @pointermove="onPointerMove"
      @pointerup="onPointerUp"
      @pointercancel="endDrag(false)"
      @lostpointercapture="onLostCapture"
    >
      <li
        v-for="tab in tabs"
        :key="tab.id"
        :data-tab-id="tab.id"
        :class="{ active: tab.id === activeId, dragged: tab.id === dragging }"
      >
        <button
          type="button"
          class="tab"
          :title="tab.title"
          :aria-current="tab.id === activeId ? 'page' : undefined"
          aria-keyshortcuts="Control+Shift+ArrowLeft Control+Shift+ArrowRight"
          @pointerdown="onPointerDown($event, tab.id)"
          @click="onTabClick(tab.id)"
          @keydown="onTabKey($event, tab.id)"
        >
          {{ tab.title }}
        </button>
        <button
          type="button"
          class="close"
          :aria-label="`Fermer « ${tab.title} »`"
          :title="`Fermer « ${tab.title} »`"
          :disabled="busy"
          @click="emit('close', tab.id)"
        >
          <AppIcon name="remove" />
        </button>
      </li>
    </ul>
    <button
      v-if="canScrollRight"
      type="button"
      class="scroll right"
      tabindex="-1"
      aria-hidden="true"
      title="Signets suivants"
      @click="scrollBy(1)"
    >
      <AppIcon name="chevron-right" />
    </button>
  </nav>
</template>

<style scoped>
/*
 * Les pages ouvertes sont des signets qui pendent du haut de la feuille : le signet de la page lue, en ruban, descend
 * plus bas que les autres. Au-delà de la largeur de la feuille, ils défilent sur leur ligne sans faire défiler l'écran.
 */
.page-tabs {
  position: relative;
}
ul {
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
  display: flex;
  align-items: flex-start;
  gap: 0.4rem;
  margin: 0;
  padding: 0;
  list-style: none;
}
/* Les signets se resserrent avant de défiler : une dizaine tient sur la feuille. */
li {
  flex: 0 1 auto;
  position: relative;
  min-width: 7rem;
  display: flex;
  align-items: flex-end;
  max-width: 13rem;
  min-height: 2.5rem;
  padding: 0.6rem 0.2rem 0.7rem 0.15rem;
  background: var(--wood-sunken);
  color: var(--ink-muted);
  /* La queue fendue d'un signet. */
  clip-path: polygon(0 0, 100% 0, 100% 100%, 50% calc(100% - 0.45rem), 0 100%);
}
li:hover {
  background: var(--wood-raised);
  color: var(--ink);
}
li.active {
  flex-shrink: 0;
  min-height: 3.25rem;
  background: var(--ribbon);
  color: var(--on-ribbon);
}
.tab {
  min-width: 0;
  padding: 0.1rem 0.15rem 0.1rem 0.6rem;
  border: 0;
  background: transparent;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 0.95rem;
  letter-spacing: 0.04em;
  color: inherit;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tab:hover:not(:disabled) {
  background: transparent;
}
/* La croix se pose sur le bout du titre, sur le fond du signet : le titre garde toute la largeur. */
.close {
  position: absolute;
  right: 0.2rem;
  bottom: 0.65rem;
  display: grid;
  place-items: center;
  width: 1.6rem;
  height: 1.6rem;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: inherit;
  color: inherit;
  opacity: 0;
  pointer-events: none;
}
/* La croix se montre sur le signet survolé ou lu, et au clavier ; toujours au toucher. */
li:hover .close,
li.active .close,
li:focus-within .close {
  opacity: 1;
  pointer-events: auto;
}
li.active .tab {
  padding-right: 1.7rem;
}
@media (hover: none) {
  .close {
    opacity: 1;
    pointer-events: auto;
  }
  .tab {
    padding-right: 1.7rem;
  }
}
.close .icon {
  width: 0.95rem;
  height: 0.95rem;
}
.close:hover:not(:disabled) {
  background: var(--wood-sunken);
}
li.active .close:hover:not(:disabled) {
  background: var(--ribbon);
  box-shadow: inset 0 0 0 1px var(--on-ribbon);
}
.tab:focus-visible,
.close:focus-visible {
  outline: 2px solid currentColor;
  outline-offset: -2px;
  border-radius: 4px;
  opacity: 1;
}
/* Les flèches posées aux bouts de la ligne quand des signets dépassent ; la feuille s'estompe sous elles. */
.scroll {
  position: absolute;
  z-index: 1;
  top: 0;
  bottom: 0.4rem;
  display: grid;
  place-items: center;
  width: 2.4rem;
  padding: 0;
  border: 0;
  border-radius: 0;
  color: var(--ink);
}
.scroll.left {
  left: -0.6rem;
  justify-items: start;
  background: linear-gradient(90deg, var(--sheet) 55%, transparent);
}
.scroll.right {
  right: -0.6rem;
  justify-items: end;
  background: linear-gradient(270deg, var(--sheet) 55%, transparent);
}
.scroll:hover:not(:disabled) {
  border: 0;
  color: var(--ribbon);
}
.scroll.left:hover:not(:disabled) {
  background: linear-gradient(90deg, var(--sheet) 55%, transparent);
}
.scroll.right:hover:not(:disabled) {
  background: linear-gradient(270deg, var(--sheet) 55%, transparent);
}
.scroll .icon {
  width: 20px;
  height: 20px;
}
/* Le signet glissé se cercle de laiton, comme une page glissée dans l'arbre ; les autres s'écartent pour lui. */
li.dragged {
  outline: 2px solid var(--brass);
  outline-offset: -2px;
}
li.dragged:not(.active) {
  background: var(--wood-raised);
  color: var(--ink);
}
</style>

<style>
/* Pendant le glisser d'un signet, le texte ne se sélectionne pas et le curseur dit qu'on le tient. */
body.dragging-tab {
  user-select: none;
}
body.dragging-tab * {
  cursor: grabbing !important;
}
</style>
