<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import FoldSection from '../../src/components/FoldSection.vue'
import type { NotebookContext } from '../../src/modules/notebookModule'
import {
  meaningApi,
  mythicApi,
  type FateAnswer,
  type Meaning,
  type MeaningTable,
  type MeaningTables,
  type Odds,
  type Scene,
  type SceneEnd,
} from './api'
import {
  adjustmentsText,
  answerBlocks,
  eventLines,
  meaningBlocks,
  sceneBlocks,
  sceneEndBlocks,
  sceneEndText,
} from './result'

/**
 * L'oracle Mythic dans le Carnet : le facteur de chaos de la campagne, le test et la fin d'une scène, une question
 * oui/non, et deux mots tirés dans les tables de sens du joueur ; chaque résultat s'écrit dans la page ouverte, ou
 * s'affiche et se copie sans page.
 */
const props = defineProps<{ context: NotebookContext }>()
const api = mythicApi(props.context.campaignId)

const MIN_CHAOS = 1
const MAX_CHAOS = 9

/** `null` tant que le serveur n'a pas répondu. */
const chaos = ref<number | null>(null)
const odds = ref<Odds[]>([])
const chosen = ref('FIFTY_FIFTY')
const question = ref('')
const expectedScene = ref('')
/** Le dernier résultat écrit dans la page, rappelé sous les champs. */
const last = ref<
  { answer: FateAnswer } | { meaning: Meaning } | { scene: Scene } | { sceneEnd: SceneEnd } | null
>(null)
/** `null` tant que le serveur n'a pas répondu. */
const meaningTables = ref<MeaningTables | null>(null)
const chosenTable = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

const tableKey = (table: MeaningTable) => `${table.kind}:${table.element ?? ''}`
const columns = computed(() => meaningTables.value?.tables.filter((t) => t.kind !== 'ELEMENT') ?? [])
const elements = computed(() => meaningTables.value?.tables.filter((t) => t.kind === 'ELEMENT') ?? [])
const error = ref<string | null>(null)
const busy = ref(false)

const ready = computed(() => chaos.value !== null && odds.value.length > 0)

const reason = (e: unknown) => (e instanceof Error ? e.message : String(e))

onMounted(async () => {
  try {
    const [current, all] = await Promise.all([api.chaos(), api.odds()])
    chaos.value = current
    odds.value = all
  } catch (e) {
    error.value = `Oracle indisponible : ${reason(e)}`
  }
  try {
    showTables(await meaningApi.tables())
  } catch (e) {
    error.value ??= `Tables de sens indisponibles : ${reason(e)}`
  }
})

function showTables(tables: MeaningTables) {
  meaningTables.value = tables
  const first = tables.tables[0]
  if (!tables.tables.some((t) => tableKey(t) === chosenTable.value)) chosenTable.value = first ? tableKey(first) : ''
}

async function importTables(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  // Vidé pour qu'un même fichier, corrigé sur le disque, puisse être choisi de nouveau.
  input.value = ''
  if (!file) return
  error.value = null
  busy.value = true
  try {
    showTables(await meaningApi.importTables(await file.text()))
  } catch (e) {
    error.value = `Tables non importées : ${reason(e)}`
  } finally {
    busy.value = false
  }
}

async function drawMeaning() {
  error.value = null
  const table = meaningTables.value?.tables.find((t) => tableKey(t) === chosenTable.value)
  if (!table) return
  busy.value = true
  try {
    const meaning = await meaningApi.draw(table)
    props.context.writeResult(meaningBlocks(meaning))
    last.value = { meaning }
  } catch (e) {
    error.value = `Sens non tiré : ${reason(e)}`
  } finally {
    busy.value = false
  }
}

async function changeChaos(step: number) {
  if (chaos.value === null) return
  error.value = null
  busy.value = true
  try {
    chaos.value = await api.setChaos(chaos.value + step)
  } catch (e) {
    error.value = `Chaos non gardé : ${reason(e)}`
  } finally {
    busy.value = false
  }
}

async function testScene() {
  error.value = null
  busy.value = true
  try {
    const scene = await api.testScene(expectedScene.value.trim())
    props.context.writeResult(sceneBlocks(scene))
    last.value = { scene }
    expectedScene.value = ''
  } catch (e) {
    error.value = `Scène non testée : ${reason(e)}`
  } finally {
    busy.value = false
  }
}

