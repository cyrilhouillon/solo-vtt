<script setup lang="ts">
import StarterKit from '@tiptap/starter-kit'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import { watch } from 'vue'
import { api } from '../api'
import { imageRefusal } from '../images'
import { PageImage } from '../notebook/pageImage'
import { PageLink } from '../notebook/pageLink'
import PageLinkBubble from './PageLinkBubble.vue'
import type { PageDocument, PageSummary } from '../types'

/**
 * Le corps d'une page, en texte riche : titres, gras, italique, listes, citations, à la frappe (`# `, `- `, `> `, `**…**`)
 * ou au clavier (Ctrl+B, Ctrl+I) ; Ctrl+Z et Ctrl+Y défont et refont. Un éditeur par page : l'historique ne passe pas de
 * l'une à l'autre. Verrouillée, la page se lit avec la même mise en page ; seul le curseur change. `[[` lie une page du
 * Carnet (`pages`), dont le lien suit le titre. `insertImage` pose une image du disque au curseur ; une image déposée ou
 * collée sur le texte entre de même.
 */
const props = defineProps<{ content: PageDocument | null; editable: boolean; label: string; pages: readonly PageSummary[] }>()
/** Le document après chaque modification du joueur ; `open` : suivre un lien ; `refuse` : une image déposée ou collée refusée. */
const emit = defineEmits<{ change: [content: PageDocument]; open: [pageId: string]; refuse: [reason: string] }>()

const editor = useEditor({
  content: props.content ?? '',
  // Les liens web ne sont pas demandés : seuls les liens vers une page du Carnet.
  extensions: [
    StarterKit.configure({ link: false }),
    // La liste suit le Carnet, relu à chaque geste : un lien affiche le titre actuel de sa cible.
    PageLink.configure({
      pages: () => props.pages,
      locked: () => !props.editable,
      onOpen: (pageId) => emit('open', pageId),
    }),
    PageImage,
  ],
  editable: props.editable,
  autofocus: props.editable ? 'end' : false,
  editorProps: {
    attributes: { class: 'page-text', 'aria-label': props.label },
    // Verrouillée, aucune touche (Ctrl+Z compris) ni aucun collage ne modifie la page. ProseMirror n'appelle déjà aucun
    // de ces gestionnaires sur une vue non éditable : c'est une garde, qui laisse Entrée ouvrir un lien.
    handleKeyDown: (view) => !view.editable,
    handlePaste: (view, event) => {
      if (!view.editable) return true
      const files = filesOf(event.clipboardData)
      if (!files.length) return false
      void insertFiles(files)
      return true
    },
    // ProseMirror empêche le comportement par défaut quand ce gestionnaire rend vrai : le navigateur n'ouvre pas le fichier.
    handleDrop: (view, event) => {
      if (!view.editable) return true
      const files = filesOf(event.dataTransfer)
      if (!files.length) return false
      void insertFiles(files, view.posAtCoords({ left: event.clientX, top: event.clientY })?.pos)
      return true
    },
  },
  onUpdate: ({ editor }) => {
    if (editor.isEditable) emit('change', editor.getJSON() as PageDocument)
  },
})

/**
 * Envoie l'image au serveur, puis l'insère au curseur, en un seul pas d'annulation. Rend la raison d'un refus ou d'un
 * échec, sans rien insérer ; `null` une fois l'image posée.
 */
async function insertImage(file: File, position?: number): Promise<string | null> {
  const refusal = imageRefusal(file)
  if (refusal) return refusal
  let imageId: string
  try {
    imageId = await api.uploadImage(file)
  } catch (e) {
    return `Image non envoyée : ${e instanceof Error ? e.message : String(e)}`
  }
  const current = editor.value
  // Verrouillée ou quittée pendant l'envoi : l'image reste sur le serveur, la page ne change pas.
  if (!current || !current.isEditable) return null
  current.chain().focus().insertPageImage({ imageId, alt: file.name }, position).run()
  return null
}

const filesOf = (data: DataTransfer | null) => Array.from(data?.files ?? [])

/**
 * Les fichiers déposés ou collés, dans l'ordre : le premier au point visé, les suivants au curseur, qui suit l'image
 * posée. Le premier refus est signalé, les autres images entrent quand même.
 */
async function insertFiles(files: File[], position?: number) {
  let refusal: string | null = null
  let at = position
  for (const file of files) {
    const reason = await insertImage(file, at)
    if (reason) refusal ??= reason
    else at = undefined
  }
  if (refusal) emit('refuse', refusal)
}

defineExpose({ insertImage })

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
  <PageLinkBubble v-if="editor" :editor="editor" :pages="pages" @open="(pageId) => emit('open', pageId)" />
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
.page-editor :deep(.page-image) {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 0 0 0.75rem;
  border-radius: 2px;
}
/* En écriture, l'image sélectionnée (clic ou flèches) est cerclée comme un lien sélectionné. */
.page-editor :deep(.page-text[contenteditable='true'] .page-image.ProseMirror-selectednode) {
  outline: 2px solid var(--brass);
  outline-offset: 2px;
}
.page-editor :deep(hr) {
  border: none;
  border-top: 1px solid var(--rule);
  margin: 1rem 0;
}
</style>
