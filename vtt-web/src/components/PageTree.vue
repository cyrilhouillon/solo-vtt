<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, provide, ref, toRef } from 'vue'
import PageBranch from './PageBranch.vue'
import { treeContext, type MenuPlace, type TreeDrag, type TreeEdit } from '../notebook/context'
import { buildTree, canMove, splitSections } from '../notebook/tree'
import { notebookModules } from '../modules/registry'
import type { PageSummary } from '../types'

/**
 * L'arbre des pages du Carnet. Il ne garde que l'état de l'interface (menu ouvert, champ ouvert) ; les branches
 * dépliées et chaque geste qui touche le serveur reviennent au Carnet.
 */
const props = defineProps<{
  pages: PageSummary[]
  currentId: string | null
  expanded: ReadonlySet<string>
  busy: boolean
  /** L'adresse d'une page, que porte sa ligne. */
  pageHref: (pageId: string) => string
}>()
const emit = defineEmits<{
  open: [pageId: string]
  toggle: [pageId: string]
  /** `done` : la page est créée, le champ de l'arbre peut se fermer. */
  create: [title: string, parentId: string, done: () => void]
  rename: [pageId: string, title: string]
  /** Range la page sous `parentId` (`null` : la racine), relâchée là au bout d'un glisser permis. */
  drop: [pageId: string, parentId: string | null]
  /** Demande le choix d'une destination pour la page. */
  move: [pageId: string]
  /** Demande la suppression de la page, à confirmer. */
  delete: [pageId: string]
}>()

/** Les pages ordinaires, puis chaque section sous le titre de son module (son id si le module manque au client). */
const groups = computed(() => {
  const { ordinary, sections } = splitSections(buildTree(props.pages))
  return {
    ordinary,
    sections: sections.map((section) => ({
      ...section,
      title: notebookModules.find((module) => module.id === section.id)?.title ?? section.id,
    })),
  }
})
const root = ref<HTMLElement | null>(null)
const rootZone = ref<HTMLElement | null>(null)
const menuId = ref<string | null>(null)
const menuPlace = ref<MenuPlace | null>(null)
const edit = ref<TreeEdit | null>(null)
const draft = ref('')

/** Au-delà de ce déplacement (px), le geste est un glisser et non plus un clic. */
const DRAG_THRESHOLD = 6
/** Survol d'une branche repliée avant qu'elle s'ouvre (ms). */
const EXPAND_DELAY = 600
/** Bande du haut et du bas de l'arbre où il défile (px), et vitesse au bord (px/s). */
const EDGE = 32
const MAX_SCROLL = 600

const drag = ref<TreeDrag | null>(null)
/** Le geste en cours, armé dès l'appui, glisser seulement au-delà du seuil. */
let gesture: { pageId: string; pointerId: number; startX: number; startY: number; x: number; y: number } | null = null
const ghost = ref<{ title: string; x: number; y: number } | null>(null)
let expandTimer: ReturnType<typeof setTimeout> | undefined
let expandTarget: string | null = null
let frame = 0
let lastFrame = 0

/**
 * À la souris ou au stylet, l'appui sur le titre d'une ligne arme un glisser. Au doigt, rien : le doigt fait défiler
 * l'arbre, et le menu « Déplacer sous… » reste le chemin du déplacement.
 */
function onTreePointerDown(e: PointerEvent) {
  if (e.pointerType === 'touch' || e.button !== 0 || props.busy || gesture) return
  const link = (e.target as Element | null)?.closest('.page-link')
  const pageId = link?.closest<HTMLElement>('.row[data-page-id]')?.dataset.pageId
  // Une racine de section ne se glisse pas : l'appui reste un clic qui l'ouvre.
  if (!pageId || props.pages.find((p) => p.id === pageId)?.section != null) return
  gesture = { pageId, pointerId: e.pointerId, startX: e.clientX, startY: e.clientY, x: e.clientX, y: e.clientY }
  window.addEventListener('pointermove', onDragMove)
  window.addEventListener('pointerup', onDragEnd)
  window.addEventListener('pointercancel', cancelDrag)
  window.addEventListener('blur', cancelDrag)
  window.addEventListener('keydown', onDragKey, true)
}

