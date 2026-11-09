<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import type { NotebookModule } from '../modules/notebookModule'

/**
 * Les modules de la campagne : une case par module du client, cochée s'il est actif. Enregistrer remplace la liste ; un
 * module décoché garde ses données, qu'il retrouve recoché. Un refus du serveur s'affiche ici, le dialogue reste ouvert.
 */
const props = defineProps<{ open: boolean; available: NotebookModule[] | null; active: string[]; busy: boolean; error: string | null }>()
const emit = defineEmits<{ save: [modules: string[]]; close: [] }>()

const dialog = ref<HTMLDialogElement | null>(null)
const chosen = ref<string[]>([])

/** Fermer attend la fin de l'envoi : sa réponse, refus compris, doit s'afficher ici. */
function close() {
  if (!props.busy) emit('close')
}

watch(
  () => props.open,
  async (open) => {
    const el = dialog.value
    if (!el) return
    if (!open) {
      if (el.open) el.close()
      return
    }
    chosen.value = [...props.active]
    if (!el.open) el.showModal()
    await nextTick()
    el.querySelector<HTMLInputElement>('input')?.focus()
  },
)
</script>

<template>
  <dialog
    ref="dialog"
    class="campaign-modules"
    aria-labelledby="campaign-modules-title"
    @cancel.prevent="close"
    @keydown.esc.prevent="close"
  >
    <h2 id="campaign-modules-title" class="panel-title">Modules de la campagne</h2>
    <hr class="tapered" />
    <form @submit.prevent="emit('save', chosen)">
      <p v-if="!available" class="hint">Modules installés illisibles : réessayer en rouvrant le Carnet.</p>
      <p v-else-if="!available.length" class="hint">Aucun module installé.</p>
      <label v-for="module in available" :key="module.id" class="module">
        <input v-model="chosen" type="checkbox" :value="module.id" :disabled="busy" />
        {{ module.title }}
      </label>
      <p v-if="error" class="error-text" role="alert">{{ error }}</p>
      <div class="actions">
        <button type="button" :disabled="busy" @click="close">Annuler</button>
        <button type="submit" :disabled="busy || !available">Enregistrer</button>
      </div>
    </form>
  </dialog>
</template>

<style scoped>
.campaign-modules {
  width: min(24rem, calc(100vw - 2rem));
  box-sizing: border-box;
  padding: 1.1rem 1.25rem 1.25rem;
  border: 1px solid var(--rule);
  border-radius: 4px;
  background: var(--wood);
  color: var(--ink);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
.campaign-modules::backdrop {
  background: rgba(0, 0, 0, 0.55);
}
.tapered {
  margin: -0.5rem 0 0.75rem;
}
form {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
.module {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}
.hint {
  margin: 0;
  color: var(--ink-muted);
}
.error-text {
  margin: 0;
  color: var(--danger);
  overflow-wrap: anywhere;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6rem;
}
</style>
