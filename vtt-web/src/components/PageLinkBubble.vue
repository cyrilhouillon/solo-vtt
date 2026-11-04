<script setup lang="ts">
import type { Editor } from '@tiptap/core'
import { NodeSelection } from '@tiptap/pm/state'
import { BubbleMenu } from '@tiptap/vue-3/menus'
import { computed, nextTick, ref, watch } from 'vue'
import { MAX_TITLE_LENGTH } from '../notebook/context'
import { PAGE_LINK } from '../notebook/pageLink'
import type { PageSummary } from '../types'

/**
 * La bulle d'un lien sélectionné, page déverrouillée : ouvrir sa cible, ou changer le texte qu'il affiche (vide : le titre
 * de la cible). Entrée ou sortie du champ pose le texte en un seul pas d'annulation ; Échap rend l'ancien.
 */
const props = defineProps<{ editor: Editor; pages: readonly PageSummary[] }>()
const emit = defineEmits<{ open: [pageId: string] }>()

/** Le lien sélectionné, `null` sinon ; l'état de l'éditeur est réactif, la bulle suit la sélection. */
const link = computed(() => {
  const selection = props.editor.state.selection
  if (!(selection instanceof NodeSelection) || selection.node.type.name !== PAGE_LINK) return null
  return { pageId: selection.node.attrs.pageId as string, label: selection.node.attrs.label as string | null }
})
const target = computed(() => (link.value ? props.pages.find((p) => p.id === link.value?.pageId) : undefined))

const draft = ref('')
const bubble = ref<HTMLElement | null>(null)
watch(
  link,
  async (current) => {
    draft.value = current?.label ?? ''
    await nextTick()
    // Tiptap rend le cadre de la bulle focusable (`tabindex=0`) : Tab s'y arrêterait pour rien avant « Ouvrir ».
    if (bubble.value?.parentElement) bubble.value.parentElement.tabIndex = -1
  },
  { immediate: true },
)

function shouldShow({ editor }: { editor: Editor }): boolean {
  const selection = editor.state.selection
  return editor.isEditable && selection instanceof NodeSelection && selection.node.type.name === PAGE_LINK
}

/** Pose le texte saisi s'il a changé ; blanc : le lien reprend le titre de sa cible. */
function apply() {
  if (!link.value) return
  const label = draft.value.trim() || null
  if (label === link.value.label) return
  props.editor.chain().updateAttributes(PAGE_LINK, { label }).run()
}

function submit() {
  apply()
  props.editor.commands.focus()
}

function cancel() {
  draft.value = link.value?.label ?? ''
  props.editor.commands.focus()
}

function open() {
  if (target.value) emit('open', target.value.id)
}
</script>

<template>
  <BubbleMenu :editor="editor" :should-show="shouldShow" :options="{ placement: 'bottom-start', flip: true }">
    <div v-if="link" ref="bubble" class="link-bubble">
      <button v-if="target" type="button" @click="open">Ouvrir</button>
      <span v-else class="dead">Page introuvable</span>
      <label>
        Texte affiché
        <input
          v-model="draft"
          type="text"
          :maxlength="MAX_TITLE_LENGTH"
          :placeholder="target?.title ?? ''"
          @keydown.enter.prevent="submit"
          @keydown.esc.prevent="cancel"
          @blur="apply"
        />
      </label>
    </div>
  </BubbleMenu>
</template>

<style scoped>
.link-bubble {
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.4rem 0.5rem;
  background: var(--wood-raised);
  border: 1px solid var(--rule);
  border-radius: 4px;
  font-size: 0.95rem;
}
label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--ink-muted);
  white-space: nowrap;
}
input {
  width: 12rem;
}
.dead {
  color: var(--ink-muted);
  font-style: italic;
  white-space: nowrap;
}
</style>
