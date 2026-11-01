<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { CELL } from '../map/grid'
import type { DistanceUnit } from '../types'

/** `tokenCount` : nombre de créatures de la carte actuelle, `null` s'il n'y a pas encore de carte. */
const props = defineProps<{ tokenCount: number | null }>()
const emit = defineEmits<{ create: [width: number, height: number, unit: DistanceUnit] }>()

const width = ref(30)
const height = ref(20)
const unit = ref<DistanceUnit>('METRE')
const open = ref(false)
const firstField = ref<HTMLInputElement | null>(null)
const openButton = ref<HTMLButtonElement | null>(null)
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

async function show() {
  open.value = true
  await nextTick()
  firstField.value?.focus()
}

/** Referme le formulaire et rend le focus au bouton qui l'ouvre. */
async function close() {
  open.value = false
  await nextTick()
  openButton.value?.focus()
}

function submit() {
  emit('create', width.value, height.value, unit.value)
  close()
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
</script>

<template>
  <button v-if="tokenCount !== null && !open" ref="openButton" type="button" @click="show">Nouvelle carte…</button>
  <form v-else ref="form" class="new-map" @submit.prevent="submit" @keydown.esc.stop="close">
    <span class="title">Nouvelle carte</span>
    <label>
      Largeur
      <input ref="firstField" v-model.number="width" @input="remember" type="number" min="1" step="any" required />
    </label>
    <label>
      Hauteur
      <input v-model.number="height" @input="remember" type="number" min="1" step="any" required />
    </label>
    <fieldset class="unit">
      <legend class="sr-only">Unité</legend>
      <label><input v-model="unit" type="radio" value="METRE" /> mètres (m)</label>
      <label><input v-model="unit" type="radio" value="FOOT" /> pieds (ft)</label>
    </fieldset>
    <template v-if="tokenCount !== null">
      <p class="warning" role="note">Remplace la carte actuelle{{ describeLoss(tokenCount) }}.</p>
      <button v-if="tokenCount > 0" type="button" class="danger" @click="replace">Remplacer la carte</button>
      <button v-else type="submit" class="danger">Remplacer la carte</button>
      <button type="button" @click="close">Annuler</button>
    </template>
    <button v-else type="submit">Créer la carte</button>
  </form>
</template>

<style scoped>
.new-map {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem 1.25rem;
}
.title {
  font-weight: 600;
}
label {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  color: var(--text-muted);
}
input[type='number'] {
  width: 5.5rem;
}
.unit {
  display: flex;
  gap: 0.75rem;
  border: 0;
  padding: 0;
  margin: 0;
}
.warning {
  margin: 0;
  color: var(--danger);
}
.danger {
  border-color: var(--danger);
}
</style>
