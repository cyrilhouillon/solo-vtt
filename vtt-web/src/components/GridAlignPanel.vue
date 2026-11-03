<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { alignedMap, type Alignment } from '../map/alignment'
import { CELL } from '../map/grid'
import type { DistanceUnit } from '../types'

/**
 * Le calage de la grille de la carte actuelle, sous le formulaire de nouvelle carte.
 * `calibration` : le calage en cours d'aperçu (en pixels de l'image) et la taille naturelle de l'image, `null` hors calage.
 * `refusal` : pourquoi le calage n'est pas possible en ce moment (combat), `null` sinon.
 * `busy` : le calage est envoyé et pas encore revenu.
 */
const props = defineProps<{
  calibration: { alignment: Alignment; image: { width: number; height: number } } | null
  unit: DistanceUnit
  refusal: string | null
  busy: boolean
}>()
const emit = defineEmits<{ start: []; change: [alignment: Alignment]; apply: []; cancel: [] }>()

const firstField = ref<HTMLInputElement | null>(null)
const startButton = ref<HTMLButtonElement | null>(null)

/** Au centième près pour l'affichage. */
const rounded = (value: number) => Math.round(value * 100) / 100
const decimal = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 })

/** Les valeurs affichées ; le champ en cours de saisie n'est jamais réécrit (« 32, » n'est pas encore un nombre). */
const fields = reactive<Record<keyof Alignment, number | ''>>({ offsetX: 0, offsetY: 0, cellPixels: 0 })
const editing = ref<keyof Alignment | null>(null)
watch(
  () => props.calibration?.alignment,
  (a) => {
    if (!a) return
    for (const key of Object.keys(fields) as (keyof Alignment)[]) if (key !== editing.value) fields[key] = rounded(a[key])
  },
  { immediate: true },
)

// Le calage ouvert donne le focus au premier champ ; refermé, il le rend au bouton.
watch(
  () => props.calibration !== null,
  (open, before) => {
    if (open === before) return
    editing.value = null
    nextTick(() => (open ? firstField.value : startButton.value)?.focus())
  },
)

// Après la mise à jour de v-model, qui écoute le même événement.
function set(field: keyof Alignment) {
  editing.value = field
  nextTick(() => {
    const value = fields[field]
    const calibration = props.calibration
    if (!calibration || typeof value !== 'number' || !Number.isFinite(value)) return
    if (field === 'cellPixels' && !(value > 0)) return
    emit('change', { ...calibration.alignment, [field]: value })
  })
}

const result = computed(() => {
  const c = props.calibration
  if (!c) return null
  const map = alignedMap(c.image.width, c.image.height, props.unit, c.alignment)
  return { ...map, cellsAcross: map.width / CELL[props.unit], cellsDown: map.height / CELL[props.unit] }
})
const unitSymbol = computed(() => (props.unit === 'METRE' ? 'm' : 'ft'))
</script>

<template>
  <section class="grid-align" aria-labelledby="grid-align-title">
    <hr class="tapered" />
    <h2 id="grid-align-title" class="panel-title">Caler la grille</h2>
    <template v-if="!calibration">
      <p class="hint">Faire tomber la grille de la carte actuelle sur celle de son image ; les créatures restent en place.</p>
      <p v-if="refusal" class="hint">{{ refusal }}.</p>
      <div class="actions">
        <button ref="startButton" type="button" :disabled="refusal !== null" @click="emit('start')">Caler la grille</button>
      </div>
    </template>
    <form v-else class="fields" @submit.prevent="emit('apply')" @keydown.esc.stop="emit('cancel')">
      <p class="hint">Glisser la carte, ou régler au pixel de l'image ; flèches dans les champs.</p>
      <label>
        Décalage horizontal
        <span class="field">
          <input ref="firstField" v-model.number="fields.offsetX" @input="set('offsetX')" @blur="editing = null" type="number" step="1" />
          px
        </span>
      </label>
      <label>
        Décalage vertical
        <span class="field">
          <input v-model.number="fields.offsetY" @input="set('offsetY')" @blur="editing = null" type="number" step="1" />
          px
        </span>
      </label>
      <label>
        Taille de case
        <span class="field">
          <input v-model.number="fields.cellPixels" @input="set('cellPixels')" @blur="editing = null" type="number" min="1" step="0.1" required />
          px
        </span>
      </label>
      <p v-if="result" class="result" aria-live="polite">
        {{ decimal.format(result.cellsAcross) }} × {{ decimal.format(result.cellsDown) }} cases ·
        {{ decimal.format(result.width) }} × {{ decimal.format(result.height) }} {{ unitSymbol }}
      </p>
      <div class="actions">
        <button type="submit" class="primary" :disabled="busy">Valider</button>
        <button type="button" @click="emit('cancel')">Annuler</button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.grid-align,
.fields {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}
.grid-align {
  margin-top: 1.4rem;
}
.grid-align .tapered {
  margin: 0;
}
.hint {
  margin: 0;
  color: var(--ink-muted);
}
label {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  color: var(--ink-muted);
}
.field {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--ink);
}
input[type='number'] {
  width: 6rem;
}
.result {
  margin: 0;
  color: var(--ink);
}
.actions {
  display: flex;
  gap: 0.5rem;
}
.primary {
  background: var(--wood-raised);
  border-color: var(--ink-muted);
  font-weight: 700;
}
</style>
