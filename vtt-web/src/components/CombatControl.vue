<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { Combat } from '../types'

/**
 * Le combat dans la barre : hors combat, « Lancer le combat » ; en combat, le round, la créature qui joue,
 * « Fin du tour » et « Terminer le combat ». `blocked` : une demande est en cours, un second clic n'en lance pas une autre.
 */
const props = defineProps<{ combat: Combat | null; creatureCount: number; blocked: boolean }>()
const emit = defineEmits<{ start: []; endTurn: []; endCombat: [] }>()

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

/**
 * Terminer le combat ne se reprend pas (le relancer retire l'initiative) : il se fait en deux temps,
 * comme le retrait d'une créature en combat. La confirmation se ferme dès que le tour change ou que le combat finit.
 */
const confirming = ref(false)
const endButton = ref<HTMLButtonElement | null>(null)
const confirmButton = ref<HTMLButtonElement | null>(null)

watch(
  () => (props.combat ? `${props.combat.round}:${props.combat.activeId ?? ''}` : null),
  () => {
    confirming.value = false
  },
)

function requestEnd() {
  confirming.value = true
  void nextTick(() => confirmButton.value?.focus())
}

/** Après « Confirmer », le focus attend la réponse : « Lancer le combat » si le combat est fini, sinon le bouton de fin. */
const startButton = ref<HTMLButtonElement | null>(null)
let focusAfterEnd = false

function confirmEnd() {
  confirming.value = false
  focusAfterEnd = true
  emit('endCombat')
}

watch(
  () => props.blocked,
  (blocked) => {
    if (blocked || !focusAfterEnd) return
    focusAfterEnd = false
    void nextTick(() => (startButton.value ?? endButton.value)?.focus())
  },
)

/** Annule une confirmation ouverte ; rend vrai s'il y en avait une (Échap s'arrête là). */
function cancelEnd(): boolean {
  if (!confirming.value) return false
  confirming.value = false
  void nextTick(() => endButton.value?.focus())
  return true
}

defineExpose({ cancelEnd })
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
      <span v-if="confirming" role="group" class="confirm" aria-label="Terminer le combat ?">
        <span class="ask" aria-hidden="true">Terminer le combat ?</span>
        <button ref="confirmButton" type="button" class="danger" :disabled="blocked" @click="confirmEnd">Confirmer</button>
        <button type="button" @click="cancelEnd">Annuler</button>
      </span>
      <button
        v-else
        ref="endButton"
        type="button"
        class="ghost"
        :disabled="blocked"
        title="Terminer le combat (à confirmer)"
        @click="requestEnd"
      >
        Terminer le combat
      </button>
    </template>
    <button
      v-else
      ref="startButton"
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
.ghost {
  border-color: transparent;
  background: transparent;
  color: var(--ink-muted);
}
.ghost:hover:not(:disabled) {
  color: var(--danger);
  border-color: var(--danger);
  background: transparent;
}
.confirm {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  white-space: nowrap;
}
.ask {
  font-weight: 700;
  color: var(--danger);
}
.confirm .danger {
  border-color: var(--danger);
  color: var(--danger);
}
</style>