function onDragMove(e: PointerEvent) {
  if (!gesture || e.pointerId !== gesture.pointerId) return
  gesture.x = e.clientX
  gesture.y = e.clientY
  if (!drag.value) {
    if (Math.hypot(e.clientX - gesture.startX, e.clientY - gesture.startY) < DRAG_THRESHOLD) return
    menuId.value = null
    drag.value = { pageId: gesture.pageId, target: undefined, allowed: false }
    document.body.classList.add('dragging-page')
    lastFrame = performance.now()
    frame = requestAnimationFrame(autoScroll)
  }
  e.preventDefault()
  aim()
}

/** Vise la ligne (ou la zone Racine) sous le pointeur, et ouvre une branche repliée survolée assez longtemps. */
function aim() {
  if (!gesture || !drag.value) return
  const title = props.pages.find((p) => p.id === gesture?.pageId)?.title ?? ''
  ghost.value = { title, x: gesture.x, y: gesture.y }
  const under = document.elementFromPoint(gesture.x, gesture.y)
  const inTree = under && (root.value?.contains(under) || rootZone.value?.contains(under)) ? under : null
  const rowId = inTree?.closest<HTMLElement>('.row[data-page-id]')?.dataset.pageId
  const target = inTree?.closest('.drop-root') ? null : rowId
  drag.value = {
    pageId: gesture.pageId,
    target,
    allowed: target !== undefined && canMove(props.pages, gesture.pageId, target),
  }
  const collapsed = rowId && !props.expanded.has(rowId) && props.pages.some((p) => p.parentId === rowId) ? rowId : null
  if (collapsed !== expandTarget) {
    clearTimeout(expandTimer)
    expandTarget = collapsed
    if (collapsed) expandTimer = setTimeout(() => drag.value && emit('toggle', collapsed), EXPAND_DELAY)
  }
}

/** Près du haut ou du bas de l'arbre, il défile, plus vite vers le bord ; la cible suit ce qui passe sous le pointeur. */
function autoScroll(now: number) {
  const el = root.value
  if (!gesture || !drag.value || !el) return
  const elapsed = Math.min(0.1, (now - lastFrame) / 1000)
  lastFrame = now
  const { top, bottom } = el.getBoundingClientRect()
  const depth = gesture.y < top + EDGE ? gesture.y - (top + EDGE) : gesture.y > bottom - EDGE ? gesture.y - (bottom - EDGE) : 0
  if (depth !== 0) {
    const before = el.scrollTop
    el.scrollTop += Math.max(-1, Math.min(1, depth / EDGE)) * MAX_SCROLL * elapsed
    if (el.scrollTop !== before) aim()
  }
  frame = requestAnimationFrame(autoScroll)
}

function onDragEnd(e: PointerEvent) {
  if (!gesture || e.pointerId !== gesture.pointerId) return
  const done = drag.value
  if (done) {
    // Le clic qui suit le relâcher ouvrirait la page glissée, ou celle visée.
    window.addEventListener('click', swallowClick, { capture: true, once: true })
    setTimeout(() => window.removeEventListener('click', swallowClick, { capture: true }), 0)
    if (done.allowed && done.target !== undefined) emit('drop', done.pageId, done.target)
  }
  cancelDrag()
}

function swallowClick(e: MouseEvent) {
  e.stopPropagation()
  e.preventDefault()
}

function onDragKey(e: KeyboardEvent) {
  if (e.key !== 'Escape' || !gesture) return
  e.preventDefault()
  e.stopPropagation()
  cancelDrag()
}

/** Échap, perte du pointeur ou de la fenêtre : le glisser s'annule sans rien envoyer. */
function cancelDrag() {
  gesture = null
  drag.value = null
  ghost.value = null
  clearTimeout(expandTimer)
  expandTarget = null
  cancelAnimationFrame(frame)
  document.body.classList.remove('dragging-page')
  window.removeEventListener('pointermove', onDragMove)
  window.removeEventListener('pointerup', onDragEnd)
  window.removeEventListener('pointercancel', cancelDrag)
  window.removeEventListener('blur', cancelDrag)
  window.removeEventListener('keydown', onDragKey, true)
}

