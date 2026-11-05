<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { PageDeletion, PageSummary } from '../types'

/**
 * Confirmer la suppression d'une page : le message dit ses sous-pages et les pages qui y renvoient, dont les liens
 * mourront. La page part à la corbeille, d'où elle se restaure.
 */
const props = defineProps<{ page: PageSummary | null; deletion: PageDeletion | null; busy: boolean }>()
const emit = defineEmits<{ confirm: []; close: [] }>()

const dialog = ref<HTMLDialogElement | null>(null)
const cancelButton = ref<HTMLButtonElement | null>(null)

const question = computed(() => {
  const title = `« ${props.page?.title ?? ''} »`
  const count = props.deletion?.subpageCount ?? 0
  if (count === 0) return `Supprimer ${title} ?`
  return count === 1 ? `Supprimer ${title} et sa sous-page ?` : `Supprimer ${title} et ses ${count} sous-pages ?`
})

const citing = computed(() => {
  const count = props.deletion?.citingCount ?? 0
  if (count === 0) return null
  return count === 1
    ? '1 page y renvoie : son lien s’affichera mort.'
    : `${count} pages y renvoient : leurs liens s’afficheront morts.`
})

const along = computed(() => {
  const count = props.deletion?.subpageCount ?? 0
  return count === 0 ? '' : count === 1 ? ' avec sa sous-page' : ' avec ses sous-pages'
})

watch(
  () => props.page,
  async (page) => {
    const el = dialog.value
    if (!el) return
    if (!page) {
      if (el.open) el.close()
      return
    }
    if (!el.open) el.showModal()
    // Annuler d'abord : Entrée par mégarde ne supprime rien.
    await nextTick()
    cancelButton.value?.focus()
  },
)
</script>

<template>
  <dialog
    ref="dialog"
    class="delete"
    aria-labelledby="delete-title"
    aria-describedby="delete-question"
    @cancel.prevent="emit('close')"
    @keydown.esc.prevent="emit('close')"
  >
    <form v-if="page" method="dialog" @submit.prevent="emit('confirm')">
      <h2 id="delete-title" class="panel-title">Supprimer « {{ page.title }} »</h2>
      <hr class="tapered" />
      <div id="delete-question" class="message">
        <p v-if="!deletion" class="hint">Lecture de ce qui part avec elle…</p>
        <template v-else>
          <p>{{ question }}</p>
          <p v-if="citing">{{ citing }}</p>
          <p class="hint">
            Elle ira à la corbeille, d'où elle se restaure{{ along }}.
          </p>
        </template>
      </div>
      <div class="actions">
        <button type="submit" class="danger" :disabled="!deletion || busy">Supprimer</button>
        <button ref="cancelButton" type="button" @click="emit('close')">Annuler</button>
      </div>
    </form>
  </dialog>
</template>

<style scoped>
.delete {
  width: min(28rem, calc(100vw - 2rem));
  box-sizing: border-box;
  padding: 1.1rem 1.25rem 1.25rem;
  border: 1px solid var(--rule);
  border-radius: 4px;
  background: var(--wood);
  color: var(--ink);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
.delete::backdrop {
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
.message {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  overflow-wrap: anywhere;
}
.message p {
  margin: 0;
}
.hint {
  color: var(--ink-muted);
}
.actions {
  display: flex;
  gap: 0.6rem;
}
.danger {
  border-color: var(--danger);
  color: var(--danger);
  font-weight: 700;
}
</style>
