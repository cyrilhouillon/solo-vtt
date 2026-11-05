<script setup lang="ts">
import { computed, ref } from 'vue'
import AppIcon from './AppIcon.vue'
import type { PageSummary } from '../types'

/**
 * La barre des pages ouvertes, dans l'ordre. Le titre de chaque onglet vient de `pages`, que le Carnet relit à chaque
 * geste : un renommage s'y voit aussitôt.
 */
const props = defineProps<{
  tabIds: readonly string[]
  activeId: string | null
  pages: readonly PageSummary[]
  busy: boolean
}>()
const emit = defineEmits<{ activate: [pageId: string]; close: [pageId: string] }>()

const tabs = computed(() => {
  const titles = new Map(props.pages.map((p) => [p.id, p.title]))
  return props.tabIds.map((id) => ({ id, title: titles.get(id) ?? 'Page introuvable' }))
})

const bar = ref<HTMLElement | null>(null)

/** Vrai quand le focus est dans la barre : fermer un onglet au clavier le rend ensuite à l'onglet actif. */
function hasFocus() {
  return !!bar.value?.contains(document.activeElement)
}

/** Donne le focus à l'onglet actif, sinon au dernier. */
function focusActive() {
  const tabs = bar.value?.querySelectorAll<HTMLButtonElement>('.tab')
  if (!tabs?.length) return
  const index = props.activeId ? props.tabIds.indexOf(props.activeId) : -1
  tabs[index >= 0 ? index : tabs.length - 1]?.focus()
}

defineExpose({ hasFocus, focusActive })
</script>

<template>
  <nav ref="bar" class="page-tabs" aria-label="Pages ouvertes">
    <ul>
      <li v-for="tab in tabs" :key="tab.id" :class="{ active: tab.id === activeId }">
        <button
          type="button"
          class="tab"
          :title="tab.title"
          :aria-current="tab.id === activeId ? 'page' : undefined"
          @click="emit('activate', tab.id)"
        >
          {{ tab.title }}
        </button>
        <button
          type="button"
          class="close"
          :aria-label="`Fermer « ${tab.title} »`"
          :title="`Fermer « ${tab.title} »`"
          :disabled="busy"
          @click="emit('close', tab.id)"
        >
          <AppIcon name="remove" />
        </button>
      </li>
    </ul>
  </nav>
</template>

<style scoped>
/* Les onglets défilent sur leur ligne au-delà de la largeur de la page, sans faire défiler l'écran. */
.page-tabs {
  margin-bottom: 1rem;
  border-bottom: 1px solid var(--rule);
  overflow-x: auto;
  overflow-y: hidden;
}
ul {
  display: flex;
  gap: 0.25rem;
  margin: 0;
  padding: 0;
  list-style: none;
}
li {
  flex: none;
  display: flex;
  align-items: center;
  max-width: 14rem;
  border-radius: 4px 4px 0 0;
  box-shadow: inset 0 -3px 0 transparent;
}
li:hover {
  background: var(--wood-raised);
}
li.active {
  box-shadow: inset 0 -3px 0 var(--ink);
}
.tab {
  min-width: 0;
  padding: 0.4rem 0.3rem 0.35rem 0.8rem;
  border: 0;
  background: transparent;
  font-family: var(--font-title);
  font-weight: 700;
  font-size: 1.05rem;
  letter-spacing: 0.04em;
  color: var(--ink-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
li:hover .tab,
li.active .tab {
  color: var(--ink);
}
.close {
  flex: none;
  display: grid;
  place-items: center;
  width: 1.75rem;
  height: 1.75rem;
  margin-right: 0.25rem;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--ink-muted);
}
.close .icon {
  width: 1rem;
  height: 1rem;
}
.close:hover:not(:disabled) {
  color: var(--ink);
  background: var(--wood-sunken);
}
.tab:focus-visible,
.close:focus-visible {
  outline: 2px solid var(--control-border);
  outline-offset: -2px;
  border-radius: 4px;
}
</style>
