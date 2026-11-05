<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import type { TrashedPage } from '../types'

/**
 * La corbeille du Carnet : une ligne par suppression, la plus récente d'abord, que l'on restaure ; « Vider » efface tout,
 * après une confirmation sur place.
 */
const props = defineProps<{ open: boolean; entries: TrashedPage[] | null; busy: boolean }>()
const emit = defineEmits<{ restore: [pageId: string]; empty: []; close: [] }>()

const dialog = ref<HTMLDialogElement | null>(null)
const closeButton = ref<HTMLButtonElement | null>(null)
const emptyButton = ref<HTMLButtonElement | null>(null)
/** « Vider » attend sa confirmation. */
const confirming = ref(false)

const when = new Intl.DateTimeFormat('fr', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })

function deletedAt(entry: TrashedPage): string {
  const date = new Date(entry.deletedAt)
  return Number.isNaN(date.getTime()) ? '' : when.format(date)
}

function subpages(count: number): string {
  return count === 1 ? 'et 1 sous-page' : `et ${count} sous-pages`
}

watch(
  () => props.open,
  async (open) => {
    const el = dialog.value
    if (!el) return
    confirming.value = false
    if (!open) {
      if (el.open) el.close()
      return
    }
    if (!el.open) el.showModal()
    await nextTick()
    closeButton.value?.focus()
  },
)

/** Vidée, la corbeille n'a plus que « Fermer » à offrir au focus. */
watch(
  () => props.entries?.length,
  async (count) => {
    if (count !== 0 || !props.open) return
    confirming.value = false
    await nextTick()
    closeButton.value?.focus()
  },
)

async function askEmpty() {
  confirming.value = true
  await nextTick()
  dialog.value?.querySelector<HTMLButtonElement>('.confirm .danger')?.focus()
}

async function cancelEmpty() {
  confirming.value = false
  await nextTick()
  emptyButton.value?.focus()
}

function confirmEmpty() {
  confirming.value = false
  emit('empty')
}

/** Échap annule d'abord la confirmation, puis ferme. */
function onEscape() {
  if (confirming.value) void cancelEmpty()
  else emit('close')
}
</script>

<template>
  <dialog ref="dialog" class="trash" aria-labelledby="trash-title" @cancel.prevent="onEscape" @keydown.esc.prevent="onEscape">
    <h2 id="trash-title" class="panel-title">Corbeille</h2>
    <hr class="tapered" />
    <p v-if="entries === null" class="hint">Lecture de la corbeille…</p>
    <p v-else-if="!entries.length" class="hint">La corbeille est vide.</p>
    <ul v-else class="entries">
      <li v-for="entry in entries" :key="entry.id" class="entry">
        <div class="what">
          <span class="title">{{ entry.title }}</span>
          <span v-if="entry.subpageCount" class="hint">{{ subpages(entry.subpageCount) }}</span>
          <time class="hint" :datetime="entry.deletedAt">{{ deletedAt(entry) }}</time>
        </div>
        <button type="button" :disabled="busy" :aria-label="`Restaurer « ${entry.title} »`" @click="emit('restore', entry.id)">
          Restaurer
        </button>
      </li>
    </ul>
    <div class="actions">
      <span v-if="confirming" class="confirm" role="group" aria-label="Vider la corbeille ?">
        <span>Tout effacer pour de bon ?</span>
        <button type="button" class="danger" :disabled="busy" @click="confirmEmpty">Vider</button>
        <button type="button" @click="cancelEmpty">Annuler</button>
      </span>
      <button v-else ref="emptyButton" type="button" class="danger" :disabled="!entries?.length || busy" @click="askEmpty">
        Vider la corbeille
      </button>
      <button ref="closeButton" type="button" class="close" @click="emit('close')">Fermer</button>
    </div>
  </dialog>
</template>

<style scoped>
.trash {
  width: min(30rem, calc(100vw - 2rem));
  max-height: min(36rem, calc(100vh - 2rem));
  box-sizing: border-box;
  padding: 1.1rem 1.25rem 1.25rem;
  border: 1px solid var(--rule);
  border-radius: 4px;
  background: var(--wood);
  color: var(--ink);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
.trash[open] {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.trash::backdrop {
  background: rgba(0, 0, 0, 0.55);
}
.tapered {
  margin: -0.5rem 0 0;
}
.hint {
  margin: 0;
  color: var(--ink-muted);
}
.entries {
  list-style: none;
  margin: 0;
  padding: 0;
  min-height: 0;
  max-height: 20rem;
  overflow-y: auto;
  border-top: 1px solid var(--rule);
}
.entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.5rem 0.1rem;
  border-bottom: 1px solid var(--rule);
}
.what {
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  column-gap: 0.5rem;
  overflow-wrap: anywhere;
}
.title {
  font-weight: 700;
}
.what time {
  flex-basis: 100%;
  font-size: 0.9rem;
}
.entry button {
  flex: none;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.6rem;
}
.confirm {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
}
.close {
  margin-left: auto;
}
.danger {
  border-color: var(--danger);
  color: var(--danger);
}
</style>
