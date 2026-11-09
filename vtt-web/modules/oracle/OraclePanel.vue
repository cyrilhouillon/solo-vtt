<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { NotebookContext } from '../../src/modules/notebookModule'
import { oracleApi, type D100, type Draw, type OracleList, type YesNo } from './api'
import { d100Blocks, drawBlocks, drawText, yesNoBlocks, yesNoText } from './result'

/**
 * L'oracle générique dans le Carnet : une question oui/non à chances égales, un d100, ou une ligne tirée dans une liste
 * du joueur ; chaque jet, tiré au serveur, s'écrit dans la page ouverte.
 */
const props = defineProps<{ context: NotebookContext }>()
const api = oracleApi(props.context.campaignId)

/** Même limite que le serveur (`OracleResource.MAX_QUESTION_LENGTH`). */
const MAX_QUESTION_LENGTH = 500

const question = ref('')
/** Le dernier jet écrit dans la page, rappelé sous les boutons. */
const last = ref<{ yesNo: YesNo } | { d100: D100 } | { draw: Draw } | null>(null)
/** Les listes du joueur, relues à chaque ouverture du choix ; `null` tant qu'elles ne sont pas lues. */
const lists = ref<OracleList[] | null>(null)
const chosenList = ref('')
const error = ref<string | null>(null)
const busy = ref(false)

const reason = (e: unknown) => (e instanceof Error ? e.message : String(e))

/** Tire au serveur puis écrit dans la page ; refusé avant tout appel sans page ouverte. */
async function roll<T>(
  draw: (question: string) => Promise<T>,
  blocks: (result: T) => Parameters<NotebookContext['writeToOpenPage']>[0],
  keep: (result: T) => void,
) {
  error.value = null
  if (!props.context.hasOpenPage()) {
    error.value = 'Aucune page ouverte : ouvrir une page pour y écrire le jet.'
    return
  }
  busy.value = true
  try {
    const result = await draw(question.value)
    const refusal = props.context.writeToOpenPage(blocks(result))
    if (refusal) {
      error.value = refusal
      return
    }
    keep(result)
  } catch (e) {
    error.value = `Jet non tiré : ${reason(e)}`
  } finally {
    busy.value = false
  }
}

/** Relit les listes : le joueur en crée et en renomme dans l'arbre pendant que le panneau est ouvert. */
async function readLists() {
  try {
    lists.value = await api.lists()
    if (!lists.value.some((list) => list.id === chosenList.value)) chosenList.value = lists.value[0]?.id ?? ''
  } catch (e) {
    error.value = `Listes illisibles : ${reason(e)}`
  }
}

onMounted(readLists)

function drawLine() {
  if (!chosenList.value) return
  const listId = chosenList.value
  void roll(() => api.draw(listId), drawBlocks, (draw) => (last.value = { draw }))
}

function askYesNo() {
  void roll(api.yesNo, yesNoBlocks, (yesNo) => (last.value = { yesNo }))
}

function rollD100() {
  void roll(api.d100, d100Blocks, (d100) => (last.value = { d100 }))
}
</script>

<template>
  <div class="oracle">
    <form class="question" @submit.prevent="askYesNo">
      <label>
        Question
        <input v-model="question" type="text" :maxlength="MAX_QUESTION_LENGTH" placeholder="Le pont tient-il ?" />
      </label>
      <div class="buttons">
        <button type="submit" class="ask" :disabled="busy">Oui/non</button>
        <button type="button" class="ask" :disabled="busy" @click="rollD100">d100</button>
      </div>
    </form>
    <hr class="rule" />
    <div class="lists">
      <template v-if="lists?.length">
        <label>
          Liste
          <select v-model="chosenList" :disabled="busy" @focus="readLists" @pointerdown="readLists">
            <option v-for="list in lists" :key="list.id" :value="list.id">{{ list.title }}</option>
          </select>
        </label>
        <button type="button" class="ask" :disabled="busy || !chosenList" @click="drawLine">Tirer</button>
      </template>
      <template v-else-if="lists">
        <span class="label">Listes</span>
        <p class="muted">Aucune liste : ajouter une sous-page à « Listes », une ligne par puce.</p>
        <button type="button" class="refresh" :disabled="busy" @click="readLists">Relire les listes</button>
      </template>
    </div>
    <p v-if="error" class="error-text" role="alert">{{ error }}</p>
    <p v-else-if="last && 'yesNo' in last" class="last" aria-live="polite">
      <strong>{{ yesNoText(last.yesNo) }}</strong>
      <span v-if="last.yesNo.question" class="muted"> · {{ last.yesNo.question }}</span>
    </p>
    <p v-else-if="last && 'draw' in last" class="last" aria-live="polite">
      <strong>{{ drawText(last.draw) }}</strong>
      <span class="muted"> · d{{ last.draw.sides }} {{ last.draw.roll }}, ligne {{ last.draw.line }}</span>
    </p>
    <p v-else-if="last" class="last" aria-live="polite">
      <strong>d100 : {{ last.d100.roll }}</strong>
      <span v-if="last.d100.question" class="muted"> · {{ last.d100.question }}</span>
    </p>
  </div>
</template>

<style scoped>
.oracle {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.rule {
  margin: 0;
  border: 0;
  border-top: 1px solid var(--rule);
}
.lists {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
.lists p {
  margin: 0;
}
.label {
  color: var(--ink-muted);
}
.refresh {
  align-self: flex-start;
}
select {
  width: 100%;
  box-sizing: border-box;
  font: inherit;
}
.question {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
label {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  color: var(--ink-muted);
}
input {
  width: 100%;
  box-sizing: border-box;
  font: inherit;
}
.buttons {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.ask {
  align-self: flex-start;
  font-weight: 700;
  background: var(--wood-raised);
}
.error-text {
  margin: 0;
  color: var(--danger);
}
.last {
  margin: 0;
  overflow-wrap: anywhere;
}
.muted {
  color: var(--ink-muted);
}
</style>
