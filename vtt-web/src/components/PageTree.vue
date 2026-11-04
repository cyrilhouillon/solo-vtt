<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, provide, ref, toRef } from 'vue'
import PageBranch from './PageBranch.vue'
import { treeContext, type TreeEdit } from '../notebook/context'
import { buildTree } from '../notebook/tree'
import type { PageSummary } from '../types'

/**
 * L'arbre des pages du Carnet. Il ne garde que l'état de l'interface (menu ouvert, champ ouvert) ; les branches
 * dépliées et chaque geste qui touche le serveur reviennent au Carnet.
 */
const props = defineProps<{ pages: PageSummary[]; currentId: string | null; expanded: ReadonlySet<string>; busy: boolean }>()
const emit = defineEmits<{
  open: [pageId: string]
  toggle: [pageId: string]
  create: [title: string, parentId: string]
  rename: [pageId: string, title: string]
  /** Demande le choix d'une destination pour la page. */
  move: [pageId: string]
}>()

const nodes = computed(() => buildTree(props.pages))
const root = ref<HTMLElement | null>(null)
const menuId = ref<string | null>(null)
const edit = ref<TreeEdit | null>(null)
const draft = ref('')

/** Rend le focus à un bouton de la ligne, après que l'arbre s'est redessiné. */
async function focusRow(pageId: string, selector: '.page-link' | '.more') {
  await nextTick()
  root.value?.querySelector<HTMLElement>(`.row[data-page-id="${CSS.escape(pageId)}"] ${selector}`)?.focus()
}

function closeMenu(returnFocus = false) {
  const id = menuId.value
  menuId.value = null
  if (returnFocus && id) void focusRow(id, '.more')
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
  edit.value = null
  const title = draft.value.trim()
  if (current.kind === 'rename') {
    const before = props.pages.find((p) => p.id === current.pageId)?.title
    if (title && title !== before) emit('rename', current.pageId, title)
    void focusRow(current.pageId, '.page-link')
  } else if (title) {
    emit('create', title, current.pageId)
  } else {
    void focusRow(current.pageId, '.page-link')
  }
}

function cancelEdit() {
  const current = edit.value
  edit.value = null
  if (current) void focusRow(current.pageId, '.page-link')
}

/** Rend le focus à la ligne d'une page : après le dialogue de déplacement, son bouton de menu a disparu. */
defineExpose({ focusPage: (pageId: string) => focusRow(pageId, '.page-link') })

provide(treeContext, {
  currentId: toRef(props, 'currentId'),
  expanded: toRef(props, 'expanded'),
  busy: toRef(props, 'busy'),
  menuId,
  edit,
  draft,
  open: (pageId) => emit('open', pageId),
  toggle: (pageId) => emit('toggle', pageId),
  toggleMenu: async (pageId) => {
    menuId.value = menuId.value === pageId ? null : pageId
    if (!menuId.value) return
    await nextTick()
    root.value?.querySelector<HTMLElement>('.menu button')?.focus()
  },
  startEdit,
  submitEdit,
  cancelEdit,
  requestMove: (pageId) => {
    menuId.value = null
    emit('move', pageId)
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
})
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onPointerDown)
  document.removeEventListener('keydown', onKeyDown)
})
</script>

<template>
  <div ref="root" class="tree">
    <PageBranch :nodes="nodes" :depth="0" />
  </div>
</template>

<style scoped>
.tree {
  min-height: 0;
  overflow-y: auto;
  /* Place pour le contour de focus des lignes et le menu de la dernière. */
  padding: 2px 2px 0.5rem;
  margin: -2px;
}
</style>
