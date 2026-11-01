<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { CELL } from '../map/grid'
import type { DistanceUnit } from '../types'

/** `tokenCount` : nombre de créatures de la carte actuelle, `null` s'il n'y a pas encore de carte. */
const props = defineProps<{ tokenCount: number | null }>()
const emit = defineEmits<{ create: [width: number, height: number, unit: DistanceUnit]; cancel: [] }>()

const width = ref(30)
const height = ref(20)
const unit = ref<DistanceUnit>('METRE')
const firstField = ref<HTMLInputElement | null>(null)
const form = ref<HTMLFormElement | null>(null)

/** Dernières dimensions saisies, et leur unité : la référence des conversions. */
let typed = { unit: unit.value, width: width.value, height: height.value }
// Après la mise à jour de v-model, qui écoute le même événement.
function remember() {
  nextTick(() => (typed = { unit: unit.value, width: width.value, height: height.value }))
}

// Changer d'unité garde le même nombre de cases (30 × 20 m donne 100 × 65 ft) ;
// revenir à l'unité saisie rend les valeurs saisies, sans arrondi cumulé.
watch(unit, (to) => {
  const source = typed
  const convert = (value: number) => Math.max(1, Math.round(value / CELL[source.unit])) * CELL[to]
  const [w, h] = to === source.unit ? [source.width, source.height] : [convert(source.width), convert(source.height)]
  width.value = w
  height.value = h
})

// Ouvert depuis le rail sur une carte existante : le focus va au premier champ.
onMounted(() => {
  if (props.tokenCount !== null) firstField.value?.focus()
})

function submit() {
  emit('create', width.value, height.value, unit.value)
}

/**
 * Remplacer une carte peuplée demande un clic explicite : ce bouton n'est pas de type submit,
 * donc Entrée dans un champ ne déclenche rien.
 */
function replace() {
  if (form.value?.reportValidity()) submit()
}

function describeLoss(count: number): string {
  if (count === 0) return ''
  return count === 1 ? ' et sa créature' : ` et ses ${count} créatures`
}

const unitSymbol = (u: DistanceUnit) => (u === 'METRE' ? 'm' : 'ft')
</script>

<template>
  <form ref="form" class="new-map" aria-labelledby="new-map-title" @submit.prevent="submit" @keydown.esc.stop="emit('cancel')">
    <h2 id="new-map-title" class="panel-title">Nouvelle carte</h2>
    <hr class="tapered" />
    <fieldset class="unit">
      <legend>Unité</legend>
      <label><input v-model="unit" type="radio" value="METRE" /> mètres</label>
      <label><input v-model="unit" type="radio" value="FOOT" /> pieds</label>
    </fieldset>
    <div class="size">
      <label>
        Largeur
        <span class="field">
          <input ref="firstField" v-model.number="width" @input="remember" type="number" min="1" step="any" required />
          {{ unitSymbol(unit) }}
        </span>
      </label>
      <label>
        Hauteur
        <span class="field">
          <input v-model.number="height" @input="remember" type="number" min="1" step="any" required />
          {{ unitSymbol(unit) }}
        </span>
      </label>
    </div>
    <template v-if="tokenCount !== null">
      <p class="warning" role="note">Remplace la carte actuelle{{ describeLoss(tokenCount) }}.</p>
      <div class="actions">
        <button v-if="tokenCount > 0" type="button" class="danger" @click="replace">Remplacer la carte</button>
        <button v-else type="submit" class="danger">Remplacer la carte</button>
        <button type="button" @click="emit('cancel')">Annuler</button>
      </div>
    </template>
    <div v-else class="actions">
      <button type="submit" class="primary">Créer la carte</button>
    </div>
  </form>
</template>

<style scoped>
.new-map {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}
.new-map .tapered {
  margin: -0.5rem 0 0;
}
fieldset {
  border: 0;
  padding: 0;
  margin: 0;
  display: flex;
  gap: 1rem;
}
legend {
  float: left;
  margin-right: 0.25rem;
  font-family: var(--font-title);
  color: var(--ink-muted);
}
.size {
  display: flex;
  gap: 1rem;
}
label {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  color: var(--ink-muted);
}
.unit label {
  flex-direction: row;
  align-items: center;
  gap: 0.35rem;
  color: var(--ink);
}
.field {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: var(--ink);
}
input[type='number'] {
  width: 5rem;
}
.warning {
  margin: 0;
  color: var(--danger);
}
.actions {
  display: flex;
  gap: 0.5rem;
}
.danger {
  border-color: var(--danger);
  color: var(--danger);
}
.primary {
  background: var(--wood-raised);
  border-color: var(--ink-muted);
  font-weight: 700;
}
</style>