async function endScene(inControl: boolean) {
  error.value = null
  busy.value = true
  try {
    const end = await api.endScene(inControl)
    chaos.value = end.after
    props.context.writeResult(sceneEndBlocks(end))
    last.value = { sceneEnd: end }
  } catch (e) {
    error.value = `Scène non close : ${reason(e)}`
  } finally {
    busy.value = false
  }
}

async function ask() {
  error.value = null
  busy.value = true
  try {
    const answer = await api.ask(question.value.trim(), chosen.value)
    props.context.writeResult(answerBlocks(answer))
    last.value = { answer }
    question.value = ''
  } catch (e) {
    error.value = `Question non posée : ${reason(e)}`
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="mythic">
    <div class="chaos" role="group" aria-labelledby="mythic-chaos-label">
      <span id="mythic-chaos-label" class="chaos-label">Facteur de chaos</span>
      <button
        type="button"
        class="step"
        aria-label="Baisser le chaos"
        :disabled="busy || chaos === null || chaos <= MIN_CHAOS"
        @click="changeChaos(-1)"
      >
        −
      </button>
      <output class="chaos-value" aria-live="polite">{{ chaos ?? '…' }}</output>
      <button
        type="button"
        class="step"
        aria-label="Monter le chaos"
        :disabled="busy || chaos === null || chaos >= MAX_CHAOS"
        @click="changeChaos(1)"
      >
        +
      </button>
    </div>

    <FoldSection title="Question" storage-key="mythic.question" nested>
      <form class="stack" @submit.prevent="ask">
        <input v-model="question" type="text" maxlength="500" placeholder="Le garde est-il corrompu ?" aria-label="Question (facultative)" />
        <fieldset class="odds" :disabled="!ready">
          <legend>Probabilité</legend>
          <label v-for="o in odds" :key="o.code" class="odd" :class="{ checked: chosen === o.code }">
            <input v-model="chosen" type="radio" name="mythic-odds" :value="o.code" />
            {{ o.label }}
          </label>
        </fieldset>
        <button type="submit" class="primary wide" :disabled="busy || !ready">Demander à l’oracle</button>
      </form>
    </FoldSection>

    <FoldSection title="Scène" storage-key="mythic.scene" :open="false" nested>
      <form class="stack" @submit.prevent="testScene">
        <input v-model="expectedScene" type="text" maxlength="500" placeholder="On entre au donjon" aria-label="Scène attendue (facultative)" />
        <button type="submit" class="primary wide" :disabled="busy || chaos === null">Tester la scène</button>
        <div class="end" role="group" aria-labelledby="mythic-end-label">
          <span id="mythic-end-label" class="hint">Clore la scène</span>
          <div class="pair">
            <button type="button" :disabled="busy || chaos === null" @click="endScene(true)">Contrôle gardé</button>
            <button type="button" :disabled="busy || chaos === null" @click="endScene(false)">Contrôle perdu</button>
          </div>
        </div>
      </form>
    </FoldSection>

    <FoldSection title="Sens" storage-key="mythic.meaning" :open="false" nested>
      <div class="stack">
        <template v-if="meaningTables?.imported">
          <select v-model="chosenTable" aria-label="Table de sens">
            <option v-for="t in columns" :key="tableKey(t)" :value="tableKey(t)">{{ t.label }}</option>
            <optgroup v-if="elements.length" label="Éléments">
              <option v-for="t in elements" :key="tableKey(t)" :value="tableKey(t)">{{ t.label }}</option>
            </optgroup>
          </select>
          <button type="button" class="primary wide" :disabled="busy" @click="drawMeaning">Tirer deux mots</button>
          <button type="button" class="replace" :disabled="busy" @click="fileInput?.click()">Remplacer les tables</button>
        </template>
        <template v-else-if="meaningTables">
          <p class="hint">Tables de sens non importées : importer le fichier de vos tables.</p>
          <button type="button" class="wide" :disabled="busy" @click="fileInput?.click()">Importer les tables</button>
        </template>
        <input ref="fileInput" type="file" accept=".txt,text/plain" hidden @change="importTables" />
      </div>
    </FoldSection>

    <p v-if="error" class="error-text" role="alert">{{ error }}</p>
    <div v-else-if="last" class="last" aria-live="polite">
      <template v-if="'answer' in last">
        <strong class="verdict">{{ last.answer.answerLabel }}</strong>
        <span class="facts">d100 {{ last.answer.roll }} · {{ last.answer.odds.label }} · chaos {{ last.answer.chaos }}</span>
        <span v-for="line in last.answer.randomEvent ? eventLines(last.answer.randomEvent) : []" :key="line" class="event">{{ line }}</span>
      </template>
      <template v-else-if="'scene' in last">
        <strong class="verdict">{{ last.scene.outcomeLabel }}</strong>
        <span class="facts">d10 {{ last.scene.roll }} · chaos {{ last.scene.chaos }}</span>
        <span v-if="last.scene.adjustments.length" class="event">{{ adjustmentsText(last.scene) }}</span>
        <span v-for="line in last.scene.randomEvent ? eventLines(last.scene.randomEvent) : []" :key="line" class="event">{{ line }}</span>
      </template>
      <template v-else-if="'sceneEnd' in last">
        <strong class="verdict">Fin de scène</strong>
        <span class="facts">{{ sceneEndText(last.sceneEnd) }}</span>
      </template>
      <template v-else>
        <strong class="verdict">{{ last.meaning.first }} · {{ last.meaning.second }}</strong>
        <span class="facts">{{ last.meaning.table }} · d100 {{ last.meaning.firstRoll }} / {{ last.meaning.secondRoll }}</span>
      </template>
    </div>
  </div>
</template>

<style scoped>
.mythic {
  display: flex;
  flex-direction: column;
}
.chaos {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0 0 0.9rem;
}
.chaos-label {
  flex: 1;
  color: var(--ink-muted);
}
.step {
  width: 2.1rem;
  height: 2.1rem;
  padding: 0;
  font-weight: 700;
  font-size: 1.1rem;
}
.chaos-value {
  min-width: 2rem;
  text-align: center;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 1.7rem;
  line-height: 1;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
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
/* La probabilité se coche d'un clic : les neuf degrés en vue, du moins au plus probable. */
.odds {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.3rem;
  margin: 0;
  padding: 0;
  border: 0;
  min-width: 0;
}
.odds legend {
  padding: 0;
  margin-bottom: 0.35rem;
  color: var(--ink-muted);
  font-size: 0.95rem;
}
.odd {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 2.4rem;
  padding: 0.25rem 0.3rem;
  border: 1px solid var(--rule);
  border-radius: 4px;
  background: var(--wood-sunken);
  color: var(--ink-muted);
  font-size: 0.88rem;
  line-height: 1.15;
  text-align: center;
  cursor: pointer;
}
.odd:hover {
  color: var(--ink);
  border-color: var(--control-border);
}
.odd.checked {
  border-color: var(--ink);
  box-shadow: inset 0 0 0 1px var(--ink);
  background: var(--wood-raised);
  color: var(--ink);
  font-weight: 700;
}
.odd input {
  position: absolute;
  inset: 0;
  margin: 0;
  opacity: 0;
  cursor: pointer;
}
.odd:has(input:focus-visible) {
  outline: 2px solid var(--brass);
  outline-offset: 2px;
}
.odds:disabled .odd {
  opacity: 0.5;
  cursor: not-allowed;
}
.wide {
  width: 100%;
  padding-block: 0.55rem;
}
.end {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  margin-top: 0.2rem;
}
.pair {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.4rem;
}
.hint {
  margin: 0;
  color: var(--ink-muted);
}
.replace {
  align-self: flex-start;
  padding: 0;
  border: 0;
  background: none;
  color: var(--ink-muted);
  text-decoration: underline;
  font-size: 0.9rem;
}
.replace:hover:not(:disabled) {
  background: none;
  color: var(--ink);
}
.error-text {
  margin: 0.9rem 0 0;
  color: var(--danger);
}
/* Le dernier tirage : le verdict en rubrique, puis les dés. */
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
.event {
  margin-top: 0.2rem;
  font-size: 0.95rem;
}
</style>
