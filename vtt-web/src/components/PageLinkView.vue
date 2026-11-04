<script setup lang="ts">
import { NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'
import { computed } from 'vue'
import { linkText } from '../notebook/links'
import type { PageLinkOptions } from '../notebook/pageLink'

/**
 * Un lien de page dans le texte : le titre actuel de sa cible, ou son texte choisi. Page verrouillée, un clic ou Entrée
 * l'ouvre ; en écriture, un clic le sélectionne (ProseMirror). Une cible absente donne un lien mort, qui n'ouvre rien.
 */
const props = defineProps(nodeViewProps)

const options = computed(() => props.extension.options as PageLinkOptions)
const pageId = computed(() => props.node.attrs.pageId as string)
const label = computed(() => props.node.attrs.label as string | null)
const target = computed(() => options.value.pages().find((p) => p.id === pageId.value))
const text = computed(() => linkText(options.value.pages(), pageId.value, label.value) ?? 'page introuvable')

function onClick(event: MouseEvent) {
  // Le nœud n'est pas éditable : sans cela, le navigateur suivrait l'ancre.
  event.preventDefault()
  if (target.value && !props.editor.isEditable) options.value.onOpen(pageId.value)
}
</script>

<template>
  <NodeViewWrapper as="span" class="page-link-node">
    <a
      v-if="target"
      class="page-link"
      :href="`#page-${pageId}`"
      :title="label === null ? undefined : target.title"
      @click="onClick"
    >{{ text }}</a>
    <a v-else class="page-link dead" aria-disabled="true" title="Cette page n'existe plus" @click="onClick">{{ text }}</a>
  </NodeViewWrapper>
</template>

<style scoped>
.page-link {
  color: var(--ink);
  text-decoration: underline;
  text-decoration-color: var(--brass);
  text-decoration-thickness: 2px;
  text-underline-offset: 0.18em;
  cursor: pointer;
}
.page-link:hover {
  text-decoration-color: var(--ink);
}
.page-link:focus-visible {
  outline: 2px solid var(--control-border);
  outline-offset: 2px;
  border-radius: 2px;
}
.page-link.dead {
  color: var(--ink-muted);
  text-decoration: line-through;
  text-decoration-color: var(--ink-muted);
  text-decoration-thickness: 1px;
  cursor: not-allowed;
}
.page-link-node.ProseMirror-selectednode .page-link {
  background: var(--wood-sunken);
  outline: 1px solid var(--brass);
  border-radius: 2px;
}
[contenteditable='true'] .page-link {
  cursor: default;
}
</style>