/** Rend le focus à un bouton de la ligne, après que l'arbre s'est redessiné. */
async function focusRow(pageId: string, selector: '.page-link' | '.more') {
  await nextTick()
  root.value?.querySelector<HTMLElement>(`.row[data-page-id="${CSS.escape(pageId)}"] ${selector}`)?.focus()
}

function closeMenu(returnFocus = false) {
  const id = menuId.value
  menuId.value = null
  menuPlace.value = null
  if (returnFocus && id) void focusRow(id, '.more')
}

/** Écart entre le bouton ⋯ et son menu (px). */
const MENU_GAP = 2

/** Ouvre le menu sous son bouton, ou au-dessus s'il dépasserait le bas de la fenêtre, puis donne le focus au premier choix. */
async function toggleMenu(pageId: string) {
  if (menuId.value === pageId) return closeMenu()
  const button = root.value?.querySelector<HTMLElement>(`.row[data-page-id="${CSS.escape(pageId)}"] .more`)
  if (!button) return
  const rect = button.getBoundingClientRect()
  // Un élément fixe se place dans la fenêtre sans ses barres de défilement, que `innerWidth` compterait.
  const { clientWidth, clientHeight } = document.documentElement
  const right = clientWidth - rect.right
  menuId.value = pageId
  menuPlace.value = { top: rect.bottom + MENU_GAP, right }
  await nextTick()
  const menu = root.value?.querySelector<HTMLElement>('.menu')
  if (menu && menu.getBoundingClientRect().bottom > clientHeight && rect.top > menu.offsetHeight + MENU_GAP) {
    menuPlace.value = { bottom: clientHeight - rect.top + MENU_GAP, right }
  }
  menu?.querySelector<HTMLElement>('button')?.focus({ preventScroll: true })
}

/** Fixe, le menu ne suivrait pas sa ligne : tout défilement ou redimensionnement le ferme. */
function onViewportChange() {
  if (menuId.value) closeMenu()
}

function startEdit(kind: TreeEdit['kind'], pageId: string) {
  menuId.value = null
  draft.value = kind === 'rename' ? (props.pages.find((p) => p.id === pageId)?.title ?? '') : ''
  edit.value = { kind, pageId }
  if (kind === 'child' && !props.expanded.has(pageId)) emit('toggle', pageId)
}

/** Entrée ou sortie du champ : un titre vide ou inchangé n'envoie rien. */
function submitEdit() {
  const current = edit.value
  if (!current) return
  const title = draft.value.trim()
  if (current.kind === 'child' && title) {
    // Le champ reste ouvert, titre compris, tant que la page n'est pas créée : un refus ne perd pas la frappe.
    emit('create', title, current.pageId, () => {
      if (edit.value === current) edit.value = null
    })
    return
  }
  edit.value = null
  if (current.kind === 'rename') {
    const before = props.pages.find((p) => p.id === current.pageId)?.title
    if (title && title !== before) emit('rename', current.pageId, title)
  }
  void focusRow(current.pageId, '.page-link')
}

function cancelEdit() {
  const current = edit.value
  edit.value = null
  if (current) void focusRow(current.pageId, '.page-link')
}

/**
 * Rend le focus à la ligne d'une page : à son titre après le dialogue de déplacement (son bouton de menu a disparu), à son
 * bouton de menu après une suppression annulée.
 */
defineExpose({
  focusPage: (pageId: string) => focusRow(pageId, '.page-link'),
  focusMenu: (pageId: string) => focusRow(pageId, '.more'),
})

