<script setup lang="ts">
import StarterKit from '@tiptap/starter-kit'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import { watch } from 'vue'
import type { PageDocument } from '../types'

/**
 * Le corps d'une page, en texte riche : titres, gras, italique, listes, citations, à la frappe (`# `, `- `, `> `, `**…**`)
 * ou au clavier (Ctrl+B, Ctrl+I) ; Ctrl+Z et Ctrl+Y défont et refont. Un éditeur par page : l'historique ne passe pas de
 * l'une à l'autre. Verrouillée, la page se lit avec la même mise en page ; seul le curseur change.
 */
const props = defineProps<{ content: PageDocument | null; editable: boolean; label: string }>()
/** Le document après chaque modification du joueur. */
const emit = defineEmits<{ change: [content: PageDocument] }>()

const editor = useEditor({
  content: props.content ?? '',
  // Pas de liens dans cette story : les liens vers une page viendront avec leur propre extension.
  extensions: [StarterKit.configure({ link: false })],
  editable: props.editable,
  autofocus: props.editable ? 'end' : false,
  editorProps: {
    attributes: { class: 'page-text', 'aria-label': props.label },
    // Verrouillée, aucune touche (Ctrl+Z compris) ni aucun collage ne modifie la page.
    handleKeyDown: (view) => !view.editable,
    handlePaste: (view) => !view.editable,
    handleDrop: (view) => !view.editable,
  },
  onUpdate: ({ editor }) => {
    if (editor.isEditable) emit('change', editor.getJSON() as PageDocument)
  },
})

watch(
  () => props.editable,
  (editable) => {
    // Sans émettre de mise à jour : déverrouiller n'est pas une modification à enregistrer.
    editor.value?.setEditable(editable, false)
    if (editable) editor.value?.commands.focus('end')
  },
)
</script>

<template>
  <EditorContent :editor="editor" class="page-editor" />
</template>

<style scoped>
.page-editor :deep(.page-text) {
  min-height: 12rem;
  outline: none;
  color: var(--ink);
  font-size: 1.1rem;
  line-height: 1.6;
  overflow-wrap: anywhere;
  cursor: text;
}
.page-editor :deep(.page-text[contenteditable='false']) {
  cursor: default;
}
.page-editor :deep(.page-text:focus-visible) {
  outline: 2px solid var(--control-border);
  outline-offset: 0.5rem;
  border-radius: 2px;
}
.page-editor :deep(.page-text > :first-child) {
  margin-top: 0;
}
.page-editor :deep(p) {
  margin: 0 0 0.75rem;
}
.page-editor :deep(h1),
.page-editor :deep(h2),
.page-editor :deep(h3),
.page-editor :deep(h4),
.page-editor :deep(h5),
.page-editor :deep(h6) {
  margin: 1.4rem 0 0.6rem;
  font-family: var(--font-title);
  font-weight: 700;
  line-height: 1.25;
  color: var(--ink);
}
.page-editor :deep(h1) {
  font-size: 1.6rem;
}
.page-editor :deep(h2) {
  font-size: 1.35rem;
}
.page-editor :deep(h3),
.page-editor :deep(h4),
.page-editor :deep(h5),
.page-editor :deep(h6) {
  font-size: 1.15rem;
}
.page-editor :deep(ul),
.page-editor :deep(ol) {
  margin: 0 0 0.75rem;
  padding-left: 1.5rem;
}
.page-editor :deep(li > p) {
  margin: 0 0 0.25rem;
}
.page-editor :deep(blockquote) {
  margin: 0 0 0.75rem;
  padding: 0.1rem 0 0.1rem 1rem;
  border-left: 3px solid var(--rule);
  color: var(--ink-muted);
  font-style: italic;
}
.page-editor :deep(strong) {
  font-weight: 700;
}
.page-editor :deep(code) {
  padding: 0 0.25rem;
  background: var(--wood-sunken);
  border-radius: 2px;
}
.page-editor :deep(pre) {
  margin: 0 0 0.75rem;
  padding: 0.6rem 0.8rem;
  background: var(--wood-sunken);
  border-radius: 4px;
  overflow-x: auto;
}
.page-editor :deep(hr) {
  border: none;
  border-top: 1px solid var(--rule);
  margin: 1rem 0;
}
</style>
