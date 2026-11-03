<script setup lang="ts">
import { computed, ref } from 'vue'
import { describeLoss } from '../replaceWarning'
import { formatLength, type SavedMapView } from '../types'

/**
 * `maps` : les cartes enregistrées, `null` tant que la liste n'est pas lue.
 * `tokenCount` : nombre de créatures de la carte actuelle, `null` s'il n'y a pas de carte.
 * `busy` : un enregistrement ou un chargement est en route.
 */
const props = defineProps<{ maps: SavedMapView[] | null; tokenCount: number | null; busy: boolean }>()
const emit = defineEmits<{ save: [name: string]; load: [name: string]; cancel: [] }>()

/** Comme le serveur (`MapResource.MAX_NAME_LENGTH`). */
const MAX_NAME_LENGTH = 80

const name = ref('')
const trimmed = computed(() => name.value.trim())
const taken = computed(() => props.maps?.some((m) => m.name === trimmed.value) ?? false)
/** La carte dont le chargement attend confirmation : il remplacerait la carte actuelle. */
const confirming = ref<string | null>(null)

function save() {
  if (!trimmed.value || props.busy) return
  emit('save', trimmed.value)
}

function askLoad(map: string) {
  if (props.tokenCount === null) emit('load', map)
  else confirming.value = map
}

function load(map: string) {
  confirming.value = null
  emit('load', map)
}

function describe(map: SavedMapView): string {
  const creatures = map.creatureCount > 1 ? `${map.creatureCount} créatures` : `${map.creatureCount} créature`
  return `${formatLength(map.width, map.unitSymbol)} × ${formatLength(map.height, map.unitSymbol)} · ${creatures}`
}

/** Échap annule d'abord une confirmation, puis ferme le tiroir. */
function escape() {
  if (confirming.value) confirming.value = null
  else emit('cancel')
}
</script>

<template>
  <section class="saved-maps" aria-labelledby="saved-maps-title" @keydown.esc.stop="escape">
    <h2 id="saved-maps-title" class="panel-title">Cartes enregistrées</h2>
    <hr class="tapered" />
    <form class="save" @submit.prevent="save">
      <label>
        Nom
        <input v-model="name" type="text" :maxlength="MAX_NAME_LENGTH" required :disabled="tokenCount === null" />
      </label>
      <p v-if="tokenCount === null" class="hint">Créer ou charger une carte pour l'enregistrer.</p>
      <p v-else-if="taken" class="warning" role="note">Remplace la carte enregistrée « {{ trimmed }} ».</p>
      <div class="actions">
        <button type="submit" :disabled="tokenCount === null || !trimmed || busy">Enregistrer</button>
      </div>
    </form>
    <hr class="tapered" />
    <p v-if="maps === null" class="hint">Lecture des cartes…</p>
    <p v-else-if="!maps.length" class="hint">Aucune carte enregistrée.</p>
    <ul v-else class="list">
      <li v-for="map in maps" :key="map.name">
        <strong class="name">{{ map.name }}</strong>
        <span class="info">{{ describe(map) }}</span>
        <template v-if="confirming === map.name && tokenCount !== null">
          <p class="warning" role="note">Remplace la carte actuelle{{ describeLoss(tokenCount) }}.</p>
          <div class="actions">
            <button type="button" class="danger" :disabled="busy" @click="load(map.name)">Charger</button>
            <button type="button" @click="confirming = null">Annuler</button>
          </div>
        </template>
        <div v-else class="actions">
          <button type="button" :disabled="busy" :aria-label="`Charger ${map.name}`" @click="askLoad(map.name)">Charger</button>
        </div>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.saved-maps {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}
.saved-maps .tapered {
  margin: -0.5rem 0 0;
}
.save {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
label {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  color: var(--ink-muted);
}
input[type='text'] {
  width: 100%;
  box-sizing: border-box;
}
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}
.list li {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.3rem;
}
.name {
  color: var(--ink);
  overflow-wrap: anywhere;
}
.info,
.hint {
  margin: 0;
  color: var(--ink-muted);
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
</style>
