<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, campaignApi } from '../api'
import CombatThumbnail from './CombatThumbnail.vue'
import type { CombatEntry } from '../types'

/**
 * L'entrée d'un combat dans le Carnet : son état et sa carte en petit, telle qu'il l'a laissée. Ouvrir le combat le
 * rejoue s'il est terminé ; en cours, il est d'abord repris s'il n'est pas déjà la partie de la carte en cours.
 */
const props = defineProps<{ campaignId: string; combatId: string }>()
const emit = defineEmits<{
  /** Le combat à ouvrir sur l'écran de combat : rejoué s'il est terminé, en direct sinon. */
  open: [combatId: string, ended: boolean]
  error: [message: string]
}>()

const api = campaignApi(props.campaignId)
const entry = ref<CombatEntry | null>(null)
/** Le combat ne se relit plus (règles changées) : 409. */
const unreadable = ref(false)
const busy = ref(false)

onMounted(async () => {
  try {
    entry.value = await api.combatEntry(props.combatId)
  } catch (e) {
    if (e instanceof ApiError && e.status === 409) unreadable.value = true
    else emit('error', e instanceof Error ? e.message : String(e))
  }
})

async function openCombat() {
  const current = entry.value
  if (!current || busy.value) return
  busy.value = true
  try {
    if (!current.ended && !current.current) await api.resumeCombat(props.combatId)
    emit('open', props.combatId, current.ended)
  } catch (e) {
    emit('error', e instanceof Error ? e.message : String(e))
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="combat-entry" aria-label="Combat">
    <p v-if="unreadable" class="state">Combat illisible : il ne se rejoue plus avec les règles actuelles.</p>
    <template v-else-if="entry">
      <p class="state">{{ entry.ended ? 'Terminé' : 'En cours' }}</p>
      <button type="button" class="map" :disabled="busy" :aria-label="entry.ended ? 'Revoir le combat' : 'Reprendre le combat'" @click="openCombat">
        <CombatThumbnail :scene="entry.scene" />
      </button>
      <button type="button" class="action" :disabled="busy" @click="openCombat">
        {{ entry.ended ? 'Revoir le combat' : 'Reprendre le combat' }}
      </button>
    </template>
  </section>
</template>

<style scoped>
.combat-entry {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.9rem;
  margin-top: 0.5rem;
}
.state {
  margin: 0;
  font-style: italic;
  color: var(--ink-muted);
}
.map {
  display: block;
  width: 100%;
  padding: 0;
  background: none;
  border: none;
  cursor: pointer;
}
.map:focus-visible {
  outline: 2px solid var(--control-border);
  outline-offset: 3px;
  border-radius: 4px;
}
.action {
  padding: 0.45rem 1rem;
  font: inherit;
  color: var(--on-brass);
  background: var(--brass);
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.action:disabled,
.map:disabled {
  cursor: wait;
}
</style>
