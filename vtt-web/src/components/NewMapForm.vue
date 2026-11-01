<script setup lang="ts">
import { ref } from 'vue'
import type { DistanceUnit } from '../types'

const emit = defineEmits<{ create: [width: number, height: number, unit: DistanceUnit] }>()

const width = ref(30)
const height = ref(20)
const unit = ref<DistanceUnit>('METRE')

function submit() {
  emit('create', width.value, height.value, unit.value)
}
</script>

<template>
  <form class="new-map" @submit.prevent="submit">
    <span class="title">Nouvelle carte</span>
    <label>
      Largeur
      <input v-model.number="width" type="number" min="1" step="any" required />
    </label>
    <label>
      Hauteur
      <input v-model.number="height" type="number" min="1" step="any" required />
    </label>
    <fieldset class="unit">
      <legend class="sr-only">Unité</legend>
      <label><input v-model="unit" type="radio" value="METRE" /> mètres</label>
      <label><input v-model="unit" type="radio" value="FOOT" /> pieds</label>
    </fieldset>
    <button type="submit">Créer la carte</button>
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
</style>
