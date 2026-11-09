<script setup lang="ts">
import type { JSONContent } from '@tiptap/core'
import { computed, nextTick, ref, watch } from 'vue'
import type { PageDocument, PageSummary } from '../types'
import PageEditor from './PageEditor.vue'

/**
 * Le résultat d'un module quand aucune page ne peut le recevoir : rendu comme dans une page, en lecture, avec l'état de
 * sa copie au presse-papiers. Échap ou « Fermer » le referment ; un lien de page l'ouvre et ferme le dialogue.
 */
const props = defineProps<{
  /** `null` : fermé. */
  blocks: JSONContent[] | null
  pages: readonly PageSummary[]
  pageHref: (pageId: string) => string
  /** `undefined` pendant la copie, `null` une fois copié, sinon la raison de l'échec. */
  copyError: string | null | undefined
}>()
const emit = defineEmits<{ copy: []; open: [pageId: string]; close: [] }>()

const dialog = ref<HTMLDialogElement | null>(null)
const closeButton = ref<HTMLButtonElement | null>(null)

const doc = computed<PageDocument | null>(() => (props.blocks ? { type: 'doc', content: props.blocks } : null))
/** Un éditeur par résultat : `PageEditor` ne relit pas son contenu. */
const shown = ref(0)

watch(
  () => props.blocks,
  async (blocks) => {
    const el = dialog.value
    if (!el) return
    if (blocks) {
      shown.value++
      if (!el.open) el.showModal()
      await nextTick()
      closeButton.value?.focus()
    } else if (el.open) el.close()
  },
  { flush: 'post' },
)
</script>

<template>
  <!-- L'événement `close` part en différé : arrivé après un nouveau résultat, déjà rouvert, il ne le ferme pas. -->
  <dialog ref="dialog" class="result" aria-labelledby="result-title" @close="dialog?.open || emit('close')">
    <h2 id="result-title" class="title">Résultat</h2>
    <p class="hint">Aucune page ouverte pour l'écrire.</p>
    <hr class="tapered" />
    <div v-if="doc" class="body">
      <PageEditor
        :key="shown"
        :content="doc"
        :editable="false"
        label="Résultat"
        :pages="pages"
        :page-href="pageHref"
        @open="(pageId) => emit('open', pageId)"
      />
    </div>
    <p class="status" role="status">
      <template v-if="copyError === undefined">Copie…</template>
      <template v-else-if="copyError === null">Copié dans le presse-papiers.</template>
      <span v-else class="error-text">Copie automatique impossible ({{ copyError }}) : cliquer Copier.</span>
    </p>
    <div class="actions">
      <button type="button" @click="emit('copy')">Copier</button>
      <button ref="closeButton" type="button" @click="emit('close')">Fermer</button>
    </div>
  </dialog>
</template>

<style scoped>
/* Même panneau de bois que les autres dialogues : un objet posé sur la table. */
.result {
  width: min(32rem, calc(100vw - 2rem));
  padding: 1.1rem 1.25rem 1.25rem;
  border: 1px solid var(--rule);
  border-radius: 4px;
  background: var(--wood);
  color: var(--ink);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
.result::backdrop {
  background: rgba(0, 0, 0, 0.45);
}
.title {
  margin: 0;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 1.35rem;
  line-height: 1.15;
}
.hint {
  margin: 0.25rem 0 0;
  color: var(--ink-muted);
}
.body :deep(.page-text) {
  min-height: 0;
}
.status {
  margin: 0.5rem 0 0;
  color: var(--ink-muted);
}
.error-text {
  color: var(--danger);
}
.actions {
  margin-top: 1.1rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}
</style>
