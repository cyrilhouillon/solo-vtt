<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { buildTree, canMove, type PageNode } from '../notebook/tree'
import type { PageSummary } from '../types'

/**
 * Choisir où ranger une page : la racine ou une autre page, jamais elle-même, l'une de ses sous-pages ni sa place
 * actuelle. C'est le chemin clavier et tactile du glisser-déposer de l'arbre.
 */
const props = defineProps<{ pages: PageSummary[]; pageId: string | null; busy: boolean }>()
const emit = defineEmits<{ move: [parentId: string | null]; close: [] }>()

const ROOT = ''
const dialog = ref<HTMLDialogElement | null>(null)
const choice = ref<string | null>(null)
const page = computed(() => props.pages.find((p) => p.id === props.pageId) ?? null)

/** Toutes les pages, en arbre indenté, chacune dite permise ou non. */
const targets = computed(() => {
  const id = props.pageId
  if (!id) return []
  const rows: { page: PageSummary; depth: number; allowed: boolean }[] = []
  const walk = (nodes: PageNode[], depth: number) =>
    nodes.forEach((node) => {
      rows.push({ page: node.page, depth, allowed: canMove(props.pages, id, node.page.id) })
      walk(node.children, depth + 1)
    })
  walk(buildTree(props.pages), 0)
  return rows
})
const rootAllowed = computed(() => !!props.pageId && canMove(props.pages, props.pageId, null))

watch(
  () => props.pageId,
  async (id) => {
    const el = dialog.value
    if (!el) return
    if (!id) {
      if (el.open) el.close()
      return
    }
    choice.value = null
    if (!el.open) el.showModal()
    await nextTick()
    el.querySelector<HTMLInputElement>('input:not(:disabled)')?.focus()
  },
)

function submit() {
  if (choice.value === null) return
  emit('move', choice.value === ROOT ? null : choice.value)
}
</script>

<template>
  <dialog ref="dialog" class="move" aria-labelledby="move-title" @cancel.prevent="emit('close')" @keydown.esc.prevent="emit('close')">
    <form v-if="page" method="dialog" @submit.prevent="submit">
      <h2 id="move-title" class="panel-title">Déplacer « {{ page.title }} »</h2>
      <hr class="tapered" />
      <p class="hint">Ses sous-pages la suivent.</p>
      <fieldset>
        <legend class="sr-only">Ranger sous</legend>
        <label class="target" :class="{ refused: !rootAllowed }">
          <input v-model="choice" type="radio" name="parent" :value="ROOT" :disabled="!rootAllowed" />
          Racine du Carnet
        </label>
        <label
          v-for="row in targets"
          :key="row.page.id"
          class="target"
          :class="{ refused: !row.allowed }"
          :style="{ '--depth': row.depth + 1 }"
        >
          <input v-model="choice" type="radio" name="parent" :value="row.page.id" :disabled="!row.allowed" />
          {{ row.page.title }}
        </label>
      </fieldset>
      <div class="actions">
        <button type="submit" class="primary" :disabled="choice === null || busy">Déplacer</button>
        <button type="button" @click="emit('close')">Annuler</button>
      </div>
    </form>
  </dialog>
</template>

<style scoped>
.move {
  width: min(28rem, calc(100vw - 2rem));
  max-height: min(36rem, calc(100vh - 2rem));
  box-sizing: border-box;
  padding: 1.1rem 1.25rem 1.25rem;
  border: 1px solid var(--rule);
  border-radius: 4px;
  background: var(--wood);
  color: var(--ink);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
.move::backdrop {
  background: rgba(0, 0, 0, 0.55);
}
form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.tapered {
  margin: -0.5rem 0 0;
}
.hint {
  margin: 0;
  color: var(--ink-muted);
}
fieldset {
  margin: 0;
  padding: 0.25rem 0;
  border: 0;
  border-top: 1px solid var(--rule);
  border-bottom: 1px solid var(--rule);
  max-height: 20rem;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.target {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.2rem 0.4rem 0.2rem calc(0.4rem + (var(--depth, 0) * 1.1rem));
  border-radius: 4px;
  overflow-wrap: anywhere;
  cursor: pointer;
}
.target:hover:not(.refused) {
  background: var(--wood-raised);
}
.target.refused {
  color: var(--ink-muted);
  cursor: not-allowed;
}
input[type='radio'] {
  accent-color: var(--brass);
}
.actions {
  display: flex;
  gap: 0.6rem;
}
.primary {
  background: var(--wood-raised);
  border-color: var(--ink-muted);
  font-weight: 700;
}
</style>
