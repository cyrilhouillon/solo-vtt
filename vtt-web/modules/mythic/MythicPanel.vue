<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { NotebookContext } from '../../src/modules/notebookModule'
import { mythicApi, type FateAnswer, type Odds } from './api'
import { answerBlocks } from './result'

/**
 * L'oracle Mythic dans le Carnet : le facteur de chaos de la campagne, et une question oui/non dont la réponse s'écrit
 * dans la page ouverte.
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
const last = ref<FateAnswer | null>(null)
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
})

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

async function ask() {
  error.value = null
  // Refusé avant le jet : sans page, la réponse n'aurait nulle part où s'écrire.
  if (!props.context.hasOpenPage()) {
    error.value = 'Aucune page ouverte : ouvrir une page pour y écrire la réponse.'
    return
  }
  busy.value = true
  try {
    const answer = await api.ask(question.value.trim(), chosen.value)
    const refusal = props.context.writeToOpenPage(answerBlocks(answer))
    if (refusal) {
      error.value = refusal
      return
    }
    last.value = answer
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
    <p v-if="error" class="error-text" role="alert">{{ error }}</p>
    <p v-else-if="last" class="last" aria-live="polite">
      <strong>{{ last.answerLabel }}</strong>
      <span class="muted"> · d100 {{ last.roll }} · {{ last.odds.label }}, chaos {{ last.chaos }}</span>
      <template v-if="last.randomEvent">
        <br />
        Événement aléatoire : {{ last.randomEvent.focusLabel }}
      </template>
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