provide(treeContext, {
  currentId: toRef(props, 'currentId'),
  expanded: toRef(props, 'expanded'),
  busy: toRef(props, 'busy'),
  menuId,
  menuPlace,
  edit,
  drag,
  draft,
  pageHref: (pageId) => props.pageHref(pageId),
  open: (pageId) => emit('open', pageId),
  toggle: (pageId) => emit('toggle', pageId),
  toggleMenu,
  startEdit,
  submitEdit,
  cancelEdit,
  requestMove: (pageId) => {
    menuId.value = null
    emit('move', pageId)
  },
  requestDelete: (pageId) => {
    menuId.value = null
    emit('delete', pageId)
  },
})

/** Un clic hors du menu le ferme ; Échap aussi, et rend le focus à son bouton. */
function onPointerDown(e: PointerEvent) {
  if (menuId.value && !(e.target as Element | null)?.closest('.menu, .more')) closeMenu()
}
function onKeyDown(e: KeyboardEvent) {
  if (e.key === 'Escape' && menuId.value) {
    e.preventDefault()
    closeMenu(true)
  }
}
onMounted(() => {
  document.addEventListener('pointerdown', onPointerDown)
  document.addEventListener('keydown', onKeyDown)
  document.addEventListener('scroll', onViewportChange, true)
  window.addEventListener('resize', onViewportChange)
})
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onPointerDown)
  document.removeEventListener('keydown', onKeyDown)
  document.removeEventListener('scroll', onViewportChange, true)
  window.removeEventListener('resize', onViewportChange)
  cancelDrag()
})
</script>

<template>
  <div class="tree-frame">
    <!-- Posée sur le titre du panneau, la zone ne décale aucune ligne de l'arbre au début du glisser. -->
    <div
      v-if="drag"
      ref="rootZone"
      class="drop-root"
      :class="{ 'drop-ok': drag.target === null && drag.allowed, 'drop-refused': drag.target === null && !drag.allowed }"
    >
      Racine du Carnet
    </div>
    <div ref="root" class="tree" @pointerdown="onTreePointerDown">
      <PageBranch :nodes="groups.ordinary" :depth="0" />
      <section v-for="section in groups.sections" :key="section.id" class="section" :aria-labelledby="`section-${section.id}`">
        <h3 :id="`section-${section.id}`" class="section-title">{{ section.title }}</h3>
        <PageBranch :nodes="section.nodes" :depth="0" />
      </section>
    </div>
  </div>
  <div v-if="ghost" class="ghost" :style="{ left: `${ghost.x}px`, top: `${ghost.y}px` }" aria-hidden="true">
    {{ ghost.title }}
  </div>
</template>

<style scoped>
.tree-frame {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.tree {
  min-height: 0;
  overflow-y: auto;
  /* Place pour le contour de focus des lignes. */
  padding: 2px;
  margin: -2px;
}
.section {
  margin-top: 1.1rem;
  padding-top: 0.7rem;
  border-top: 1px solid var(--rule);
}
.section-title {
  margin: 0 0 0.35rem 0.15rem;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 0.95rem;
  letter-spacing: 0.06em;
  color: var(--ink-muted);
}
.drop-root {
  position: absolute;
  z-index: 1;
  left: 0;
  right: 0;
  bottom: calc(100% + 0.5rem);
  background: var(--wood);
  padding: 0.35rem 0.6rem;
  border: 1px dashed var(--rule);
  border-radius: 4px;
  color: var(--ink-muted);
  font-style: italic;
}
.drop-root.drop-ok {
  border-style: solid;
  border-color: var(--brass);
  color: var(--ink);
  background: var(--wood-raised);
}
.drop-root.drop-refused {
  border-color: var(--danger);
}
/* Le titre suit le pointeur, juste à sa droite : il ne cache pas la ligne visée. */
.ghost {
  position: fixed;
  z-index: 10;
  transform: translate(14px, -50%);
  max-width: 16rem;
  padding: 0.3rem 0.6rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  pointer-events: none;
  background: var(--wood-raised);
  border: 1px solid var(--brass);
  border-radius: 4px;
  color: var(--ink);
}
</style>

<style>
/* Pendant un glisser de page, le texte ne se sélectionne pas et le curseur dit qu'on tient la page. */
body.dragging-page {
  user-select: none;
  cursor: grabbing;
}
body.dragging-page * {
  cursor: grabbing !important;
}
</style>
