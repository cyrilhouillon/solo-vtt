<script setup lang="ts">
import { onMounted, ref } from 'vue'
import FoldSection from '../../src/components/FoldSection.vue'
import type { NotebookContext } from '../../src/modules/notebookModule'
import { oracleApi, type D100, type Draw, type OracleList, type YesNo } from './api'
import { d100Blocks, drawBlocks, drawText, yesNoBlocks, yesNoText } from './result'

/**
 * L'oracle générique dans le Carnet : une question oui/non à chances égales, un d100, ou une ligne tirée dans une liste
 * du joueur ; chaque jet, tiré au serveur, s'écrit dans la page ouverte, ou s'affiche et se copie sans page.
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

/** Tire au serveur puis écrit le résultat : dans la page ouverte, sinon dans un dialogue et au presse-papiers. */
async function roll<T>(
  draw: (question: string) => Promise<T>,
  blocks: (result: T) => Parameters<NotebookContext['writeResult']>[0],
  keep: (result: T) => void,
) {
  error.value = null
  busy.value = true
  try {
    const result = await draw(question.value)
    props.context.writeResult(blocks(result))
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
    if (error.value?.startsWith('Listes illisibles')) error.value = null
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
    <FoldSection title="Question" storage-key="oracle.question" nested class="first">
      <form class="stack" @submit.prevent="askYesNo">
        <input v-model="question" type="text" :maxlength="MAX_QUESTION_LENGTH" placeholder="Le pont tient-il ?" aria-label="Question (facultative)" />
        <div class="pair">
          <button type="submit" class="primary" :disabled="busy">Oui ou non</button>
          <button type="button" :disabled="busy" @click="rollD100">Lancer un d100</button>
        </div>
      </form>
    </FoldSection>
    <FoldSection title="Listes" storage-key="oracle.lists" nested>
      <div class="stack">
        <template v-if="lists?.length">
          <select v-model="chosenList" :disabled="busy" aria-label="Liste" @focus="readLists" @pointerdown="readLists">
            <option v-for="list in lists" :key="list.id" :value="list.id">{{ list.title }}</option>
          </select>
          <button type="button" class="primary wide" :disabled="busy || !chosenList" @click="drawLine">Tirer une ligne</button>
        </template>
        <template v-else-if="lists">
          <p class="hint">Aucune liste : ajouter une sous-page à « Listes », une ligne par puce.</p>
          <button type="button" class="wide" :disabled="busy" @click="readLists">Relire les listes</button>
        </template>
      </div>
    </FoldSection>
    <p v-if="error" class="error-text" role="alert">{{ error }}</p>
    <div v-else-if="last" class="last" aria-live="polite">
      <template v-if="'yesNo' in last">
        <strong class="verdict">{{ yesNoText(last.yesNo) }}</strong>
        <span v-if="last.yesNo.question" class="facts">{{ last.yesNo.question }}</span>
      </template>
      <template v-else-if="'draw' in last">
        <strong class="verdict">{{ drawText(last.draw) }}</strong>
        <span class="facts">d{{ last.draw.sides }} {{ last.draw.roll }} · ligne {{ last.draw.line }}</span>
      </template>
      <template v-else>
        <strong class="verdict">d100 : {{ last.d100.roll }}</strong>
        <span v-if="last.d100.question" class="facts">{{ last.d100.question }}</span>
      </template>
    </div>
  </div>
</template>

<style scoped>
.oracle {
  display: flex;
  flex-direction: column;
}
/* Sous le titre du module, la première sous-section n'a pas de filet au-dessus. */
.first {
  border-top: 0;
}
.stack {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
input[type='text'],
select {
  width: 100%;
}
.pair {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.4rem;
}
.pair button,
.wide {
  padding-block: 0.55rem;
}
.wide {
  width: 100%;
}
.hint {
  margin: 0;
  color: var(--ink-muted);
}
.error-text {
  margin: 0.9rem 0 0;
  color: var(--danger);
}
.last {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  margin-top: 0.9rem;
  padding: 0.75rem 0.9rem;
  border-radius: 4px;
  background: var(--wood-sunken);
  overflow-wrap: anywhere;
}
.verdict {
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 1.35rem;
  line-height: 1.2;
  color: var(--rubric);
}
.facts {
  color: var(--ink-muted);
  font-size: 0.92rem;
  font-variant-numeric: tabular-nums;
}
</style>
