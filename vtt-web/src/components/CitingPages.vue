<script setup lang="ts">
import { computed } from 'vue'
import { byTitle } from '../notebook/tree'
import type { PageSummary } from '../types'

/**
 * `pageIds` : les pages qui citent la page ouverte, lues au serveur à son ouverture. Leur titre vient de `pages`, que le
 * Carnet relit à chaque geste : un renommage s'y voit sans relire la liste.
 */
const props = defineProps<{ pageIds: readonly string[]; pages: readonly PageSummary[] }>()
const emit = defineEmits<{ open: [pageId: string] }>()

const citing = computed(() => {
  const ids = new Set(props.pageIds)
  return props.pages.filter((p) => ids.has(p.id)).sort(byTitle)
})
</script>

<template>
  <aside class="citing" aria-labelledby="citing-title">
    <hr class="tapered" />
    <h3 id="citing-title" class="panel-title">Pages qui renvoient ici</h3>
    <ul v-if="citing.length">
      <li v-for="p in citing" :key="p.id">
        <button type="button" class="citing-link" @click="emit('open', p.id)">{{ p.title }}</button>
      </li>
    </ul>
    <p v-else class="none">Aucune page ne renvoie ici.</p>
  </aside>
</template>

<style scoped>
.citing {
  margin-top: 2.5rem;
}
.citing .tapered {
  margin: 0 0 0.9rem;
}
ul {
  margin: 0.6rem 0 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
/* Même allure qu'un lien de page dans le texte. */
.citing-link {
  padding: 0;
  font: inherit;
  font-size: 1.05rem;
  text-align: left;
  color: var(--ink);
  background: none;
  border: none;
  text-decoration: underline;
  text-decoration-color: var(--brass);
  text-decoration-thickness: 2px;
  text-underline-offset: 0.18em;
  cursor: pointer;
}
.citing-link:hover {
  text-decoration-color: var(--ink);
}
.citing-link:focus-visible {
  outline: 2px solid var(--control-border);
  outline-offset: 2px;
  border-radius: 2px;
}
.none {
  margin: 0.6rem 0 0;
  font-style: italic;
  color: var(--ink-muted);
}
</style>
