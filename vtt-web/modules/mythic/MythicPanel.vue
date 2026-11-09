<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
      <span id="mythic-chaos-label">Chaos</span>
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
    <hr class="rule" />
    <form class="scene" @submit.prevent="testScene">
      <label>
        Scène attendue
        <input v-model="expectedScene" type="text" maxlength="500" placeholder="On entre au donjon" />
      </label>
      <button type="submit" class="ask" :disabled="busy || chaos === null">Tester la scène</button>
      <div class="end" role="group" aria-labelledby="mythic-end-label">
        <span id="mythic-end-label" class="label">Clore la scène</span>
        <div class="end-buttons">
          <button type="button" :disabled="busy || chaos === null" @click="endScene(true)">Contrôle gardé</button>
          <button type="button" :disabled="busy || chaos === null" @click="endScene(false)">Contrôle perdu</button>
        </div>
      </div>
    </form>
    <hr class="rule" />
    <form class="question" @submit.prevent="ask">
      <label>
        Question
        <input v-model="question" type="text" maxlength="500" placeholder="Le garde est-il corrompu ?" />
      </label>
      <label>
        Probabilité
        <select v-model="chosen" :disabled="!ready">
          <option v-for="o in odds" :key="o.code" :value="o.code">{{ o.label }}</option>
        </select>
      </label>
      <button type="submit" class="ask" :disabled="busy || !ready">Demander</button>
    </form>
    <hr class="rule" />
    <div class="meaning">
      <template v-if="meaningTables?.imported">
        <label>
          Sens
          <select v-model="chosenTable">
            <option v-for="t in columns" :key="tableKey(t)" :value="tableKey(t)">{{ t.label }}</option>
            <optgroup v-if="elements.length" label="Éléments">
              <option v-for="t in elements" :key="tableKey(t)" :value="tableKey(t)">{{ t.label }}</option>
            </optgroup>
          </select>
        </label>
        <button type="button" class="ask" :disabled="busy" @click="drawMeaning">Tirer</button>
        <button type="button" class="replace" :disabled="busy" @click="fileInput?.click()">Remplacer les tables</button>
      </template>
      <template v-else-if="meaningTables">
        <span class="label">Sens</span>
        <p class="muted">Tables de sens non importées : importer le fichier de vos tables.</p>
        <button type="button" class="ask" :disabled="busy" @click="fileInput?.click()">Importer</button>
      </template>
      <input ref="fileInput" type="file" accept=".txt,text/plain" hidden @change="importTables" />
    </div>
    <p v-if="error" class="error-text" role="alert">{{ error }}</p>
    <p v-else-if="last && 'answer' in last" class="last" aria-live="polite">
      <strong>{{ last.answer.answerLabel }}</strong>
      <span class="muted"> · d100 {{ last.answer.roll }} · {{ last.answer.odds.label }}, chaos {{ last.answer.chaos }}</span>
      <template v-if="last.answer.randomEvent">
        <template v-for="line in eventLines(last.answer.randomEvent)" :key="line"><br />{{ line }}</template>
      </template>
    </p>
    <p v-else-if="last && 'scene' in last" class="last" aria-live="polite">
      <strong>{{ last.scene.outcomeLabel }}</strong>
      <span class="muted"> · d10 {{ last.scene.roll }} · chaos {{ last.scene.chaos }}</span>
      <template v-if="last.scene.adjustments.length">
        <br />
        {{ adjustmentsText(last.scene) }}
      </template>
      <template v-if="last.scene.randomEvent">
        <template v-for="line in eventLines(last.scene.randomEvent)" :key="line"><br />{{ line }}</template>
      </template>
    </p>
    <p v-else-if="last && 'sceneEnd' in last" class="last" aria-live="polite">
      <strong>Fin de scène</strong>
      <span class="muted"> · {{ sceneEndText(last.sceneEnd) }}</span>
    </p>
    <p v-else-if="last" class="last" aria-live="polite">
      <strong>{{ last.meaning.first }} · {{ last.meaning.second }}</strong>
      <span class="muted"> · {{ last.meaning.table }}, d100 {{ last.meaning.firstRoll }} / {{ last.meaning.secondRoll }}</span>
    </p>
  </div>
</template>

<style scoped>
.mythic {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.chaos {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--ink-muted);
}
.chaos span {
  flex: 1;
}
.step {
  width: 2rem;
  padding: 0.2rem 0;
  font-weight: 700;
}
.chaos-value {
  min-width: 1.5rem;
  text-align: center;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 1.4rem;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
}
.rule {
  margin: 0;
  border: 0;
  border-top: 1px solid var(--rule);
}
.scene,
.question,
.meaning {
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
input,
select {
  width: 100%;
  box-sizing: border-box;
  font: inherit;
}
.ask {
  align-self: flex-start;
  font-weight: 700;
  background: var(--wood-raised);
}
.label {
  color: var(--ink-muted);
}
.end {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.end-buttons {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.meaning p {
  margin: 0;
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
.error-text {
  margin: 0;
  color: var(--danger);
}
.last {
  margin: 0;
  color: var(--ink);
}
.muted {
  color: var(--ink-muted);
}
</style>
