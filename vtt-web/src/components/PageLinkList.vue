<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { PageSummary } from '../types'

/**
 * Les pages proposées après `[[`, sous le curseur : flèches pour choisir, Entrée ou Tab pour insérer, Échap pour fermer
 * (le plugin de suggestion le traite). Le focus reste dans le texte.
 */
const props = defineProps<{ items: PageSummary[]; pages: readonly PageSummary[]; command: (page: PageSummary) => void }>()

const active = ref(0)
watch(
  () => props.items,
  () => (active.value = 0),
)

const titles = computed(() => new Map(props.pages.map((p) => [p.id, p.title])))

function parentTitle(page: PageSummary): string | undefined {
  return page.parentId === null ? undefined : titles.value.get(page.parentId)
}

function choose(index: number) {
  const page = props.items[index]
  if (page) props.command(page)
}

/** Appelé par le plugin à chaque touche tant que la liste est ouverte ; vrai quand la touche est prise. */
function onKeyDown(event: KeyboardEvent): boolean {
  const count = props.items.length
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    if (count) active.value = (active.value + (event.key === 'ArrowDown' ? 1 : count - 1)) % count
    return true
  }
  if (event.key === 'Enter' || event.key === 'Tab') {
    // Sans page à proposer, Entrée garde son effet ordinaire dans le texte.
    if (!count) return event.key === 'Tab'
    choose(active.value)
    return true
  }
  return false
}

defineExpose({ onKeyDown })
</script>

<template>
  <div class="page-links">
    <ul v-if="items.length" role="listbox" aria-label="Pages à lier">
      <li
        v-for="(page, index) in items"
        :key="page.id"
        role="option"
        :aria-selected="index === active"
        :class="{ active: index === active }"
        @mousedown.prevent="choose(index)"
        @mousemove="active = index"
      >
        <span class="title">{{ page.title }}</span>
        <span v-if="parentTitle(page)" class="parent">{{ parentTitle(page) }}</span>
      </li>
    </ul>
    <p v-else class="none">Aucune page</p>
  </div>
</template>

<style scoped>
.page-links {
  z-index: 5;
  min-width: 12rem;
  max-width: 22rem;
  padding: 0.3rem;
  background: var(--wood-raised);
  border: 1px solid var(--rule);
  border-radius: 4px;
  font-family: var(--font-body);
  color: var(--ink);
}
ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}
li {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.3rem 0.5rem;
  border: 1px solid transparent;
  border-radius: 4px;
  cursor: pointer;
}
li.active {
  background: var(--wood-sunken);
  border-color: var(--brass);
}
.title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.parent,
.none {
  color: var(--ink-muted);
  font-size: 0.9em;
  white-space: nowrap;
}
.none {
  margin: 0;
  padding: 0.3rem 0.5rem;
  font-style: italic;
}
</style>
