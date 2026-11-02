<script setup lang="ts">
import { computed } from 'vue'
import type { Combat } from '../types'

/**
 * Le combat dans la barre : hors combat, « Lancer le combat » ; en combat, le round, la créature qui joue,
 * et « Fin du tour ». `blocked` : une demande est en cours, un second clic n'en lance pas une autre.
 */
const props = defineProps<{ combat: Combat | null; creatureCount: number; blocked: boolean }>()
const emit = defineEmits<{ start: []; endTurn: [] }>()

/** Pourquoi le combat ne peut pas commencer, ou `null`. */
const startRefusal = computed(() => {
  if (props.creatureCount === 0) return "Poser au moins une créature avant de lancer le combat."
  return null
})

const activeName = computed(() => {
  const c = props.combat
  if (!c?.activeId) return null
  for (const group of c.groups) {
    const member = group.members.find((m) => m.creatureId === c.activeId)
    if (member) return member.name
  }
  return c.activeId
})
</script>

<template>
  <div class="combat">
    <template v-if="combat">
      <p class="turn" aria-live="polite">
        Round {{ combat.round }} ·
        <strong>{{ activeName ? `Tour de ${activeName}` : 'Qui joue ?' }}</strong>
      </p>
      <button
        type="button"
        :disabled="blocked || !combat.activeId"
        aria-keyshortcuts="F"
        title="Touche F"
        @click="emit('endTurn')"
      >
        Fin du tour
      </button>
    </template>
    <button
      v-else
      type="button"
      :disabled="blocked || startRefusal !== null"
      :title="startRefusal ?? undefined"
      @click="emit('start')"
    >
      Lancer le combat
    </button>
  </div>
</template>

<style scoped>
.combat {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.turn {
  margin: 0;
  color: var(--ink-muted);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.turn strong {
  color: var(--ink);
}
</style>
