<script setup lang="ts">
import { ref } from 'vue'
import type { NotebookContext } from '../../src/modules/notebookModule'
import { oracleApi, type D100, type YesNo } from './api'
import { d100Blocks, yesNoBlocks, yesNoText } from './result'

/**
 * L'oracle générique dans le Carnet : une question oui/non à chances égales, ou un d100 ; chaque jet, tiré au serveur,
 * s'écrit dans la page ouverte.
 */
const props = defineProps<{ context: NotebookContext }>()
const api = oracleApi(props.context.campaignId)

/** Même limite que le serveur (`OracleResource.MAX_QUESTION_LENGTH`). */
const MAX_QUESTION_LENGTH = 500

const question = ref('')
/** Le dernier jet écrit dans la page, rappelé sous les boutons. */
const last = ref<{ yesNo: YesNo } | { d100: D100 } | null>(null)
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
    <p v-if="error" class="error-text" role="alert">{{ error }}</p>
    <p v-else-if="last && 'yesNo' in last" class="last" aria-live="polite">
      <strong>{{ yesNoText(last.yesNo) }}</strong>
      <span v-if="last.yesNo.question" class="muted"> · {{ last.yesNo.question }}</span>
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
