<script setup lang="ts">
import { ref } from 'vue'
import type { NotebookContext } from '../../src/modules/notebookModule'
import { dndApi } from './api'

/** Le panneau D&D du Carnet : créer un personnage joueur, rangé sous « Personnages joueurs » et ouvert aussitôt. */
const props = defineProps<{ context: NotebookContext }>()
const api = dndApi(props.context.campaignId)

/** Même limite que le titre d'une page. */
const MAX_NAME_LENGTH = 120

const name = ref('')
const error = ref<string | null>(null)
const busy = ref(false)

async function create() {
  const title = name.value.trim()
  if (!title || busy.value) return
  error.value = null
  busy.value = true
  try {
    const sheet = await api.create(title)
    name.value = ''
    await props.context.openPage(sheet.pageId)
  } catch (e) {
    error.value = `Personnage non créé : ${e instanceof Error ? e.message : String(e)}`
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <form class="dnd" @submit.prevent="create">
    <input v-model="name" type="text" :maxlength="MAX_NAME_LENGTH" placeholder="Aria" aria-label="Nom du personnage" required />
    <button type="submit" class="primary" :disabled="busy || !name.trim()">Nouveau personnage</button>
    <p v-if="error" class="error-text" role="alert">{{ error }}</p>
  </form>
</template>

<style scoped>
.dnd {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
input {
  width: 100%;
}
button {
  padding-block: 0.55rem;
}
.error-text {
  margin: 0;
  color: var(--danger);
}
</style>
