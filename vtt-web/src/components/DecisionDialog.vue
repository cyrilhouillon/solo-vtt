<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import type { Decision } from '../types'

/**
 * La question que pose le moteur au milieu d'une action (réaction Bouclier…). Modal : la page derrière
 * est inerte tant qu'il n'y a pas de réponse. Rien ne le ferme hormis une option.
 */
const props = defineProps<{ decision: Decision | null; answering: boolean }>()
const emit = defineEmits<{ answer: [option: string] }>()

const dialog = ref<HTMLDialogElement | null>(null)
const firstOption = ref<HTMLButtonElement[]>([])

async function sync() {
  const el = dialog.value
  if (!el) return
  if (props.decision && !el.open) {
    el.showModal()
    await nextTick()
    firstOption.value[0]?.focus()
  } else if (!props.decision && el.open) {
    el.close()
  }
}

/**
 * Échap annulé dès `keydown` n'émet pas de demande de fermeture ; sans cela, Chromium (close watchers)
 * ferme au second Échap sans passer par `cancel`. Filet : tant que la décision attend, il se rouvre.
 */
function onClose() {
  if (props.decision) void sync()
}

watch(() => props.decision?.id, sync)
onMounted(sync)
</script>

<template>
  <dialog
    ref="dialog"
    class="decision"
    aria-labelledby="decision-title"
    aria-describedby="decision-prompt"
    @keydown.esc.prevent
    @cancel.prevent
    @close="onClose"
  >
    <template v-if="decision">
      <h2 id="decision-title" class="title">Réaction · {{ decision.chooserName }}</h2>
      <p id="decision-prompt" class="prompt">{{ decision.prompt }}</p>
      <div class="options">
        <button
          v-for="option in decision.options"
          :key="option"
          ref="firstOption"
          type="button"
          :disabled="answering"
          @click="emit('answer', option)"
        >
          {{ option }}
        </button>
      </div>
    </template>
  </dialog>
</template>

<style scoped>
/* Un panneau de bois posé sur la table : seule ombre permise, celle du bandeau flottant. */
.decision {
  max-width: min(32rem, calc(100vw - 2rem));
  padding: 1.1rem 1.25rem 1.25rem;
  border: 1px solid var(--rule);
  border-radius: 4px;
  background: var(--wood);
  color: var(--ink);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
.decision::backdrop {
  background: rgba(0, 0, 0, 0.55);
}
.title {
  margin: 0;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 1.35rem;
  line-height: 1.15;
  color: var(--rubric);
}
.prompt {
  margin: 0.6rem 0 1.1rem;
  line-height: 1.4;
}
.options {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}
.options button:focus-visible {
  outline: 2px solid var(--brass);
  outline-offset: 2px;
}
</style>
